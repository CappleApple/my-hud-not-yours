package com.cappleapple.myhudnotyours.model;

public final class TextStyle {
    public TextMode mode = TextMode.CURRENT_MAX;
    public String format = "{current} / {max}";
    public TextAlignment alignment = TextAlignment.CENTER;
    public int offsetX = 0;
    public int offsetY = 0;
    public float scale = 1.0F;
    public boolean shadow = true;
    public int color = 0xFFFFFFFF;
}
