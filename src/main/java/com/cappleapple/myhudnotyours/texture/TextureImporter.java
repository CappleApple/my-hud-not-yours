package com.cappleapple.myhudnotyours.texture;

import com.cappleapple.myhudnotyours.MyHudNotYours;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.client.Minecraft;

public final class TextureImporter {
    private TextureImporter() {
    }

    public static void chooseAndImport(Runnable completed) {
        if (GraphicsEnvironment.isHeadless()) {
            MyHudNotYours.LOGGER.warn("Cannot open a texture file picker in a headless environment");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Import HUD Texture");
            chooser.setFileFilter(new FileNameExtensionFilter("PNG textures", "png"));
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
            Path selected = chooser.getSelectedFile().toPath();
            CompletableFuture.runAsync(() -> {
                try {
                    importFile(selected);
                    Minecraft.getInstance().execute(() -> {
                        ManagedTextureResolver.get().clear();
                        TextureCatalog.invalidate();
                        completed.run();
                    });
                } catch (Exception exception) {
                    MyHudNotYours.LOGGER.error("Could not import HUD texture {}", selected, exception);
                }
            });
        });
    }

    public static Path importFile(Path selected) throws Exception {
        return importFile(selected, LayoutStore.managedTextureDirectory());
    }

    static Path importFile(Path selected, Path directory) throws Exception {
        if (!Files.isRegularFile(selected)
                || !selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            throw new IllegalArgumentException("Only PNG files can be imported");
        }
        Files.createDirectories(directory);
        String fileName = selected.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        Path target = unique(directory, fileName);
        Files.copy(selected, target, StandardCopyOption.COPY_ATTRIBUTES);
        Path metadata = selected.resolveSibling(selected.getFileName() + ".mcmeta");
        if (Files.isRegularFile(metadata)) {
            Files.copy(metadata, target.resolveSibling(target.getFileName() + ".mcmeta"),
                    StandardCopyOption.COPY_ATTRIBUTES);
        }
        return target;
    }

    private static Path unique(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) return candidate;
        int dot = fileName.toLowerCase(Locale.ROOT).lastIndexOf(".png");
        String stem = dot < 0 ? fileName : fileName.substring(0, dot);
        String suffix = dot < 0 ? "" : fileName.substring(dot);
        for (int index = 2; ; index++) {
            candidate = directory.resolve(stem + "-" + index + suffix);
            if (!Files.exists(candidate)) return candidate;
        }
    }
}
