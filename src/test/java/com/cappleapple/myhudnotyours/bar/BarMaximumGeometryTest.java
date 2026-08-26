package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BarMaximumGeometryTest {
    @Test
    void addsConfiguredPixelsForMaximumAboveBaseline() {
        assertEquals(160.0, BarMaximumGeometry.size(120.0, 2.0, 20.0, 40.0));
        assertEquals(22.0, BarMaximumGeometry.size(12.0, 0.5, 20.0, 40.0));
    }

    @Test
    void missingOrLowerMaximumKeepsBaseSize() {
        assertEquals(120.0, BarMaximumGeometry.size(120.0, 2.0, -1.0, 40.0));
        assertEquals(120.0, BarMaximumGeometry.size(120.0, 2.0, 20.0, 10.0));
        assertEquals(120.0, BarMaximumGeometry.size(120.0, 0.0, 20.0, 40.0));
    }

    @Test
    void pathologicalModdedMaximumCannotCreateUnboundedGuiGeometry() {
        assertEquals(BarMaximumGeometry.MAX_DYNAMIC_SIZE,
                BarMaximumGeometry.size(120.0, 1024.0, 1.0, Double.MAX_VALUE));
    }
}
