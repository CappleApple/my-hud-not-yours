package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BarIdleTrackerTest {
    @Test
    void firstSampleIsIdleAndNumericChangesAreDetectedOnce() {
        BarIdleTracker tracker = new BarIdleTracker();

        assertFalse(tracker.changedThisTick("mana", sample(100.0)));
        assertFalse(tracker.changedThisTick("mana", sample(100.0)));
        assertTrue(tracker.changedThisTick("mana", sample(80.0)));
        assertFalse(tracker.changedThisTick("mana", sample(80.0)));
    }

    @Test
    void healthEffectAndAbsorptionChangesCountAsActivity() {
        BarIdleTracker tracker = new BarIdleTracker();
        NumericBarSnapshot normal = health(20.0,
                new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, 0.0));
        NumericBarSnapshot absorbed = health(20.0,
                new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, 4.0));
        NumericBarSnapshot poisoned = health(20.0,
                new HealthBarEffects(HealthBarEffects.VisualState.POISONED, 4.0));

        assertFalse(tracker.changedThisTick("health", normal));
        assertTrue(tracker.changedThisTick("health", absorbed));
        assertTrue(tracker.changedThisTick("health", poisoned));
    }

    @Test
    void dataChangeRevealsImmediatelyThenSharedDelayStartsOnNextIdleTick() {
        BarIdleTracker idle = new BarIdleTracker();
        BarVisibilityAnimator visibility = new BarVisibilityAnimator();

        boolean initialIdle = !idle.changedThisTick("mana", sample(100.0));
        visibility.tick("mana", initialIdle, 500, 500);
        assertTrue(visibility.render("mana", initialIdle, 1.0F) <= 0.0001F);

        boolean changedIdle = !idle.changedThisTick("mana", sample(80.0));
        visibility.tick("mana", changedIdle, 500, 500);
        assertFalse(changedIdle);
        assertTrue(visibility.render("mana", changedIdle, 1.0F) >= 0.9999F);

        boolean nextIdle = !idle.changedThisTick("mana", sample(80.0));
        visibility.tick("mana", nextIdle, 500, 500);
        assertTrue(nextIdle);
        assertTrue(visibility.render("mana", nextIdle, 1.0F) >= 0.9999F);
    }

    private static NumericBarSnapshot sample(double current) {
        return new NumericBarSnapshot(current, 0.0, 100.0, "Mana", null, true);
    }

    private static NumericBarSnapshot health(double current, HealthBarEffects effects) {
        return new NumericBarSnapshot(current, 0.0, 20.0,
                "Health", null, true, effects);
    }
}
