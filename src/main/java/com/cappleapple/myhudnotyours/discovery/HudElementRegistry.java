package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import com.cappleapple.myhudnotyours.model.HudElementType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Registry of stable named layers plus their latest opaque draw regions. */
public final class HudElementRegistry {
    private static final HudElementRegistry INSTANCE = new HudElementRegistry();
    private final Map<String, HudElementDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, HudElementRuntime> runtime = new LinkedHashMap<>();

    private HudElementRegistry() {
    }

    public static HudElementRegistry get() {
        return INSTANCE;
    }

    public synchronized HudElementDefinition discover(ResourceLocation layer, int order, int screenWidth, int screenHeight) {
        HudElementDefinition known = KnownHudElements.definition(layer);
        HudElementDefinition definition = known != null ? known : new HudElementDefinition(
                layer.toString(), generatedName(layer), HudElementType.UNKNOWN, layer.getNamespace(),
                "named_gui_layer/" + layer.getPath(), "");
        definitions.putIfAbsent(definition.stableId(), definition);
        runtime.computeIfAbsent(definition.stableId(), ignored -> new HudElementRuntime());
        HudElementLayout layout = layout(definition);
        if (layout.lockedToDefault) return definition;
        synchronizeKnownMetadata(definition, layout);
        if (!layout.initialized) {
            Bounds fallback = KnownHudElements.fallbackBounds(layer, screenWidth, screenHeight);
            if (fallback != null) layout.initializeFrom(fallback, screenWidth, screenHeight);
        }
        return definition;
    }

    private static void synchronizeKnownMetadata(HudElementDefinition definition, HudElementLayout layout) {
        boolean changed = false;
        if (definition.classification() != HudElementType.UNKNOWN
                && layout.classification != definition.classification()) {
            layout.classification = definition.classification();
            changed = true;
        }
        String source = definition.semanticSourceId();
        if (source != null && !source.isBlank() && !source.equals(layout.barSourceId)) {
            layout.barSourceId = source;
            applySourceColor(layout);
            changed = true;
        }
        if (changed) LayoutStore.markDirty();
    }

    public synchronized HudElementLayout layout(HudElementDefinition definition) {
        return LayoutStore.get().elements.computeIfAbsent(definition.stableId(), ignored -> {
            HudElementLayout layout = new HudElementLayout();
            layout.id = definition.stableId();
            layout.displayName = definition.displayName();
            layout.classification = definition.classification();
            layout.barSourceId = definition.semanticSourceId() == null ? "" : definition.semanticSourceId();
            applySourceColor(layout);
            LayoutStore.markDirty();
            return layout;
        });
    }

    private static void applySourceColor(HudElementLayout layout) {
        layout.bar.filled.color = switch (layout.barSourceId) {
            case "minecraft:hunger" -> 0xFFD68A32;
            case "minecraft:armor" -> 0xFFBFC7D5;
            case "minecraft:air" -> 0xFF48BDE8;
            case "minecraft:experience" -> 0xFF80D43B;
            case "minecraft:mount_health" -> 0xFFB86A5F;
            default -> 0xFFCC3333;
        };
    }

    public synchronized void update(HudElementDefinition definition, Bounds bounds, int renderOrder,
                                    Set<ResourceLocation> textures, boolean rendered,
                                    int screenWidth, int screenHeight) {
        HudElementLayout layout = layout(definition);
        if (!layout.initialized && bounds != null && bounds.width() > 0.0 && bounds.height() > 0.0) {
            layout.initializeFrom(bounds, screenWidth, screenHeight);
            LayoutStore.markDirty();
        }
        runtime.computeIfAbsent(definition.stableId(), ignored -> new HudElementRuntime())
                .update(bounds, renderOrder, textures, rendered);
    }

    public synchronized HudElementDefinition definition(String id) {
        return definitions.get(id);
    }

    public synchronized HudElementRuntime runtime(String id) {
        return runtime.get(id);
    }

    public synchronized List<HudElementDefinition> discovered() {
        List<HudElementDefinition> result = new ArrayList<>(definitions.values());
        result.sort(Comparator.comparingInt(definition -> {
            HudElementRuntime state = runtime.get(definition.stableId());
            return state == null ? Integer.MAX_VALUE : state.renderOrder();
        }));
        return result;
    }

    public synchronized List<HudElementDefinition> at(double x, double y, int screenWidth, int screenHeight) {
        List<HudElementDefinition> result = new ArrayList<>();
        for (HudElementDefinition definition : definitions.values()) {
            HudElementLayout layout = layout(definition);
            if (layout.lockedToDefault
                    || layout.renderMode == com.cappleapple.myhudnotyours.model.RenderMode.HIDDEN) continue;
            HudElementRuntime state = runtime.get(definition.stableId());
            if (state == null || !state.rendered() || System.currentTimeMillis() - state.lastSeenMillis() > 250L) continue;
            Bounds bounds = state.bounds() != null ? state.bounds() : layout.resolvedBounds(screenWidth, screenHeight);
            if (bounds.contains(x, y)) result.add(definition);
        }
        result.sort(Comparator.comparingInt(definition -> {
            HudElementRuntime state = runtime.get(definition.stableId());
            return state == null ? -1 : -state.renderOrder();
        }));
        return result;
    }

    private static String generatedName(ResourceLocation id) {
        String[] words = id.getPath().replace('/', ' ').replace('_', ' ').split(" +");
        StringBuilder name = new StringBuilder(id.getNamespace()).append(": ");
        for (String word : words) {
            if (word.isEmpty()) continue;
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1)).append(' ');
        }
        return name.toString().trim();
    }
}
