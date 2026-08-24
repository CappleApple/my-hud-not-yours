package com.cappleapple.myhudnotyours.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class AnimationSpecTest {
    @Test
    void stillTextureUsesWholeImage() {
        AnimationSpec spec = AnimationSpec.parse(null, 32, 12);
        AnimationSpec.FrameRegion frame = spec.frameAt(10_000, 32);
        assertFalse(spec.animated());
        assertEquals(32, frame.width());
        assertEquals(12, frame.height());
    }

    @Test
    void standardVerticalStripAdvancesAtFrameTime() {
        AnimationSpec spec = AnimationSpec.parse(JsonParser.parseString("""
                {"animation":{"frametime":2}}
                """).getAsJsonObject(), 16, 48);
        assertTrue(spec.animated());
        assertEquals(0, spec.frameAt(0, 16).y());
        assertEquals(16, spec.frameAt(100, 16).y());
        assertEquals(32, spec.frameAt(200, 16).y());
    }

    @Test
    void explicitFramesHonorPerFrameDurations() {
        AnimationSpec spec = AnimationSpec.parse(JsonParser.parseString("""
                {"animation":{"width":8,"height":8,"frames":[{"index":3,"time":3},1]}}
                """).getAsJsonObject(), 16, 16);
        assertEquals(8, spec.frameAt(0, 16).x());
        assertEquals(8, spec.frameAt(0, 16).y());
        assertEquals(8, spec.frameAt(150, 16).x());
        assertEquals(0, spec.frameAt(150, 16).y());
    }
}
