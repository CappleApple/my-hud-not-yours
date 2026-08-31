package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.model.BarLayerStyle;
import com.cappleapple.myhudnotyours.model.BarStyle;
import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.FillDirection;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import com.cappleapple.myhudnotyours.model.LayerMode;
import com.cappleapple.myhudnotyours.model.RenderMode;
import com.cappleapple.myhudnotyours.model.TextAlignment;
import com.cappleapple.myhudnotyours.model.TextMode;
import com.cappleapple.myhudnotyours.model.TextureScaleMode;
import com.cappleapple.myhudnotyours.texture.ManagedTextureResolver;
import com.cappleapple.myhudnotyours.texture.TextureHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class CustomBarRenderer {
    private static final ResourceLocation BOSS_BACKGROUND = ResourceLocation.withDefaultNamespace("boss_bar/red_background");
    private static final ResourceLocation BOSS_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/red_progress");
    private static final ResourceLocation BOSS_GREEN_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/green_progress");
    private static final ResourceLocation BOSS_BLUE_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/blue_progress");
    private static final ResourceLocation BOSS_YELLOW_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/yellow_progress");
    private static final ResourceLocation BOSS_WHITE_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/white_progress");
    private static final TrailAnimator TRAILS = new TrailAnimator();
    private static final BarVisibilityAnimator VISIBILITY = new BarVisibilityAnimator();
    private static final BarIdleTracker IDLE = new BarIdleTracker();
    private static final Map<String, Boolean> HIDE_REQUESTS = new HashMap<>();
    private static final Set<String> TRACKED_BARS = new HashSet<>();
    private static final Set<String> AVAILABLE_BARS = new HashSet<>();
    private static float renderOpacity = 1.0F;

    private CustomBarRenderer() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        Set<String> activeTrailIds = new HashSet<>();
        Set<String> activeVisibilityIds = new HashSet<>();
        Set<String> availableBarIds = new HashSet<>();
        Set<String> trackedBarIds = new HashSet<>();
        for (HudElementLayout layout : LayoutStore.get().elements.values()) {
            if (!layout.semanticBar()) continue;
            NumericBarSource source = BarSourceRegistry.get(layout.barSourceId);
            if (source == null) continue;
            NumericBarSnapshot snapshot = source.snapshot(minecraft);
            if (snapshot == null) continue;
            trackedBarIds.add(layout.id);
            activeVisibilityIds.add(layout.id);
            boolean modControlsLayout = layout.customized && !layout.lockedToDefault;
            if (modControlsLayout && snapshot.valueAvailable()
                    && layout.observeBarMaximum(snapshot.maximum() - snapshot.minimum())) {
                LayoutStore.markDirty();
            }
            boolean dataChanged = snapshot.valueAvailable() && IDLE.changedThisTick(layout.id, snapshot);
            boolean valueHidden = modControlsLayout && snapshot.valueAvailable()
                    && BarVisibilityPolicy.hideForValue(layout, snapshot);
            boolean sourceHidden = !snapshot.valueAvailable() || BarVisibilityPolicy.hideForSource(snapshot);
            boolean creativeHidden = modControlsLayout && !layout.showInCreative
                    && minecraft.player != null && minecraft.player.isCreative();
            boolean idleHidden = modControlsLayout && !layout.showOnIdle && !dataChanged;
            boolean hideRequested = sourceHidden || valueHidden || creativeHidden || idleHidden;
            HIDE_REQUESTS.put(layout.id, hideRequested);
            VISIBILITY.tick(layout.id, hideRequested,
                    modControlsLayout ? layout.hideDelayMillis : 0,
                    modControlsLayout ? layout.hideFadeMillis : 0);
            if (snapshot.valueAvailable()) availableBarIds.add(layout.id);
            if (!modControlsLayout || !snapshot.valueAvailable()) continue;
            if (layout.renderMode != RenderMode.CUSTOM && layout.renderMode != RenderMode.BOSS_BAR) continue;
            activeTrailIds.add(layout.id);
            TRAILS.tick(layout.id, snapshot.renderFraction(), layout.bar.trail.mode,
                    layout.bar.trail.delayMillis, layout.bar.trail.catchUpMillis,
                    sourceHidden || valueHidden || idleHidden);
        }
        TRAILS.retainOnly(activeTrailIds);
        VISIBILITY.retainOnly(activeVisibilityIds);
        IDLE.retainOnly(trackedBarIds);
        HIDE_REQUESTS.keySet().retainAll(trackedBarIds);
        TRACKED_BARS.clear();
        TRACKED_BARS.addAll(trackedBarIds);
        AVAILABLE_BARS.clear();
        AVAILABLE_BARS.addAll(availableBarIds);
    }

    public static void clearTrailState() {
        TRAILS.clear();
        VISIBILITY.clear();
        IDLE.clear();
        HIDE_REQUESTS.clear();
        TRACKED_BARS.clear();
        AVAILABLE_BARS.clear();
    }

    public static float visibilityOpacity(String elementId, boolean hideRequested, float partialTick) {
        return VISIBILITY.render(elementId, hideRequested, partialTick);
    }

    public static float visibilityOpacity(String elementId, float partialTick) {
        if (TRACKED_BARS.contains(elementId) && !AVAILABLE_BARS.contains(elementId)) return 0.0F;
        return VISIBILITY.render(elementId, HIDE_REQUESTS.getOrDefault(elementId, false), partialTick);
    }

    public static void render(GuiGraphics graphics, HudElementLayout layout, NumericBarSnapshot snapshot,
                              Bounds bounds, double effectiveScale,
                              float partialTick, float visibilityOpacity) {
        if (!snapshot.valueAvailable()) return;
        int x = (int) Math.round(bounds.x());
        int y = (int) Math.round(bounds.y());
        int width = Math.max(1, (int) Math.round(bounds.width()));
        int height = Math.max(1, (int) Math.round(bounds.height()));
        double value = snapshot.renderFraction();
        double trail = TRAILS.render(layout.id, value, partialTick);

        float previousRenderOpacity = renderOpacity;
        renderOpacity = Math.max(0.0F, Math.min(1.0F, visibilityOpacity));
        resetTint(graphics);
        RenderSystem.enableBlend();
        try {
            if (layout.renderMode == RenderMode.BOSS_BAR) {
                renderBoss(graphics, layout.bar, snapshot, x, y, width, height, value, trail, effectiveScale);
            } else {
                renderCustom(graphics, layout.bar, snapshot, x, y, width, height, value, trail, effectiveScale);
            }
            renderText(graphics, layout.bar, snapshot, x, y, width, height, effectiveScale);
        } finally {
            RenderSystem.disableBlend();
            renderOpacity = previousRenderOpacity;
            resetTint(graphics);
        }
    }

    private static void renderCustom(GuiGraphics graphics, BarStyle style, NumericBarSnapshot snapshot,
                                     int x, int y, int width, int height,
                                     double value, double trail, double elementScale) {
        SegmentSpec segments = segmentSpec(style, snapshot);
        drawLayer(graphics, style.background, x, y, width, height, elementScale, segments);
        int border = Math.max(0, Math.min((int) Math.round(style.borderThickness * elementScale),
                Math.min(width, height) / 2));
        int innerX = x + border;
        int innerY = y + border;
        int innerWidth = Math.max(0, width - border * 2);
        int innerHeight = Math.max(0, height - border * 2);
        drawLayer(graphics, style.empty, innerX, innerY, innerWidth, innerHeight, elementScale, segments);
        if (trail > value) {
            drawFraction(graphics, style.trail.layer, innerX, innerY, innerWidth, innerHeight,
                    style.fillDirection, 0.0, trail, elementScale, segments);
            drawFraction(graphics, style.filled, innerX, innerY, innerWidth, innerHeight,
                    style.fillDirection, 0.0, value, elementScale, segments);
            drawHealthEffects(graphics, style, snapshot, innerX, innerY, innerWidth, innerHeight,
                    elementScale, segments);
        } else {
            drawFraction(graphics, style.filled, innerX, innerY, innerWidth, innerHeight,
                    style.fillDirection, 0.0, value, elementScale, segments);
            drawHealthEffects(graphics, style, snapshot, innerX, innerY, innerWidth, innerHeight,
                    elementScale, segments);
            if (trail < value) {
                drawFraction(graphics, style.trail.layer, innerX, innerY, innerWidth, innerHeight,
                        style.fillDirection, trail, value, elementScale, segments);
            }
        }
        if (style.frame.mode == LayerMode.SOLID) {
            drawSolidFrame(graphics, style.frame, x, y, width, height, Math.max(1, border), elementScale);
        } else {
            drawLayer(graphics, style.frame, x, y, width, height, elementScale, segments);
        }
    }

    private static void renderBoss(GuiGraphics graphics, BarStyle style, NumericBarSnapshot snapshot,
                                   int x, int y, int width, int height,
                                   double value, double trail, double elementScale) {
        LayerRect background = layerRect(style.background, x, y, width, height, elementScale);
        tint(graphics, 0xFFFFFFFF, style.background == null ? 1.0F : style.background.opacity);
        drawBossSprite(graphics, BOSS_BACKGROUND, background.x, background.y, background.width, background.height);
        resetTint(graphics);
        // The background and progress share a depth. Flush the background before
        // changing scissor/tint state so batching cannot replay it as a foreground draw.
        graphics.flush();
        if (trail > value) {
            drawBossFraction(graphics, style.trail.layer, x, y, width, height,
                    style.fillDirection, 0.0, trail, elementScale, true);
            drawBossFraction(graphics, style.filled, x, y, width, height,
                    style.fillDirection, 0.0, value, elementScale, false);
            drawBossHealthEffects(graphics, style, snapshot, x, y, width, height, elementScale);
        } else {
            drawBossFraction(graphics, style.filled, x, y, width, height,
                    style.fillDirection, 0.0, value, elementScale, false);
            drawBossHealthEffects(graphics, style, snapshot, x, y, width, height, elementScale);
            if (trail < value) {
                drawBossFraction(graphics, style.trail.layer, x, y, width, height,
                        style.fillDirection, trail, value, elementScale, true);
            }
        }
    }

    private static void drawHealthEffects(GuiGraphics graphics, BarStyle style, NumericBarSnapshot snapshot,
                                          int x, int y, int width, int height, double elementScale,
                                          SegmentSpec segments) {
        HealthBarEffects effects = snapshot.healthEffects();
        if (effects == null) return;
        double healthEnd = snapshot.healthRenderFraction();
        int healthColor = effects.healthOverlayColor();
        if (healthColor != 0 && healthEnd > 0.0) {
            drawTintedFraction(graphics, style.filled, x, y, width, height,
                    style.fillDirection, 0.0, healthEnd, healthColor, elementScale, segments);
        }
        double totalEnd = snapshot.renderFraction();
        double absorptionStart = snapshot.absorptionRenderStartFraction();
        if (effects.absorption() > 0.0 && totalEnd > absorptionStart) {
            drawTintedFraction(graphics, style.filled, x, y, width, height,
                    style.fillDirection, absorptionStart, totalEnd,
                    effects.absorptionOverlayColor(), elementScale, segments);
        }
    }

    private static void drawBossHealthEffects(GuiGraphics graphics, BarStyle style,
                                              NumericBarSnapshot snapshot,
                                              int x, int y, int width, int height,
                                              double elementScale) {
        HealthBarEffects effects = snapshot.healthEffects();
        if (effects == null) return;
        double healthEnd = snapshot.healthRenderFraction();
        int healthColor = effects.healthOverlayColor();
        if (healthColor != 0 && healthEnd > 0.0) {
            drawBossTintedFraction(graphics, style.filled, x, y, width, height,
                    style.fillDirection, 0.0, healthEnd, healthColor, elementScale);
        }
        double totalEnd = snapshot.renderFraction();
        double absorptionStart = snapshot.absorptionRenderStartFraction();
        if (effects.absorption() > 0.0 && totalEnd > absorptionStart) {
            drawBossTintedFraction(graphics, style.filled, x, y, width, height,
                    style.fillDirection, absorptionStart, totalEnd,
                    effects.absorptionOverlayColor(), elementScale);
        }
    }

    private static void drawTintedFraction(GuiGraphics graphics, BarLayerStyle layer,
                                           int x, int y, int width, int height,
                                           FillDirection direction, double from, double to,
                                           int tintColor, double elementScale, SegmentSpec segments) {
        if (layer == null || layer.mode == LayerMode.NONE || width <= 0 || height <= 0) return;
        LayerRect rect = layerRect(layer, x, y, width, height, elementScale);
        Clip clip = clip(rect.x, rect.y, rect.width, rect.height, direction, from, to);
        if (clip.width <= 0 || clip.height <= 0) return;
        graphics.flush();
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        drawLayerAtTinted(graphics, layer, rect.x, rect.y, rect.width, rect.height, tintColor, segments);
        graphics.flush();
        graphics.disableScissor();
    }

    private static void drawBossTintedFraction(GuiGraphics graphics, BarLayerStyle layer,
                                               int x, int y, int width, int height,
                                               FillDirection direction, double from, double to,
                                               int tintColor, double elementScale) {
        LayerRect rect = layerRect(layer, x, y, width, height, elementScale);
        Clip clip = clip(rect.x, rect.y, rect.width, rect.height, direction, from, to);
        if (clip.width <= 0 || clip.height <= 0) return;
        graphics.flush();
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        ResourceLocation sprite = bossEffectSprite(tintColor);
        int spriteTint = tintColor == HealthBarEffects.WITHER_OVERLAY ? tintColor : 0xFFFFFFFF;
        tint(graphics, spriteTint, layer == null ? 1.0F : layer.opacity);
        drawBossSprite(graphics, sprite, rect.x, rect.y, rect.width, rect.height);
        graphics.flush();
        resetTint(graphics);
        graphics.disableScissor();
    }

    private static ResourceLocation bossEffectSprite(int tintColor) {
        if (tintColor == HealthBarEffects.POISON_OVERLAY) return BOSS_GREEN_PROGRESS;
        if (tintColor == HealthBarEffects.FROZEN_OVERLAY) return BOSS_BLUE_PROGRESS;
        if (tintColor == HealthBarEffects.ABSORPTION_OVERLAY) return BOSS_YELLOW_PROGRESS;
        return BOSS_WHITE_PROGRESS;
    }

    private static void drawBossFraction(GuiGraphics graphics, BarLayerStyle layer,
                                         int x, int y, int width, int height,
                                         FillDirection direction, double from, double to,
                                         double elementScale, boolean useLayerColor) {
        LayerRect rect = layerRect(layer, x, y, width, height, elementScale);
        Clip clip = clip(rect.x, rect.y, rect.width, rect.height, direction, from, to);
        if (clip.width <= 0 || clip.height <= 0) return;
        graphics.flush();
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        if (useLayerColor && layer != null) tint(graphics, layer);
        else tint(graphics, 0xFFFFFFFF, layer == null ? 1.0F : layer.opacity);
        drawBossSprite(graphics, BOSS_PROGRESS, rect.x, rect.y, rect.width, rect.height);
        graphics.flush();
        resetTint(graphics);
        graphics.disableScissor();
    }

    private static void drawBossSprite(GuiGraphics graphics, ResourceLocation sprite,
                                       int x, int y, int width, int height) {
        // The cropped blitSprite overload interprets the requested destination size
        // as a 182x5 source crop. Scale the complete vanilla sprite explicitly so
        // background, trail, and progress always occupy the identical rectangle.
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(width / 182.0F, height / 5.0F, 1.0F);
        graphics.blitSprite(sprite, 0, 0, 182, 5);
        graphics.pose().popPose();
    }

    private static void drawFraction(GuiGraphics graphics, BarLayerStyle layer,
                                     int x, int y, int width, int height,
                                     FillDirection direction, double from, double to,
                                     double elementScale, SegmentSpec segments) {
        if (layer == null || layer.mode == LayerMode.NONE || width <= 0 || height <= 0) return;
        LayerRect rect = layerRect(layer, x, y, width, height, elementScale);
        Clip clip = clip(rect.x, rect.y, rect.width, rect.height, direction, from, to);
        if (clip.width <= 0 || clip.height <= 0) return;
        graphics.flush();
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        drawLayerAt(graphics, layer, rect.x, rect.y, rect.width, rect.height, segments);
        graphics.flush();
        graphics.disableScissor();
    }

    private static Clip clip(int x, int y, int width, int height, FillDirection direction, double from, double to) {
        from = Math.max(0.0, Math.min(1.0, from));
        to = Math.max(from, Math.min(1.0, to));
        return switch (direction) {
            case LEFT_TO_RIGHT -> new Clip(x + (int) Math.floor(width * from), y,
                    (int) Math.ceil(width * to) - (int) Math.floor(width * from), height);
            case RIGHT_TO_LEFT -> new Clip(x + width - (int) Math.ceil(width * to), y,
                    (int) Math.ceil(width * to) - (int) Math.floor(width * from), height);
            case TOP_TO_BOTTOM -> new Clip(x, y + (int) Math.floor(height * from), width,
                    (int) Math.ceil(height * to) - (int) Math.floor(height * from));
            case BOTTOM_TO_TOP -> new Clip(x, y + height - (int) Math.ceil(height * to), width,
                    (int) Math.ceil(height * to) - (int) Math.floor(height * from));
        };
    }

    private static LayerRect layerRect(BarLayerStyle layer, int x, int y, int width, int height,
                                       double elementScale) {
        if (layer == null) return new LayerRect(x, y, Math.max(1, width), Math.max(1, height));
        Bounds transformed = BarLayerGeometry.resolve(layer, x, y, width, height, elementScale);
        int left = (int) Math.round(transformed.x());
        int top = (int) Math.round(transformed.y());
        int right = (int) Math.round(transformed.right());
        int bottom = (int) Math.round(transformed.bottom());
        return new LayerRect(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private static void drawLayer(GuiGraphics graphics, BarLayerStyle layer,
                                  int x, int y, int width, int height, double elementScale,
                                  SegmentSpec segments) {
        if (layer == null || layer.mode == LayerMode.NONE || width <= 0 || height <= 0) return;
        LayerRect rect = layerRect(layer, x, y, width, height, elementScale);
        drawLayerAt(graphics, layer, rect.x, rect.y, rect.width, rect.height, segments);
    }

    private static void drawLayerAt(GuiGraphics graphics, BarLayerStyle layer,
                                    int x, int y, int width, int height, SegmentSpec segments) {
        if (layer == null || layer.mode == LayerMode.NONE || width <= 0 || height <= 0) return;
        if (layer.mode == LayerMode.SOLID) {
            graphics.fill(x, y, x + width, y + height, withOpacity(layer.color, layer.opacity));
            return;
        }
        TextureHandle handle = ManagedTextureResolver.get().resolve(layer.texture, Util.getMillis());
        if (handle == null) return;
        tint(graphics, layer);
        switch (layer.textureScale) {
            case STRETCH -> blit(graphics, handle, x, y, width, height,
                    handle.sourceX(), handle.sourceY(), handle.sourceWidth(), handle.sourceHeight());
            case TILE -> tile(graphics, handle, x, y, width, height);
            case SEGMENTED -> segmented(graphics, handle, x, y, width, height, segments);
            case NINE_SLICE -> nineSlice(graphics, handle, layer, x, y, width, height);
        }
        resetTint(graphics);
    }

    private static void drawLayerAtTinted(GuiGraphics graphics, BarLayerStyle layer,
                                          int x, int y, int width, int height, int tintColor,
                                          SegmentSpec segments) {
        if (layer == null || layer.mode == LayerMode.NONE || width <= 0 || height <= 0) return;
        if (layer.mode == LayerMode.SOLID) {
            graphics.fill(x, y, x + width, y + height, withOpacity(tintColor, layer.opacity));
            return;
        }
        TextureHandle handle = ManagedTextureResolver.get().resolve(layer.texture, Util.getMillis());
        if (handle == null) return;
        tint(graphics, tintColor, layer.opacity);
        switch (layer.textureScale) {
            case STRETCH -> blit(graphics, handle, x, y, width, height,
                    handle.sourceX(), handle.sourceY(), handle.sourceWidth(), handle.sourceHeight());
            case TILE -> tile(graphics, handle, x, y, width, height);
            case SEGMENTED -> segmented(graphics, handle, x, y, width, height, segments);
            case NINE_SLICE -> nineSlice(graphics, handle, layer, x, y, width, height);
        }
        resetTint(graphics);
    }

    private static void tile(GuiGraphics graphics, TextureHandle handle, int x, int y, int width, int height) {
        int tileWidth = Math.max(1, handle.sourceWidth());
        int tileHeight = Math.max(1, handle.sourceHeight());
        for (int dy = 0; dy < height; dy += tileHeight) {
            for (int dx = 0; dx < width; dx += tileWidth) {
                int pieceWidth = Math.min(tileWidth, width - dx);
                int pieceHeight = Math.min(tileHeight, height - dy);
                blit(graphics, handle, x + dx, y + dy, pieceWidth, pieceHeight,
                        handle.sourceX(), handle.sourceY(), pieceWidth, pieceHeight);
            }
        }
    }

    private static void segmented(GuiGraphics graphics, TextureHandle handle,
                                  int x, int y, int width, int height, SegmentSpec spec) {
        int axisLength = spec.vertical ? height : width;
        int visibleCount = Math.max(1, Math.min(spec.count, axisLength));
        for (int index = 0; index < visibleCount; index++) {
            int start = BarSegmentGeometry.cellStart(axisLength, index, visibleCount);
            int end = BarSegmentGeometry.cellEnd(axisLength, index, visibleCount);
            int cellSize = end - start;
            if (cellSize <= 0) continue;
            if (spec.vertical) {
                blit(graphics, handle, x, y + start, width, cellSize,
                        handle.sourceX(), handle.sourceY(), handle.sourceWidth(), handle.sourceHeight());
            } else {
                blit(graphics, handle, x + start, y, cellSize, height,
                        handle.sourceX(), handle.sourceY(), handle.sourceWidth(), handle.sourceHeight());
            }
        }
    }

    private static SegmentSpec segmentSpec(BarStyle style, NumericBarSnapshot snapshot) {
        int count = BarSegmentGeometry.segmentCount(snapshot, style.maximumPerSegment);
        boolean vertical = style.fillDirection == FillDirection.TOP_TO_BOTTOM
                || style.fillDirection == FillDirection.BOTTOM_TO_TOP;
        return new SegmentSpec(count, vertical);
    }

    private static void nineSlice(GuiGraphics graphics, TextureHandle handle, BarLayerStyle layer,
                                  int x, int y, int width, int height) {
        layer.margins.clamp(handle.sourceWidth(), handle.sourceHeight());
        int left = Math.min(layer.margins.left, width / 2);
        int right = Math.min(layer.margins.right, Math.max(0, width - left));
        int top = Math.min(layer.margins.top, height / 2);
        int bottom = Math.min(layer.margins.bottom, Math.max(0, height - top));
        int sourceCenterWidth = Math.max(0, handle.sourceWidth() - layer.margins.left - layer.margins.right);
        int sourceCenterHeight = Math.max(0, handle.sourceHeight() - layer.margins.top - layer.margins.bottom);
        int centerWidth = Math.max(0, width - left - right);
        int centerHeight = Math.max(0, height - top - bottom);
        int sx = handle.sourceX();
        int sy = handle.sourceY();
        int sr = sx + handle.sourceWidth() - layer.margins.right;
        int sb = sy + handle.sourceHeight() - layer.margins.bottom;

        blit(graphics, handle, x, y, left, top, sx, sy, layer.margins.left, layer.margins.top);
        blit(graphics, handle, x + left, y, centerWidth, top, sx + layer.margins.left, sy, sourceCenterWidth, layer.margins.top);
        blit(graphics, handle, x + width - right, y, right, top, sr, sy, layer.margins.right, layer.margins.top);
        blit(graphics, handle, x, y + top, left, centerHeight, sx, sy + layer.margins.top, layer.margins.left, sourceCenterHeight);
        blit(graphics, handle, x + left, y + top, centerWidth, centerHeight, sx + layer.margins.left,
                sy + layer.margins.top, sourceCenterWidth, sourceCenterHeight);
        blit(graphics, handle, x + width - right, y + top, right, centerHeight, sr,
                sy + layer.margins.top, layer.margins.right, sourceCenterHeight);
        blit(graphics, handle, x, y + height - bottom, left, bottom, sx, sb, layer.margins.left, layer.margins.bottom);
        blit(graphics, handle, x + left, y + height - bottom, centerWidth, bottom,
                sx + layer.margins.left, sb, sourceCenterWidth, layer.margins.bottom);
        blit(graphics, handle, x + width - right, y + height - bottom, right, bottom,
                sr, sb, layer.margins.right, layer.margins.bottom);
    }

    private static void blit(GuiGraphics graphics, TextureHandle handle, int x, int y, int width, int height,
                             int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return;
        graphics.blit(handle.location(), x, y, width, height, (float) sourceX, (float) sourceY,
                sourceWidth, sourceHeight, handle.textureWidth(), handle.textureHeight());
    }

    private static void drawSolidFrame(GuiGraphics graphics, BarLayerStyle layer,
                                       int x, int y, int width, int height, int thickness,
                                       double elementScale) {
        LayerRect rect = layerRect(layer, x, y, width, height, elementScale);
        x = rect.x;
        y = rect.y;
        width = rect.width;
        height = rect.height;
        int color = withOpacity(layer.color, layer.opacity);
        graphics.fill(x, y, x + width, y + Math.min(thickness, height), color);
        graphics.fill(x, Math.max(y, y + height - thickness), x + width, y + height, color);
        graphics.fill(x, y, x + Math.min(thickness, width), y + height, color);
        graphics.fill(Math.max(x, x + width - thickness), y, x + width, y + height, color);
    }

    private static void renderText(GuiGraphics graphics, BarStyle style, NumericBarSnapshot snapshot,
                                   int x, int y, int width, int height, double elementScale) {
        if (style.text.mode == TextMode.OFF) return;
        String current = number(snapshot.current());
        String maximum = number(snapshot.maximum());
        String percent = Integer.toString((int) Math.round(snapshot.fraction() * 100.0));
        String text = switch (style.text.mode) {
            case OFF -> "";
            case CURRENT -> current;
            case CURRENT_MAX -> current + " / " + maximum;
            case PERCENTAGE -> percent + "%";
            case CUSTOM -> style.text.format.replace("{current}", current).replace("{max}", maximum)
                    .replace("{percent}", percent).replace("{name}", snapshot.displayName());
        };
        if (text.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        float scale = Math.max(0.0625F, Math.min(16.0F, style.text.scale * (float) elementScale));
        float textWidth = minecraft.font.width(text) * scale;
        float textX = switch (style.text.alignment) {
            case LEFT -> x;
            case CENTER -> x + (width - textWidth) / 2.0F;
            case RIGHT -> x + width - textWidth;
        };
        float textY = y + (height - minecraft.font.lineHeight * scale) / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(textX + style.text.offsetX * elementScale,
                textY + style.text.offsetY * elementScale, 2.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, text, 0, 0, withOpacity(style.text.color, 1.0F), style.text.shadow);
        graphics.pose().popPose();
    }

    private static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int withOpacity(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF)
                * Math.max(0.0F, Math.min(1.0F, opacity)) * renderOpacity);
        return (color & 0x00FFFFFF) | alpha << 24;
    }

    private static void tint(GuiGraphics graphics, BarLayerStyle layer) {
        tint(graphics, layer.color, layer.opacity);
    }

    private static void tint(GuiGraphics graphics, int color, float opacity) {
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                ((color >>> 24) & 0xFF) / 255.0F * opacity * renderOpacity);
    }

    private static void resetTint(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private record Clip(int x, int y, int width, int height) {
    }

    private record LayerRect(int x, int y, int width, int height) {
    }

    private record SegmentSpec(int count, boolean vertical) {
    }
}
