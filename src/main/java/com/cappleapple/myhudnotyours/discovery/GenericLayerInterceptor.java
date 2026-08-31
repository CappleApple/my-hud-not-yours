package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.bar.BarSourceRegistry;
import com.cappleapple.myhudnotyours.bar.CustomBarRenderer;
import com.cappleapple.myhudnotyours.bar.NumericBarSnapshot;
import com.cappleapple.myhudnotyours.bar.NumericBarSource;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.editor.HudEditorScreen;
import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import com.cappleapple.myhudnotyours.model.HudElementRelations;
import com.cappleapple.myhudnotyours.model.RenderMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Applies one scoped transform/cancellation per NeoForge named GUI layer. */
public final class GenericLayerInterceptor {
    private static final HudElementRegistry REGISTRY = HudElementRegistry.get();
    private static int order;
    private static ActiveLayer active;

    private GenericLayerInterceptor() {
    }

    public static void onGuiPre(RenderGuiEvent.Pre event) {
        order = 0;
        active = null;
    }

    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        GuiGraphics graphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ResourceLocation layerId = event.getName();
        // Camera overlays are screen-space render effects (vignette, portal,
        // spyglass, helmet overlays), not independently placeable HUD widgets.
        // Return before discovery/tracking so their renderer is never modified.
        if (layerId.equals(VanillaGuiLayers.CAMERA_OVERLAYS)) return;
        HudElementDefinition definition = REGISTRY.discover(layerId, order++, graphics.guiWidth(), graphics.guiHeight());
        HudElementLayout layout = REGISTRY.layout(definition);
        // Hard lock bypass: discovery keeps the row available for unlocking,
        // but no renderer tracking, transforms, cancellation, replacement, or
        // side-effect emulation may run while this flag is set.
        if (layout.lockedToDefault) return;
        refreshKnownNativePosition(layerId, layout, graphics.guiWidth(), graphics.guiHeight());
        HudFrameTracker.begin(definition);
        active = new ActiveLayer(definition, layout, false, false, false, 1.0);
        boolean editorPreview = editorPreviewRequested(definition);

        // Untouched and reset elements are strict vanilla passthroughs. Bounds
        // are still observed for editor discovery, but rendering is never
        // canceled, replaced, tinted, or pose-transformed until explicitly
        // customized by a user or modpack configuration.
        if (!layout.customized) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean creativeHidden = !layout.showInCreative
                && minecraft.player != null && minecraft.player.isCreative();
        if (!editorPreview && creativeHidden && !layout.semanticBar()) {
            preserveVanillaBarSideEffects(event, definition, false);
            finishCanceled(graphics, false, partialTick);
            event.setCanceled(true);
            return;
        }

        NumericBarSource source = definition.semanticBar() ? BarSourceRegistry.get(layout.barSourceId) : null;
        NumericBarSnapshot snapshot = source == null ? null : source.snapshot(minecraft);
        if (snapshot != null && snapshot.valueAvailable()
                && layout.observeBarMaximum(snapshot.maximum() - snapshot.minimum())) {
            LayoutStore.markDirty();
        }
        float visibilityOpacity = editorPreview ? 1.0F
                : CustomBarRenderer.visibilityOpacity(layout.id, partialTick)
                * HudElementVisibility.parentOpacity(layout, partialTick);
        if (!editorPreview && visibilityOpacity <= 0.0001F) {
            preserveVanillaBarSideEffects(event, definition, false);
            finishCanceled(graphics, false, partialTick);
            event.setCanceled(true);
            return;
        }

        if (!editorPreview && layout.renderMode == RenderMode.HIDDEN) {
            preserveVanillaBarSideEffects(event, definition, false);
            finishCanceled(graphics, false, partialTick);
            event.setCanceled(true);
            return;
        }

        HudElementRelations.Resolved resolved = resolved(layout,
                graphics.guiWidth(), graphics.guiHeight(), partialTick);
        if ((layout.renderMode == RenderMode.CUSTOM || layout.renderMode == RenderMode.BOSS_BAR) && source != null) {
            NumericBarSnapshot renderedSnapshot = snapshot;
            if (editorPreview && snapshot != null && !snapshot.valueAvailable()) {
                renderedSnapshot = new NumericBarSnapshot(snapshot.current(), snapshot.minimum(), snapshot.maximum(),
                        snapshot.displayName(), snapshot.icon(), true, snapshot.healthEffects());
            }
            boolean rendered = renderedSnapshot != null && renderedSnapshot.valueAvailable()
                    && (renderedSnapshot.active() || visibilityOpacity > 0.0001F || editorPreview);
            preserveVanillaBarSideEffects(event, definition, rendered);
            if (rendered) CustomBarRenderer.render(graphics, layout, renderedSnapshot,
                    resolved.bounds(), resolved.scale(), partialTick, visibilityOpacity);
            finishCanceled(graphics, rendered, partialTick);
            event.setCanceled(true);
            return;
        }

