package com.cappleapple.myhudnotyours.discovery;

/** Vanilla hotbar-side bookkeeping applied when a semantic renderer is replaced. */
public final class HudStackingPolicy {
    static final int SINGLE_HEALTH_ROW_HEIGHT = 10;

    private HudStackingPolicy() {
    }

    public static int singleHealthRow(int heightBeforeHealth) {
        return heightBeforeHealth + SINGLE_HEALTH_ROW_HEIGHT;
    }
}
