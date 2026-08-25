package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.myhudnotyours.model.BarLayerStyle;
import com.cappleapple.myhudnotyours.model.Bounds;
import org.junit.jupiter.api.Test;

class BarLayerGeometryTest {
    @Test
    void localScaleIsCenteredAndOffsetsUseElementScale() {
        BarLayerStyle layer = new BarLayerStyle();
        layer.offsetX = 5.0;
        layer.offsetY = -2.0;
        layer.scaleX = 1.2;
        layer.scaleY = 0.5;

        assertEquals(new Bounds(10.0, 21.0, 120.0, 10.0),
                BarLayerGeometry.resolve(layer, 10.0, 20.0, 100.0, 20.0, 2.0));
    }

    @Test
    void overallElementTransformScalesEveryLayerPartTogether() {
        BarLayerStyle layer = new BarLayerStyle();
        layer.offsetX = 5.0;
        layer.offsetY = -2.0;
        layer.scaleX = 1.2;
        layer.scaleY = 0.5;

        Bounds normal = BarLayerGeometry.resolve(layer, 10.0, 20.0, 100.0, 20.0, 1.0);
        Bounds doubled = BarLayerGeometry.resolve(layer, 20.0, 40.0, 200.0, 40.0, 2.0);
        assertEquals(normal.x() * 2.0, doubled.x(), 0.0001);
        assertEquals(normal.y() * 2.0, doubled.y(), 0.0001);
        assertEquals(normal.width() * 2.0, doubled.width(), 0.0001);
        assertEquals(normal.height() * 2.0, doubled.height(), 0.0001);
    }
}
