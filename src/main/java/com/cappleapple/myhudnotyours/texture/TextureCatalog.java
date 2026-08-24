package com.cappleapple.myhudnotyours.texture;

import com.cappleapple.myhudnotyours.MyHudNotYours;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.model.TextureReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** On-demand, cached enumeration of active resource views and managed imports. */
public final class TextureCatalog {
    private static List<Entry> cache;

    private TextureCatalog() {
    }

    public static synchronized List<Entry> entries() {
        if (cache == null) cache = scan();
        return cache;
    }

    public static synchronized void invalidate() {
        cache = null;
    }

    private static List<Entry> scan() {
        List<Entry> entries = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources("textures",
                id -> id.getPath().toLowerCase(Locale.ROOT).endsWith(".png"));
        resources.forEach((id, resource) -> {
            String pack = resource.sourcePackId();
            Category category = classify(id, pack);
            entries.add(new Entry(new TextureReference(TextureReference.Kind.RESOURCE, id.toString()),
                    id.toString(), id.getNamespace(), id.getPath(), pack, category));
        });

        Path root = LayoutStore.managedTextureDirectory();
        if (Files.isDirectory(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .forEach(path -> {
                            String relative = root.relativize(path).toString().replace('\\', '/');
                            entries.add(new Entry(new TextureReference(TextureReference.Kind.IMPORTED, relative),
                                    "imported:" + relative, MyHudNotYours.MOD_ID, relative,
                                    "Imported", Category.IMPORTED));
                        });
            } catch (IOException exception) {
                MyHudNotYours.LOGGER.warn("Could not enumerate imported HUD textures", exception);
            }
        }
        entries.sort(Comparator.comparing(Entry::displayId));
        return List.copyOf(entries);
    }

    private static Category classify(ResourceLocation id, String pack) {
        String source = pack.toLowerCase(Locale.ROOT);
        if (id.getNamespace().equals("minecraft") && (source.equals("vanilla") || source.contains("builtin"))) {
            return Category.MINECRAFT;
        }
        if (source.startsWith("mod/") || source.startsWith("mod:") || source.equals(id.getNamespace())) {
            return Category.MODS;
        }
        return id.getNamespace().equals("minecraft") && source.equals("vanilla")
                ? Category.MINECRAFT : Category.RESOURCE_PACKS;
    }

    public enum Category {
        ALL("All"),
        MINECRAFT("Minecraft"),
        MODS("Mods"),
        RESOURCE_PACKS("Packs"),
        IMPORTED("Imported");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record Entry(TextureReference reference, String displayId, String namespace,
                        String path, String sourcePack, Category category) {
        public boolean matches(String query) {
            String normalized = query.toLowerCase(Locale.ROOT).trim();
            return normalized.isEmpty() || displayId.toLowerCase(Locale.ROOT).contains(normalized)
                    || sourcePack.toLowerCase(Locale.ROOT).contains(normalized);
        }
    }
}
