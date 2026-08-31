package com.cappleapple.myhudnotyours.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Resolves parent-local transforms and conditional centered stacking. */
public final class HudElementRelations {
    private static final double MINIMUM_SCALE = 0.0001;

    private HudElementRelations() {
    }

    public static Resolved resolve(HudElementLayout layout, Map<String, HudElementLayout> layouts,
                                   int screenWidth, int screenHeight, Predicate<String> stackVisible) {
        return resolve(layout, layouts, screenWidth, screenHeight, stackVisible, new HashSet<>());
    }

    public static boolean wouldCreateCycle(String childId, String candidateId,
                                           Map<String, HudElementLayout> layouts) {
        if (candidateId == null || candidateId.isBlank()) return false;
        return reaches(candidateId, childId, layouts, new HashSet<>());
    }

    /** Applies a screen-space drag while preserving a parent-local child offset. */
    public static void moveByScreen(HudElementLayout layout, double dx, double dy,
                                    Map<String, HudElementLayout> layouts,
                                    int screenWidth, int screenHeight,
                                    Predicate<String> stackVisible) {
        if (stackActive(layout, layouts, stackVisible)) {
            layout.stackOffsetX += dx;
            layout.stackOffsetY += dy;
            return;
        }
        double parentScale = parentScale(layout, layouts, screenWidth, screenHeight, stackVisible);
        layout.move(dx / parentScale, dy / parentScale);
    }

    /** Links without moving or resizing the child at the moment of linkage. */
    public static void setParent(HudElementLayout child, String parentId,
                                 Map<String, HudElementLayout> layouts,
                                 int screenWidth, int screenHeight,
                                 Predicate<String> stackVisible) {
        Resolved before = resolve(child, layouts, screenWidth, screenHeight, stackVisible);
        if (parentId == null || parentId.isBlank()) {
            child.parentId = "";
            child.anchor = ScreenAnchor.nearest(before.bounds.centerX(), before.bounds.centerY(),
                    screenWidth, screenHeight);
            child.offsetX = before.bounds.x() - child.anchor.x(screenWidth);
            child.offsetY = before.bounds.y() - child.anchor.y(screenHeight);
            child.scale = before.scale;
            return;
        }
        HudElementLayout parent = layouts.get(parentId);
        if (parent == null || parent == child || wouldCreateCycle(child.id, parentId, layouts)) return;
        Resolved parentTransform = resolve(parent, layouts, screenWidth, screenHeight, stackVisible);
        double scale = Math.max(MINIMUM_SCALE, parentTransform.scale);
        child.parentId = parentId;
        child.offsetX = (before.bounds.x() - parentTransform.bounds.x()) / scale;
        child.offsetY = (before.bounds.y() - parentTransform.bounds.y()) / scale;
        child.scale = before.scale / scale;
    }

    private static Resolved resolve(HudElementLayout layout, Map<String, HudElementLayout> layouts,
                                    int screenWidth, int screenHeight, Predicate<String> stackVisible,
                                    Set<String> path) {
        if (layout == null) return new Resolved(new Bounds(0.0, 0.0, 0.0, 0.0), 1.0);
        String key = layout.id == null || layout.id.isBlank()
                ? "@" + System.identityHashCode(layout) : layout.id;
        if (!path.add(key)) return root(layout, screenWidth, screenHeight);
        try {
            double x;
            double y;
            double effectiveScale;
            HudElementLayout parent = relation(layout.parentId, layout, layouts);
            if (parent == null) {
                x = layout.anchor.x(screenWidth) + layout.offsetX;
                y = layout.anchor.y(screenHeight) + layout.offsetY;
                effectiveScale = layout.scale;
            } else {
                Resolved resolvedParent = resolve(parent, layouts, screenWidth, screenHeight, stackVisible, path);
                x = resolvedParent.bounds.x() + layout.offsetX * resolvedParent.scale;
                y = resolvedParent.bounds.y() + layout.offsetY * resolvedParent.scale;
                effectiveScale = layout.scale * resolvedParent.scale;
            }

            double width = layout.unscaledWidth() * effectiveScale;
            double height = layout.unscaledHeight() * effectiveScale;
            HudElementLayout stackTarget = relation(layout.stackOnId, layout, layouts);
            if (stackTarget != null && stackVisible.test(stackTarget.id)) {
                Resolved target = resolve(stackTarget, layouts, screenWidth, screenHeight, stackVisible, path);
                x = target.bounds.centerX() - width / 2.0 + layout.stackOffsetX;
                y = target.bounds.y() - height + layout.stackOffsetY;
            }
            return new Resolved(new Bounds(x, y, width, height), effectiveScale);
        } finally {
            path.remove(key);
        }
    }

    private static Resolved root(HudElementLayout layout, int screenWidth, int screenHeight) {
        double scale = layout.scale;
        return new Resolved(new Bounds(layout.anchor.x(screenWidth) + layout.offsetX,
                layout.anchor.y(screenHeight) + layout.offsetY,
                layout.unscaledWidth() * scale, layout.unscaledHeight() * scale), scale);
    }

    private static double parentScale(HudElementLayout layout, Map<String, HudElementLayout> layouts,
                                      int screenWidth, int screenHeight, Predicate<String> stackVisible) {
        HudElementLayout parent = relation(layout.parentId, layout, layouts);
        if (parent == null) return 1.0;
        return Math.max(MINIMUM_SCALE,
                resolve(parent, layouts, screenWidth, screenHeight, stackVisible).scale);
    }

    private static boolean stackActive(HudElementLayout layout, Map<String, HudElementLayout> layouts,
                                       Predicate<String> stackVisible) {
        HudElementLayout target = relation(layout.stackOnId, layout, layouts);
        return target != null && stackVisible.test(target.id);
    }

    private static HudElementLayout relation(String id, HudElementLayout self,
                                             Map<String, HudElementLayout> layouts) {
        if (id == null || id.isBlank() || id.equals(self.id)) return null;
        return layouts.get(id);
    }

    private static boolean reaches(String currentId, String targetId,
                                   Map<String, HudElementLayout> layouts, Set<String> visited) {
        if (currentId.equals(targetId)) return true;
        if (!visited.add(currentId)) return false;
        HudElementLayout current = layouts.get(currentId);
        if (current == null) return false;
        return reachesRelation(current.parentId, targetId, layouts, visited)
                || reachesRelation(current.stackOnId, targetId, layouts, visited);
    }

    private static boolean reachesRelation(String id, String targetId,
                                           Map<String, HudElementLayout> layouts, Set<String> visited) {
        return id != null && !id.isBlank() && reaches(id, targetId, layouts, visited);
    }

    public record Resolved(Bounds bounds, double scale) {
    }
}
