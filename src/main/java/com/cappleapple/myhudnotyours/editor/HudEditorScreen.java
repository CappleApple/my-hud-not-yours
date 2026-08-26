package com.cappleapple.myhudnotyours.editor;

import com.cappleapple.myhudnotyours.bar.BarSourceRegistry;
import com.cappleapple.myhudnotyours.bar.NumericBarSource;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.discovery.HudElementDefinition;
import com.cappleapple.myhudnotyours.discovery.HudElementRegistry;
import com.cappleapple.myhudnotyours.discovery.HudElementRuntime;
import com.cappleapple.myhudnotyours.discovery.HudSnapTargets;
import com.cappleapple.myhudnotyours.discovery.SnapEngine;
import com.cappleapple.myhudnotyours.model.BarLayerStyle;
import com.cappleapple.myhudnotyours.model.BarStyle;
import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import com.cappleapple.myhudnotyours.model.HudElementType;
import com.cappleapple.myhudnotyours.model.LayerMode;
import com.cappleapple.myhudnotyours.model.NineSliceMargins;
import com.cappleapple.myhudnotyours.model.RenderMode;
import com.cappleapple.myhudnotyours.model.TextureReference;
import com.cappleapple.myhudnotyours.model.TextureScaleMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Transparent, live HUD design surface. */
public final class HudEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 190;
    private static final int LIST_WIDTH = 126;
    private static final int LOCKED_OUTLINE = 0xFF00E5FF;
    private static final ItemStack LOCKED_ICON = Items.BARRIER.getDefaultInstance();
    private static String rememberedSelectedId;
    private static boolean rememberedListPreview;
    private static final int[] COLOR_PRESETS = {
            0xFFFFFFFF, 0xFF10141C, 0xFFCC3333, 0xFFD68A32, 0xFF80D43B,
            0xFF48BDE8, 0xFF8267D8, 0xFFFFF2A8, 0xFFEE78B7
    };
    private final HudElementRegistry registry = HudElementRegistry.get();
    private final List<HitTarget> hitTargets = new ArrayList<>();
    private final List<NumericTarget> numericTargets = new ArrayList<>();
    private String selectedId;
    private boolean listPreviewSelection;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;
    private int listScroll;
    private int propertyScroll;
    private int propertyMaxScroll;
    private PropertyPage page = PropertyPage.BASIC;
    private int selectedLayer = 2;
    private SnapEngine.SnapResult snap = SnapEngine.SnapResult.none();
    private EditBox elementSearch;
    private String elementQuery = "";
    private EditBox numericEditor;
    private NumericTarget activeNumeric;

    public HudEditorScreen() {
        super(Component.translatable("gui.myhudnotyours.editor"));
    }

    @Override
    protected void init() {
        if (selectedId == null && !registry.discovered().isEmpty()) {
            if (rememberedSelectedId != null && registry.definition(rememberedSelectedId) != null) {
                selectedId = rememberedSelectedId;
                listPreviewSelection = rememberedListPreview;
            } else {
                select(registry.discovered().getFirst().stableId(), false);
            }
            revealSelectedRow();
        }
        elementSearch = new EditBox(font, 9, 45, LIST_WIDTH - 10, 17, Component.literal("Search elements"));
        elementSearch.setMaxLength(128);
        elementSearch.setHint(Component.literal("Search…"));
        elementSearch.setValue(elementQuery);
        elementSearch.setResponder(value -> {
            elementQuery = value;
            listScroll = 0;
        });
        // The list is rendered manually so it remains above the live HUD, but
        // the edit box must still be a real child for vanilla focus, keyboard,
        // clipboard, and narration dispatch.
        addWidget(elementSearch);
        numericEditor = new EditBox(font, 0, 0, 80, 17, Component.literal("Numeric value"));
        numericEditor.setMaxLength(32);
        numericEditor.setTextColor(0xFFE9EEF5);
        numericEditor.setVisible(false);
        activeNumeric = null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hitTargets.clear();
        numericTargets.clear();
        graphics.fill(0, 0, width, 20, 0xCC10151D);
        graphics.drawString(font, "HUD unlocked", 7, 6, 0xFF8FE8FF, true);
        graphics.drawString(font, "Drag to move  •  Wheel to scale  •  Ctrl bypasses snap  •  Shift is precise  •  Middle resets",
                88, 6, 0xFFE8EDF4, false);

        renderElementFeedback(graphics, mouseX, mouseY);
        renderSnapGuides(graphics);
        renderElementList(graphics, mouseX, mouseY);
        renderProperties(graphics, mouseX, mouseY);
        if (activeNumeric != null && numericTargets.stream()
                .noneMatch(target -> target.value.id.equals(activeNumeric.value.id))) {
            finishNumericEdit();
        }
    }

    private void renderElementFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
        HudElementDefinition selected = selectedId == null ? null : registry.definition(selectedId);
        if (listPreviewSelection && selected != null && !registry.layout(selected).lockedToDefault
                && !currentlyRendered(selected)) {
            Bounds bounds = editorBounds(selected);
            if (dragging || bounds.contains(mouseX, mouseY)) shadeBounds(graphics, bounds);
            outlineOutside(graphics, bounds, 0xFF8FE8FF);
            renderElementName(graphics, selected, bounds);
            return;
        }

        HudElementDefinition definition = highlightedElement(mouseX, mouseY);
        if (definition == null) return;
        Bounds bounds = editorBounds(definition);
        shadeBounds(graphics, bounds);
        renderElementName(graphics, definition, bounds);
    }

    private void shadeBounds(GuiGraphics graphics, Bounds bounds) {
        int left = (int) Math.floor(bounds.x());
        int top = (int) Math.floor(bounds.y());
        int right = (int) Math.ceil(bounds.right());
        int bottom = (int) Math.ceil(bounds.bottom());
        graphics.fill(left, top, right, bottom, 0x403A9BFF);
    }

    private void renderElementName(GuiGraphics graphics, HudElementDefinition definition, Bounds bounds) {
        HudElementLayout layout = registry.layout(definition);
        HudElementRuntime runtime = registry.runtime(definition.stableId());
        int left = (int) Math.floor(bounds.x());
        int top = (int) Math.floor(bounds.y());
        int bottom = (int) Math.ceil(bounds.bottom());
        String label = layout.displayName;
        int labelWidth = font.width(label) + 5;
        int labelX = Math.max(0, Math.min(width - labelWidth, left));
        int above = top - 11;
        int below = bottom + 2;
        int labelY = above >= 21 ? above : Math.min(height - 10, below);
        graphics.fill(labelX, labelY, labelX + labelWidth, labelY + 9, 0xD0121821);
        graphics.drawString(font, label, labelX + 2, labelY + 1, 0xFF8FE8FF, false);
        if (LayoutStore.get().debugMode) renderDebug(graphics, definition, runtime, bounds, labelX, labelY + 10);
    }

    private boolean currentlyRendered(HudElementDefinition definition) {
        HudElementRuntime runtime = registry.runtime(definition.stableId());
        return runtime != null && runtime.rendered()
                && System.currentTimeMillis() - runtime.lastSeenMillis() < 250L;
    }

    private HudElementDefinition highlightedElement(int mouseX, int mouseY) {
        HudElementDefinition selected = selectedId == null ? null : registry.definition(selectedId);
        if (dragging) {
            return selected != null && !registry.layout(selected).lockedToDefault ? selected : null;
        }
        if (mouseY <= 20 || mouseX <= LIST_WIDTH + 4 || mouseX >= width - PANEL_WIDTH - 4) return null;
        if (selected != null && !registry.layout(selected).lockedToDefault
                && editorBounds(selected).contains(mouseX, mouseY)) {
            return selected;
        }
        List<HudElementDefinition> hovered = registry.at(mouseX, mouseY, width, height);
        return hovered.isEmpty() ? null : hovered.getFirst();
    }

    private void renderDebug(GuiGraphics graphics, HudElementDefinition definition, HudElementRuntime runtime,
                             Bounds bounds, int x, int y) {
        HudElementLayout layout = registry.layout(definition);
        List<String> lines = new ArrayList<>();
        lines.add("id=" + definition.stableId());
        lines.add("source=" + definition.sourceNamespace() + " path=" + definition.renderPath());
        lines.add(String.format(Locale.ROOT, "bounds=%.0f,%.0f %.0fx%.0f type=%s order=%d", bounds.x(), bounds.y(),
                bounds.width(), bounds.height(), definition.classification(), runtime == null ? -1 : runtime.renderOrder()));
        lines.add("semantic=" + (layout.barSourceId.isBlank() ? "none" : layout.barSourceId));
        if (runtime != null && !runtime.textures().isEmpty()) lines.add("textures=" + runtime.textures());
        for (String line : lines) {
            int lineWidth = font.width(line) + 4;
            graphics.fill(x, y, x + lineWidth, y + 9, 0xE00A0E14);
            graphics.drawString(font, line, x + 2, y + 1, 0xFFC0FFD1, false);
            y += 9;
        }
    }

    private void renderSnapGuides(GuiGraphics graphics) {
        if (!dragging) return;
        if (snap.snappedX()) graphics.fill((int) Math.round(snap.guideX()), 20,
                (int) Math.round(snap.guideX()) + 1, height, 0xC0FF6EB4);
        if (snap.snappedY()) graphics.fill(0, (int) Math.round(snap.guideY()), width,
                (int) Math.round(snap.guideY()) + 1, 0xC0FF6EB4);
    }

    private void renderElementList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 4;
        int y = 26;
        int bottom = height - 6;
        graphics.fill(x, y, x + LIST_WIDTH, bottom, 0xD010151D);
        graphics.drawString(font, "Elements", x + 6, y + 6, 0xFFFFFFFF, true);
        elementSearch.setX(x + 5);
        elementSearch.setY(y + 19);
        elementSearch.setWidth(LIST_WIDTH - 10);
        elementSearch.render(graphics, mouseX, mouseY, 0.0F);
        y += 41;
        List<HudElementDefinition> definitions = new ArrayList<>(registry.discovered());
        String query = elementQuery.trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            definitions.removeIf(definition -> !matchesElementSearch(definition, query));
        }
        // List.sort is stable, so each group retains the registry's render order.
        definitions.sort((left, right) -> Integer.compare(
                elementListPriority(registry.layout(left)), elementListPriority(registry.layout(right))));
        int visible = Math.max(1, (bottom - y - 4) / 16);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, definitions.size() - visible)));
        for (int index = listScroll; index < definitions.size() && y + 15 <= bottom; index++) {
            HudElementDefinition definition = definitions.get(index);
            HudElementLayout layout = registry.layout(definition);
            boolean selected = definition.stableId().equals(selectedId);
            int rowY = y;
            graphics.fill(x + 3, rowY, x + LIST_WIDTH - 3, rowY + 14, selected ? 0xD0367185 : 0x80212833);
            if (layout.lockedToDefault) {
                graphics.renderOutline(x + 3, rowY, LIST_WIDTH - 6, 14, LOCKED_OUTLINE);
            } else if (layout.customized) {
                graphics.renderOutline(x + 3, rowY, LIST_WIDTH - 6, 14, 0xFFFFD84A);
            }
            int color = layout.renderMode == RenderMode.HIDDEN ? 0xFF89919C : 0xFFE8EDF4;
            int labelWidth = layout.lockedToDefault ? LIST_WIDTH - 31 : LIST_WIDTH - 14;
            graphics.drawString(font, ellipsis(layout.displayName, labelWidth), x + 7, rowY + 3, color, false);
            if (layout.lockedToDefault) {
                graphics.renderItem(LOCKED_ICON, x + LIST_WIDTH - 21, rowY - 1);
            }
            hitTargets.add(new HitTarget(x + 3, rowY, LIST_WIDTH - 6, 14,
                    () -> selectFromList(definition.stableId())));
            y += 16;
        }
    }

    private boolean matchesElementSearch(HudElementDefinition definition, String query) {
        HudElementLayout layout = registry.layout(definition);
        return layout.displayName.toLowerCase(Locale.ROOT).contains(query)
                || definition.stableId().toLowerCase(Locale.ROOT).contains(query)
                || definition.sourceNamespace().toLowerCase(Locale.ROOT).contains(query)
                || definition.classification().name().toLowerCase(Locale.ROOT).contains(query);
    }

    private static int elementListPriority(HudElementLayout layout) {
        if (layout.lockedToDefault) return 2;
        return layout.customized ? 0 : 1;
    }

    private void renderProperties(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = width - PANEL_WIDTH - 4;
        int y = 26;
        graphics.fill(x, y, width - 4, height - 6, 0xEA10151D);
        HudElementDefinition definition = selectedId == null ? null : registry.definition(selectedId);
        if (definition == null) {
            graphics.drawString(font, "Click a visible element", x + 9, y + 10, 0xFFFFFFFF, true);
            return;
        }
        HudElementLayout layout = registry.layout(definition);
        graphics.drawString(font, ellipsis(layout.displayName, PANEL_WIDTH - 18), x + 9, y + 8, 0xFFFFFFFF, true);
        graphics.drawString(font, definition.classification().name(), x + 9, y + 19, 0xFF8295A8, false);
        y += 33;

        button(graphics, x + 5, y, PANEL_WIDTH - 10, 17,
                "Lock to default: " + (layout.lockedToDefault ? "On" : "Off"),
                layout.lockedToDefault, () -> setLockedToDefault(layout, !layout.lockedToDefault));
        y += 22;
        if (layout.lockedToDefault) {
            propertyScroll = 0;
            propertyMaxScroll = 0;
            graphics.drawString(font, "Vanilla renderer locked.", x + 9, y + 2, LOCKED_OUTLINE, false);
            graphics.drawString(font, "Unlock to edit this element.", x + 9, y + 14, 0xFFB7C3CF, false);
            return;
        }

        int tabWidth = (PANEL_WIDTH - 10) / PropertyPage.values().length;
        for (PropertyPage candidate : PropertyPage.values()) {
            int tabX = x + 5 + candidate.ordinal() * tabWidth;
            button(graphics, tabX, y, tabWidth - 2, 15, candidate.shortName,
                    candidate == page, () -> {
                        page = candidate;
                        propertyScroll = 0;
                    });
        }
        y += 20;
        int contentTop = y;
        propertyMaxScroll = Math.max(0, propertyContentHeight(layout) - (height - 7 - contentTop));
        propertyScroll = Math.max(0, Math.min(propertyScroll, propertyMaxScroll));
        int firstPropertyTarget = hitTargets.size();
        int firstNumericTarget = numericTargets.size();
        graphics.flush();
        graphics.enableScissor(x + 4, contentTop, width - 5, height - 7);
        y -= propertyScroll;
        switch (page) {
            case BASIC -> renderBasic(graphics, layout, x + 7, y);
            case STYLE -> renderStyle(graphics, layout, x + 7, y);
            case TEXTURES -> renderTextures(graphics, layout, x + 7, y);
            case TRAIL -> renderTrail(graphics, layout, x + 7, y);
        }
        graphics.flush();
        graphics.disableScissor();
        hitTargets.subList(firstPropertyTarget, hitTargets.size()).removeIf(target ->
                target.y + target.height <= contentTop || target.y >= height - 7);
        numericTargets.subList(firstNumericTarget, numericTargets.size()).removeIf(target ->
                target.y + target.height <= contentTop || target.y >= height - 7);
    }

    private void renderBasic(GuiGraphics graphics, HudElementLayout layout, int x, int y) {
        int contentWidth = PANEL_WIDTH - 14;
        y = labeledCycle(graphics, x, y, contentWidth, "Mode", pretty(layout.renderMode), () -> {
            layout.renderMode = layout.renderMode.next(layout.semanticBar());
            changed();
        });
        y = labeledCycle(graphics, x, y, contentWidth, "Anchor", pretty(layout.anchor), () -> {
            layout.changeAnchor(layout.anchor.next(), width, height);
            changed();
        });
        y = labeledCycle(graphics, x, y, contentWidth, "In creative",
                layout.showInCreative ? "Show" : "Hide", () -> {
                    layout.showInCreative = !layout.showInCreative;
                    changed();
                });
        y = stepper(graphics, x, y, contentWidth, "Scale", numericValue("basic.scale",
                () -> layout.scale * 100.0, value -> layout.scale = value / 100.0,
                25.0, 400.0, 5.0, 0, "%"));
        y = stepper(graphics, x, y, contentWidth, "X", numericValue("basic.x",
                () -> layout.offsetX, value -> layout.offsetX = Math.round(value),
                -1_000_000.0, 1_000_000.0, 1.0, 0, ""));
        y = stepper(graphics, x, y, contentWidth, "Y", numericValue("basic.y",
                () -> layout.offsetY, value -> layout.offsetY = Math.round(value),
                -1_000_000.0, 1_000_000.0, 1.0, 0, ""));
        if (layout.classification == HudElementType.BAR) {
            NumericBarSource source = BarSourceRegistry.get(layout.barSourceId);
            y = labeledCycle(graphics, x, y, contentWidth, "Source",
                    source == null ? layout.barSourceId : source.displayName(), () -> {
                        layout.barSourceId = BarSourceRegistry.nextId(layout.barSourceId);
                        layout.resetObservedBarMaximum();
                        changed();
                    });
            y = labeledCycle(graphics, x, y, contentWidth, "When full",
                    layout.hideWhenFull ? "Hide" : "Show", () -> {
                        layout.hideWhenFull = !layout.hideWhenFull;
                        changed();
                    });
            y = labeledCycle(graphics, x, y, contentWidth, "When empty",
                    layout.hideWhenEmpty ? "Hide" : "Show", () -> {
                        layout.hideWhenEmpty = !layout.hideWhenEmpty;
                        changed();
                    });
            y = stepper(graphics, x, y, contentWidth, "Width", numericValue("basic.width",
                    () -> layout.bar.width, value -> layout.bar.width = (int) Math.round(value),
                    8.0, 1024.0, 4.0, 0, ""));
            y = stepper(graphics, x, y, contentWidth, "Height", numericValue("basic.height",
                    () -> layout.bar.height, value -> layout.bar.height = (int) Math.round(value),
                    3.0, 512.0, 1.0, 0, ""));
            y = stepper(graphics, x, y, contentWidth, "Width/Max", numericValue("basic.width_per_maximum",
                    () -> layout.bar.widthPerMaximum, value -> {
                        layout.bar.widthPerMaximum = value;
                        layout.dynamicSizingChanged();
                    }, 0.0, 1024.0, 0.25, 2, ""));
            y = stepper(graphics, x, y, contentWidth, "Height/Max", numericValue("basic.height_per_maximum",
                    () -> layout.bar.heightPerMaximum, value -> {
                        layout.bar.heightPerMaximum = value;
                        layout.dynamicSizingChanged();
                    }, 0.0, 512.0, 0.25, 2, ""));
            y = labeledCycle(graphics, x, y, contentWidth, "Fill", pretty(layout.bar.fillDirection), () -> {
                layout.bar.fillDirection = layout.bar.fillDirection.next(); changed();
            });
            y = labeledCycle(graphics, x, y, contentWidth, "Text", pretty(layout.bar.text.mode), () -> {
                layout.bar.text.mode = layout.bar.text.mode.next(); changed();
            });
            y = labeledCycle(graphics, x, y, contentWidth, "Align", pretty(layout.bar.text.alignment), () -> {
                layout.bar.text.alignment = layout.bar.text.alignment.next(); changed();
            });
            y = stepper(graphics, x, y, contentWidth, "Text scale", numericValue("basic.text_scale",
                    () -> layout.bar.text.scale * 100.0, value -> layout.bar.text.scale = (float) (value / 100.0),
                    25.0, 400.0, 10.0, 0, "%"));
            y = labeledCycle(graphics, x, y, contentWidth, "Shadow", layout.bar.text.shadow ? "On" : "Off", () -> {
                layout.bar.text.shadow = !layout.bar.text.shadow; changed();
            });
        }
        button(graphics, x, y + 3, contentWidth, 16, "Reset this element", false, () -> {
            layout.resetPlacement(width, height);
            resetChanged();
        });
        button(graphics, x, y + 23, contentWidth, 16,
                    "Debug details: " + (LayoutStore.get().debugMode ? "On" : "Off"), false, () -> {
                    LayoutStore.get().debugMode = !LayoutStore.get().debugMode;
                    LayoutStore.markDirty();
                });
    }

    private void renderStyle(GuiGraphics graphics, HudElementLayout layout, int x, int y) {
        int contentWidth = PANEL_WIDTH - 14;
        if (!layout.semanticBar()) {
            graphics.drawString(font, "Advanced styles require a", x + 3, y + 3, 0xFFABB5C0, false);
            graphics.drawString(font, "data-linked numeric bar.", x + 3, y + 14, 0xFFABB5C0, false);
            return;
        }
        BarLayerStyle layer = layout.bar.layer(selectedLayer);
        y = labeledCycle(graphics, x, y, contentWidth, "Layer", BarStyle.layerName(selectedLayer), () -> {
            selectedLayer = (selectedLayer + 1) % 5;
        });
        y = labeledCycle(graphics, x, y, contentWidth, "Kind", pretty(layer.mode), () -> {
            layer.mode = layer.mode.next(); changed();
        });
        y = labeledCycle(graphics, x, y, contentWidth, "Color", colorName(layer.color),
                () -> openColorPicker(layout, selectedLayer));
        renderPalette(graphics, layout.bar, layer, x, y, contentWidth);
        y += 24;
        y = stepper(graphics, x, y, contentWidth, "Opacity", numericValue("style.opacity." + selectedLayer,
                () -> layer.opacity * 100.0, value -> layer.opacity = (float) (value / 100.0),
                0.0, 100.0, 5.0, 0, "%"));
        y = stepper(graphics, x, y, contentWidth, "Layer X", numericValue("style.offset_x." + selectedLayer,
                () -> layer.offsetX, value -> layer.offsetX = value,
                -4096.0, 4096.0, 1.0, 1, ""));
        y = stepper(graphics, x, y, contentWidth, "Layer Y", numericValue("style.offset_y." + selectedLayer,
                () -> layer.offsetY, value -> layer.offsetY = value,
                -4096.0, 4096.0, 1.0, 1, ""));
        y = stepper(graphics, x, y, contentWidth, "Layer width", numericValue("style.scale_x." + selectedLayer,
                () -> layer.scaleX * 100.0, value -> layer.scaleX = value / 100.0,
                5.0, 800.0, 5.0, 0, "%"));
        y = stepper(graphics, x, y, contentWidth, "Layer height", numericValue("style.scale_y." + selectedLayer,
                () -> layer.scaleY * 100.0, value -> layer.scaleY = value / 100.0,
                5.0, 800.0, 5.0, 0, "%"));
        button(graphics, x, y, contentWidth, 17, "Reset layer transform", false, () -> {
            layer.resetTransform();
            changed();
        });
        y += 21;
        y = stepper(graphics, x, y, contentWidth, "Border", numericValue("style.border",
                () -> layout.bar.borderThickness, value -> layout.bar.borderThickness = (int) Math.round(value),
                0.0, 32.0, 1.0, 0, ""));
        y = labeledCycle(graphics, x, y, contentWidth, "Texture sizing", pretty(layer.textureScale), () -> {
            layer.textureScale = layer.textureScale.next(); changed();
        });
        if (layer.textureScale == TextureScaleMode.NINE_SLICE) {
            y = stepper(graphics, x, y, contentWidth, "9-slice edges",
                    marginValue("style.margins." + selectedLayer, layer.margins));
        } else if (layer.textureScale == TextureScaleMode.SEGMENTED) {
            y = stepper(graphics, x, y, contentWidth, "Max/Seg",
                    numericValue("style.maximum_per_segment",
                            () -> layout.bar.maximumPerSegment,
                            value -> layout.bar.maximumPerSegment = value,
                            0.5, 1_000_000.0, 0.5, 1, ""));
        }
    }

    private void renderTextures(GuiGraphics graphics, HudElementLayout layout, int x, int y) {
        int contentWidth = PANEL_WIDTH - 14;
        if (!layout.semanticBar()) {
            graphics.drawString(font, "Texture layers require a", x + 3, y + 3, 0xFFABB5C0, false);
            graphics.drawString(font, "data-linked numeric bar.", x + 3, y + 14, 0xFFABB5C0, false);
            return;
        }
        for (int index = 0; index < 5; index++) {
            int layerIndex = index;
            BarLayerStyle layer = layout.bar.layer(index);
            String assigned = layer.texture.assigned() ? shortTexture(layer.texture.id) : "Choose…";
            button(graphics, x, y, contentWidth, 18, BarStyle.layerName(index) + ": " + assigned,
                    selectedLayer == index, () -> openTextureBrowser(layout, layerIndex));
            y += 22;
        }
        BarLayerStyle selected = layout.bar.layer(selectedLayer);
        y += 3;
        button(graphics, x, y, contentWidth, 17, "Clear " + BarStyle.layerName(selectedLayer), false, () -> {
            selected.texture = new TextureReference();
            if (selected.mode == LayerMode.TEXTURE) selected.mode = LayerMode.SOLID;
            changed();
        });
        y += 21;
        y = labeledCycle(graphics, x, y, contentWidth, "Sizing", pretty(selected.textureScale), () -> {
            selected.textureScale = selected.textureScale.next(); changed();
        });
        if (selected.textureScale == TextureScaleMode.NINE_SLICE) {
            stepper(graphics, x, y, contentWidth, "Edges L/T/R/B",
                    marginValue("textures.margins." + selectedLayer, selected.margins));
        }
    }

    private void renderTrail(GuiGraphics graphics, HudElementLayout layout, int x, int y) {
        int contentWidth = PANEL_WIDTH - 14;
        if (!layout.semanticBar()) {
            graphics.drawString(font, "Change trails require a", x + 3, y + 3, 0xFFABB5C0, false);
            graphics.drawString(font, "data-linked numeric bar.", x + 3, y + 14, 0xFFABB5C0, false);
            return;
        }
        y = labeledCycle(graphics, x, y, contentWidth, "Mode", pretty(layout.bar.trail.mode), () -> {
            layout.bar.trail.mode = layout.bar.trail.mode.next(); changed();
        });
        y = stepper(graphics, x, y, contentWidth, "Delay", numericValue("trail.delay",
                () -> layout.bar.trail.delayMillis,
                value -> layout.bar.trail.delayMillis = (int) Math.round(value),
                0.0, 10_000.0, 100.0, 0, " ms"));
        y = stepper(graphics, x, y, contentWidth, "Catch-up", numericValue("trail.catch_up",
                () -> layout.bar.trail.catchUpMillis,
                value -> layout.bar.trail.catchUpMillis = (int) Math.round(value),
                100.0, 10_000.0, 100.0, 0, " ms"));
        BarLayerStyle layer = layout.bar.trail.layer;
        y = labeledCycle(graphics, x, y, contentWidth, "Layer", pretty(layer.mode), () -> {
            layer.mode = layer.mode.next(); changed();
        });
        y = labeledCycle(graphics, x, y, contentWidth, "Color", colorName(layer.color),
                () -> openColorPicker(layout, 4));
        renderPalette(graphics, layout.bar, layer, x, y, contentWidth);
        y += 24;
        y = stepper(graphics, x, y, contentWidth, "Opacity", numericValue("trail.opacity",
                () -> layer.opacity * 100.0, value -> layer.opacity = (float) (value / 100.0),
                0.0, 100.0, 5.0, 0, "%"));
        button(graphics, x, y, contentWidth, 18,
                layer.texture.assigned() ? "Texture: " + shortTexture(layer.texture.id) : "Choose trail texture…",
                false, () -> openTextureBrowser(layout, 4));
    }

    private void openTextureBrowser(HudElementLayout layout, int layerIndex) {
        selectedLayer = layerIndex;
        minecraft.setScreen(new TextureBrowserScreen(this, reference -> {
            BarLayerStyle layer = layout.bar.layer(layerIndex);
            layer.texture = reference.copy();
            if (reference.assigned()) layer.mode = LayerMode.TEXTURE;
            changed();
        }));
    }

    private void openColorPicker(HudElementLayout layout, int layerIndex) {
        selectedLayer = layerIndex;
        BarLayerStyle layer = layout.bar.layer(layerIndex);
        minecraft.setScreen(new ColorPickerScreen(this, layer.color, layer.opacity, selection -> {
            layer.color = 0xFF000000 | selection.rgb();
            layer.opacity = selection.opacity();
            layout.bar.rememberColor(layer.color);
            changed();
        }));
    }

    private int labeledCycle(GuiGraphics graphics, int x, int y, int width, String label, String value, Runnable action) {
        graphics.drawString(font, label, x + 2, y + 5, 0xFF9DAAB8, false);
        button(graphics, x + 69, y, width - 69, 17, value, false, action);
        return y + 21;
    }

    private int stepper(GuiGraphics graphics, int x, int y, int width, String label, NumericValue value) {
        graphics.drawString(font, label, x + 2, y + 5, 0xFF9DAAB8, false);
        button(graphics, x + 69, y, 18, 17, "−", false, () -> value.adjust().accept(-1));
        int fieldX = x + 89;
        int fieldWidth = width - 109;
        NumericTarget target = new NumericTarget(fieldX, y, fieldWidth, 17, value);
        numericTargets.add(target);
        if (activeNumeric != null && activeNumeric.value().id().equals(value.id())) {
            activeNumeric = target;
            numericEditor.setX(fieldX);
            numericEditor.setY(y);
            numericEditor.setWidth(fieldWidth);
            numericEditor.setVisible(true);
            numericEditor.render(graphics, 0, 0, 0.0F);
        } else {
            graphics.fill(fieldX, y, fieldX + fieldWidth, y + 17, 0xA0222B36);
            String display = value.display().get();
            int valueX = fieldX + (fieldWidth - font.width(display)) / 2;
            graphics.drawString(font, display, valueX, y + 5, 0xFFE9EEF5, false);
        }
        button(graphics, x + width - 18, y, 18, 17, "+", false, () -> value.adjust().accept(1));
        return y + 21;
    }

    private NumericValue numericValue(String key, DoubleSupplier getter, DoubleConsumer setter,
                                      double minimum, double maximum, double step,
                                      int decimals, String suffix) {
        String id = numericId(key);
        return new NumericValue(id,
                () -> formatNumber(getter.getAsDouble(), decimals) + suffix,
                () -> formatNumber(getter.getAsDouble(), decimals),
                HudEditorScreen::validNumericInput,
                text -> {
                    double parsed;
                    try {
                        parsed = Double.parseDouble(text);
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                    if (!Double.isFinite(parsed)) return false;
                    double value = Math.max(minimum, Math.min(maximum, parsed));
                    if (decimals == 0) value = Math.round(value);
                    if (Math.abs(value - getter.getAsDouble()) <= 0.000001) return true;
                    setter.accept(value);
                    changed();
                    return true;
                },
                direction -> {
                    double value = Math.max(minimum, Math.min(maximum, getter.getAsDouble() + direction * step));
                    if (decimals == 0) value = Math.round(value);
                    setter.accept(value);
                    changed();
                });
    }

    private NumericValue marginValue(String key, NineSliceMargins margins) {
        Supplier<String> text = () -> margins.left + "/" + margins.top + "/" + margins.right + "/" + margins.bottom;
        return new NumericValue(numericId(key), text, text, HudEditorScreen::validMarginInput,
                value -> applyMargins(margins, value),
                direction -> {
                    margins.left = boundedMargin(margins.left + direction);
                    margins.top = boundedMargin(margins.top + direction);
                    margins.right = boundedMargin(margins.right + direction);
                    margins.bottom = boundedMargin(margins.bottom + direction);
                    changed();
                });
    }

    private boolean applyMargins(NineSliceMargins margins, String text) {
        String[] parts = text.split("/", -1);
        if (parts.length != 1 && parts.length != 4) return false;
        int left;
        int top;
        int right;
        int bottom;
        try {
            if (parts.length == 1) {
                int value = boundedMargin(Integer.parseInt(parts[0]));
                left = value;
                top = value;
                right = value;
                bottom = value;
            } else {
                left = boundedMargin(Integer.parseInt(parts[0]));
                top = boundedMargin(Integer.parseInt(parts[1]));
                right = boundedMargin(Integer.parseInt(parts[2]));
                bottom = boundedMargin(Integer.parseInt(parts[3]));
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (left == margins.left && top == margins.top && right == margins.right && bottom == margins.bottom) {
            return true;
        }
        margins.left = left;
        margins.top = top;
        margins.right = right;
        margins.bottom = bottom;
        changed();
        return true;
    }

    private static boolean validNumericInput(String text) {
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) return true;
        try {
            return Double.isFinite(Double.parseDouble(text));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validMarginInput(String text) {
        return text.matches("[0-9]*(/[0-9]*){0,3}");
    }

    private static int boundedMargin(int value) {
        return Math.max(0, Math.min(4096, value));
    }

    private static String formatNumber(double value, int decimals) {
        if (decimals <= 0) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private String numericId(String key) {
        return (selectedId == null ? "" : selectedId) + "|" + key;
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label,
                        boolean active, Runnable action) {
        graphics.fill(x, y, x + width, y + height, active ? 0xE03A778B : 0xD026303C);
        graphics.fill(x, y, x + width, y + 1, active ? 0xFF8FE8FF : 0xFF43505E);
        String display = ellipsis(label, width - 6);
        graphics.drawString(font, display, x + (width - font.width(display)) / 2,
                y + (height - font.lineHeight) / 2 + 1, active ? 0xFFFFFFFF : 0xFFE2E8EF, false);
        hitTargets.add(new HitTarget(x, y, width, height, action));
    }

    private void renderPalette(GuiGraphics graphics, BarStyle bar, BarLayerStyle layer,
                               int x, int y, int width) {
        List<Integer> colors = bar.palette(COLOR_PRESETS);
        int swatch = Math.max(10, (width - colors.size() * 2) / colors.size());
        for (int index = 0; index < colors.size(); index++) {
            int color = colors.get(index);
            int swatchX = x + index * (swatch + 2);
            graphics.fill(swatchX, y + 2, swatchX + swatch, y + 18, color);
            if ((layer.color & 0x00FFFFFF) == (color & 0x00FFFFFF)) {
                graphics.renderOutline(swatchX - 1, y + 1, swatch + 2, 18, 0xFFFFFFFF);
            }
            hitTargets.add(new HitTarget(swatchX, y + 2, swatch, 16, () -> {
                layer.color = (layer.color & 0xFF000000) | (color & 0x00FFFFFF);
                bar.rememberColor(color);
                changed();
            }));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && elementSearch.isMouseOver(mouseX, mouseY)) {
            finishNumericEdit();
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            elementSearch.setFocused(false);
            setFocused(null);
        }
        if (button == 0) {
            for (int index = numericTargets.size() - 1; index >= 0; index--) {
                NumericTarget target = numericTargets.get(index);
                if (!target.contains(mouseX, mouseY)) continue;
                if (activeNumeric != null && activeNumeric.value().id().equals(target.value().id())) {
                    activeNumeric = target;
                    return numericEditor.mouseClicked(mouseX, mouseY, button);
                }
                finishNumericEdit();
                beginNumericEdit(target);
                return true;
            }
        }
        finishNumericEdit();
        for (int index = hitTargets.size() - 1; index >= 0; index--) {
            HitTarget target = hitTargets.get(index);
            if (target.contains(mouseX, mouseY)) {
                target.action.run();
                return true;
            }
        }
        HudElementDefinition selectedDefinition = selectedId == null ? null : registry.definition(selectedId);
        if (selectedDefinition != null && !registry.layout(selectedDefinition).lockedToDefault
                && editorBounds(selectedDefinition).contains(mouseX, mouseY)
                && beginElementInteraction(registry.layout(selectedDefinition), mouseX, mouseY, button)) {
            return true;
        }
        List<HudElementDefinition> hits = registry.at(mouseX, mouseY, width, height);
        if (!hits.isEmpty()) {
            selectFromCanvas(hits.getFirst().stableId());
            HudElementLayout layout = selectedLayout();
            return beginElementInteraction(layout, mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean beginElementInteraction(HudElementLayout layout, double mouseX, double mouseY, int button) {
        if (layout == null || layout.lockedToDefault) return false;
        if (button == 2) {
            layout.resetPlacement(width, height);
            resetChanged();
            return true;
        }
        if (button != 0) return false;
        dragging = true;
        lastDragX = mouseX;
        lastDragY = mouseY;
        snap = SnapEngine.SnapResult.none();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeNumeric != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                    || keyCode == GLFW.GLFW_KEY_TAB) {
                finishNumericEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelNumericEdit();
                return true;
            }
            return numericEditor.keyPressed(keyCode, scanCode, modifiers);
        }
        if (elementSearch.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                elementSearch.setFocused(false);
                setFocused(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeNumeric != null) return numericEditor.charTyped(codePoint, modifiers);
        if (elementSearch.isFocused()) return super.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        HudElementLayout layout = selectedLayout();
        if (!dragging || button != 0 || layout == null || layout.lockedToDefault) {
            if (layout != null && layout.lockedToDefault) dragging = false;
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        double precision = hasShiftDown() ? 0.25 : 1.0;
        layout.move((mouseX - lastDragX) * precision, (mouseY - lastDragY) * precision);
        lastDragX = mouseX;
        lastDragY = mouseY;
        if (!hasControlDown()) {
            Bounds moving = layout.resolvedBounds(width, height);
            List<Bounds> others = new ArrayList<>();
            for (HudElementDefinition definition : registry.discovered()) {
                if (definition.stableId().equals(selectedId)) continue;
                HudElementLayout otherLayout = registry.layout(definition);
                if (otherLayout.lockedToDefault) {
                    Bounds lockedDefault = HudSnapTargets.lockedDefault(definition, otherLayout, width, height);
                    if (lockedDefault != null) others.add(lockedDefault);
                    continue;
                }
                HudElementRuntime runtime = registry.runtime(definition.stableId());
                if (runtime != null && runtime.rendered() && runtime.bounds() != null) others.add(runtime.bounds());
            }
            snap = SnapEngine.snap(moving, others, width, height, 5.0);
            layout.move(snap.deltaX(), snap.deltaY());
        } else {
            snap = SnapEngine.SnapResult.none();
        }
        changed();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            snap = SnapEngine.SnapResult.none();
            LayoutStore.saveNow();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int numericDirection = (int) Math.signum(scrollY);
        if (numericDirection != 0) {
            for (int index = numericTargets.size() - 1; index >= 0; index--) {
                NumericTarget target = numericTargets.get(index);
                if (!target.contains(mouseX, mouseY)) continue;
                boolean restoreFocus = activeNumeric != null
                        && activeNumeric.value().id().equals(target.value().id());
                finishNumericEdit();
                target.value().adjust().accept(numericDirection);
                if (restoreFocus) beginNumericEdit(target);
                return true;
            }
        }
        if (mouseX <= LIST_WIDTH + 4) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (mouseX >= width - PANEL_WIDTH - 4) {
            propertyScroll = Math.max(0, Math.min(propertyMaxScroll,
                    propertyScroll - (int) Math.signum(scrollY) * 18));
            return true;
        }
        HudElementLayout layout = selectedLayout();
        if (layout != null && !layout.lockedToDefault && scrollY != 0.0) {
            layout.scale = Math.max(0.25, Math.min(4.0, layout.scale + Math.signum(scrollY) * 0.05));
            changed();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        finishNumericEdit();
        LayoutStore.saveNow();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private HudElementLayout selectedLayout() {
        HudElementDefinition definition = selectedId == null ? null : registry.definition(selectedId);
        return definition == null ? null : registry.layout(definition);
    }

    private Bounds editorBounds(HudElementDefinition definition) {
        HudElementLayout layout = registry.layout(definition);
        // Configured bounds must drive a selected inactive element so the box
        // follows the cursor even though no fresh runtime bounds are arriving.
        if (definition.stableId().equals(selectedId)) return layout.resolvedBounds(width, height);
        HudElementRuntime runtime = registry.runtime(definition.stableId());
        if (runtime != null && runtime.bounds() != null) return runtime.bounds();
        return layout.resolvedBounds(width, height);
    }

    public boolean isListPreviewSelected(String id) {
        return listPreviewSelection && id != null && id.equals(selectedId);
    }

    private void selectFromList(String id) {
        select(id, true);
    }

    private void selectFromCanvas(String id) {
        select(id, false);
    }

    private void select(String id, boolean fromList) {
        if (!id.equals(selectedId)) propertyScroll = 0;
        selectedId = id;
        listPreviewSelection = fromList;
        rememberedSelectedId = id;
        rememberedListPreview = fromList;
    }

    private void revealSelectedRow() {
        List<HudElementDefinition> definitions = new ArrayList<>(registry.discovered());
        definitions.sort((left, right) -> Integer.compare(
                elementListPriority(registry.layout(left)), elementListPriority(registry.layout(right))));
        int selectedIndex = -1;
        for (int index = 0; index < definitions.size(); index++) {
            if (definitions.get(index).stableId().equals(selectedId)) {
                selectedIndex = index;
                break;
            }
        }
        if (selectedIndex < 0) return;
        int listTop = 26 + 41;
        int visibleRows = Math.max(1, (height - 6 - listTop - 4) / 16);
        if (selectedIndex < listScroll) listScroll = selectedIndex;
        else if (selectedIndex >= listScroll + visibleRows) listScroll = selectedIndex - visibleRows + 1;
    }

    private void beginNumericEdit(NumericTarget target) {
        activeNumeric = target;
        numericEditor.setFilter(target.value().filter());
        numericEditor.setX(target.x());
        numericEditor.setY(target.y());
        numericEditor.setWidth(target.width());
        numericEditor.setValue(target.value().edit().get());
        numericEditor.setVisible(true);
        numericEditor.setFocused(true);
        numericEditor.setHighlightPos(0);
    }

    private void finishNumericEdit() {
        if (activeNumeric == null) return;
        activeNumeric.value().commit().apply(numericEditor.getValue());
        activeNumeric = null;
        numericEditor.setFocused(false);
        numericEditor.setVisible(false);
    }

    private void cancelNumericEdit() {
        if (activeNumeric == null) return;
        activeNumeric = null;
        numericEditor.setFocused(false);
        numericEditor.setVisible(false);
    }

    private int propertyContentHeight(HudElementLayout layout) {
        if (!layout.semanticBar()) {
            if (page == PropertyPage.BASIC) {
                return layout.classification == HudElementType.BAR ? 421 : 169;
            }
            return 30;
        }
        return switch (page) {
            case BASIC -> 421;
            case STYLE -> layout.bar.layer(selectedLayer).textureScale == TextureScaleMode.SEGMENTED ? 321 : 300;
            case TEXTURES -> 210;
            case TRAIL -> 185;
        };
    }

    private void changed() {
        HudElementLayout layout = selectedLayout();
        if (layout != null) {
            layout.customized = true;
            layout.sanitize();
        }
        LayoutStore.markDirty();
    }

    private void resetChanged() {
        HudElementLayout layout = selectedLayout();
        if (layout != null) layout.sanitize();
        LayoutStore.markDirty();
    }

    private void setLockedToDefault(HudElementLayout layout, boolean locked) {
        finishNumericEdit();
        layout.lockedToDefault = locked;
        dragging = false;
        snap = SnapEngine.SnapResult.none();
        propertyScroll = 0;
        LayoutStore.markDirty();
        LayoutStore.saveNow();
    }

    private String ellipsis(String value, int maximumWidth) {
        if (font.width(value) <= maximumWidth) return value;
        String shortened = value;
        while (!shortened.isEmpty() && font.width(shortened + "…") > maximumWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }

    private static String pretty(Enum<?> value) {
        String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static String shortTexture(String id) {
        int slash = Math.max(id.lastIndexOf('/'), id.lastIndexOf('\\'));
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static String colorName(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0x00FFFFFF);
    }

    private static void outlineOutside(GuiGraphics graphics, Bounds bounds, int color) {
        int left = (int) Math.floor(bounds.x()) - 1;
        int top = (int) Math.floor(bounds.y()) - 1;
        int right = (int) Math.ceil(bounds.right()) + 1;
        int bottom = (int) Math.ceil(bounds.bottom()) + 1;
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top + 1, left + 1, bottom - 1, color);
        graphics.fill(right - 1, top + 1, right, bottom - 1, color);
    }

    private enum PropertyPage {
        BASIC("Basic"), STYLE("Style"), TEXTURES("Texture"), TRAIL("Trail");

        private final String shortName;

        PropertyPage(String shortName) {
            this.shortName = shortName;
        }
    }

    private record HitTarget(int x, int y, int width, int height, Runnable action) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record NumericValue(String id, Supplier<String> display, Supplier<String> edit,
                                Predicate<String> filter, TextCommit commit, IntConsumer adjust) {
    }

    private record NumericTarget(int x, int y, int width, int height, NumericValue value) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    @FunctionalInterface
    private interface TextCommit {
        boolean apply(String text);
    }
}
