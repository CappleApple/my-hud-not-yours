package com.cappleapple.myhudnotyours.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BarStyleColorHistoryTest {
    @Test
    void historyIsPerBarMostRecentFirstUniqueAndBounded() {
        BarStyle first = new BarStyle();
        BarStyle second = new BarStyle();
        for (int index = 0; index < 12; index++) first.rememberColor(index);
        first.rememberColor(5);

        assertEquals(BarStyle.COLOR_HISTORY_LIMIT, first.recentColors.size());
        assertEquals(0xFF000005, first.recentColors.getFirst());
        assertEquals(List.of(), second.recentColors);
    }

    @Test
    void sanitizeRemovesNullsDuplicatesAndExcessEntries() {
        BarStyle style = new BarStyle();
        style.recentColors = new ArrayList<>();
        style.recentColors.add(0x00112233);
        style.recentColors.add(null);
        style.recentColors.add(0xAA112233);
        for (int index = 0; index < 12; index++) style.recentColors.add(index);

        style.sanitizeColorHistory();

        assertEquals(BarStyle.COLOR_HISTORY_LIMIT, style.recentColors.size());
        assertEquals(0xFF112233, style.recentColors.getFirst());
    }

    @Test
    void recentChoicesDisplacePresetSlots() {
        BarStyle style = new BarStyle();
        int[] defaults = {0xFF000001, 0xFF000002, 0xFF000003};
        style.rememberColor(0xFFABCDEF);

        assertEquals(List.of(0xFFABCDEF, 0xFF000001, 0xFF000002), style.palette(defaults));
    }
}
