package com.cappleapple.myhudnotyours.model;

public final class NineSliceMargins {
    public int left = 3;
    public int top = 3;
    public int right = 3;
    public int bottom = 3;

    public void clamp(int textureWidth, int textureHeight) {
        left = Math.max(0, Math.min(left, textureWidth));
        right = Math.max(0, Math.min(right, Math.max(0, textureWidth - left)));
        top = Math.max(0, Math.min(top, textureHeight));
        bottom = Math.max(0, Math.min(bottom, Math.max(0, textureHeight - top)));
    }
}
