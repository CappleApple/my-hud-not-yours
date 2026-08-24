package com.cappleapple.myhudnotyours.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudStackingPolicyTest {
    @Test
    void replacedHealthAlwaysConsumesExactlyOneVanillaRow() {
        assertEquals(49, HudStackingPolicy.singleHealthRow(39));
        assertEquals(73, HudStackingPolicy.singleHealthRow(63));
    }
}
