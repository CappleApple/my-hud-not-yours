package com.cappleapple.myhudnotyours.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.junit.jupiter.api.Test;

class HudSnapTargetsTest {
    @Test
    void lockedTargetUsesNativeBoundsRatherThanStoredCustomPlacement() {
        HudElementDefinition definition = KnownHudElements.definition(VanillaGuiLayers.PLAYER_HEALTH);
        HudElementLayout layout = new HudElementLayout();
        layout.lockedToDefault = true;
        layout.initializeFrom(new Bounds(69, 141, 81, 20), 320, 180);
        layout.move(75, -40);
        layout.scale = 2.0;

        Bounds target = HudSnapTargets.lockedDefault(definition, layout, 320, 180);
        assertEquals(new Bounds(69, 141, 81, 20), target);
    }

    @Test
    void lockedKnownTargetTracksResolutionAtItsDefaultAnchor() {
        HudElementDefinition definition = KnownHudElements.definition(VanillaGuiLayers.PLAYER_HEALTH);
        HudElementLayout layout = new HudElementLayout();
        layout.lockedToDefault = true;
        layout.initializeFrom(new Bounds(69, 141, 81, 20), 320, 180);

        Bounds target = HudSnapTargets.lockedDefault(definition, layout, 640, 360);
        assertEquals(new Bounds(229, 321, 81, 20), target);
    }

    @Test
    void unlockedElementDoesNotUseLockedDefaultResolver() {
        HudElementDefinition definition = KnownHudElements.definition(VanillaGuiLayers.PLAYER_HEALTH);
        HudElementLayout layout = new HudElementLayout();
        layout.initializeFrom(new Bounds(69, 141, 81, 10), 320, 180);
        assertNull(HudSnapTargets.lockedDefault(definition, layout, 320, 180));
    }
}
