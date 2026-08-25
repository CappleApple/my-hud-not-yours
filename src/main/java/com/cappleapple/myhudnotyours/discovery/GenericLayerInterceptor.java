package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.bar.BarSourceRegistry;
import com.cappleapple.myhudnotyours.bar.BarVisibilityPolicy;
import com.cappleapple.myhudnotyours.bar.CustomBarRenderer;
import com.cappleapple.myhudnotyours.bar.NumericBarSnapshot;
import com.cappleapple.myhudnotyours.bar.NumericBarSource;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.editor.HudEditorScreen;
import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
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
        active = new ActiveLayer(definition, layout, false, false);
        boolean editorPreview = editorPreviewRequested(definition);

        // Untouched and reset elements are strict vanilla passthroughs. Bounds
        // are still observed for editor discovery, but rendering is never
        // canceled, replaced, tinted, or pose-transformed until explicitly
        // customized by a user or modpack configuration.
        if (!layout.customized) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!editorPreview && !layout.showInCreative && minecraft.player != null && minecraft.player.isCreative()) {
            preserveVanillaBarSideEffects(event, definition, false);
            finishCanceled(graphics, false);
            event.setCanceled(true);
            return;
        }

        NumericBarSource source = definition.semanticBar() ? BarSourceRegistry.get(layout.barSourceId) : null;
        NumericBarSnapshot snapshot = source == null ? null : source.snapshot(minecraft);
        if (!editorPreview && snapshot != null && BarVisibilityPolicy.hideForValue(layout, snapshot)) {
            preserveVanillaBarSideEffects(event, definition, false);
            finishCanceled(graphics, false);
            event.setCanceled(true);
            return;
        }

        if (!editorPreview && layout.renderMode == RenderMode.HIDDEN) {
            preserveVanillaBarSideEffects(event, definition, false);
            finishCanceled(graphics, false);
            event.setCanceled(true);
            return;
        }

        if ((layout.renderMode == RenderMode.CUSTOM || layout.renderMode == RenderMode.BOSS_BAR) && source != null) {
            NumericBarSnapshot renderedSnapshot = snapshot;
            if (editorPreview && snapshot != null && !snapshot.active()) {
                renderedSnapshot = new NumericBarSnapshot(snapshot.current(), snapshot.minimum(), snapshot.maximum(),
                        snapshot.displayName(), snapshot.icon(), true);
            }
            boolean rendered = renderedSnapshot != null && renderedSnapshot.active();
            preserveVanillaBarSideEffects(event, definition, rendered);
            if (rendered) CustomBarRenderer.render(graphics, layout, renderedSnapshot,
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
            finishCanceled(graphics, rendered);
            event.setCanceled(true);
            return;
        }

        if (layout.initialized && needsTransform(layout)) {
            Bounds target = layout.resolvedBounds(graphics.guiWidth(), graphics.guiHeight());
            graphics.pose().pushPose();
            graphics.pose().translate(target.x() - layout.nativeX, target.y() - layout.nativeY, 0.0);
            graphics.pose().translate(layout.nativeX, layout.nativeY, 0.0);
            graphics.pose().scale((float) layout.scale, (float) layout.scale, 1.0F);
            graphics.pose().translate(-layout.nativeX, -layout.nativeY, 0.0);
            active = new ActiveLayer(definition, layout, true, true);
        }
    }

    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        ActiveLayer current = active;
        if (current == null || !current.definition.stableId().equals(event.getName().toString())) return;
        if (current.posePushed) event.getGuiGraphics().pose().popPose();
        HudFrameTracker.FrameResult frame = HudFrameTracker.end();
        reconcileNativeBounds(current, frame.bounds(), event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
        REGISTRY.update(current.definition, frame.bounds(), order - 1, frame.textures(), frame.bounds() != null,
                event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
        active = null;
    }

    private static void finishCanceled(GuiGraphics graphics, boolean rendered) {
        ActiveLayer current = active;
        HudFrameTracker.FrameResult frame = HudFrameTracker.end();
        Bounds bounds = current.layout.resolvedBounds(graphics.guiWidth(), graphics.guiHeight());
        REGISTRY.update(current.definition, bounds, order - 1, frame.textures(),
                rendered && frame.bounds() != null,
                graphics.guiWidth(), graphics.guiHeight());
        active = null;
    }

    private static void reconcileNativeBounds(ActiveLayer current, Bounds rendered, int width, int height) {
        if (rendered == null) return;
        HudElementLayout layout = current.layout;
        if (!layout.initialized) return;
        if (current.transformed) {
            Bounds target = layout.resolvedBounds(width, height);
            double scale = Math.max(0.0001, layout.scale);
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

    private static boolean needsTransform(HudElementLayout layout) {
        return Math.abs(layout.scale - 1.0) > 0.0001
                || Math.abs(layout.resolvedBounds(Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight()).x() - layout.nativeX) > 0.001
                || Math.abs(layout.resolvedBounds(Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight()).y() - layout.nativeY) > 0.001;
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
                               boolean posePushed, boolean transformed) {
    }
}
