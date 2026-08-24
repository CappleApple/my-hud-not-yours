package com.cappleapple.myhudnotyours.model;

public enum TextMode {
    OFF,
    CURRENT,
    CURRENT_MAX,
    PERCENTAGE,
    CUSTOM;

    public TextMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
