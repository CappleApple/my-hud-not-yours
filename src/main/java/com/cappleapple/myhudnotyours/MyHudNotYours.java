package com.cappleapple.myhudnotyours;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MyHudNotYours.MOD_ID)
public final class MyHudNotYours {
    public static final String MOD_ID = "myhudnotyours";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MyHudNotYours(IEventBus ignored) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
