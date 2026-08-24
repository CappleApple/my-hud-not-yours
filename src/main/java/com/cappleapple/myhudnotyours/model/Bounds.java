package com.cappleapple.myhudnotyours.model;

/** Immutable GUI-coordinate rectangle. */
public record Bounds(double x, double y, double width, double height) {
    public Bounds {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    public Bounds translated(double dx, double dy) {
        return new Bounds(x + dx, y + dy, width, height);
    }
}
