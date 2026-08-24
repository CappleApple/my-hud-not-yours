package com.cappleapple.myhudnotyours.bar;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record NumericBarSnapshot(double current, double minimum, double maximum, String displayName,
                                 @Nullable ResourceLocation icon, boolean active) {
    public double fraction() {
        double range = maximum - minimum;
        if (range <= 0.0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (current - minimum) / range));
    }
}
