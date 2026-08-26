package com.cappleapple.myhudnotyours.bar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthBarEffectsTest {
    @Test
    void matchesVanillaVisualStatePrecedence() {
        assertEquals(HealthBarEffects.VisualState.NORMAL,
                HealthBarEffects.select(false, false, false));
        assertEquals(HealthBarEffects.VisualState.FROZEN,
                HealthBarEffects.select(false, false, true));
        assertEquals(HealthBarEffects.VisualState.WITHERED,
                HealthBarEffects.select(false, true, true));
        assertEquals(HealthBarEffects.VisualState.POISONED,
                HealthBarEffects.select(true, true, true));
    }

    @Test
    void absorptionOccupiesItsOwnEffectiveHealthSegment() {
        NumericBarSnapshot snapshot = snapshot(10.0,
                new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, 4.0));

        assertEquals(0.5, snapshot.healthRenderFraction(), 0.000001);
        assertEquals(0.7, snapshot.renderFraction(), 0.000001);
        assertEquals(0.5, snapshot.absorptionRenderStartFraction(), 0.000001);
        assertEquals(0.5, snapshot.fraction(), 0.000001);
    }

    @Test
    void fullHealthStillShowsASeparateAbsorptionSegment() {
        NumericBarSnapshot snapshot = snapshot(20.0,
                new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, 4.0));

        assertEquals(1.0, snapshot.healthRenderFraction(), 0.000001);
        assertEquals(1.0, snapshot.renderFraction(), 0.000001);
        assertEquals(0.8, snapshot.absorptionRenderStartFraction(), 0.000001);
    }

    @Test
    void witherAlsoWithersAbsorptionButOtherStatesKeepItGold() {
        HealthBarEffects wither = new HealthBarEffects(HealthBarEffects.VisualState.WITHERED, 4.0);
        HealthBarEffects poison = new HealthBarEffects(HealthBarEffects.VisualState.POISONED, 4.0);

        assertEquals(wither.healthOverlayColor(), wither.absorptionOverlayColor());
        assertNotEquals(poison.healthOverlayColor(), poison.absorptionOverlayColor());
        assertEquals(HealthBarEffects.ABSORPTION_OVERLAY, poison.absorptionOverlayColor());
    }

    @Test
    void automaticOverlayColorsAreOpaqueAndVisuallyDistinct() {
        int[] colors = {
                HealthBarEffects.POISON_OVERLAY,
                HealthBarEffects.WITHER_OVERLAY,
                HealthBarEffects.FROZEN_OVERLAY,
                HealthBarEffects.ABSORPTION_OVERLAY
        };
        for (int color : colors) assertEquals(0xFF, color >>> 24);
        assertTrue(rgbDistance(HealthBarEffects.POISON_OVERLAY, HealthBarEffects.WITHER_OVERLAY) > 120);
        assertTrue(rgbDistance(HealthBarEffects.POISON_OVERLAY, HealthBarEffects.FROZEN_OVERLAY) > 150);
        assertTrue(rgbDistance(HealthBarEffects.FROZEN_OVERLAY, HealthBarEffects.ABSORPTION_OVERLAY) > 150);
    }

    @Test
    void negativeAbsorptionIsSanitized() {
        HealthBarEffects effects = new HealthBarEffects(HealthBarEffects.VisualState.NORMAL, -5.0);
        assertEquals(0.0, effects.absorption());
    }

    private static NumericBarSnapshot snapshot(double health, HealthBarEffects effects) {
        return new NumericBarSnapshot(health, 0.0, 20.0, "Health", null, true, effects);
    }

    private static double rgbDistance(int left, int right) {
        int red = (left >> 16 & 0xFF) - (right >> 16 & 0xFF);
        int green = (left >> 8 & 0xFF) - (right >> 8 & 0xFF);
        int blue = (left & 0xFF) - (right & 0xFF);
        return Math.sqrt(red * red + green * green + blue * blue);
    }
}
