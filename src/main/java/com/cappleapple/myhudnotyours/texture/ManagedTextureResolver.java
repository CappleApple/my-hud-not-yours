package com.cappleapple.myhudnotyours.texture;

import com.cappleapple.myhudnotyours.MyHudNotYours;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.model.TextureReference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/** Loads selected resource-pack or imported PNGs once and resolves standard animated frames. */
public final class ManagedTextureResolver {
    private static final ManagedTextureResolver INSTANCE = new ManagedTextureResolver();
    private final Map<String, LoadedTexture> loaded = new HashMap<>();
    private int sequence;

    private ManagedTextureResolver() {
    }

    public static ManagedTextureResolver get() {
        return INSTANCE;
    }

    @Nullable
    public synchronized TextureHandle resolve(TextureReference reference, long nowMillis) {
        if (reference == null || !reference.assigned()) return null;
        LoadedTexture texture = loaded.get(reference.key());
        if (texture == null) {
            texture = load(reference);
            if (texture == null) return null;
            loaded.put(reference.key(), texture);
        }
        AnimationSpec.FrameRegion frame = texture.animation.frameAt(nowMillis, texture.width);
        return new TextureHandle(texture.location, texture.width, texture.height,
                frame.x(), frame.y(), frame.width(), frame.height(), texture.animation.animated());
    }

    public synchronized void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (LoadedTexture texture : loaded.values()) minecraft.getTextureManager().release(texture.location);
        loaded.clear();
    }

    @Nullable
    private LoadedTexture load(TextureReference reference) {
        try {
            NativeImage image;
            JsonObject metadata;
            if (reference.kind == TextureReference.Kind.RESOURCE) {
                ResourceLocation id = ResourceLocation.tryParse(reference.id);
                if (id == null) return null;
                ResourceManager manager = Minecraft.getInstance().getResourceManager();
                Resource resource = manager.getResource(id).orElse(null);
                if (resource == null) return null;
                try (InputStream stream = resource.open()) {
                    image = NativeImage.read(stream);
                }
                metadata = readMetadata(manager, ResourceLocation.fromNamespaceAndPath(
                        id.getNamespace(), id.getPath() + ".mcmeta"));
            } else {
                Path root = LayoutStore.managedTextureDirectory().toAbsolutePath().normalize();
                Path png = root.resolve(reference.id).normalize();
                if (!png.startsWith(root) || !Files.isRegularFile(png)) return null;
                try (InputStream stream = Files.newInputStream(png)) {
                    image = NativeImage.read(stream);
                }
                metadata = readMetadata(png.resolveSibling(png.getFileName() + ".mcmeta"));
            }
            ResourceLocation location = MyHudNotYours.id("managed/" + Integer.toUnsignedString(reference.key().hashCode(), 16)
                    + "_" + sequence++);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            return new LoadedTexture(location, image.getWidth(), image.getHeight(),
                    AnimationSpec.parse(metadata, image.getWidth(), image.getHeight()));
        } catch (Exception exception) {
            MyHudNotYours.LOGGER.warn("Could not load HUD texture {}", reference.key(), exception);
            return null;
        }
    }

    @Nullable
    private static JsonObject readMetadata(ResourceManager manager, ResourceLocation id) {
        Resource resource = manager.getResource(id).orElse(null);
        if (resource == null) return null;
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static JsonObject readMetadata(Path path) {
        if (!Files.isRegularFile(path)) return null;
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private record LoadedTexture(ResourceLocation location, int width, int height, AnimationSpec animation) {
    }
}
