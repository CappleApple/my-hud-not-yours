package com.cappleapple.myhudnotyours.model;

public final class TrailStyle {
    public TrailMode mode = TrailMode.DECREASE_ONLY;
    public int delayMillis = 500;
    public int catchUpMillis = 700;
    public BarLayerStyle layer = BarLayerStyle.solid(0xCCFFF2A8);
}
