package com.cappleapple.myhudnotyours.compat.irons_spellbooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.bar.BarVisibilityPolicy;
import com.cappleapple.myhudnotyours.bar.NumericBarSnapshot;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import org.junit.jupiter.api.Test;

class IronsSpellbooksManaSourceTest {
    @Test
    void increasedFractionalMaximumStillHidesAtIronsDisplayedFullValue() {
        int maximum = IronsSpellbooksManaSource.displayedMaximum(150.75);
        NumericBarSnapshot full = new NumericBarSnapshot(150.0, 0.0, maximum,
                "Mana", null, true, null, true);
        HudElementLayout layout = new HudElementLayout();
        layout.hideWhenFull = true;

        assertEquals(150, maximum);
        assertEquals(1.0, full.fraction());
        assertTrue(BarVisibilityPolicy.hideForValue(layout, full));
    }
}
