package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BarSegmentGeometryTest {
    @Test
    void createsOneSegmentPerConfiguredAmountOfMaximumHealth() {
        assertEquals(10, BarSegmentGeometry.segmentCount(20.0, 2.0));
        assertEquals(20, BarSegmentGeometry.segmentCount(40.0, 2.0));
        assertEquals(4, BarSegmentGeometry.segmentCount(20.0, 5.0));
        assertEquals(11, BarSegmentGeometry.segmentCount(21.0, 2.0));
    }

    @Test
    void currentHealthAndAbsorptionDoNotChangeSegmentCount() {
        NumericBarSnapshot lowWithAbsorption = new NumericBarSnapshot(3.0, 0.0, 20.0,
                "Health", null, true,
                new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, 12.0));
        NumericBarSnapshot fullWithoutAbsorption = new NumericBarSnapshot(20.0, 0.0, 20.0,
                "Health", null, true,
                new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, 0.0));

        assertEquals(10, BarSegmentGeometry.segmentCount(lowWithAbsorption, 2.0));
        assertEquals(10, BarSegmentGeometry.segmentCount(fullWithoutAbsorption, 2.0));
    }

    @Test
    void invalidRangesFallBackSafelyAndHugeCountsAreBounded() {
        assertEquals(1, BarSegmentGeometry.segmentCount(0.0, 2.0));
        assertEquals(1, BarSegmentGeometry.segmentCount(20.0, 0.0));
        assertEquals(1, BarSegmentGeometry.segmentCount(Double.NaN, 2.0));
        assertEquals(BarSegmentGeometry.MAX_SEGMENTS,
                BarSegmentGeometry.segmentCount(1_000_000.0, 0.5));
    }

    @Test
    void roundedCellsCoverTheEntireAxisWithoutGaps() {
        int length = 17;
        int count = 10;
        assertEquals(0, BarSegmentGeometry.cellStart(length, 0, count));
        for (int index = 0; index < count - 1; index++) {
            assertEquals(BarSegmentGeometry.cellEnd(length, index, count),
                    BarSegmentGeometry.cellStart(length, index + 1, count));
        }
        assertEquals(length, BarSegmentGeometry.cellEnd(length, count - 1, count));
    }
}
