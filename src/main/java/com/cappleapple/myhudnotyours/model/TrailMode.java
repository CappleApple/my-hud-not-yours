package com.cappleapple.myhudnotyours.model;

public enum TrailMode {
    OFF,
    DECREASE_ONLY,
    INCREASE_ONLY,
    BOTH;

    public TrailMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean tracks(double oldValue, double newValue) {
        if (oldValue == newValue || this == OFF) return false;
        return this == BOTH
                || (this == DECREASE_ONLY && newValue < oldValue)
                || (this == INCREASE_ONLY && newValue > oldValue);
    }
}
