package com.cappleapple.myhudnotyours.model;

import com.cappleapple.myhudnotyours.bar.BarMaximumGeometry;

public final class HudElementLayout {
    public String id = "";
    public String displayName = "Unknown HUD Element";
    public HudElementType classification = HudElementType.UNKNOWN;
    public ScreenAnchor anchor = ScreenAnchor.TOP_LEFT;
    public double offsetX = 0.0;
    public double offsetY = 0.0;
    public double scale = 1.0;
    public float opacity = 1.0F;
    public RenderMode renderMode = RenderMode.ORIGINAL;
    public String barSourceId = "";
    public BarStyle bar = new BarStyle();
    /** True only after an author deliberately changes this element. */
    public boolean customized = false;
    /** Hard bypass: new elements must be explicitly unlocked before editing. */
    public boolean lockedToDefault = true;
    /** Customized elements may be independently suppressed in creative mode. */
    public boolean showInCreative = true;
    /** Semantic bars may disappear at either end of their value range. */
    public boolean hideWhenFull = false;
    public boolean hideWhenEmpty = false;
    /** When false, an unchanged semantic bar uses the configured hide timing. */
    public boolean showOnIdle = true;
    /** Delay before a configured conditional hide begins. */
    public int hideDelayMillis = 0;
    /** Duration of the fade after {@link #hideDelayMillis}. */
    public int hideFadeMillis = 0;
    /** Optional local-transform parent HUD element. */
    public String parentId = "";
    /** Optional element that conditionally positions this element directly above it. */
    public String stackOnId = "";
    public double stackOffsetX = 0.0;
    public double stackOffsetY = 0.0;
    public boolean initialized = false;
    public double nativeX = 0.0;
    public double nativeY = 0.0;
    public double nativeWidth = 40.0;
    public double nativeHeight = 12.0;
    public int nativeScreenWidth = 0;
    public int nativeScreenHeight = 0;
    /** Last live maximum sampled from the semantic source; never serialized. */
    public transient double observedBarMaximum = -1.0;

    public boolean semanticBar() {
        return classification == HudElementType.BAR && barSourceId != null && !barSourceId.isBlank();
    }

    public Bounds resolvedBounds(int screenWidth, int screenHeight) {
        double x = anchor.x(screenWidth) + offsetX;
        double y = anchor.y(screenHeight) + offsetY;
        return new Bounds(x, y, unscaledWidth() * scale, unscaledHeight() * scale);
    }

    public double unscaledWidth() {
        boolean replacement = renderMode == RenderMode.CUSTOM || renderMode == RenderMode.BOSS_BAR;
        double barWidth = BarMaximumGeometry.size(bar.width, bar.widthPerMaximum,
                bar.sizeBaselineMaximum, observedBarMaximum);
        return replacement ? barWidth : nativeWidth;
    }

    public double unscaledHeight() {
        boolean replacement = renderMode == RenderMode.CUSTOM || renderMode == RenderMode.BOSS_BAR;
        double barHeight = BarMaximumGeometry.size(bar.height, bar.heightPerMaximum,
                bar.sizeBaselineMaximum, observedBarMaximum);
        return replacement ? barHeight : nativeHeight;
    }

    /** Returns true when a missing persisted baseline was initialized. */
    public boolean observeBarMaximum(double maximumRange) {
        if (!Double.isFinite(maximumRange) || maximumRange <= 0.0) return false;
        observedBarMaximum = maximumRange;
        if ((bar.widthPerMaximum > 0.0 || bar.heightPerMaximum > 0.0)
                && bar.sizeBaselineMaximum <= 0.0) {
            bar.sizeBaselineMaximum = maximumRange;
            return true;
        }
        return false;
    }

    public void dynamicSizingChanged() {
        if (bar.widthPerMaximum <= 0.0 && bar.heightPerMaximum <= 0.0) {
            bar.sizeBaselineMaximum = -1.0;
        } else if (bar.sizeBaselineMaximum <= 0.0 && observedBarMaximum > 0.0) {
            bar.sizeBaselineMaximum = observedBarMaximum;
        }
    }

    public void resetObservedBarMaximum() {
        observedBarMaximum = -1.0;
        bar.sizeBaselineMaximum = -1.0;
    }

    public void initializeFrom(Bounds nativeBounds, int screenWidth, int screenHeight) {
        nativeX = nativeBounds.x();
        nativeY = nativeBounds.y();
        nativeWidth = Math.max(1.0, nativeBounds.width());
        nativeHeight = Math.max(1.0, nativeBounds.height());
        nativeScreenWidth = screenWidth;
        nativeScreenHeight = screenHeight;
        if (!initialized) {
            anchor = ScreenAnchor.nearest(nativeBounds.centerX(), nativeBounds.centerY(), screenWidth, screenHeight);
            offsetX = nativeBounds.x() - anchor.x(screenWidth);
            offsetY = nativeBounds.y() - anchor.y(screenHeight);
            initialized = true;
        }
    }

