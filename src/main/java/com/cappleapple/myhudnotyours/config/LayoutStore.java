package com.cappleapple.myhudnotyours.config;

import com.cappleapple.myhudnotyours.MyHudNotYours;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.neoforged.fml.loading.FMLPaths;

/** Human-readable, atomic, debounce-saved HUD layout persistence. */
public final class LayoutStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long SAVE_DEBOUNCE_MILLIS = 250L;
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(MyHudNotYours.MOD_ID);
    private static final Path FILE = DIRECTORY.resolve("hud-layout.json");
    private static final Path LEGACY_DIRECTORY = FMLPaths.CONFIGDIR.get().resolve("elementsnotscreens");
    private static final Path LEGACY_FILE = LEGACY_DIRECTORY.resolve("hud-layout.json");
    private static HudConfig config = new HudConfig();
    private static boolean loaded;
    private static boolean dirty;
    private static long dirtySince;

    private LayoutStore() {
    }

    public static synchronized HudConfig get() {
        if (!loaded) load();
        return config;
    }

    public static synchronized void load() {
        HudConfig replacement = new HudConfig();
        boolean migratingLegacyLayout = !Files.isRegularFile(FILE) && Files.isRegularFile(LEGACY_FILE);
        Path source = migratingLegacyLayout ? LEGACY_FILE : FILE;
        if (Files.isRegularFile(source)) {
            try (Reader reader = Files.newBufferedReader(source)) {
                HudConfig parsed = GSON.fromJson(reader, HudConfig.class);
                if (parsed != null) replacement = parsed;
            } catch (Exception exception) {
                MyHudNotYours.LOGGER.error("Could not read HUD layout {}; keeping defaults", source, exception);
            }
        }
        replacement.sanitize();
        config = replacement;
        loaded = true;
        dirty = migratingLegacyLayout;
        if (migratingLegacyLayout) {
            migrateLegacyTextures();
            saveNow();
            MyHudNotYours.LOGGER.info("Migrated legacy HUD configuration from {} to {}; the original was retained",
                    LEGACY_DIRECTORY, DIRECTORY);
        }
    }

    public static synchronized void markDirty() {
        get();
        dirty = true;
        dirtySince = System.currentTimeMillis();
    }

    public static synchronized void tick() {
        if (dirty && System.currentTimeMillis() - dirtySince >= SAVE_DEBOUNCE_MILLIS) saveNow();
    }

    public static synchronized void saveNow() {
        if (!loaded || !dirty) return;
        config.sanitize();
        Path temporary = DIRECTORY.resolve("hud-layout.json.tmp");
        try {
            Files.createDirectories(DIRECTORY);
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException exception) {
            MyHudNotYours.LOGGER.error("Could not save HUD layout {}", FILE, exception);
        }
    }

    public static Path managedTextureDirectory() {
        return DIRECTORY.resolve("textures");
    }

    private static void migrateLegacyTextures() {
        Path source = LEGACY_DIRECTORY.resolve("textures");
        Path destination = managedTextureDirectory();
        if (!Files.isDirectory(source)) return;
        try (var paths = Files.walk(source)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path legacyPath = iterator.next();
                Path relative = source.relativize(legacyPath);
                Path migratedPath = destination.resolve(relative).normalize();
                if (!migratedPath.startsWith(destination)) continue;
                if (Files.isDirectory(legacyPath)) {
                    Files.createDirectories(migratedPath);
                } else if (!Files.exists(migratedPath)) {
                    Files.createDirectories(migratedPath.getParent());
                    Files.copy(legacyPath, migratedPath, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        } catch (Exception exception) {
            MyHudNotYours.LOGGER.warn("Could not migrate every legacy imported HUD texture from {}",
                    source, exception);
        }
    }
}
