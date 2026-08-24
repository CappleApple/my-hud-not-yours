package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.model.BarLayerStyle;
import com.cappleapple.myhudnotyours.model.BarStyle;
import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.FillDirection;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import com.cappleapple.myhudnotyours.model.LayerMode;
import com.cappleapple.myhudnotyours.model.RenderMode;
import com.cappleapple.myhudnotyours.model.TextAlignment;
import com.cappleapple.myhudnotyours.model.TextMode;
import com.cappleapple.myhudnotyours.model.TextureScaleMode;
import com.cappleapple.myhudnotyours.texture.ManagedTextureResolver;
import com.cappleapple.myhudnotyours.texture.TextureHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class CustomBarRenderer {
    private static final ResourceLocation BOSS_BACKGROUND = ResourceLocation.withDefaultNamespace("boss_bar/red_background");
    private static final ResourceLocation BOSS_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/red_progress");
    private static final TrailAnimator TRAILS = new TrailAnimator();

    private CustomBarRenderer() {
    }

    public static void render(GuiGraphics graphics, HudElementLayout layout, NumericBarSnapshot snapshot) {
        if (!snapshot.active()) return;
        Bounds bounds = layout.resolvedBounds(graphics.guiWidth(), graphics.guiHeight());
        int x = (int) Math.round(bounds.x());
        int y = (int) Math.round(bounds.y());
        int width = Math.max(1, (int) Math.round(bounds.width()));
        int height = Math.max(1, (int) Math.round(bounds.height()));
        double value = snapshot.fraction();
        long now = Util.getMillis();
        double trail = TRAILS.sample(layout.id, value, now, layout.bar.trail.mode,
                layout.bar.trail.delayMillis, layout.bar.trail.catchUpMillis);

        RenderSystem.enableBlend();
        if (layout.renderMode == RenderMode.BOSS_BAR) {
            renderBoss(graphics, layout.bar, x, y, width, height, value, trail);
        } else {
            renderCustom(graphics, layout.bar, x, y, width, height, value, trail);
        }
        RenderSystem.disableBlend();
        renderText(graphics, layout.bar, snapshot, x, y, width, height);
    }

    private static void renderCustom(GuiGraphics graphics, BarStyle style, int x, int y, int width, int height,
                                     double value, double trail) {
        drawLayer(graphics, style.background, x, y, width, height);
        int border = Math.max(0, Math.min((int) Math.round(style.borderThickness), Math.min(width, height) / 2));
        int innerX = x + border;
        int innerY = y + border;
        int innerWidth = Math.max(0, width - border * 2);
        int innerHeight = Math.max(0, height - border * 2);
        drawLayer(graphics, style.empty, innerX, innerY, innerWidth, innerHeight);
        if (trail > value) {
            drawFraction(graphics, style.trail.layer, innerX, innerY, innerWidth, innerHeight, style.fillDirection, 0.0, trail);
            drawFraction(graphics, style.filled, innerX, innerY, innerWidth, innerHeight, style.fillDirection, 0.0, value);
        } else {
            drawFraction(graphics, style.filled, innerX, innerY, innerWidth, innerHeight, style.fillDirection, 0.0, value);
            if (trail < value) {
                drawFraction(graphics, style.trail.layer, innerX, innerY, innerWidth, innerHeight, style.fillDirection, trail, value);
            }
        }
        if (style.frame.mode == LayerMode.SOLID) {
            drawSolidFrame(graphics, style.frame, x, y, width, height, Math.max(1, border));
        } else {
            drawLayer(graphics, style.frame, x, y, width, height);
        }
    }

    private static void renderBoss(GuiGraphics graphics, BarStyle style, int x, int y, int width, int height,
                                   double value, double trail) {
        drawBossSprite(graphics, BOSS_BACKGROUND, x, y, width, height);
        // The background and progress share a depth. Flush the background before
        // changing scissor/tint state so batching cannot replay it as a foreground draw.
        graphics.flush();
        if (trail > value) {
            tint(graphics, style.trail.layer);
            drawBossFraction(graphics, x, y, width, height, style.fillDirection, 0.0, trail);
            resetTint(graphics);
            drawBossFraction(graphics, x, y, width, height, style.fillDirection, 0.0, value);
        } else {
            drawBossFraction(graphics, x, y, width, height, style.fillDirection, 0.0, value);
            if (trail < value) {
                tint(graphics, style.trail.layer);
                drawBossFraction(graphics, x, y, width, height, style.fillDirection, trail, value);
                resetTint(graphics);
            }
        }
    }

    private static void drawBossFraction(GuiGraphics graphics, int x, int y, int width, int height,
                                         FillDirection direction, double from, double to) {
        Clip clip = clip(x, y, width, height, direction, from, to);
        if (clip.width <= 0 || clip.height <= 0) return;
        graphics.flush();
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        drawBossSprite(graphics, BOSS_PROGRESS, x, y, width, height);
        graphics.flush();
        graphics.disableScissor();
    }

    private static void drawBossSprite(GuiGraphics graphics, ResourceLocation sprite,
                                       int x, int y, int width, int height) {
        // The cropped blitSprite overload interprets the requested destination size
        // as a 182x5 source crop. Scale the complete vanilla sprite explicitly so
        // background, trail, and progress always occupy the identical rectangle.
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(width / 182.0F, height / 5.0F, 1.0F);
        graphics.blitSprite(sprite, 0, 0, 182, 5);
        graphics.pose().popPose();
    }

    private static void drawFraction(GuiGraphics graphics, BarLayerStyle layer, int x, int y, int width, int height,
                                     FillDirection direction, double from, double to) {
        Clip clip = clip(x, y, width, height, direction, from, to);
        if (clip.width <= 0 || clip.height <= 0) return;
        graphics.flush();
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        drawLayer(graphics, layer, x, y, width, height);
        graphics.flush();
        graphics.disableScissor();
    }

    private static Clip clip(int x, int y, int width, int height, FillDirection direction, double from, double to) {
        from = Math.max(0.0, Math.min(1.0, from));
        to = Math.max(from, Math.min(1.0, to));
        return switch (direction) {
            case LEFT_TO_RIGHT -> new Clip(x + (int) Math.floor(width * from), y,
                    (int) Math.ceil(width * to) - (int) Math.floor(width * from), height);
            case RIGHT_TO_LEFT -> new Clip(x + width - (int) Math.ceil(width * to), y,
                    (int) Math.ceil(width * to) - (int) Math.floor(width * from), height);
            case TOP_TO_BOTTOM -> new Clip(x, y + (int) Math.floor(height * from), width,
                    (int) Math.ceil(height * to) - (int) Math.floor(height * from));
            case BOTTOM_TO_TOP -> new Clip(x, y + height - (int) Math.ceil(height * to), width,
                    (int) Math.ceil(height * to) - (int) Math.floor(height * from));
        };
    }

    private static void drawLayer(GuiGraphics graphics, BarLayerStyle layer, int x, int y, int width, int height) {
        if (layer == null || layer.mode == LayerMode.NONE || width <= 0 || height <= 0) return;
        if (layer.mode == LayerMode.SOLID) {
            graphics.fill(x, y, x + width, y + height, withOpacity(layer.color, layer.opacity));
            return;
        }
        TextureHandle handle = ManagedTextureResolver.get().resolve(layer.texture, Util.getMillis());
        if (handle == null) return;
        tint(graphics, layer);
        switch (layer.textureScale) {
            case STRETCH -> blit(graphics, handle, x, y, width, height,
                    handle.sourceX(), handle.sourceY(), handle.sourceWidth(), handle.sourceHeight());
            case TILE -> tile(graphics, handle, x, y, width, height);
            case NINE_SLICE -> nineSlice(graphics, handle, layer, x, y, width, height);
        }
        resetTint(graphics);
    }

    private static void tile(GuiGraphics graphics, TextureHandle handle, int x, int y, int width, int height) {
        int tileWidth = Math.max(1, handle.sourceWidth());
        int tileHeight = Math.max(1, handle.sourceHeight());
        for (int dy = 0; dy < height; dy += tileHeight) {
            for (int dx = 0; dx < width; dx += tileWidth) {
                int pieceWidth = Math.min(tileWidth, width - dx);
                int pieceHeight = Math.min(tileHeight, height - dy);
                blit(graphics, handle, x + dx, y + dy, pieceWidth, pieceHeight,
                        handle.sourceX(), handle.sourceY(), pieceWidth, pieceHeight);
            }
        }
    }

    private static void nineSlice(GuiGraphics graphics, TextureHandle handle, BarLayerStyle layer,
                                  int x, int y, int width, int height) {
        layer.margins.clamp(handle.sourceWidth(), handle.sourceHeight());
        int left = Math.min(layer.margins.left, width / 2);
        int right = Math.min(layer.margins.right, Math.max(0, width - left));
        int top = Math.min(layer.margins.top, height / 2);
        int bottom = Math.min(layer.margins.bottom, Math.max(0, height - top));
        int sourceCenterWidth = Math.max(0, handle.sourceWidth() - layer.margins.left - layer.margins.right);
        int sourceCenterHeight = Math.max(0, handle.sourceHeight() - layer.margins.top - layer.margins.bottom);
        int centerWidth = Math.max(0, width - left - right);
        int centerHeight = Math.max(0, height - top - bottom);
        int sx = handle.sourceX();
        int sy = handle.sourceY();
        int sr = sx + handle.sourceWidth() - layer.margins.right;
        int sb = sy + handle.sourceHeight() - layer.margins.bottom;

        blit(graphics, handle, x, y, left, top, sx, sy, layer.margins.left, layer.margins.top);
        blit(graphics, handle, x + left, y, centerWidth, top, sx + layer.margins.left, sy, sourceCenterWidth, layer.margins.top);
        blit(graphics, handle, x + width - right, y, right, top, sr, sy, layer.margins.right, layer.margins.top);
        blit(graphics, handle, x, y + top, left, centerHeight, sx, sy + layer.margins.top, layer.margins.left, sourceCenterHeight);
        blit(graphics, handle, x + left, y + top, centerWidth, centerHeight, sx + layer.margins.left,
                sy + layer.margins.top, sourceCenterWidth, sourceCenterHeight);
        blit(graphics, handle, x + width - right, y + top, right, centerHeight, sr,
                sy + layer.margins.top, layer.margins.right, sourceCenterHeight);
        blit(graphics, handle, x, y + height - bottom, left, bottom, sx, sb, layer.margins.left, layer.margins.bottom);
        blit(graphics, handle, x + left, y + height - bottom, centerWidth, bottom,
                sx + layer.margins.left, sb, sourceCenterWidth, layer.margins.bottom);
        blit(graphics, handle, x + width - right, y + height - bottom, right, bottom,
                sr, sb, layer.margins.right, layer.margins.bottom);
    }

    private static void blit(GuiGraphics graphics, TextureHandle handle, int x, int y, int width, int height,
                             int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return;
        graphics.blit(handle.location(), x, y, width, height, (float) sourceX, (float) sourceY,
                sourceWidth, sourceHeight, handle.textureWidth(), handle.textureHeight());
    }

    private static void drawSolidFrame(GuiGraphics graphics, BarLayerStyle layer,
                                       int x, int y, int width, int height, int thickness) {
        int color = withOpacity(layer.color, layer.opacity);
        graphics.fill(x, y, x + width, y + Math.min(thickness, height), color);
        graphics.fill(x, Math.max(y, y + height - thickness), x + width, y + height, color);
        graphics.fill(x, y, x + Math.min(thickness, width), y + height, color);
        graphics.fill(Math.max(x, x + width - thickness), y, x + width, y + height, color);
    }

    private static void renderText(GuiGraphics graphics, BarStyle style, NumericBarSnapshot snapshot,
                                   int x, int y, int width, int height) {
        if (style.text.mode == TextMode.OFF) return;
        String current = number(snapshot.current());
        String maximum = number(snapshot.maximum());
        String percent = Integer.toString((int) Math.round(snapshot.fraction() * 100.0));
        String text = switch (style.text.mode) {
            case OFF -> "";
            case CURRENT -> current;
            case CURRENT_MAX -> current + " / " + maximum;
            case PERCENTAGE -> percent + "%";
            case CUSTOM -> style.text.format.replace("{current}", current).replace("{max}", maximum)
                    .replace("{percent}", percent).replace("{name}", snapshot.displayName());
        };
        if (text.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        float scale = Math.max(0.25F, Math.min(4.0F, style.text.scale));
        float textWidth = minecraft.font.width(text) * scale;
        float textX = switch (style.text.alignment) {
            case LEFT -> x;
            case CENTER -> x + (width - textWidth) / 2.0F;
            case RIGHT -> x + width - textWidth;
        };
        float textY = y + (height - minecraft.font.lineHeight * scale) / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(textX + style.text.offsetX, textY + style.text.offsetY, 2.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, text, 0, 0, style.text.color, style.text.shadow);
        graphics.pose().popPose();
    }

    private static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int withOpacity(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, opacity)));
        return (color & 0x00FFFFFF) | alpha << 24;
    }

    private static void tint(GuiGraphics graphics, BarLayerStyle layer) {
        int color = layer.color;
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F * layer.opacity);
    }

    private static void resetTint(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private record Clip(int x, int y, int width, int height) {
    }
}
