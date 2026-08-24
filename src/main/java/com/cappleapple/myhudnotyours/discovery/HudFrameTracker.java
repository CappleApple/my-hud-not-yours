package com.cappleapple.myhudnotyours.discovery;

import com.cappleapple.myhudnotyours.model.Bounds;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Collects a conservative union of ordinary GuiGraphics draws while a named HUD layer is active.
 * Direct OpenGL/framebuffer rendering cannot be measured safely and is intentionally left alone.
 */
public final class HudFrameTracker {
    private static final ThreadLocal<Accumulator> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> SUSPEND_DEPTH = ThreadLocal.withInitial(() -> 0);

    private HudFrameTracker() {
    }

    public static void begin(HudElementDefinition definition) {
        ACTIVE.set(new Accumulator(definition));
    }

    public static FrameResult end() {
        Accumulator accumulator = ACTIVE.get();
        ACTIVE.remove();
        return accumulator == null ? new FrameResult(null, Set.of()) : accumulator.finish();
    }

    public static void suspend() {
        SUSPEND_DEPTH.set(SUSPEND_DEPTH.get() + 1);
    }

    public static void resume() {
        SUSPEND_DEPTH.set(Math.max(0, SUSPEND_DEPTH.get() - 1));
    }

    public static void recordRect(Matrix4f matrix, double x1, double y1, double x2, double y2) {
        if (SUSPEND_DEPTH.get() > 0) return;
        Accumulator accumulator = ACTIVE.get();
        if (accumulator != null) accumulator.record(matrix, x1, y1, x2, y2);
    }

    public static void recordTexture(ResourceLocation texture) {
        if (SUSPEND_DEPTH.get() > 0) return;
        Accumulator accumulator = ACTIVE.get();
        if (accumulator != null && texture != null) accumulator.textures.add(texture);
    }

    public record FrameResult(Bounds bounds, Set<ResourceLocation> textures) {
    }

    private static final class Accumulator {
        private final HudElementDefinition definition;
        private final Set<ResourceLocation> textures = new LinkedHashSet<>();
        private final Vector3f scratch = new Vector3f();
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        private Accumulator(HudElementDefinition definition) {
            this.definition = definition;
        }

        private void record(Matrix4f matrix, double x1, double y1, double x2, double y2) {
            point(matrix, x1, y1);
            point(matrix, x2, y1);
            point(matrix, x1, y2);
            point(matrix, x2, y2);
        }

        private void point(Matrix4f matrix, double x, double y) {
            matrix.transformPosition((float) x, (float) y, 0.0F, scratch);
            minX = Math.min(minX, scratch.x);
            minY = Math.min(minY, scratch.y);
            maxX = Math.max(maxX, scratch.x);
            maxY = Math.max(maxY, scratch.y);
        }

        private FrameResult finish() {
            Bounds bounds = Double.isFinite(minX)
                    ? new Bounds(minX, minY, Math.max(0.0, maxX - minX), Math.max(0.0, maxY - minY))
                    : null;
            return new FrameResult(bounds, Set.copyOf(textures));
        }
    }
}
