package com.cappleapple.myhudnotyours.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.myhudnotyours.model.HudElementType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class KnownHudElementsTest {
    @Test
    void ironsManaOverlayUsesItsRealSemanticManaSource() {
        HudElementDefinition definition = KnownHudElements.definition(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_overlay"));

        assertNotNull(definition);
        assertEquals(HudElementType.BAR, definition.classification());
        assertEquals("irons_spellbooks:mana", definition.semanticSourceId());
        assertTrue(definition.semanticBar());
    }
}
