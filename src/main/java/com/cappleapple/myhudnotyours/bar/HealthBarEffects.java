package com.cappleapple.myhudnotyours.bar;

import java.util.Objects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Vanilla heart presentation state carried by the semantic health source.
 *
 * <p>The custom bar deliberately keeps this separate from the numeric health
 * value: text still reports health/max health, while rendering can reserve a
 * distinct segment for absorption and tint the health segment like vanilla
 * poisoned, withered, and frozen hearts.</p>
 */
public record HealthBarEffects(VisualState visualState, double absorption) {
    // Opaque, high-chroma overrides keep automatic state readable over a
    // user-selected fill instead of producing a low-saturation blended color.
    public static final int POISON_OVERLAY = 0xFF57D63D;
    public static final int WITHER_OVERLAY = 0xFF544557;
    public static final int FROZEN_OVERLAY = 0xFF55D8FF;
    public static final int ABSORPTION_OVERLAY = 0xFFFFD83D;

    public HealthBarEffects {
        visualState = Objects.requireNonNullElse(visualState, VisualState.NORMAL);
        absorption = Math.max(0.0, absorption);
    }

    public static HealthBarEffects from(Player player) {
        VisualState state = select(player.hasEffect(MobEffects.POISON),
                player.hasEffect(MobEffects.WITHER), player.isFullyFrozen());
        return new HealthBarEffects(state, player.getAbsorptionAmount());
    }

    /** Matches vanilla's poison, then wither, then frozen precedence. */
    public static VisualState select(boolean poisoned, boolean withered, boolean frozen) {
        if (poisoned) return VisualState.POISONED;
        if (withered) return VisualState.WITHERED;
        if (frozen) return VisualState.FROZEN;
        return VisualState.NORMAL;
    }

    public int healthOverlayColor() {
        return switch (visualState) {
            case NORMAL -> 0;
            case POISONED -> POISON_OVERLAY;
            case WITHERED -> WITHER_OVERLAY;
            case FROZEN -> FROZEN_OVERLAY;
        };
    }

    /** Vanilla uses withered hearts, rather than gold hearts, for absorption while withered. */
    public int absorptionOverlayColor() {
        return visualState == VisualState.WITHERED ? WITHER_OVERLAY : ABSORPTION_OVERLAY;
    }

    public enum VisualState {
        NORMAL,
        POISONED,
        WITHERED,
        FROZEN
    }
}
