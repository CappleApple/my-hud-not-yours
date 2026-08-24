package com.cappleapple.myhudnotyours.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColorHexCodecTest {
    @Test
    void sixDigitHexPreservesOpacity() {
        ColorHexCodec.ParsedColor parsed = ColorHexCodec.parse("#12aBcF", 0.4F).orElseThrow();

        assertEquals(0x12ABCF, parsed.rgb());
        assertEquals(0.4F, parsed.opacity(), 0.0001F);
        assertEquals("#12ABCF", ColorHexCodec.formatRgb(parsed.rgb()));
    }

    @Test
    void rgbaHexAndShorthandSetOpacity() {
        ColorHexCodec.ParsedColor rgba = ColorHexCodec.parse("33669980", 1.0F).orElseThrow();
        ColorHexCodec.ParsedColor shortRgba = ColorHexCodec.parse("#F008", 1.0F).orElseThrow();

        assertEquals(0x336699, rgba.rgb());
        assertEquals(128.0F / 255.0F, rgba.opacity(), 0.0001F);
        assertEquals(0xFF0000, shortRgba.rgb());
        assertEquals(136.0F / 255.0F, shortRgba.opacity(), 0.0001F);
    }

    @Test
    void filterAllowsTypingButParserRequiresACompleteColor() {
        assertTrue(ColorHexCodec.isPotentialInput("#12a"));
        assertFalse(ColorHexCodec.isPotentialInput("#12xz"));
        assertTrue(ColorHexCodec.parse("#12345", 1.0F).isEmpty());
    }
}
