package com.cappleapple.myhudnotyours.model;

public enum LayerMode {
    NONE,
    SOLID,
    TEXTURE;

    public LayerMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
