package com.cappleapple.myhudnotyours.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.Bounds;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import org.junit.jupiter.api.Test;

class HudConfigTest {
    @Test
    void versionOneMigrationOnlyActivatesPreviouslyEditedLayouts() {
        HudConfig config = new HudConfig();
        config.configVersion = 1;
        HudElementLayout untouched = initialized("untouched");
        HudElementLayout moved = initialized("moved");
        moved.offsetY += 4.0;
        config.elements.put(untouched.id, untouched);
        config.elements.put(moved.id, moved);

        config.sanitize();

        assertFalse(untouched.customized);
        assertTrue(moved.customized);
        assertTrue(config.configVersion >= 2);
    }

    @Test
    void versionTwoModpackPlacementActivatesWithoutManualFlag() {
        HudConfig config = new HudConfig();
        HudElementLayout authored = initialized("authored");
        authored.offsetX -= 12.0;
        config.elements.put(authored.id, authored);

        config.sanitize();

        assertTrue(authored.customized);
    }

    @Test
    void preVisibilityConfigsKeepCreativeElementsVisible() {
        HudConfig config = new HudConfig();
        config.configVersion = 3;
        HudElementLayout legacy = initialized("legacy");
        legacy.showInCreative = false;
        config.elements.put(legacy.id, legacy);

        config.sanitize();

        assertTrue(legacy.showInCreative);
        assertTrue(config.configVersion >= 4);
    }

    @Test
    void conditionalVisibilityTimingsAreSanitized() {
        HudElementLayout layout = initialized("timed");
        layout.hideDelayMillis = -1;
        layout.hideFadeMillis = 100_000;

        layout.sanitize();

        assertEquals(0, layout.hideDelayMillis);
        assertEquals(60_000, layout.hideFadeMillis);
    }

    @Test
    void preIdleVisibilityConfigsRemainVisibleWhileIdle() {
        HudConfig config = new HudConfig();
        config.configVersion = 5;
        HudElementLayout legacy = initialized("legacy-idle");
        legacy.showOnIdle = false;
        config.elements.put(legacy.id, legacy);

        config.sanitize();

        assertTrue(legacy.showOnIdle);
        assertTrue(config.configVersion >= 6);
    }

    @Test
    void preDefaultLockConfigLocksOnlyUntouchedLayouts() {
        HudConfig config = new HudConfig();
        config.configVersion = 6;
        HudElementLayout untouched = initialized("untouched-lock");
        untouched.lockedToDefault = false;
        HudElementLayout customized = initialized("customized-lock");
        customized.lockedToDefault = false;
        customized.customized = true;
        config.elements.put(untouched.id, untouched);
        config.elements.put(customized.id, customized);

        config.sanitize();

        assertTrue(untouched.lockedToDefault);
        assertFalse(customized.lockedToDefault);
        assertTrue(config.configVersion >= 7);
    }

    private static HudElementLayout initialized(String id) {
        HudElementLayout layout = new HudElementLayout();
        layout.id = id;
        layout.initializeFrom(new Bounds(20.0, 30.0, 80.0, 10.0), 320, 180);
        return layout;
    }
}
