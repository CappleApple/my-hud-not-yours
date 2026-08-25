package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.model.TrailMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tick-driven delayed bar animation. Render calls only interpolate between the
 * two most recent tick states; they never advance the delay or catch-up clocks.
 */
public final class TrailAnimator {
    static final int MILLIS_PER_TICK = 50;

    private final Map<String, TrailState> states = new HashMap<>();

    public void tick(String elementId, double value, TrailMode mode,
                     int delayMillis, int catchUpMillis) {
        states.computeIfAbsent(elementId, ignored -> new TrailState())
                .tick(value, mode, delayMillis, catchUpMillis);
    }

    public double render(String elementId, double fallbackValue, float partialTick) {
        TrailState state = states.get(elementId);
        return state == null ? fallbackValue : state.render(partialTick);
    }

    public void retainOnly(Set<String> activeElementIds) {
        states.keySet().removeIf(id -> !activeElementIds.contains(id));
    }

    public void clear() {
        states.clear();
    }

    static int millisecondsToTicks(int milliseconds) {
        if (milliseconds <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE,
                ((long) milliseconds + MILLIS_PER_TICK - 1L) / MILLIS_PER_TICK);
    }

    public static final class TrailState {
        private boolean initialized;
        private TrailMode mode = TrailMode.OFF;
        private double primary;
        private double previousTrail;
        private double trail;
        private double catchFrom;
        private double outerValue;
        private int activeDirection;
        private int delayTicksRemaining;
        private int catchTicksElapsed;
        private int catchTicksTotal = 1;

        /** Advances this state exactly once for one client tick. */
        public void tick(double value, TrailMode nextMode, int delayMillis, int catchUpMillis) {
            if (!initialized) {
                initialized = true;
                mode = nextMode;
                snapTo(value);
                return;
            }

            previousTrail = trail;

            if (nextMode == TrailMode.OFF) {
                mode = nextMode;
                snapTo(value);
                return;
            }

            // Changing the trail rule invalidates the direction of any pending
            // animation. Start the newly selected rule from the live value.
            if (nextMode != mode) {
                mode = nextMode;
                snapTo(value);
                return;
            }

            if (Double.compare(value, primary) != 0) {
                if (mode.tracks(primary, value)) {
                    // Keep the visible outer edge when another tracked change
                    // arrives before the current trail has caught up.
                    trail = value < primary ? Math.max(trail, primary) : Math.min(trail, primary);
                    previousTrail = trail;
                    primary = value;
                    catchFrom = trail;
                    outerValue = trail;
                    activeDirection = value < trail ? -1 : 1;
                    delayTicksRemaining = millisecondsToTicks(delayMillis);
                    catchTicksElapsed = 0;
                    catchTicksTotal = Math.max(1, millisecondsToTicks(catchUpMillis));
                    if (delayTicksRemaining == 0) advanceCatchUp();
                    return;
                } else if (!retargetActiveTrail(value)) {
                    snapTo(value);
                    return;
                }
            }

            if (Double.compare(trail, primary) == 0) {
                resetTiming();
                return;
            }

            if (delayTicksRemaining > 0) {
                delayTicksRemaining--;
                if (delayTicksRemaining > 0) return;
            }
            advanceCatchUp();
        }

        /** Reads the tick state without modifying any animation timing. */
        public double render(float partialTick) {
            if (!initialized) return 0.0;
            double partial = Math.max(0.0, Math.min(1.0, partialTick));
            return previousTrail + (trail - previousTrail) * partial;
        }

        private void advanceCatchUp() {
            catchTicksElapsed = Math.min(catchTicksTotal, catchTicksElapsed + 1);
            double raw = (double) catchTicksElapsed / catchTicksTotal;
            double eased = raw * raw * (3.0 - 2.0 * raw);
            double candidate = catchFrom + (primary - catchFrom) * eased;
            if (activeDirection < 0) {
                // Healing can move the destination upward, but it must never
                // resurrect trail that has already caught down past that point.
                trail = Math.max(primary, Math.min(trail, candidate));
            } else if (activeDirection > 0) {
                trail = Math.min(primary, Math.max(trail, candidate));
            } else {
                trail = candidate;
            }
            if (catchTicksElapsed >= catchTicksTotal) {
                trail = primary;
                resetTiming();
            }
        }

        /**
         * Opposite-direction changes do not cancel an active one-way trail as
         * long as the live value remains inside its retained outer value.
         */
        private boolean retargetActiveTrail(double value) {
            if (activeDirection < 0 && value < outerValue) {
                primary = value;
                trail = Math.max(trail, value);
                return true;
            }
            if (activeDirection > 0 && value > outerValue) {
                primary = value;
                trail = Math.min(trail, value);
                return true;
            }
            return false;
        }

        private void snapTo(double value) {
            primary = value;
            previousTrail = value;
            trail = value;
            catchFrom = value;
            outerValue = value;
            resetTiming();
        }

        private void resetTiming() {
            delayTicksRemaining = 0;
            catchTicksElapsed = 0;
            catchTicksTotal = 1;
            catchFrom = trail;
            outerValue = trail;
            activeDirection = 0;
        }
    }
}
