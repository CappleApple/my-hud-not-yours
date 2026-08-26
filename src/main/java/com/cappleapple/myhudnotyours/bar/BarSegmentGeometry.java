package com.cappleapple.myhudnotyours.bar;

/** Pure segment-count and pixel-boundary math for segmented bar textures. */
public final class BarSegmentGeometry {
    public static final int MAX_SEGMENTS = 4096;

    private BarSegmentGeometry() {
    }

    /** One segment is created for each configured amount of the source's maximum range. */
    public static int segmentCount(double maximumRange, double maximumPerSegment) {
        if (!Double.isFinite(maximumRange) || maximumRange <= 0.0
                || !Double.isFinite(maximumPerSegment) || maximumPerSegment <= 0.0) {
            return 1;
        }
        return Math.max(1, Math.min(MAX_SEGMENTS, (int) Math.ceil(maximumRange / maximumPerSegment)));
    }

    public static int segmentCount(NumericBarSnapshot snapshot, double maximumPerSegment) {
        return segmentCount(snapshot.maximum() - snapshot.minimum(), maximumPerSegment);
    }

    public static int cellStart(int length, int index, int count) {
        if (length <= 0 || count <= 0) return 0;
        int boundedIndex = Math.max(0, Math.min(count, index));
        return (int) ((long) length * boundedIndex / count);
    }

    public static int cellEnd(int length, int index, int count) {
        return cellStart(length, index + 1, count);
    }
}
