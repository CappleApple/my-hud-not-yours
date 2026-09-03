package com.cappleapple.myhudnotyours.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudElementLayoutTest {
    @Test
    void newLayoutsStartLockedToDefault() {
        assertTrue(new HudElementLayout().lockedToDefault);
    }

    @Test
    void untouchedNativePlacementIsNotMigratedAsCustomized() {
        HudElementLayout layout = new HudElementLayout();
        layout.initializeFrom(new Bounds(42.0, 73.0, 120.0, 12.0), 320, 180);

        assertFalse(layout.hasLegacyCustomization());
    }

    @Test
    void movedLegacyPlacementIsMigratedAsCustomized() {
        HudElementLayout layout = new HudElementLayout();
        layout.initializeFrom(new Bounds(42.0, 73.0, 120.0, 12.0), 320, 180);
        layout.offsetX += 1.0;

        assertTrue(layout.hasLegacyCustomization());
    }

    @Test
    void resetRestoresStrictVanillaPassthrough() {
        HudElementLayout layout = new HudElementLayout();
        layout.initializeFrom(new Bounds(42.0, 73.0, 120.0, 12.0), 320, 180);
        layout.customized = true;
        layout.renderMode = RenderMode.CUSTOM;
        layout.scale = 1.5;

        layout.resetPlacement(320, 180);

        assertFalse(layout.customized);
        assertFalse(layout.hasLegacyCustomization());
    }

    @Test
    void segmentedMaximumAmountIsSanitized() {
        HudElementLayout layout = new HudElementLayout();
        layout.bar.maximumPerSegment = Double.NaN;

        layout.sanitize();

        assertEquals(2.0, layout.bar.maximumPerSegment);
        layout.bar.maximumPerSegment = 0.0;
        layout.sanitize();
        assertEquals(0.5, layout.bar.maximumPerSegment);
    }

    @Test
    void dynamicBarSizeStartsAtObservedBaselineAndGrowsWithMaximum() {
        HudElementLayout layout = new HudElementLayout();
        layout.renderMode = RenderMode.CUSTOM;
        layout.bar.width = 120;
        layout.bar.height = 12;
        layout.bar.widthPerMaximum = 2.0;
        layout.bar.heightPerMaximum = 0.5;

        assertTrue(layout.observeBarMaximum(20.0));
        assertEquals(120.0, layout.resolvedBounds(320, 180).width());
        assertEquals(12.0, layout.resolvedBounds(320, 180).height());

        assertFalse(layout.observeBarMaximum(40.0));
        assertEquals(160.0, layout.resolvedBounds(320, 180).width());
        assertEquals(22.0, layout.resolvedBounds(320, 180).height());
    }

    @Test
    void disablingBothMaximumRatiosClearsTheBaselineForFutureUse() {
        HudElementLayout layout = new HudElementLayout();
        layout.bar.widthPerMaximum = 1.0;
        layout.observeBarMaximum(20.0);
        layout.bar.widthPerMaximum = 0.0;

        layout.dynamicSizingChanged();

        assertEquals(-1.0, layout.bar.sizeBaselineMaximum);
    }
}
