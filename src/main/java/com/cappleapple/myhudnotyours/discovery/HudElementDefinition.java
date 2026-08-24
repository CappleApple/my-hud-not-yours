package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.model.HudElementType;

public record HudElementDefinition(
        String stableId,
        String displayName,
        HudElementType classification,
        String sourceNamespace,
        String renderPath,
        String semanticSourceId) {

    public boolean semanticBar() {
        return classification == HudElementType.BAR && semanticSourceId != null && !semanticSourceId.isBlank();
    }
}