    public void changeAnchor(ScreenAnchor next, int screenWidth, int screenHeight) {
        Bounds current = resolvedBounds(screenWidth, screenHeight);
        anchor = next;
        offsetX = current.x() - next.x(screenWidth);
        offsetY = current.y() - next.y(screenHeight);
    }

    public void move(double dx, double dy) {
        offsetX += dx;
        offsetY += dy;
    }

    public void resetPlacement(int screenWidth, int screenHeight) {
        anchor = ScreenAnchor.nearest(nativeX + nativeWidth / 2.0, nativeY + nativeHeight / 2.0, screenWidth, screenHeight);
        offsetX = nativeX - anchor.x(screenWidth);
        offsetY = nativeY - anchor.y(screenHeight);
        scale = 1.0;
        opacity = 1.0F;
        renderMode = RenderMode.ORIGINAL;
        parentId = "";
        stackOnId = "";
        stackOffsetX = 0.0;
        stackOffsetY = 0.0;
        customized = false;
    }

    /** Migrates version-1 layouts, which predated the explicit passthrough flag. */
    public boolean hasLegacyCustomization() {
        if (renderMode != RenderMode.ORIGINAL || Math.abs(scale - 1.0) > 0.0001
                || Math.abs(opacity - 1.0F) > 0.0001F) {
            return true;
        }
        if (!initialized || nativeScreenWidth <= 0 || nativeScreenHeight <= 0) return false;
        double resolvedX = anchor.x(nativeScreenWidth) + offsetX;
        double resolvedY = anchor.y(nativeScreenHeight) + offsetY;
        return Math.abs(resolvedX - nativeX) > 0.001 || Math.abs(resolvedY - nativeY) > 0.001;
    }

    public void sanitize() {
        if (displayName == null) displayName = "Unknown HUD Element";
        if (classification == null) classification = HudElementType.UNKNOWN;
        if (anchor == null) anchor = ScreenAnchor.TOP_LEFT;
        if (renderMode == null) renderMode = RenderMode.ORIGINAL;
        if (barSourceId == null) barSourceId = "";
        if (parentId == null || parentId.equals(id)) parentId = "";
        if (stackOnId == null || stackOnId.equals(id)) stackOnId = "";
        if (bar == null) bar = new BarStyle();
        bar.sanitizeColorHistory();
        scale = Math.max(0.25, Math.min(4.0, scale));
        nativeWidth = Math.max(1.0, nativeWidth);
        nativeHeight = Math.max(1.0, nativeHeight);
        bar.width = Math.max(8, Math.min(1024, bar.width));
        bar.height = Math.max(3, Math.min(512, bar.height));
        if (!Double.isFinite(bar.maximumPerSegment)) bar.maximumPerSegment = 2.0;
        bar.maximumPerSegment = Math.max(0.5, Math.min(1_000_000.0, bar.maximumPerSegment));
        if (!Double.isFinite(bar.widthPerMaximum)) bar.widthPerMaximum = 0.0;
        if (!Double.isFinite(bar.heightPerMaximum)) bar.heightPerMaximum = 0.0;
        bar.widthPerMaximum = Math.max(0.0, Math.min(1024.0, bar.widthPerMaximum));
        bar.heightPerMaximum = Math.max(0.0, Math.min(512.0, bar.heightPerMaximum));
        if (!Double.isFinite(bar.sizeBaselineMaximum) || bar.sizeBaselineMaximum <= 0.0) {
            bar.sizeBaselineMaximum = -1.0;
        }
        bar.borderThickness = Math.max(0, Math.min(bar.borderThickness, Math.min(bar.width, bar.height) / 2));
        for (int layer = 0; layer < 5; layer++) bar.layer(layer).sanitizeTransform();
        bar.trail.delayMillis = Math.max(0, Math.min(10_000, bar.trail.delayMillis));
        bar.trail.catchUpMillis = Math.max(1, Math.min(10_000, bar.trail.catchUpMillis));
        hideDelayMillis = Math.max(0, Math.min(60_000, hideDelayMillis));
        hideFadeMillis = Math.max(0, Math.min(60_000, hideFadeMillis));
        if (!Double.isFinite(stackOffsetX)) stackOffsetX = 0.0;
        if (!Double.isFinite(stackOffsetY)) stackOffsetY = 0.0;
        stackOffsetX = Math.max(-1_000_000.0, Math.min(1_000_000.0, stackOffsetX));
        stackOffsetY = Math.max(-1_000_000.0, Math.min(1_000_000.0, stackOffsetY));
    }
}
