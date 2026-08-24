package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.HudElementLayout;
import org.junit.jupiter.api.Test;

class BarVisibilityPolicyTest {
    @Test
    void hidesOnlyAtEnabledRangeEndpoints() {
        HudElementLayout layout = new HudElementLayout();
        NumericBarSnapshot empty = snapshot(0.0);
        NumericBarSnapshot partial = snapshot(8.0);
        NumericBarSnapshot full = snapshot(20.0);

        assertFalse(BarVisibilityPolicy.hideForValue(layout, empty));
        assertFalse(BarVisibilityPolicy.hideForValue(layout, full));

        layout.hideWhenEmpty = true;
        assertTrue(BarVisibilityPolicy.hideForValue(layout, empty));
        assertFalse(BarVisibilityPolicy.hideForValue(layout, partial));

        layout.hideWhenFull = true;
        assertTrue(BarVisibilityPolicy.hideForValue(layout, full));
        assertFalse(BarVisibilityPolicy.hideForValue(layout, partial));
    }

    private static NumericBarSnapshot snapshot(double current) {
        return new NumericBarSnapshot(current, 0.0, 20.0, "Test", null, true);
    }
}
