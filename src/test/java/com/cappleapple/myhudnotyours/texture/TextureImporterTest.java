package com.cappleapple.myhudnotyours.texture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextureImporterTest {
    @TempDir
    Path temporary;

    @Test
    void copiesPngAndMatchingMetadataWithoutOverwriting() throws Exception {
        Path source = temporary.resolve("source").resolve("health bar.png");
        Files.createDirectories(source.getParent());
        byte[] png = {1, 2, 3, 4};
        Files.write(source, png);
        Files.writeString(source.resolveSibling("health bar.png.mcmeta"), "{\"animation\":{}}");
        Path targetDirectory = temporary.resolve("managed");

        Path first = TextureImporter.importFile(source, targetDirectory);
        Path second = TextureImporter.importFile(source, targetDirectory);

        assertEquals("health_bar.png", first.getFileName().toString());
        assertEquals("health_bar-2.png", second.getFileName().toString());
        assertArrayEquals(png, Files.readAllBytes(first));
        assertTrue(Files.isRegularFile(first.resolveSibling("health_bar.png.mcmeta")));
        assertTrue(Files.isRegularFile(second.resolveSibling("health_bar-2.png.mcmeta")));
    }
}
