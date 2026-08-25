package com.cappleapple.myhudnotyours.editor;

import com.cappleapple.myhudnotyours.model.TextureReference;
import com.cappleapple.myhudnotyours.texture.ManagedTextureResolver;
import com.cappleapple.myhudnotyours.texture.TextureCatalog;
import com.cappleapple.myhudnotyours.texture.TextureHandle;
import com.cappleapple.myhudnotyours.texture.TextureImporter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class TextureBrowserScreen extends Screen {
    private static final int TILE_WIDTH = 112;
    private static final int TILE_HEIGHT = 82;
    private final Screen parent;
    private final Consumer<TextureReference> selected;
    private final List<HitTarget> hitTargets = new ArrayList<>();
    private EditBox search;
    private TextureCatalog.Category category = TextureCatalog.Category.ALL;
    private int scrollRows;
    private List<FormattedCharSequence> hoveredDetails = List.of();

    public TextureBrowserScreen(Screen parent, Consumer<TextureReference> selected) {
        super(Component.translatable("gui.myhudnotyours.texture_browser"));
        this.parent = parent;
        this.selected = selected;
    }

    @Override
    protected void init() {
        search = new EditBox(font, 8, 28, Math.max(120, width - 210), 18, Component.literal("Search textures"));
        search.setHint(Component.literal("Search namespace, path, or pack…"));
        addRenderableWidget(search);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This screen paints an opaque background itself. Vanilla's background
        // pass would otherwise blur everything already drawn before super.render().
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF00D1118);
        hitTargets.clear();
        hoveredDetails = List.of();
        graphics.drawString(font, "Texture Browser", 9, 9, 0xFFFFFFFF, true);
        button(graphics, width - 194, 27, 90, 19, "Import PNG", () ->
                TextureImporter.chooseAndImport(() -> scrollRows = 0));
        button(graphics, width - 100, 27, 91, 19, "No texture", () -> {
            selected.accept(new TextureReference());
            minecraft.setScreen(parent);
        });

        int tabX = 8;
        int tabY = 52;
        for (TextureCatalog.Category candidate : TextureCatalog.Category.values()) {
            int tabWidth = Math.max(54, font.width(candidate.displayName()) + 16);
            categoryButton(graphics, tabX, tabY, tabWidth, candidate);
            tabX += tabWidth + 4;
        }

        List<TextureCatalog.Entry> entries = filtered();
        int columns = Math.max(1, (width - 16) / TILE_WIDTH);
        int startY = 76;
        int visibleRows = Math.max(1, (height - startY - 22) / TILE_HEIGHT);
        int totalRows = (entries.size() + columns - 1) / columns;
        scrollRows = Math.max(0, Math.min(scrollRows, Math.max(0, totalRows - visibleRows)));
        int first = scrollRows * columns;
        int last = Math.min(entries.size(), first + visibleRows * columns);
        for (int index = first; index < last; index++) {
            int visibleIndex = index - first;
            int column = visibleIndex % columns;
            int row = visibleIndex / columns;
            int x = 8 + column * TILE_WIDTH;
            int y = startY + row * TILE_HEIGHT;
            renderEntry(graphics, entries.get(index), x, y, mouseX, mouseY);
        }
        graphics.drawString(font, entries.size() + " textures  •  active resources and imported files are cached on demand",
                9, height - 14, 0xFF83909E, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredDetails.isEmpty()) graphics.renderTooltip(font, hoveredDetails, mouseX, mouseY);
    }

    private void renderEntry(GuiGraphics graphics, TextureCatalog.Entry entry, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + TILE_WIDTH - 4 && mouseY >= y && mouseY < y + TILE_HEIGHT - 4;
        graphics.fill(x, y, x + TILE_WIDTH - 4, y + TILE_HEIGHT - 4, hovered ? 0xDD2D3B49 : 0xC91B242E);
        checker(graphics, x + 4, y + 4, 48, 48);
        TextureHandle handle = ManagedTextureResolver.get().resolve(entry.reference(), Util.getMillis());
        if (handle != null) {
            graphics.blit(handle.location(), x + 4, y + 4, 48, 48,
                    (float) handle.sourceX(), (float) handle.sourceY(), handle.sourceWidth(), handle.sourceHeight(),
                    handle.textureWidth(), handle.textureHeight());
            String dimensions = handle.sourceWidth() + "×" + handle.sourceHeight() + (handle.animated() ? "  animated" : "");
            graphics.drawString(font, dimensions, x + 56, y + 29, handle.animated() ? 0xFFFFC86B : 0xFF9BA8B5, false);
        } else {
            graphics.drawString(font, "unavailable", x + 56, y + 29, 0xFFFF7070, false);
        }
        if (hovered) hoveredDetails = tooltipDetails(entry, handle);
        graphics.drawString(font, ellipsis(entry.namespace(), 48), x + 56, y + 6, 0xFF8FE8FF, false);
        graphics.drawString(font, ellipsis(entry.sourcePack(), 48), x + 56, y + 17, 0xFF8794A2, false);
        graphics.drawString(font, ellipsis(entry.path(), TILE_WIDTH - 12), x + 5, y + 57, 0xFFE5EAF0, false);
        graphics.drawString(font, entry.category().displayName(), x + 5, y + 68, 0xFF7E8B99, false);
        hitTargets.add(new HitTarget(x, y, TILE_WIDTH - 4, TILE_HEIGHT - 4, () -> {
            selected.accept(entry.reference().copy());
            minecraft.setScreen(parent);
        }));
    }

    private List<FormattedCharSequence> tooltipDetails(TextureCatalog.Entry entry, TextureHandle handle) {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal(entry.displayId()).withStyle(ChatFormatting.AQUA));
        details.add(Component.literal("Reference: " + entry.reference().key()));
        details.add(Component.literal("Namespace: " + entry.namespace()));
        details.add(Component.literal("Path: " + entry.path()));
        details.add(Component.literal("Source pack: " + entry.sourcePack()));
        details.add(Component.literal("Category: " + entry.category().displayName()));
        if (handle == null) {
            details.add(Component.literal("Texture data unavailable").withStyle(ChatFormatting.RED));
        } else {
            details.add(Component.literal("Resolved as: " + handle.location()));
            details.add(Component.literal("Texture sheet: " + handle.textureWidth() + "×" + handle.textureHeight()));
            details.add(Component.literal("Source region: " + handle.sourceX() + ", " + handle.sourceY()
                    + " — " + handle.sourceWidth() + "×" + handle.sourceHeight()));
            details.add(Component.literal("Animated: " + (handle.animated() ? "Yes" : "No"))
                    .withStyle(handle.animated() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        }

        int tooltipWidth = Math.max(80, Math.min(420, width - 32));
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component detail : details) wrapped.addAll(font.split(detail, tooltipWidth));
        return List.copyOf(wrapped);
    }

    private List<TextureCatalog.Entry> filtered() {
        String query = search == null ? "" : search.getValue();
        return TextureCatalog.entries().stream()
                .filter(entry -> category == TextureCatalog.Category.ALL || entry.category() == category)
                .filter(entry -> entry.matches(query))
                .toList();
    }

    private void categoryButton(GuiGraphics graphics, int x, int y, int width, TextureCatalog.Category candidate) {
        boolean active = category == candidate;
        graphics.fill(x, y, x + width, y + 18, active ? 0xE03A778B : 0xD0232D38);
        graphics.drawString(font, candidate.displayName(), x + (width - font.width(candidate.displayName())) / 2,
                y + 5, active ? 0xFFFFFFFF : 0xFFD1D8E0, false);
        hitTargets.add(new HitTarget(x, y, width, 18, () -> {
            category = candidate;
            scrollRows = 0;
        }));
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label, Runnable action) {
        graphics.fill(x, y, x + width, y + height, 0xDD283542);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2,
                y + (height - font.lineHeight) / 2 + 1, 0xFFFFFFFF, false);
        hitTargets.add(new HitTarget(x, y, width, height, action));
    }

    private static void checker(GuiGraphics graphics, int x, int y, int width, int height) {
        int size = 6;
        for (int py = 0; py < height; py += size) {
            for (int px = 0; px < width; px += size) {
                int color = ((px / size + py / size) & 1) == 0 ? 0xFF4A515A : 0xFF2C323A;
                graphics.fill(x + px, y + py, x + Math.min(width, px + size), y + Math.min(height, py + size), color);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int index = hitTargets.size() - 1; index >= 0; index--) {
            HitTarget target = hitTargets.get(index);
            if (target.contains(mouseX, mouseY)) {
                target.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollRows = Math.max(0, scrollRows - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String ellipsis(String text, int maximumWidth) {
        if (font.width(text) <= maximumWidth) return text;
        String shortened = text;
        while (!shortened.isEmpty() && font.width(shortened + "…") > maximumWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }

    private record HitTarget(int x, int y, int width, int height, Runnable action) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
