package com.cappleapple.myhudnotyours.bar;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Tick-driven delayed fade-out for conditional bar visibility. */
public final class BarVisibilityAnimator {
    static final int MILLIS_PER_TICK = 50;

    private final Map<String, VisibilityState> states = new HashMap<>();

    public void tick(String elementId, boolean hideRequested, int delayMillis, int fadeMillis) {
        states.computeIfAbsent(elementId, ignored -> new VisibilityState())
                .tick(hideRequested, delayMillis, fadeMillis);
    }

    /**
     * Returns the interpolated visibility. A previously unseen hidden bar is
     * hidden immediately so loading a world does not briefly flash full bars.
     */
    public float render(String elementId, boolean hideRequested, float partialTick) {
        if (!hideRequested) return 1.0F;
        VisibilityState state = states.get(elementId);
        return state == null ? 0.0F : state.render(partialTick);
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

    public static final class VisibilityState {
        private boolean initialized;
        private boolean hideRequested;
        private float previousOpacity;
        private float opacity;
        private float fadeFrom = 1.0F;
        private int delayTicksRemaining;
        private int fadeTicksElapsed;
        private int fadeTicksTotal;

        /** Advances this state exactly once for one client tick. */
        public void tick(boolean nextHideRequested, int delayMillis, int fadeMillis) {
            if (!initialized) {
                initialized = true;
                hideRequested = nextHideRequested;
                opacity = nextHideRequested ? 0.0F : 1.0F;
                previousOpacity = opacity;
                return;
            }

            previousOpacity = opacity;
            if (!nextHideRequested) {
                hideRequested = false;
                opacity = 1.0F;
                resetTiming();
                return;
            }

            if (!hideRequested) {
                hideRequested = true;
                fadeFrom = opacity;
                delayTicksRemaining = millisecondsToTicks(delayMillis);
                fadeTicksElapsed = 0;
                fadeTicksTotal = millisecondsToTicks(fadeMillis);
                if (delayTicksRemaining == 0) advanceFade();
                return;
            }

            if (opacity <= 0.0F) {
                opacity = 0.0F;
                resetTiming();
                return;
            }
            if (delayTicksRemaining > 0) {
                delayTicksRemaining--;
                if (delayTicksRemaining > 0) return;
            }
            advanceFade();
        }

        /** Reads the tick state without advancing delay or fade timing. */
        public float render(float partialTick) {
            float partial = Math.max(0.0F, Math.min(1.0F, partialTick));
            return previousOpacity + (opacity - previousOpacity) * partial;
        }

        private void advanceFade() {
            if (fadeTicksTotal <= 0) {
                opacity = 0.0F;
                resetTiming();
                return;
            }
            fadeTicksElapsed = Math.min(fadeTicksTotal, fadeTicksElapsed + 1);
            opacity = fadeFrom * (1.0F - (float) fadeTicksElapsed / fadeTicksTotal);
            if (fadeTicksElapsed >= fadeTicksTotal) {
                opacity = 0.0F;
                resetTiming();
            }
        }

        private void resetTiming() {
            delayTicksRemaining = 0;
            fadeTicksElapsed = 0;
            fadeTicksTotal = 0;
            fadeFrom = opacity;
        }
    }
}
