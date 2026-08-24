package com.cappleapple.myhudnotyours.model;

public enum RenderMode {
    ORIGINAL,
    CUSTOM,
    BOSS_BAR,
    HIDDEN;

    public RenderMode next(boolean semanticBar) {
        RenderMode[] values = semanticBar ? values() : new RenderMode[]{ORIGINAL, HIDDEN};
        for (int index = 0; index < values.length; index++) {
            if (values[index] == this) return values[(index + 1) % values.length];
        }
        return ORIGINAL;
    }
}
