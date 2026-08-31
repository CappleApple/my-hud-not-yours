package com.cappleapple.myhudnotyours.bar;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Tick-sampled semantic data changes used by the Show on idle option. */
public final class BarIdleTracker {
    private final Map<String, Sample> samples = new HashMap<>();

    /**
     * Returns true only when an already-observed numeric/effect sample changed.
     * The first sample is an idle baseline so bars do not flash on world load.
     */
    public boolean changedThisTick(String elementId, NumericBarSnapshot snapshot) {
        Sample next = Sample.from(snapshot);
        Sample previous = samples.put(elementId, next);
        return previous != null && !previous.equals(next);
    }

    public void retainOnly(Set<String> activeElementIds) {
        samples.keySet().removeIf(id -> !activeElementIds.contains(id));
    }

    public void clear() {
        samples.clear();
    }

    private record Sample(double current, double minimum, double maximum,
                          HealthBarEffects healthEffects) {
        private static Sample from(NumericBarSnapshot snapshot) {
            return new Sample(snapshot.current(), snapshot.minimum(), snapshot.maximum(),
                    snapshot.healthEffects());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Sample sample)) return false;
            return Double.compare(current, sample.current) == 0
                    && Double.compare(minimum, sample.minimum) == 0
                    && Double.compare(maximum, sample.maximum) == 0
                    && Objects.equals(healthEffects, sample.healthEffects);
        }

        @Override
        public int hashCode() {
            return Objects.hash(current, minimum, maximum, healthEffects);
        }
    }
}
