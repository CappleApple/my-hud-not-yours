package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementType;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class KnownHudElements {
    private static final ResourceLocation IRONS_SPELLBOOKS_MANA_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_overlay");
    private static final Map<ResourceLocation, HudElementDefinition> KNOWN = build();

    private KnownHudElements() {
    }

    public static HudElementDefinition definition(ResourceLocation layer) {
        return KNOWN.get(layer);
    }

    private static Map<ResourceLocation, HudElementDefinition> build() {
        Map<ResourceLocation, HudElementDefinition> definitions = new LinkedHashMap<>();
        add(definitions, VanillaGuiLayers.CROSSHAIR, "Crosshair", HudElementType.ICON, "");
        add(definitions, VanillaGuiLayers.HOTBAR, "Hotbar", HudElementType.HOTBAR, "");
        add(definitions, VanillaGuiLayers.JUMP_METER, "Mount Jump Meter", HudElementType.BAR, "minecraft:mount_jump");
        add(definitions, VanillaGuiLayers.EXPERIENCE_BAR, "Experience", HudElementType.BAR, "minecraft:experience");
        add(definitions, VanillaGuiLayers.PLAYER_HEALTH, "Health", HudElementType.BAR, "minecraft:health");
        add(definitions, VanillaGuiLayers.ARMOR_LEVEL, "Armor", HudElementType.BAR, "minecraft:armor");
        add(definitions, VanillaGuiLayers.FOOD_LEVEL, "Hunger", HudElementType.BAR, "minecraft:hunger");
        add(definitions, VanillaGuiLayers.VEHICLE_HEALTH, "Mount Health", HudElementType.BAR, "minecraft:mount_health");
        add(definitions, VanillaGuiLayers.AIR_LEVEL, "Air", HudElementType.BAR, "minecraft:air");
        add(definitions, VanillaGuiLayers.SELECTED_ITEM_NAME, "Selected Item Name", HudElementType.TEXT, "");
        add(definitions, VanillaGuiLayers.SPECTATOR_TOOLTIP, "Spectator Tooltip", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.EXPERIENCE_LEVEL, "Experience Level", HudElementType.TEXT, "");
        add(definitions, VanillaGuiLayers.EFFECTS, "Status Effects", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.BOSS_OVERLAY, "Boss Bars", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.SLEEP_OVERLAY, "Sleep Overlay", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.DEMO_OVERLAY, "Demo Overlay", HudElementType.TEXT, "");
        add(definitions, VanillaGuiLayers.DEBUG_OVERLAY, "Debug Overlay", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.SCOREBOARD_SIDEBAR, "Scoreboard", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.OVERLAY_MESSAGE, "Overlay Message", HudElementType.TEXT, "");
        add(definitions, VanillaGuiLayers.TITLE, "Title", HudElementType.TEXT, "");
        add(definitions, VanillaGuiLayers.CHAT, "Chat", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.TAB_LIST, "Player List", HudElementType.COMPOSITE, "");
        add(definitions, VanillaGuiLayers.SUBTITLE_OVERLAY, "Subtitles", HudElementType.TEXT, "");
        add(definitions, VanillaGuiLayers.SAVING_INDICATOR, "Saving Indicator", HudElementType.TEXT, "");
        add(definitions, IRONS_SPELLBOOKS_MANA_OVERLAY, "Mana Overlay", HudElementType.BAR,
                "irons_spellbooks:mana");
        return Map.copyOf(definitions);
    }

    private static void add(Map<ResourceLocation, HudElementDefinition> definitions, ResourceLocation id,
                            String name, HudElementType type, String source) {
        definitions.put(id, new HudElementDefinition(id.toString(), name, type, id.getNamespace(),
                "named_gui_layer/" + id.getPath(), source));
    }

    public static Bounds fallbackBounds(ResourceLocation layer, int width, int height) {
        if (layer.equals(VanillaGuiLayers.HOTBAR)) return new Bounds(width / 2.0 - 91, height - 23, 182, 24);
        if (layer.equals(VanillaGuiLayers.EXPERIENCE_BAR)) return new Bounds(width / 2.0 - 91, height - 32, 182, 5);
        if (layer.equals(VanillaGuiLayers.PLAYER_HEALTH)) return new Bounds(width / 2.0 - 91, height - 39, 81, 10);
        if (layer.equals(VanillaGuiLayers.ARMOR_LEVEL)) return new Bounds(width / 2.0 - 91, height - 49, 81, 10);
        if (layer.equals(VanillaGuiLayers.FOOD_LEVEL)) return new Bounds(width / 2.0 + 10, height - 39, 81, 10);
        if (layer.equals(VanillaGuiLayers.VEHICLE_HEALTH)) return new Bounds(width / 2.0 + 10, height - 39, 81, 10);
        if (layer.equals(VanillaGuiLayers.AIR_LEVEL)) return new Bounds(width / 2.0 + 10, height - 49, 81, 10);
        if (layer.equals(VanillaGuiLayers.CROSSHAIR)) return new Bounds(width / 2.0 - 8, height / 2.0 - 8, 16, 16);
        return null;
    }
}
