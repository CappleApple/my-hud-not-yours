package com.cappleapple.myhudnotyours.mixin;

import com.cappleapple.myhudnotyours.discovery.HudFrameTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphics.class)
abstract class GuiGraphicsMixin {
    @Shadow
    public abstract PoseStack pose();

    @Inject(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"))
    private void myHudNotYours$recordFill(RenderType renderType, int x1, int y1, int x2, int y2,
                                               int z, int color, CallbackInfo callback) {
        HudFrameTracker.recordRect(pose().last().pose(), x1, y1, x2, y2);
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"))
    private void myHudNotYours$recordBlit(ResourceLocation texture, int x1, int x2, int y1, int y2,
                                               int z, float u1, float u2, float v1, float v2,
                                               CallbackInfo callback) {
        HudFrameTracker.recordTexture(texture);
        HudFrameTracker.recordRect(pose().last().pose(), x1, y1, x2, y2);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I", at = @At("HEAD"))
    private void myHudNotYours$recordString(Font font, String text, float x, float y, int color,
                                                 boolean shadow, CallbackInfoReturnable<Integer> callback) {
        if (text != null) HudFrameTracker.recordRect(pose().last().pose(), x, y, x + font.width(text), y + font.lineHeight);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I", at = @At("HEAD"))
    private void myHudNotYours$recordFormatted(Font font, FormattedCharSequence text, float x, float y,
                                                    int color, boolean shadow,
                                                    CallbackInfoReturnable<Integer> callback) {
        if (text != null) HudFrameTracker.recordRect(pose().last().pose(), x, y, x + font.width(text), y + font.lineHeight);
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD"))
    private void myHudNotYours$recordItem(LivingEntity entity, Level level, ItemStack stack,
                                               int x, int y, int seed, int z,
                                               CallbackInfo callback) {
        if (!stack.isEmpty()) HudFrameTracker.recordRect(pose().last().pose(), x, y, x + 16, y + 16);
    }
}
