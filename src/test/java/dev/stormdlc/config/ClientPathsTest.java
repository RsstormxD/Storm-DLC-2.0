package dev.stormdlc.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ClientPathsTest {
    @TempDir Path root;

    @Test
    void movesExistingProfilesAndLyricsIntoTheNewConfigDirectory() throws Exception {
        Path old = root.resolve("stormvisuals");
        Files.createDirectories(old.resolve("configs"));
        Files.createDirectories(old.resolve("song-island/lyrics"));
        Files.writeString(old.resolve("configs/my-profile.json"), "saved profile");
        Files.writeString(old.resolve("song-island/lyrics/song.lrc"), "[00:01.00]saved lyric");

        Path current = ClientPaths.migrateConfigDirectory(root);

        assertEquals(root.resolve("stormdlc"), current);
        assertEquals("saved profile", Files.readString(current.resolve("configs/my-profile.json")));
        assertEquals("[00:01.00]saved lyric", Files.readString(current.resolve("song-island/lyrics/song.lrc")));
        assertFalse(Files.exists(old));
    }

    @Test
    void preservesNewSettingsWhenBothDirectoriesAlreadyExist() throws Exception {
        Path old = Files.createDirectories(root.resolve("stormvisuals"));
        Path current = Files.createDirectories(root.resolve("stormdlc"));
        Files.writeString(old.resolve("settings.json"), "previous settings");
        Files.writeString(current.resolve("settings.json"), "new settings");

        assertEquals(current, ClientPaths.migrateConfigDirectory(root));

        assertEquals("new settings", Files.readString(current.resolve("settings.json")));
        assertEquals("previous settings", Files.readString(old.resolve("settings.json")));
    }
}
