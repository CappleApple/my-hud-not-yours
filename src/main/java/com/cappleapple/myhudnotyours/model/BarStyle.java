package com.cappleapple.myhudnotyours.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class BarStyle {
    public static final int COLOR_HISTORY_LIMIT = 9;
    public int width = 120;
    public int height = 12;
    public int borderThickness = 1;
    public int cornerRadius = 0;
    /** Amount of a semantic source's maximum range represented by one segmented texture cell. */
    public double maximumPerSegment = 2.0;
    /** Extra unscaled pixels added for each maximum-stat point above the captured baseline. */
    public double widthPerMaximum = 0.0;
    public double heightPerMaximum = 0.0;
    /** Persisted maximum present when dynamic sizing was first enabled; -1 means uncaptured. */
    public double sizeBaselineMaximum = -1.0;
    public FillDirection fillDirection = FillDirection.LEFT_TO_RIGHT;
    public BarLayerStyle background = BarLayerStyle.solid(0xCC080A0E);
    public BarLayerStyle frame = BarLayerStyle.solid(0xFFE0E4EA);
    public BarLayerStyle filled = BarLayerStyle.solid(0xFFCC3333);
    public BarLayerStyle empty = BarLayerStyle.solid(0xFF2A1010);
    public TrailStyle trail = new TrailStyle();
    public TextStyle text = new TextStyle();
    /** Most-recent-first RGB choices, persisted independently for each bar. */
    public List<Integer> recentColors = new ArrayList<>();

    public BarLayerStyle layer(int index) {
        return switch (Math.floorMod(index, 5)) {
            case 0 -> background;
            case 1 -> frame;
            case 2 -> filled;
            case 3 -> empty;
            default -> trail.layer;
        };
    }

    public static String layerName(int index) {
        return switch (Math.floorMod(index, 5)) {
            case 0 -> "Background";
            case 1 -> "Frame";
            case 2 -> "Filled";
            case 3 -> "Empty";
            default -> "Trail";
        };
    }

    public void rememberColor(int color) {
        if (recentColors == null) recentColors = new ArrayList<>();
        int normalized = 0xFF000000 | color & 0x00FFFFFF;
        recentColors.removeIf(existing -> existing != null
                && (existing & 0x00FFFFFF) == (normalized & 0x00FFFFFF));
        recentColors.addFirst(normalized);
        while (recentColors.size() > COLOR_HISTORY_LIMIT) recentColors.removeLast();
    }

    public void sanitizeColorHistory() {
        if (recentColors == null) {
            recentColors = new ArrayList<>();
            return;
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (Integer color : recentColors) {
            if (color == null) continue;
            unique.add(0xFF000000 | color & 0x00FFFFFF);
            if (unique.size() == COLOR_HISTORY_LIMIT) break;
        }
        recentColors = new ArrayList<>(unique);
    }

    public List<Integer> palette(int[] defaults) {
        LinkedHashSet<Integer> colors = new LinkedHashSet<>();
        if (recentColors != null) {
            for (Integer color : recentColors) {
                if (color != null) colors.add(0xFF000000 | color & 0x00FFFFFF);
                if (colors.size() == defaults.length) break;
            }
        }
        for (int color : defaults) {
            if (colors.size() == defaults.length) break;
            colors.add(0xFF000000 | color & 0x00FFFFFF);
        }
        return List.copyOf(colors);
    }
}
