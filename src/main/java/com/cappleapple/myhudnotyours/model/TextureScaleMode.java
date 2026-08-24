package com.cappleapple.myhudnotyours.model;

public enum TextureScaleMode {
    STRETCH,
    TILE,
    NINE_SLICE;

    public TextureScaleMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
