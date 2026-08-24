package com.cappleapple.myhudnotyours.model;

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
    /** Hard bypass: the mod may list this element but must not intercept its renderer. */
    public boolean lockedToDefault = false;
    /** Customized elements may be independently suppressed in creative mode. */
    public boolean showInCreative = true;
    /** Semantic bars may disappear at either end of their value range. */
    public boolean hideWhenFull = false;
    public boolean hideWhenEmpty = false;
    public boolean initialized = false;
    public double nativeX = 0.0;
    public double nativeY = 0.0;
    public double nativeWidth = 40.0;
    public double nativeHeight = 12.0;
    public int nativeScreenWidth = 0;
    public int nativeScreenHeight = 0;

    public boolean semanticBar() {
        return classification == HudElementType.BAR && barSourceId != null && !barSourceId.isBlank();
    }

    public Bounds resolvedBounds(int screenWidth, int screenHeight) {
        double x = anchor.x(screenWidth) + offsetX;
        double y = anchor.y(screenHeight) + offsetY;
        double width = (renderMode == RenderMode.CUSTOM || renderMode == RenderMode.BOSS_BAR)
                ? bar.width * scale : nativeWidth * scale;
        double height = (renderMode == RenderMode.CUSTOM || renderMode == RenderMode.BOSS_BAR)
                ? bar.height * scale : nativeHeight * scale;
        return new Bounds(x, y, width, height);
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
        if (bar == null) bar = new BarStyle();
        bar.sanitizeColorHistory();
        scale = Math.max(0.25, Math.min(4.0, scale));
        nativeWidth = Math.max(1.0, nativeWidth);
        nativeHeight = Math.max(1.0, nativeHeight);
        bar.width = Math.max(8, Math.min(1024, bar.width));
        bar.height = Math.max(3, Math.min(512, bar.height));
        bar.borderThickness = Math.max(0, Math.min(bar.borderThickness, Math.min(bar.width, bar.height) / 2));
        bar.trail.delayMillis = Math.max(0, Math.min(10_000, bar.trail.delayMillis));
        bar.trail.catchUpMillis = Math.max(1, Math.min(10_000, bar.trail.catchUpMillis));
    }
}
