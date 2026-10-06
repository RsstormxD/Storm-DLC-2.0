package dev.stormdlc.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

public final class ClientPaths {
    private ClientPaths() {}

    public static Path configDirectory() {
        return migrateConfigDirectory(FabricLoader.getInstance().getConfigDir());
    }

    public static synchronized Path migrateConfigDirectory(Path root) {
        Path current = root.resolve("stormdlc");
        // The old directory name is kept only for upgrading existing installations.
        Path legacy = root.resolve("stormvisuals");
        if (!Files.exists(current) && Files.isDirectory(legacy)) {
            try {
                Files.move(legacy, current);
            } catch (IOException failure) {
                LoggerFactory.getLogger("StormDLC/Config").warn("Could not migrate existing client settings", failure);
            }
        }
        return current;
    }
}
