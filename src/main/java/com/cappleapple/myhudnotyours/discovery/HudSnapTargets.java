package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import net.minecraft.resources.ResourceLocation;

/** Resolves passive snap geometry without enabling renderer interception. */
public final class HudSnapTargets {
    private HudSnapTargets() {
    }

    public static Bounds lockedDefault(HudElementDefinition definition, HudElementLayout layout,
                                       int screenWidth, int screenHeight) {
        if (!layout.lockedToDefault) return null;
        ResourceLocation id = ResourceLocation.tryParse(definition.stableId());

        if (!layout.initialized) {
            return id == null ? null : KnownHudElements.fallbackBounds(id, screenWidth, screenHeight);
        }

        double x = layout.nativeX;
        double y = layout.nativeY;
        if (id != null && layout.nativeScreenWidth > 0 && layout.nativeScreenHeight > 0
                && (layout.nativeScreenWidth != screenWidth || layout.nativeScreenHeight != screenHeight)) {
            Bounds previous = KnownHudElements.fallbackBounds(id,
                    layout.nativeScreenWidth, layout.nativeScreenHeight);
            Bounds current = KnownHudElements.fallbackBounds(id, screenWidth, screenHeight);
            if (previous != null && current != null) {
                x += current.x() - previous.x();
                y += current.y() - previous.y();
            }
        }
        return new Bounds(x, y, Math.max(1.0, layout.nativeWidth), Math.max(1.0, layout.nativeHeight));
    }
}