        boolean fadeTinted = visibilityOpacity < 0.9999F;
        if (fadeTinted) {
            graphics.setColor(1.0F, 1.0F, 1.0F, visibilityOpacity);
        }
        if (layout.initialized && needsTransform(layout, resolved)) {
            Bounds target = resolved.bounds();
            graphics.pose().pushPose();
            graphics.pose().translate(target.x() - layout.nativeX, target.y() - layout.nativeY, 0.0);
            graphics.pose().translate(layout.nativeX, layout.nativeY, 0.0);
            graphics.pose().scale((float) resolved.scale(), (float) resolved.scale(), 1.0F);
            graphics.pose().translate(-layout.nativeX, -layout.nativeY, 0.0);
            active = new ActiveLayer(definition, layout, true, true, fadeTinted, resolved.scale());
        } else if (fadeTinted) {
            active = new ActiveLayer(definition, layout, false, false, true, 1.0);
        }
    }

    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        ActiveLayer current = active;
        if (current == null || !current.definition.stableId().equals(event.getName().toString())) return;
        if (current.posePushed) event.getGuiGraphics().pose().popPose();
        if (current.fadeTinted) event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
        HudFrameTracker.FrameResult frame = HudFrameTracker.end();
        reconcileNativeBounds(current, frame.bounds(), event.getGuiGraphics().guiWidth(),
                event.getGuiGraphics().guiHeight(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        REGISTRY.update(current.definition, frame.bounds(), order - 1, frame.textures(), frame.bounds() != null,
                event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
        active = null;
    }

    private static void finishCanceled(GuiGraphics graphics, boolean rendered, float partialTick) {
        ActiveLayer current = active;
        HudFrameTracker.FrameResult frame = HudFrameTracker.end();
        Bounds bounds = resolved(current.layout, graphics.guiWidth(), graphics.guiHeight(), partialTick).bounds();
        REGISTRY.update(current.definition, bounds, order - 1, frame.textures(),
                rendered && frame.bounds() != null,
                graphics.guiWidth(), graphics.guiHeight());
        active = null;
    }

    private static void reconcileNativeBounds(ActiveLayer current, Bounds rendered,
                                              int width, int height, float partialTick) {
        if (rendered == null) return;
        HudElementLayout layout = current.layout;
        if (!layout.initialized) return;
        if (current.transformed) {
            Bounds target = resolved(layout, width, height, partialTick).bounds();
            double scale = Math.max(0.0001, current.transformedScale);
            layout.nativeX += (rendered.x() - target.x()) / scale;
            layout.nativeY += (rendered.y() - target.y()) / scale;
            layout.nativeWidth = Math.max(1.0, rendered.width() / scale);
            layout.nativeHeight = Math.max(1.0, rendered.height() / scale);
        } else {
            layout.nativeX = rendered.x();
            layout.nativeY = rendered.y();
            layout.nativeWidth = Math.max(1.0, rendered.width());
            layout.nativeHeight = Math.max(1.0, rendered.height());
        }
        layout.nativeScreenWidth = width;
        layout.nativeScreenHeight = height;
    }

    private static void refreshKnownNativePosition(ResourceLocation id, HudElementLayout layout, int width, int height) {
        Bounds next = KnownHudElements.fallbackBounds(id, width, height);
        if (next == null || !layout.initialized) return;
        if (layout.nativeScreenWidth <= 0 || layout.nativeScreenHeight <= 0) {
            layout.nativeScreenWidth = width;
            layout.nativeScreenHeight = height;
            return;
        }
        if (layout.nativeScreenWidth == width && layout.nativeScreenHeight == height) return;
        Bounds previous = KnownHudElements.fallbackBounds(id, layout.nativeScreenWidth, layout.nativeScreenHeight);
        if (previous != null) {
            layout.nativeX += next.x() - previous.x();
            layout.nativeY += next.y() - previous.y();
        }
        layout.nativeScreenWidth = width;
        layout.nativeScreenHeight = height;
    }

    private static boolean needsTransform(HudElementLayout layout, HudElementRelations.Resolved resolved) {
        return Math.abs(resolved.scale() - 1.0) > 0.0001
                || Math.abs(resolved.bounds().x() - layout.nativeX) > 0.001
                || Math.abs(resolved.bounds().y() - layout.nativeY) > 0.001;
    }

    private static HudElementRelations.Resolved resolved(HudElementLayout layout,
                                                         int width, int height, float partialTick) {
        return HudElementRelations.resolve(layout, LayoutStore.get().elements, width, height,
                id -> HudElementVisibility.visibleForStack(id, partialTick));
    }

    private static boolean editorPreviewRequested(HudElementDefinition definition) {
        return Minecraft.getInstance().screen instanceof HudEditorScreen editor
                && editor.isListPreviewSelected(definition.stableId());
    }

    private static void preserveVanillaBarSideEffects(RenderGuiLayerEvent.Pre event,
                                                       HudElementDefinition definition,
                                                       boolean reserveStackSpace) {
        if (!definition.semanticBar()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int leftHeightBefore = Minecraft.getInstance().gui.leftHeight;
        int rightHeightBefore = Minecraft.getInstance().gui.rightHeight;
        HudFrameTracker.suspend();
        graphics.pose().pushPose();
        graphics.pose().translate(-1_000_000.0F, -1_000_000.0F, 0.0F);
        graphics.enableScissor(0, 0, 0, 0);
        try {
            event.getLayer().render(graphics, event.getPartialTick());
        } finally {
            graphics.disableScissor();
            graphics.pose().popPose();
            HudFrameTracker.resume();
            if (!reserveStackSpace) {
                Minecraft.getInstance().gui.leftHeight = leftHeightBefore;
                Minecraft.getInstance().gui.rightHeight = rightHeightBefore;
            } else if (definition.stableId().equals(VanillaGuiLayers.PLAYER_HEALTH.toString())) {
                Minecraft.getInstance().gui.leftHeight = HudStackingPolicy.singleHealthRow(leftHeightBefore);
            }
        }
    }

    private record ActiveLayer(HudElementDefinition definition, HudElementLayout layout,
                               boolean posePushed, boolean transformed, boolean fadeTinted,
                               double transformedScale) {
    }
}
