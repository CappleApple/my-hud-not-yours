package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.model.TrailMode;
import java.util.HashMap;
import java.util.Map;

public final class TrailAnimator {
    private final Map<String, TrailState> states = new HashMap<>();

    public double sample(String elementId, double value, long nowMillis, TrailMode mode,
                         int delayMillis, int catchUpMillis) {
        return states.computeIfAbsent(elementId, ignored -> new TrailState())
                .sample(value, nowMillis, mode, delayMillis, catchUpMillis);
    }

    public void clear() {
        states.clear();
    }

    public static final class TrailState {
        private boolean initialized;
        private double primary;
        private double trail;
        private long holdUntil;
        private long catchStart = -1L;
        private double catchFrom;

        public double sample(double value, long nowMillis, TrailMode mode,
                             int delayMillis, int catchUpMillis) {
            if (!initialized) {
                initialized = true;
                primary = trail = value;
                holdUntil = nowMillis;
                return value;
            }

            if (mode == TrailMode.OFF) {
                primary = trail = value;
                holdUntil = nowMillis;
                catchStart = -1L;
                return value;
            }

            // React to the newest source value before advancing an older
            // catch-up. Otherwise a frame gap can retroactively finish the old
            // animation and discard the visible outer trail just as another
            // tracked change arrives.
            if (Double.compare(value, primary) != 0) {
                if (mode.tracks(primary, value)) {
                    trail = value < primary ? Math.max(trail, primary) : Math.min(trail, primary);
                    holdUntil = nowMillis + Math.max(0, delayMillis);
                    catchStart = -1L;
                } else {
                    trail = value;
                    holdUntil = nowMillis;
                    catchStart = -1L;
                }
                primary = value;
            }

            advance(value, nowMillis, Math.max(1, catchUpMillis));
            return trail;
        }

        private void advance(double target, long nowMillis, int catchUpMillis) {
            if (Double.compare(trail, target) == 0 || nowMillis < holdUntil) return;
            if (catchStart < 0L) {
                // Animation time is rendered time, not time that elapsed while
                // this bar was not sampled. Starting at holdUntil could make
                // the first post-delay frame compute raw >= 1 and visibly snap.
                catchStart = nowMillis;
                catchFrom = trail;
            }
            double raw = Math.max(0.0, Math.min(1.0, (double) (nowMillis - catchStart) / catchUpMillis));
            double eased = raw * raw * (3.0 - 2.0 * raw);
            trail = catchFrom + (target - catchFrom) * eased;
            if (raw >= 1.0) {
                trail = target;
                catchStart = -1L;
            }
        }
    }
}
