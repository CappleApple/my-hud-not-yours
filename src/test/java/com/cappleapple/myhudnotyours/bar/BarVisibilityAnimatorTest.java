package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BarVisibilityAnimatorTest {
    @Test
    void delaysThenFadesOnClientTicksOnly() {
        BarVisibilityAnimator.VisibilityState state = new BarVisibilityAnimator.VisibilityState();
        state.tick(false, 100, 200);

        state.tick(true, 100, 200);
        assertEquals(1.0F, state.render(1.0F), 0.0001F);
        state.tick(true, 100, 200);
        assertEquals(1.0F, state.render(1.0F), 0.0001F);
        state.tick(true, 100, 200);
        assertEquals(0.75F, state.render(1.0F), 0.0001F);
        assertEquals(0.875F, state.render(0.5F), 0.0001F);

        state.tick(true, 100, 200);
        state.tick(true, 100, 200);
        state.tick(true, 100, 200);
        assertEquals(0.0F, state.render(1.0F), 0.0001F);
    }

    @Test
    void becomingVisibleCancelsDelayOrFadeImmediately() {
        BarVisibilityAnimator.VisibilityState state = new BarVisibilityAnimator.VisibilityState();
        state.tick(false, 0, 200);
        state.tick(true, 0, 200);
        assertEquals(0.75F, state.render(1.0F), 0.0001F);

        state.tick(false, 0, 200);
        assertEquals(1.0F, state.render(1.0F), 0.0001F);
    }

    @Test
    void initiallyHiddenBarsDoNotFlashOnWorldLoad() {
        BarVisibilityAnimator animator = new BarVisibilityAnimator();
        animator.tick("health", true, 1_000, 1_000);

        assertEquals(0.0F, animator.render("health", true, 0.5F), 0.0001F);
        assertEquals(0.0F, animator.render("missing", true, 0.5F), 0.0001F);
        assertEquals(1.0F, animator.render("missing", false, 0.5F), 0.0001F);
    }
}
