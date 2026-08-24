package com.cappleapple.myhudnotyours.model;

public final class BarLayerStyle {
    public LayerMode mode = LayerMode.SOLID;
    public int color = 0xFFFFFFFF;
    public float opacity = 1.0F;
    public TextureReference texture = new TextureReference();
    public TextureScaleMode textureScale = TextureScaleMode.STRETCH;
    public NineSliceMargins margins = new NineSliceMargins();

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
}
