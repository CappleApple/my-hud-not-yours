package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.model.HudElementLayout;

/** Value-range visibility shared by original and replacement bar renderers. */
public final class BarVisibilityPolicy {
    private static final double EPSILON = 0.000001;

    private BarVisibilityPolicy() {
    }

    public static boolean hideForValue(HudElementLayout layout, NumericBarSnapshot snapshot) {
        if (snapshot.healthEffects() != null && snapshot.healthEffects().absorption() > EPSILON) {
            return false;
        }
        double fraction = snapshot.renderFraction();
        if (layout.hideWhenEmpty && fraction <= EPSILON) return true;
        return layout.hideWhenFull && fraction >= 1.0 - EPSILON;
    }

    /** The source has a valid value but its own contextual renderer wants to be hidden. */
    public static boolean hideForSource(NumericBarSnapshot snapshot) {
        return snapshot.valueAvailable() && !snapshot.active();
    }
}
