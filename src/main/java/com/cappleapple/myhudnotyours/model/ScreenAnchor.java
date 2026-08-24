package com.cappleapple.myhudnotyours.model;

public enum ScreenAnchor {
    TOP_LEFT(0.0, 0.0),
    TOP_CENTER(0.5, 0.0),
    TOP_RIGHT(1.0, 0.0),
    CENTER_LEFT(0.0, 0.5),
    CENTER(0.5, 0.5),
    CENTER_RIGHT(1.0, 0.5),
    BOTTOM_LEFT(0.0, 1.0),
    BOTTOM_CENTER(0.5, 1.0),
    BOTTOM_RIGHT(1.0, 1.0);

    private final double horizontal;
    private final double vertical;

    ScreenAnchor(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public double x(int screenWidth) {
        return screenWidth * horizontal;
    }

    public double y(int screenHeight) {
        return screenHeight * vertical;
    }

    public ScreenAnchor next() {
        ScreenAnchor[] anchors = values();
        return anchors[(ordinal() + 1) % anchors.length];
    }

    public static ScreenAnchor nearest(double x, double y, int screenWidth, int screenHeight) {
        ScreenAnchor best = TOP_LEFT;
        double distance = Double.MAX_VALUE;
        for (ScreenAnchor anchor : values()) {
            double dx = x - anchor.x(screenWidth);
            double dy = y - anchor.y(screenHeight);
            double candidate = dx * dx + dy * dy;
            if (candidate < distance) {
                distance = candidate;
                best = anchor;
            }
        }
        return best;
    }
}
