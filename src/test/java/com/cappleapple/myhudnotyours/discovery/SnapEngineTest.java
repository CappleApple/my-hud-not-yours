package com.cappleapple.myhudnotyours.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.Bounds;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapEngineTest {
    @Test
    void snapsElementCenterToScreenCenter() {
        SnapEngine.SnapResult result = SnapEngine.snap(new Bounds(147, 70, 10, 10), List.of(), 320, 180, 5);
        assertTrue(result.snappedX());
        assertEquals(3.0, result.deltaX(), 0.0001);
        assertEquals(160.0, result.guideX(), 0.0001);
    }

    @Test
    void snapsToNearbyElementEdge() {
        SnapEngine.SnapResult result = SnapEngine.snap(new Bounds(97, 50, 20, 10),
                List.of(new Bounds(120, 20, 20, 20)), 500, 300, 4);
        assertTrue(result.snappedX());
        assertEquals(3.0, result.deltaX(), 0.0001);
    }

    @Test
    void leavesDistantElementAlone() {
        SnapEngine.SnapResult result = SnapEngine.snap(new Bounds(81, 71, 11, 11), List.of(), 320, 180, 3);
        assertFalse(result.snappedX());
        assertFalse(result.snappedY());
    }
}
