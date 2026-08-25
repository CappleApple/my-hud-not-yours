package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.TrailMode;
import org.junit.jupiter.api.Test;

class TrailAnimatorTest {
    @Test
    void convertsConfiguredMillisecondsToWholeClientTicks() {
        assertEquals(0, TrailAnimator.millisecondsToTicks(0));
        assertEquals(1, TrailAnimator.millisecondsToTicks(1));
        assertEquals(1, TrailAnimator.millisecondsToTicks(50));
        assertEquals(2, TrailAnimator.millisecondsToTicks(51));
        assertEquals(10, TrailAnimator.millisecondsToTicks(500));
        assertEquals(14, TrailAnimator.millisecondsToTicks(700));
    }

    @Test
    void decreaseTrailHoldsForConfiguredTicksThenCatchesUp() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 500, 500);
        state.tick(0.7, TrailMode.DECREASE_ONLY, 500, 500);

        for (int tick = 0; tick < 9; tick++) {
            state.tick(0.7, TrailMode.DECREASE_ONLY, 500, 500);
            assertEquals(1.0, state.render(1.0F), 0.0001);
        }

        state.tick(0.7, TrailMode.DECREASE_ONLY, 500, 500);
        double firstCatchTick = state.render(1.0F);
        assertTrue(firstCatchTick < 1.0 && firstCatchTick > 0.7);

        for (int tick = 1; tick < 10; tick++) {
            state.tick(0.7, TrailMode.DECREASE_ONLY, 500, 500);
        }
        assertEquals(0.7, state.render(1.0F), 0.0001);
    }

    @Test
    void renderSamplesNeverAdvanceDelayOrCatchUp() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 100, 700);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 100, 700);

        for (int frame = 0; frame < 1_000; frame++) {
            assertEquals(1.0, state.render((frame % 101) / 100.0F), 0.0001);
        }

        state.tick(0.4, TrailMode.DECREASE_ONLY, 100, 700);
        assertEquals(1.0, state.render(1.0F), 0.0001);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 100, 700);
        assertTrue(state.render(1.0F) < 1.0);
    }

    @Test
    void partialTickOnlyInterpolatesBetweenTickEndpoints() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 0, 500);
        state.tick(0.5, TrailMode.DECREASE_ONLY, 0, 500);

        double atTickStart = state.render(0.0F);
        double halfway = state.render(0.5F);
        double atTickEnd = state.render(1.0F);
        assertEquals(1.0, atTickStart, 0.0001);
        assertTrue(atTickEnd < halfway && halfway < atTickStart);
    }

    @Test
    void sevenHundredMillisecondCatchUpUsesFourteenTicks() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 0, 700);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 0, 700);

        for (int tick = 1; tick < 14; tick++) {
            assertTrue(state.render(1.0F) > 0.4);
            state.tick(0.4, TrailMode.DECREASE_ONLY, 0, 700);
        }
        assertEquals(0.4, state.render(1.0F), 0.0001);
    }

    @Test
    void repeatedDamageRestartsTickDelayAndPreservesOuterTrail() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 400, 500);
        state.tick(0.8, TrailMode.DECREASE_ONLY, 400, 500);
        for (int tick = 0; tick < 4; tick++) {
            state.tick(0.8, TrailMode.DECREASE_ONLY, 400, 500);
        }

        state.tick(0.6, TrailMode.DECREASE_ONLY, 400, 500);
        for (int tick = 0; tick < 7; tick++) {
            state.tick(0.6, TrailMode.DECREASE_ONLY, 400, 500);
            assertEquals(1.0, state.render(1.0F), 0.0001);
        }
        state.tick(0.6, TrailMode.DECREASE_ONLY, 400, 500);
        assertTrue(state.render(1.0F) < 1.0 && state.render(1.0F) > 0.6);
    }

    @Test
    void increaseOnlySnapsDamageAndTrailsHealing() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.INCREASE_ONLY, 0, 500);
        state.tick(0.5, TrailMode.INCREASE_ONLY, 0, 500);
        assertEquals(0.5, state.render(1.0F), 0.0001);

        state.tick(0.8, TrailMode.INCREASE_ONLY, 0, 500);
        assertTrue(state.render(1.0F) > 0.5 && state.render(1.0F) < 0.8);
    }

    @Test
    void decreaseOnlyHealingBelowOldHighRetargetsWithoutCancelingTrail() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 100, 500);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 100, 500);

        // Healing is an opposite-direction update, but 0.7 is still below the
        // retained high of 1.0. It must consume the next delay tick normally.
        state.tick(0.7, TrailMode.DECREASE_ONLY, 100, 500);
        assertEquals(1.0, state.render(1.0F), 0.0001);
        state.tick(0.7, TrailMode.DECREASE_ONLY, 100, 500);
        assertTrue(state.render(1.0F) < 1.0 && state.render(1.0F) > 0.7);

        for (int tick = 1; tick < 10; tick++) {
            state.tick(0.7, TrailMode.DECREASE_ONLY, 100, 500);
        }
        assertEquals(0.7, state.render(1.0F), 0.0001);
    }

    @Test
    void healingDuringCatchUpDoesNotSnapOrRestartItsSchedule() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 0, 500);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 0, 500);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 0, 500);
        double beforeHealing = state.render(1.0F);

        state.tick(0.7, TrailMode.DECREASE_ONLY, 0, 500);
        double afterHealing = state.render(1.0F);
        assertTrue(afterHealing <= beforeHealing);
        assertTrue(afterHealing > 0.7);

        // Three catch ticks have elapsed, including the healing tick. Seven
        // more complete the original ten-tick schedule.
        for (int tick = 3; tick < 10; tick++) {
            state.tick(0.7, TrailMode.DECREASE_ONLY, 0, 500);
        }
        assertEquals(0.7, state.render(1.0F), 0.0001);
    }

    @Test
    void healingBackToOldHighEndsTheExhaustedDecreaseTrail() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(1.0, TrailMode.DECREASE_ONLY, 500, 500);
        state.tick(0.4, TrailMode.DECREASE_ONLY, 500, 500);
        state.tick(1.0, TrailMode.DECREASE_ONLY, 500, 500);
        assertEquals(1.0, state.render(1.0F), 0.0001);
    }

    @Test
    void increaseOnlyDamageAboveOldLowRetargetsWithoutCancelingTrail() {
        TrailAnimator.TrailState state = new TrailAnimator.TrailState();
        state.tick(0.4, TrailMode.INCREASE_ONLY, 100, 500);
        state.tick(0.9, TrailMode.INCREASE_ONLY, 100, 500);

        // Damage is the opposite-direction update, but 0.6 is still above the
        // retained low of 0.4, so the upward trail and its timer remain active.
        state.tick(0.6, TrailMode.INCREASE_ONLY, 100, 500);
        assertEquals(0.4, state.render(1.0F), 0.0001);
        state.tick(0.6, TrailMode.INCREASE_ONLY, 100, 500);
        assertTrue(state.render(1.0F) > 0.4 && state.render(1.0F) < 0.6);

        for (int tick = 1; tick < 10; tick++) {
            state.tick(0.6, TrailMode.INCREASE_ONLY, 100, 500);
        }
        assertEquals(0.6, state.render(1.0F), 0.0001);
    }
}
