package com.cappleapple.myhudnotyours.model;

public final class BarLayerStyle {
    public LayerMode mode = LayerMode.SOLID;
    public int color = 0xFFFFFFFF;
    public float opacity = 1.0F;
    public TextureReference texture = new TextureReference();
    public TextureScaleMode textureScale = TextureScaleMode.STRETCH;
    public NineSliceMargins margins = new NineSliceMargins();
    /** Layer-local translation in unscaled HUD pixels. */
    public double offsetX = 0.0;
    public double offsetY = 0.0;
    /** Layer-local size around the center of its normal bar region. */
    public double scaleX = 1.0;
    public double scaleY = 1.0;

    public static BarLayerStyle none() {
        BarLayerStyle style = new BarLayerStyle();
        style.mode = LayerMode.NONE;
        return style;
    }

    public static BarLayerStyle solid(int color) {
        BarLayerStyle style = new BarLayerStyle();
        style.mode = LayerMode.SOLID;
        style.color = color;
        return style;
    }

    public void resetTransform() {
        offsetX = 0.0;
        offsetY = 0.0;
        scaleX = 1.0;
        scaleY = 1.0;
    }

    public void sanitizeTransform() {
        offsetX = Math.max(-4096.0, Math.min(4096.0, offsetX));
        offsetY = Math.max(-4096.0, Math.min(4096.0, offsetY));
        scaleX = Math.max(0.05, Math.min(8.0, scaleX));
        scaleY = Math.max(0.05, Math.min(8.0, scaleY));
    }
}
