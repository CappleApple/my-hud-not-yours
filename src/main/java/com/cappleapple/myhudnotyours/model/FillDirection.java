package com.cappleapple.myhudnotyours.model;

public enum FillDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    BOTTOM_TO_TOP,
    TOP_TO_BOTTOM;

    public FillDirection next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
