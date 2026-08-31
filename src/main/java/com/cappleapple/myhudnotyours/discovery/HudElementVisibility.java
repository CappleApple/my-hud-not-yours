package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.bar.CustomBarRenderer;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.model.HudElementLayout;
import com.cappleapple.myhudnotyours.model.RenderMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;

/** Resolves local and inherited element visibility without mutating renderers. */
public final class HudElementVisibility {
    private static final long RUNTIME_FRESH_MILLIS = 250L;

    private HudElementVisibility() {
    }

    public static float effectiveOpacity(String elementId, float partialTick) {
        return effectiveOpacity(elementId, partialTick, new HashSet<>());
    }

    public static float parentOpacity(HudElementLayout layout, float partialTick) {
        if (layout.parentId == null || layout.parentId.isBlank()) return 1.0F;
        return effectiveOpacity(layout.parentId, partialTick, new HashSet<>());
    }

    public static boolean visibleForStack(String elementId, float partialTick) {
        return effectiveOpacity(elementId, partialTick) > 0.0001F;
    }

    private static float effectiveOpacity(String elementId, float partialTick, Set<String> path) {
        if (elementId == null || elementId.isBlank() || !path.add(elementId)) return 1.0F;
        try {
            Map<String, HudElementLayout> layouts = LayoutStore.get().elements;
            HudElementLayout layout = layouts.get(elementId);
            if (layout == null) return 0.0F;
            float local = localOpacity(layout, partialTick);
            if (local <= 0.0001F || layout.parentId == null || layout.parentId.isBlank()) return local;
            return local * effectiveOpacity(layout.parentId, partialTick, path);
        } finally {
            path.remove(elementId);
        }
    }

    private static float localOpacity(HudElementLayout layout, float partialTick) {
        boolean controlled = layout.customized && !layout.lockedToDefault;
        if (controlled && layout.renderMode == RenderMode.HIDDEN) return 0.0F;
        if (layout.semanticBar()) return CustomBarRenderer.visibilityOpacity(layout.id, partialTick);
        if (controlled && !layout.showInCreative
                && Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.isCreative()) {
            return 0.0F;
        }
        if (layout.lockedToDefault) return 1.0F;
        HudElementRuntime runtime = HudElementRegistry.get().runtime(layout.id);
        if (runtime == null || System.currentTimeMillis() - runtime.lastSeenMillis() > RUNTIME_FRESH_MILLIS) {
            return 1.0F;
        }
        return runtime.rendered() ? 1.0F : 0.0F;
    }
}
