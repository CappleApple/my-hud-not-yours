package com.cappleapple.myhudnotyours.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HudElementRelationsTest {
    @Test
    void linkingPreservesBoundsThenParentTransformComposesWithLocalMovement() {
        Map<String, HudElementLayout> layouts = new LinkedHashMap<>();
        HudElementLayout parent = layout("parent", 40.0, 50.0, 100.0, 10.0);
        HudElementLayout child = layout("child", 180.0, 80.0, 20.0, 8.0);
        layouts.put(parent.id, parent);
        layouts.put(child.id, child);
        Bounds before = HudElementRelations.resolve(child, layouts, 320, 180, ignored -> true).bounds();

        HudElementRelations.setParent(child, parent.id, layouts, 320, 180, ignored -> true);
        Bounds linked = HudElementRelations.resolve(child, layouts, 320, 180, ignored -> true).bounds();
        assertBounds(before, linked);

        parent.move(20.0, -5.0);
        parent.scale = 2.0;
        Bounds transformed = HudElementRelations.resolve(child, layouts, 320, 180, ignored -> true).bounds();
        assertEquals(340.0, transformed.x(), 0.0001);
        assertEquals(105.0, transformed.y(), 0.0001);
        assertEquals(40.0, transformed.width(), 0.0001);

        HudElementRelations.moveByScreen(child, 10.0, 6.0, layouts,
                320, 180, ignored -> true);
        Bounds moved = HudElementRelations.resolve(child, layouts, 320, 180, ignored -> true).bounds();
        assertEquals(transformed.x() + 10.0, moved.x(), 0.0001);
        assertEquals(transformed.y() + 6.0, moved.y(), 0.0001);
    }

    @Test
    void stackCentersAboveVisibleTargetAndUsesIndependentOffsets() {
        Map<String, HudElementLayout> layouts = new LinkedHashMap<>();
        HudElementLayout target = layout("target", 100.0, 80.0, 60.0, 12.0);
        HudElementLayout stacked = layout("stacked", 10.0, 20.0, 20.0, 8.0);
        stacked.stackOnId = target.id;
        stacked.stackOffsetX = 3.0;
        stacked.stackOffsetY = -4.0;
        layouts.put(target.id, target);
        layouts.put(stacked.id, stacked);

        Bounds visible = HudElementRelations.resolve(stacked, layouts, 320, 180,
                id -> id.equals(target.id)).bounds();
        assertEquals(123.0, visible.x(), 0.0001);
        assertEquals(68.0, visible.y(), 0.0001);

        Bounds hidden = HudElementRelations.resolve(stacked, layouts, 320, 180,
                ignored -> false).bounds();
        assertEquals(10.0, hidden.x(), 0.0001);
        assertEquals(20.0, hidden.y(), 0.0001);
    }

    @Test
    void relationCyclesAreRejectedAndResolvedSafely() {
        Map<String, HudElementLayout> layouts = new LinkedHashMap<>();
        HudElementLayout first = layout("first", 0.0, 0.0, 20.0, 8.0);
        HudElementLayout second = layout("second", 30.0, 0.0, 20.0, 8.0);
        first.parentId = second.id;
        layouts.put(first.id, first);
        layouts.put(second.id, second);

        assertTrue(HudElementRelations.wouldCreateCycle(second.id, first.id, layouts));
        assertFalse(HudElementRelations.wouldCreateCycle(first.id, second.id, layouts));
        HudElementRelations.setParent(second, first.id, layouts, 320, 180, ignored -> true);
        assertTrue(second.parentId.isBlank());

        second.stackOnId = first.id;
        Bounds resolved = HudElementRelations.resolve(first, layouts, 320, 180, ignored -> true).bounds();
        assertTrue(Double.isFinite(resolved.x()));
        assertTrue(Double.isFinite(resolved.y()));
    }

    private static HudElementLayout layout(String id, double x, double y, double width, double height) {
        HudElementLayout layout = new HudElementLayout();
        layout.id = id;
        layout.anchor = ScreenAnchor.TOP_LEFT;
        layout.offsetX = x;
        layout.offsetY = y;
        layout.nativeWidth = width;
        layout.nativeHeight = height;
        layout.initialized = true;
        return layout;
    }

    private static void assertBounds(Bounds expected, Bounds actual) {
        assertEquals(expected.x(), actual.x(), 0.0001);
        assertEquals(expected.y(), actual.y(), 0.0001);
        assertEquals(expected.width(), actual.width(), 0.0001);
        assertEquals(expected.height(), actual.height(), 0.0001);
    }
}
