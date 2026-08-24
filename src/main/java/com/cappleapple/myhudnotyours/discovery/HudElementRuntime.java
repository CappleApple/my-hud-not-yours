package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.model.Bounds;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class HudElementRuntime {
    private Bounds bounds;
    private long lastSeenMillis;
    private int renderOrder;
    private Set<ResourceLocation> textures = Set.of();
    private boolean rendered;

    public Bounds bounds() {
        return bounds;
    }

    public long lastSeenMillis() {
        return lastSeenMillis;
    }

    public int renderOrder() {
        return renderOrder;
    }

    public Set<ResourceLocation> textures() {
        return textures;
    }

    public boolean rendered() {
        return rendered;
    }

    void update(Bounds bounds, int renderOrder, Set<ResourceLocation> textures, boolean rendered) {
        if (bounds != null) this.bounds = bounds;
        this.lastSeenMillis = System.currentTimeMillis();
        this.renderOrder = renderOrder;
        this.textures = textures.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(textures));
        this.rendered = rendered;
    }
}
