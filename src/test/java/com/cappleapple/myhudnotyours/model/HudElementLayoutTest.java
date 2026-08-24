package com.cappleapple.myhudnotyours.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudElementLayoutTest {
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
}
