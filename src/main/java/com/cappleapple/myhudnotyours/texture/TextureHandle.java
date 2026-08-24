package com.cappleapple.myhudnotyours.texture;

import net.minecraft.resources.ResourceLocation;

public record TextureHandle(ResourceLocation location, int textureWidth, int textureHeight,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                            boolean animated) {
}
