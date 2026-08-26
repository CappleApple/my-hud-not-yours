package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.TrailMode;
import org.junit.jupiter.api.Test;

class ContextualBarAnimationTest {
    @Test
    void firstManaReappearanceTrailsFromContextuallyHiddenFullSample() {
        TrailAnimator trails = new TrailAnimator();
        BarVisibilityAnimator visibility = new BarVisibilityAnimator();
        NumericBarSnapshot fullHidden = mana(100.0, false);

        assertTrue(fullHidden.valueAvailable());
        assertTrue(BarVisibilityPolicy.hideForSource(fullHidden));
        visibility.tick("mana", true, 500, 500);
        trails.tick("mana", fullHidden.renderFraction(), TrailMode.DECREASE_ONLY,
                100, 500, true);

        NumericBarSnapshot spent = mana(60.0, true);
        assertFalse(BarVisibilityPolicy.hideForSource(spent));
        visibility.tick("mana", false, 500, 500);
        trails.tick("mana", spent.renderFraction(), TrailMode.DECREASE_ONLY,
                100, 500, false);

        assertEquals(1.0F, visibility.render("mana", false, 1.0F), 0.0001F);
        assertEquals(1.0, trails.render("mana", spent.renderFraction(), 1.0F), 0.0001);
    }

    @Test
    void contextualHideUsesConfiguredDelayAndFadeWithoutLosingTheSample() {
        BarVisibilityAnimator visibility = new BarVisibilityAnimator();
        NumericBarSnapshot visible = mana(60.0, true);
        NumericBarSnapshot fullHidden = mana(100.0, false);

        visibility.tick("mana", BarVisibilityPolicy.hideForSource(visible), 100, 100);
        visibility.tick("mana", BarVisibilityPolicy.hideForSource(fullHidden), 100, 100);
        assertEquals(1.0F, visibility.render("mana", true, 1.0F), 0.0001F);
        visibility.tick("mana", true, 100, 100);
        assertEquals(1.0F, visibility.render("mana", true, 1.0F), 0.0001F);
        visibility.tick("mana", true, 100, 100);
        assertTrue(visibility.render("mana", true, 1.0F) > 0.0F);

        visibility.tick("mana", false, 100, 100);
        assertEquals(1.0F, visibility.render("mana", false, 1.0F), 0.0001F);
    }

    private static NumericBarSnapshot mana(double current, boolean active) {
        return new NumericBarSnapshot(current, 0.0, 100.0,
                "Mana", null, active, null, true);
    }
}
