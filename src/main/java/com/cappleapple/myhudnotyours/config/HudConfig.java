package com.cappleapple.myhudnotyours.config;

import com.cappleapple.myhudnotyours.model.HudElementLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudConfig {
    public int configVersion = 5;
    public boolean debugMode = false;
    public Map<String, HudElementLayout> elements = new LinkedHashMap<>();

    public void sanitize() {
        int loadedVersion = configVersion;
        if (elements == null) elements = new LinkedHashMap<>();
        elements.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        elements.forEach((id, layout) -> {
            layout.id = id;
            layout.sanitize();
            if (loadedVersion < 4) layout.showInCreative = true;
            if (loadedVersion < 2 || !layout.customized) {
                layout.customized = layout.hasLegacyCustomization();
            }
        });
        configVersion = 5;
    }
}
