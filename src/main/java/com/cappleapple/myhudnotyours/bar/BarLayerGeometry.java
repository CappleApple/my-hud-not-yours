package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.model.BarLayerStyle;
import com.cappleapple.myhudnotyours.model.Bounds;

/** Composes a layer-local transform with the containing HUD element scale. */
public final class BarLayerGeometry {
    private BarLayerGeometry() {
    }

    public static Bounds resolve(BarLayerStyle layer, double x, double y, double width, double height,
                                 double elementScale) {
        double scaledWidth = Math.max(1.0, width * layer.scaleX);
        double scaledHeight = Math.max(1.0, height * layer.scaleY);
        double transformedX = x + (width - scaledWidth) / 2.0 + layer.offsetX * elementScale;
        double transformedY = y + (height - scaledHeight) / 2.0 + layer.offsetY * elementScale;
        return new Bounds(transformedX, transformedY, scaledWidth, scaledHeight);
    }
}
