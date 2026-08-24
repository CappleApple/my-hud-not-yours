package com.cappleapple.myhudnotyours.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Minimal standard Minecraft animation .mcmeta reader. */
public final class AnimationSpec {
    private final int frameWidth;
    private final int frameHeight;
    private final List<Frame> frames;
    private final int totalTicks;

    private AnimationSpec(int frameWidth, int frameHeight, List<Frame> frames) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frames = List.copyOf(frames);
        this.totalTicks = frames.stream().mapToInt(Frame::durationTicks).sum();
    }

    public static AnimationSpec still(int imageWidth, int imageHeight) {
        return new AnimationSpec(imageWidth, imageHeight, List.of(new Frame(0, Integer.MAX_VALUE)));
    }

    public static AnimationSpec parse(JsonObject root, int imageWidth, int imageHeight) {
        if (root == null || !root.has("animation") || !root.get("animation").isJsonObject()) {
            return still(imageWidth, imageHeight);
        }
        JsonObject animation = root.getAsJsonObject("animation");
        int defaultTime = positive(animation, "frametime", 1);
        int width = positive(animation, "width", imageWidth);
        int height = positive(animation, "height", width);
        width = Math.max(1, Math.min(width, imageWidth));
        height = Math.max(1, Math.min(height, imageHeight));
        int columns = Math.max(1, imageWidth / width);
        int rows = Math.max(1, imageHeight / height);
        int available = columns * rows;
        List<Frame> frames = new ArrayList<>();
        JsonArray configured = animation.has("frames") && animation.get("frames").isJsonArray()
                ? animation.getAsJsonArray("frames") : null;
        if (configured != null) {
            for (JsonElement element : configured) {
                int index;
                int duration = defaultTime;
                if (element.isJsonPrimitive()) {
                    index = element.getAsInt();
                } else if (element.isJsonObject()) {
                    JsonObject frame = element.getAsJsonObject();
                    if (!frame.has("index")) continue;
                    index = frame.get("index").getAsInt();
                    duration = positive(frame, "time", defaultTime);
                } else {
                    continue;
                }
                if (index >= 0 && index < available) frames.add(new Frame(index, duration));
            }
        }
        if (frames.isEmpty()) {
            for (int index = 0; index < available; index++) frames.add(new Frame(index, defaultTime));
        }
        return new AnimationSpec(width, height, frames);
    }

    private static int positive(JsonObject object, String key, int fallback) {
        if (!object.has(key)) return fallback;
        try {
            return Math.max(1, object.get(key).getAsInt());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public FrameRegion frameAt(long nowMillis, int imageWidth) {
        int frameIndex = 0;
        if (totalTicks > 0 && totalTicks < Integer.MAX_VALUE) {
            int tick = (int) Math.floorMod(nowMillis / 50L, totalTicks);
            int elapsed = 0;
            for (Frame frame : frames) {
                elapsed += frame.durationTicks;
                if (tick < elapsed) {
                    frameIndex = frame.index;
                    break;
                }
            }
        } else {
            frameIndex = frames.getFirst().index;
        }
        int columns = Math.max(1, imageWidth / frameWidth);
        return new FrameRegion((frameIndex % columns) * frameWidth, (frameIndex / columns) * frameHeight,
                frameWidth, frameHeight);
    }

    public int frameWidth() {
        return frameWidth;
    }

    public int frameHeight() {
        return frameHeight;
    }

    public boolean animated() {
        return frames.size() > 1;
    }

    private record Frame(int index, int durationTicks) {
    }

    public record FrameRegion(int x, int y, int width, int height) {
    }
}
