package com.cappleapple.myhudnotyours.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScreenAnchorTest {
    @Test
    void anchorChangePreservesElementPosition() {
        HudElementLayout layout = new HudElementLayout();
        layout.initialized = true;
        layout.anchor = ScreenAnchor.BOTTOM_RIGHT;
        layout.offsetX = -120;
        layout.offsetY = -30;
        Bounds before = layout.resolvedBounds(640, 360);

        layout.changeAnchor(ScreenAnchor.TOP_LEFT, 640, 360);

        Bounds after = layout.resolvedBounds(640, 360);
        assertEquals(before.x(), after.x(), 0.0001);
        assertEquals(before.y(), after.y(), 0.0001);
    }

    @Test
    void anchoredPlacementFollowsResolution() {
        HudElementLayout layout = new HudElementLayout();
        layout.initialized = true;
        layout.anchor = ScreenAnchor.BOTTOM_CENTER;
        layout.offsetX = -60;
        layout.offsetY = -25;

        Bounds small = layout.resolvedBounds(320, 180);
        Bounds large = layout.resolvedBounds(640, 360);

        assertEquals(160.0, large.x() - small.x(), 0.0001);
        assertEquals(180.0, large.y() - small.y(), 0.0001);
    }
}
