package com.cappleapple.myhudnotyours.bar;

/** Pure maximum-stat-to-size calculation used by runtime and editor bounds. */
public final class BarMaximumGeometry {
    public static final double MAX_DYNAMIC_SIZE = 1_000_000.0;

    private BarMaximumGeometry() {
    }

    public static double size(double baseSize, double sizePerMaximum,
                              double baselineMaximum, double observedMaximum) {
        double safeBase = Math.max(1.0, baseSize);
        if (!Double.isFinite(sizePerMaximum) || sizePerMaximum <= 0.0
                || !Double.isFinite(baselineMaximum) || baselineMaximum <= 0.0
                || !Double.isFinite(observedMaximum) || observedMaximum <= baselineMaximum) {
            return safeBase;
        }
        return Math.max(1.0, Math.min(MAX_DYNAMIC_SIZE,
                safeBase + (observedMaximum - baselineMaximum) * sizePerMaximum));
    }
}
