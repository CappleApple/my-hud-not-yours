package com.cappleapple.myhudnotyours.editor;

import java.util.Locale;
import java.util.Optional;

/** CSS-style RGB/RGBA hex parsing for the HUD color editor. */
public final class ColorHexCodec {
    private ColorHexCodec() {
    }

    public static boolean isPotentialInput(String input) {
        if (input == null) return false;
        String value = input.startsWith("#") ? input.substring(1) : input;
        if (value.length() > 8) return false;
        for (int index = 0; index < value.length(); index++) {
            if (Character.digit(value.charAt(index), 16) < 0) return false;
        }
        return true;
    }

    public static Optional<ParsedColor> parse(String input, float existingOpacity) {
        if (input == null) return Optional.empty();
        String value = input.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() == 3 || value.length() == 4) {
            StringBuilder expanded = new StringBuilder(value.length() * 2);
            for (int index = 0; index < value.length(); index++) {
                expanded.append(value.charAt(index)).append(value.charAt(index));
            }
            value = expanded.toString();
        }
        if (value.length() != 6 && value.length() != 8) return Optional.empty();
        try {
            int rgb = Integer.parseUnsignedInt(value.substring(0, 6), 16) & 0x00FFFFFF;
            float opacity = existingOpacity;
            if (value.length() == 8) {
                opacity = Integer.parseUnsignedInt(value.substring(6, 8), 16) / 255.0F;
            }
            return Optional.of(new ParsedColor(rgb, opacity));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static String formatRgb(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0x00FFFFFF);
    }

    public record ParsedColor(int rgb, float opacity) {
    }
}
