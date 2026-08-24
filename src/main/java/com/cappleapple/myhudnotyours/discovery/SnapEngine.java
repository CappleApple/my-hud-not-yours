package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.model.Bounds;
import java.util.List;

public final class SnapEngine {
    private SnapEngine() {
    }

    public static SnapResult snap(Bounds moving, List<Bounds> others, int screenWidth, int screenHeight, double threshold) {
        AxisSnap x = closestAxis(
                new double[]{moving.x(), moving.centerX(), moving.right()},
                targets(others, screenWidth, true), threshold);
        AxisSnap y = closestAxis(
                new double[]{moving.y(), moving.centerY(), moving.bottom()},
                targets(others, screenHeight, false), threshold);
        return new SnapResult(x.delta, y.delta, x.target, y.target, x.snapped, y.snapped);
    }

    private static double[] targets(List<Bounds> others, int screenSize, boolean horizontal) {
        double[] result = new double[3 + others.size() * 3];
        result[0] = 0.0;
        result[1] = screenSize / 2.0;
        result[2] = screenSize;
        int index = 3;
        for (Bounds bounds : others) {
            result[index++] = horizontal ? bounds.x() : bounds.y();
            result[index++] = horizontal ? bounds.centerX() : bounds.centerY();
            result[index++] = horizontal ? bounds.right() : bounds.bottom();
        }
        return result;
    }

    private static AxisSnap closestAxis(double[] moving, double[] targets, double threshold) {
        double bestDistance = threshold + 1.0;
        double bestDelta = 0.0;
        double bestTarget = 0.0;
        for (double source : moving) {
            for (double target : targets) {
                double delta = target - source;
                double distance = Math.abs(delta);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestDelta = delta;
                    bestTarget = target;
                }
            }
        }
        return bestDistance <= threshold
                ? new AxisSnap(bestDelta, bestTarget, true)
                : new AxisSnap(0.0, 0.0, false);
    }

    private record AxisSnap(double delta, double target, boolean snapped) {
    }

    public record SnapResult(double deltaX, double deltaY, double guideX, double guideY,
                             boolean snappedX, boolean snappedY) {
        public static SnapResult none() {
            return new SnapResult(0.0, 0.0, 0.0, 0.0, false, false);
        }
    }
}
