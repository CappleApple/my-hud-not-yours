package com.cappleapple.myhudnotyours.model;

public enum TextAlignment {
    LEFT,
    CENTER,
    RIGHT;

    public TextAlignment next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
