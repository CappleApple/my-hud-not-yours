package com.cappleapple.myhudnotyours.bar;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record NumericBarSnapshot(double current, double minimum, double maximum, String displayName,
                                 @Nullable ResourceLocation icon, boolean active,
                                 @Nullable HealthBarEffects healthEffects) {
    public NumericBarSnapshot(double current, double minimum, double maximum, String displayName,
                              @Nullable ResourceLocation icon, boolean active) {
        this(current, minimum, maximum, displayName, icon, active, null);
    }

    public double fraction() {
        double range = maximum - minimum;
        if (range <= 0.0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (current - minimum) / range));
    }

    /**
     * Fraction used by the renderer and trail. Absorption contributes effective
     * health within the ordinary maximum-health capacity, while the public
     * numeric value and text remain ordinary health/max health.
     */
    public double renderFraction() {
        if (healthEffects == null || healthEffects.absorption() <= 0.0) return fraction();
        double range = maximum - minimum;
        if (range <= 0.0) return 0.0;
        double health = Math.max(0.0, Math.min(range, current - minimum));
        return Math.max(0.0, Math.min(1.0, (health + healthEffects.absorption()) / range));
    }

    /** End of the ordinary-health segment inside {@link #renderFraction()}. */
    public double healthRenderFraction() {
        return fraction();
    }

    /**
     * Absorption follows ordinary health until the bar reaches its maximum;
     * overflow then remains visible by overlaying the end of the filled bar.
     */
    public double absorptionRenderStartFraction() {
        if (healthEffects == null || healthEffects.absorption() <= 0.0) return renderFraction();
        double range = maximum - minimum;
        if (range <= 0.0) return 0.0;
        double absorptionFraction = Math.min(1.0, healthEffects.absorption() / range);
        return Math.max(0.0, renderFraction() - absorptionFraction);
    }
}
