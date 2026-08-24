package com.cappleapple.myhudnotyours.editor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Pixel-art HSV wheel with direct RGB/RGBA hex and opacity editing. */
public final class ColorPickerScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 370;
    private static final int MAX_PANEL_HEIGHT = 230;
    private static final int WHEEL_STEP = 2;

    private final Screen parent;
    private final Consumer<ColorSelection> committed;
    private final List<HitTarget> hitTargets = new ArrayList<>();
    private float hue;
    private float saturation;
    private float brightness;
    private float opacity;
    private EditBox hex;
    private boolean syncingHex;
    private boolean hexValid = true;
    private DragTarget dragging = DragTarget.NONE;

    public ColorPickerScreen(Screen parent, int color, float layerOpacity,
                             Consumer<ColorSelection> committed) {
        super(Component.literal("Choose HUD color"));
        this.parent = parent;
        this.committed = committed;
        float[] hsb = Color.RGBtoHSB((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        opacity = clamp01(layerOpacity * ((color >>> 24) & 0xFF) / 255.0F);
    }

    @Override
    protected void init() {
        int controlX = controlX();
        int controlWidth = controlWidth();
        int panelY = panelY();
        hex = new EditBox(font, controlX, panelY + 43, controlWidth, 18,
                Component.literal("Hex color"));
        hex.setMaxLength(9);
        hex.setFilter(ColorHexCodec::isPotentialInput);
        hex.setResponder(this::hexChanged);
        syncHex();
        addRenderableWidget(hex);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the live HUD visible and avoid vanilla's menu blur.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hitTargets.clear();
        graphics.fill(0, 0, width, height, 0x88060A0F);
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFA101720);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF536575);
        graphics.drawString(font, "Color picker", panelX + 10, panelY + 9, 0xFFFFFFFF, true);

        int wheelX = wheelX();
        int wheelY = panelY + 31;
        int wheelSize = wheelSize();
        renderWheel(graphics, wheelX, wheelY, wheelSize);
        graphics.drawString(font, "Color wheel", wheelX, panelY + 20, 0xFF9EADBA, false);

        int controlsX = controlX();
        int controlsWidth = controlWidth();
        graphics.drawString(font, "Hex  #RRGGBB / #RRGGBBAA", controlsX, panelY + 31,
                hexValid ? 0xFF9EADBA : 0xFFFF7777, false);
        renderPreview(graphics, controlsX, panelY + 67, controlsWidth, 22);
        renderSlider(graphics, Slider.HUE, controlsX, panelY + 105, controlsWidth);
        renderSlider(graphics, Slider.BRIGHTNESS, controlsX, panelY + 137, controlsWidth);
        renderSlider(graphics, Slider.OPACITY, controlsX, panelY + 169, controlsWidth);

        int buttonY = panelY + panelHeight - 25;
        int buttonWidth = (panelWidth - 24) / 2;
        button(graphics, panelX + 8, buttonY, buttonWidth, 18, "Cancel", this::onClose);
        button(graphics, panelX + 16 + buttonWidth, buttonY, buttonWidth, 18, "Use color", this::commit);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderWheel(GuiGraphics graphics, int x, int y, int size) {
        double radius = size / 2.0;
        double centerX = x + radius;
        double centerY = y + radius;
        for (int py = 0; py < size; py += WHEEL_STEP) {
            for (int px = 0; px < size; px += WHEEL_STEP) {
                double dx = px + WHEEL_STEP / 2.0 - radius;
                double dy = py + WHEEL_STEP / 2.0 - radius;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > radius) continue;
                float pixelHue = normalizedAngle((float) Math.atan2(dy, dx));
                float pixelSaturation = clamp01((float) (distance / radius));
                int color = Color.HSBtoRGB(pixelHue, pixelSaturation, brightness);
                graphics.fill(x + px, y + py, x + Math.min(size, px + WHEEL_STEP),
                        y + Math.min(size, py + WHEEL_STEP), color);
            }
        }
        double angle = hue * Math.PI * 2.0;
        int markerX = (int) Math.round(centerX + Math.cos(angle) * saturation * (radius - 2.0));
        int markerY = (int) Math.round(centerY + Math.sin(angle) * saturation * (radius - 2.0));
        graphics.renderOutline(markerX - 3, markerY - 3, 7, 7, 0xFF000000);
        graphics.renderOutline(markerX - 2, markerY - 2, 5, 5, 0xFFFFFFFF);
    }

    private void renderPreview(GuiGraphics graphics, int x, int y, int width, int height) {
        checker(graphics, x, y, width, height);
        graphics.fill(x, y, x + width, y + height, effectiveColor());
        graphics.renderOutline(x, y, width, height, 0xFF647484);
    }

    private void renderSlider(GuiGraphics graphics, Slider slider, int x, int y, int width) {
        String label = switch (slider) {
            case HUE -> "Hue  " + Math.round(hue * 360.0F) + "°";
            case BRIGHTNESS -> "Brightness  " + Math.round(brightness * 100.0F) + "%";
            case OPACITY -> "Opacity  " + Math.round(opacity * 100.0F) + "%";
        };
        graphics.drawString(font, label, x, y - 11, 0xFF9EADBA, false);
        if (slider == Slider.OPACITY) checker(graphics, x, y, width, 9);
        for (int offset = 0; offset < width; offset++) {
            float fraction = width <= 1 ? 0.0F : offset / (float) (width - 1);
            int color = switch (slider) {
                case HUE -> Color.HSBtoRGB(fraction, 1.0F, 1.0F);
                case BRIGHTNESS -> Color.HSBtoRGB(hue, saturation, fraction);
                case OPACITY -> (Math.round(fraction * 255.0F) << 24) | rgb();
            };
            graphics.fill(x + offset, y, x + offset + 1, y + 9, color);
        }
        float value = switch (slider) {
            case HUE -> hue;
            case BRIGHTNESS -> brightness;
            case OPACITY -> opacity;
        };
        int marker = x + Math.round(value * Math.max(0, width - 1));
        graphics.fill(marker - 1, y - 2, marker + 2, y + 11, 0xFF000000);
        graphics.fill(marker, y - 1, marker + 1, y + 10, 0xFFFFFFFF);
        graphics.renderOutline(x, y, width, 9, 0xFF647484);
    }

    private void hexChanged(String value) {
        if (syncingHex) return;
        var parsed = ColorHexCodec.parse(value, opacity);
        hexValid = parsed.isPresent();
        parsed.ifPresent(color -> {
            setRgb(color.rgb());
            opacity = clamp01(color.opacity());
        });
    }

    private void setRgb(int rgb) {
        float[] hsb = Color.RGBtoHSB((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF, null);
        if (hsb[1] > 0.0001F) hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    private void syncHex() {
        if (hex == null) return;
        syncingHex = true;
        hex.setValue(ColorHexCodec.formatRgb(rgb()));
        syncingHex = false;
        hexValid = true;
    }

    private void commit() {
        if (!hexValid) return;
        committed.accept(new ColorSelection(rgb(), opacity));
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int index = hitTargets.size() - 1; index >= 0; index--) {
                HitTarget target = hitTargets.get(index);
                if (!target.contains(mouseX, mouseY)) continue;
                target.action.run();
                return true;
            }
            if (insideWheel(mouseX, mouseY)) {
                clearHexFocus();
                dragging = DragTarget.WHEEL;
                updateWheel(mouseX, mouseY);
                return true;
            }
            Slider slider = sliderAt(mouseX, mouseY);
            if (slider != null) {
                clearHexFocus();
                dragging = DragTarget.valueOf(slider.name());
                updateSlider(slider, mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || dragging == DragTarget.NONE) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (dragging == DragTarget.WHEEL) updateWheel(mouseX, mouseY);
        else updateSlider(Slider.valueOf(dragging.name()), mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging != DragTarget.NONE) {
            dragging = DragTarget.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hex != null && hex.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateWheel(double mouseX, double mouseY) {
        double radius = wheelSize() / 2.0;
        double dx = mouseX - (wheelX() + radius);
        double dy = mouseY - (panelY() + 31 + radius);
        hue = normalizedAngle((float) Math.atan2(dy, dx));
        saturation = clamp01((float) (Math.sqrt(dx * dx + dy * dy) / radius));
        syncHex();
    }

    private void updateSlider(Slider slider, double mouseX) {
        float value = clamp01((float) ((mouseX - controlX()) / Math.max(1.0, controlWidth() - 1.0)));
        switch (slider) {
            case HUE -> hue = value;
            case BRIGHTNESS -> brightness = value;
            case OPACITY -> opacity = value;
        }
        syncHex();
    }

    private boolean insideWheel(double mouseX, double mouseY) {
        double radius = wheelSize() / 2.0;
        double dx = mouseX - (wheelX() + radius);
        double dy = mouseY - (panelY() + 31 + radius);
        return dx * dx + dy * dy <= radius * radius;
    }

    private Slider sliderAt(double mouseX, double mouseY) {
        if (mouseX < controlX() || mouseX >= controlX() + controlWidth()) return null;
        int relativeY = (int) Math.floor(mouseY - panelY());
        if (relativeY >= 102 && relativeY < 117) return Slider.HUE;
        if (relativeY >= 134 && relativeY < 149) return Slider.BRIGHTNESS;
        if (relativeY >= 166 && relativeY < 181) return Slider.OPACITY;
        return null;
    }

    private void clearHexFocus() {
        hex.setFocused(false);
        setFocused(null);
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height,
                        String label, Runnable action) {
        graphics.fill(x, y, x + width, y + height, 0xE02A3744);
        graphics.fill(x, y, x + width, y + 1, 0xFF5B6B7A);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2,
                y + (height - font.lineHeight) / 2 + 1, 0xFFFFFFFF, false);
        hitTargets.add(new HitTarget(x, y, width, height, action));
    }

    private int rgb() {
        return Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
    }

    private int effectiveColor() {
        return Math.round(opacity * 255.0F) << 24 | rgb();
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, Math.max(260, width - 12));
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, Math.max(210, height - 12));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int wheelSize() {
        return Math.min(132, Math.max(92, panelWidth() / 2 - 34));
    }

    private int wheelX() {
        return panelX() + 14;
    }

    private int controlX() {
        return wheelX() + wheelSize() + 18;
    }

    private int controlWidth() {
        return Math.max(86, panelX() + panelWidth() - controlX() - 12);
    }

    private static float normalizedAngle(float radians) {
        float result = (float) (radians / (Math.PI * 2.0));
        return result < 0.0F ? result + 1.0F : result;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static void checker(GuiGraphics graphics, int x, int y, int width, int height) {
        int size = 5;
        for (int py = 0; py < height; py += size) {
            for (int px = 0; px < width; px += size) {
                int color = ((px / size + py / size) & 1) == 0 ? 0xFF555E68 : 0xFF303740;
                graphics.fill(x + px, y + py, x + Math.min(width, px + size),
                        y + Math.min(height, py + size), color);
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public record ColorSelection(int rgb, float opacity) {
    }

    private enum Slider {
        HUE,
        BRIGHTNESS,
        OPACITY
    }

    private enum DragTarget {
        NONE,
        WHEEL,
        HUE,
        BRIGHTNESS,
        OPACITY
    }

    private record HitTarget(int x, int y, int width, int height, Runnable action) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
