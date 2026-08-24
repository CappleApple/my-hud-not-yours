package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.TrailMode;
import org.junit.jupiter.api.Test;

class TrailAnimatorTest {
    @Test
    void decreaseTrailHoldsThenCatchesUp() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        assertEquals(1.0, state.sample(1.0, 0, TrailMode.DECREASE_ONLY, 500, 500), 0.0001);
        assertEquals(1.0, state.sample(0.7, 100, TrailMode.DECREASE_ONLY, 500, 500), 0.0001);
        assertEquals(1.0, state.sample(0.7, 599, TrailMode.DECREASE_ONLY, 500, 500), 0.0001);
        assertEquals(1.0, state.sample(0.7, 600, TrailMode.DECREASE_ONLY, 500, 500), 0.0001);
        double halfway = state.sample(0.7, 850, TrailMode.DECREASE_ONLY, 500, 500);
        assertTrue(halfway < 1.0 && halfway > 0.7);
        assertEquals(0.7, state.sample(0.7, 1_100, TrailMode.DECREASE_ONLY, 500, 500), 0.0001);
    }

    @Test
    void repeatedDamageRestartsDelayAndPreservesHighTrail() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.sample(1.0, 0, TrailMode.DECREASE_ONLY, 400, 500);
        state.sample(0.8, 100, TrailMode.DECREASE_ONLY, 400, 500);
        state.sample(0.6, 350, TrailMode.DECREASE_ONLY, 400, 500);
        assertEquals(1.0, state.sample(0.6, 749, TrailMode.DECREASE_ONLY, 400, 500), 0.0001);
    }

    @Test
    void increaseOnlyDoesNotTrailDamage() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.sample(1.0, 0, TrailMode.INCREASE_ONLY, 500, 500);
        assertEquals(0.5, state.sample(0.5, 10, TrailMode.INCREASE_ONLY, 500, 500), 0.0001);
        assertEquals(0.5, state.sample(0.8, 20, TrailMode.INCREASE_ONLY, 500, 500), 0.0001);
    }

    @Test
    void firstSampleAfterMissedCatchUpWindowStillStartsFromOldValue() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.sample(1.0, 0, TrailMode.DECREASE_ONLY, 400, 500);
        state.sample(0.5, 100, TrailMode.DECREASE_ONLY, 400, 500);

        assertEquals(1.0, state.sample(0.5, 2_000, TrailMode.DECREASE_ONLY, 400, 500), 0.0001);
        double halfway = state.sample(0.5, 2_250, TrailMode.DECREASE_ONLY, 400, 500);
        assertTrue(halfway < 1.0 && halfway > 0.5);
        assertEquals(0.5, state.sample(0.5, 2_500, TrailMode.DECREASE_ONLY, 400, 500), 0.0001);
    }

    @Test
    void repeatedDamageAfterFrameGapPreservesTheVisibleOuterTrail() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.sample(1.0, 0, TrailMode.DECREASE_ONLY, 400, 500);
        state.sample(0.8, 100, TrailMode.DECREASE_ONLY, 400, 500);

        assertEquals(1.0, state.sample(0.6, 2_000, TrailMode.DECREASE_ONLY, 400, 500), 0.0001);
        assertEquals(1.0, state.sample(0.6, 2_399, TrailMode.DECREASE_ONLY, 400, 500), 0.0001);
        assertEquals(1.0, state.sample(0.6, 2_400, TrailMode.DECREASE_ONLY, 400, 500), 0.0001);
        double catchingUp = state.sample(0.6, 2_650, TrailMode.DECREASE_ONLY, 400, 500);
        assertTrue(catchingUp < 1.0 && catchingUp > 0.6);
    }
}
