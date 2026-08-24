package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.model.HudElementLayout;

/** Value-range visibility shared by original and replacement bar renderers. */
public final class BarVisibilityPolicy {
    private static final double EPSILON = 0.000001;

    private BarVisibilityPolicy() {
    }

    public static boolean hideForValue(HudElementLayout layout, NumericBarSnapshot snapshot) {
        if (layout.hideWhenEmpty && snapshot.current() <= snapshot.minimum() + EPSILON) return true;
        return layout.hideWhenFull && snapshot.current() >= snapshot.maximum() - EPSILON;
    }
}
