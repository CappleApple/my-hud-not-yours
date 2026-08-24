package com.cappleapple.myhudnotyours.bar;

import net.minecraft.client.Minecraft;

public interface NumericBarSource {
    String id();

    String displayName();

    NumericBarSnapshot snapshot(Minecraft minecraft);
}
