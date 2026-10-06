package dev.stormdlc.menu;

import com.mojang.blaze3d.platform.NativeImage;
import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import dev.stormdlc.config.ClientPaths;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule;

public final class MenuWallpaper {
    public record Texture(ResourceLocation id, int width, int height) {}
    private record Loaded(NativeImage image, long generation, String message) {}
    public interface WindowsWallpaper extends StdCallLibrary {
        boolean SystemParametersInfoW(int action, int size, char[] path, int flags);
    }

    private static final Logger LOG = LoggerFactory.getLogger("StormDLC/Menu");
    private static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final ResourceLocation BUNDLED = ResourceLocation.fromNamespaceAndPath("stormdlc", "menu/wallpaper.jpg");
    private static final Texture FALLBACK = new Texture(BUNDLED, 1920, 1154);
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "StormDLC-Wallpaper");
        thread.setDaemon(true);
        return thread;
    });
    private static final Object COMPLETION_LOCK = new Object();
    private static final AtomicBoolean loading = new AtomicBoolean();
    private static final AtomicReference<Loaded> completed = new AtomicReference<>();
    private static volatile long generation;
    private static volatile boolean stopped;
    private static boolean needsRefresh = true;
    private static MainMenuModule.Wallpaper selected;
    private static Texture texture;
    private static long textureSequence;
    private static volatile String status = "";

    private MenuWallpaper() {}

    public static String status() { return status; }

    public static void refresh() {
        if (stopped) return;
        generation++;
        needsRefresh = true;
    }

    public static String customPath() {
        Path file = ClientPaths.configDirectory().resolve("menu").resolve("wallpaper-path.txt");
        try {
            return Files.isRegularFile(file) && Files.size(file) <= 65536
                ? Files.readString(file, StandardCharsets.UTF_8).strip() : "";
        } catch (IOException failure) { return ""; }
    }

    public static void selectCustom(String input) throws IOException {
        String value = input.strip();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1)
            value = value.substring(1, value.length() - 1);
        if (value.isBlank() || value.length() > 32768) throw new IOException("Paste a path to a PNG or JPG image");
        Path source;
        try { source = Path.of(value).toAbsolutePath().normalize().toRealPath(); }
        catch (IOException | RuntimeException failure) { throw new IOException("Image file not found"); }
        if (!Files.isRegularFile(source) || Files.size(source) > MAX_BYTES) throw new IOException("Select an image smaller than 32 MB");
        String extension = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!extension.endsWith(".png") && !extension.endsWith(".jpg") && !extension.endsWith(".jpeg"))
            throw new IOException("Choose a PNG or JPG image");
        Path directory = ClientPaths.configDirectory().resolve("menu");
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "wallpaper-path-", ".tmp");
        try {
            Files.writeString(temporary, source.toString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, directory.resolve("wallpaper-path.txt"),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, directory.resolve("wallpaper-path.txt"), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
        MainMenuModule.INSTANCE.wallpaper.setValue(MainMenuModule.Wallpaper.CUSTOM);
        refresh();
    }

    public static Texture texture() {
        MainMenuModule module = MainMenuModule.INSTANCE;
        if (stopped || module == null) return FALLBACK;
        if (selected != module.wallpaper.getValue()) {
            selected = module.wallpaper.getValue();
            refresh();
        }
        Loaded result = completed.getAndSet(null);
        if (result != null) {
            if (result.generation() == generation) adopt(result);
            else result.image().close();
        }
        if (needsRefresh && loading.compareAndSet(false, true)) {
            needsRefresh = false;
            long requestedGeneration = generation;
            MainMenuModule.Wallpaper requested = selected;
            status = "Loading wallpaper...";
            try { WORKER.execute(() -> load(requested, requestedGeneration)); }
            catch (RejectedExecutionException ignored) { loading.set(false); }
        }
        return texture == null ? FALLBACK : texture;
    }

    private static void load(MainMenuModule.Wallpaper source, long requestedGeneration) {
        NativeImage image = null;
        String message = "Wallpaper loaded";
        try {
            try {
                Path path = switch (source) {
                    case DESKTOP -> desktopPath();
                    case CUSTOM -> {
                        String custom = customPath();
                        yield custom.isBlank() ? null : Path.of(custom);
                    }
                    case BUNDLED -> null;
                };
                if (path == null) {
                    if (source == MainMenuModule.Wallpaper.CUSTOM) throw new IOException("No custom image selected");
                    image = bundled();
                } else {
                    if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) throw new IOException("Wallpaper is missing or too large");
                    try (InputStream input = Files.newInputStream(path)) { image = decode(input); }
                }
            } catch (IOException | RuntimeException failure) {
                LOG.warn("Cannot load {} wallpaper: {}", source, failure.toString());
                image = bundled();
                message = "Image unavailable - using Storm wallpaper";
            }
            synchronized (COMPLETION_LOCK) {
                if (stopped || requestedGeneration != generation) image.close();
                else {
                    Loaded previous = completed.getAndSet(new Loaded(image, requestedGeneration, message));
                    if (previous != null) previous.image().close();
                }
                image = null;
            }
        } catch (IOException | RuntimeException | LinkageError failure) {
            if (!stopped && requestedGeneration == generation) status = "Unable to load wallpaper";
            LOG.warn("Wallpaper loader failed: {}", failure.toString());
        } finally {
            if (image != null) image.close();
            loading.set(false);
        }
    }

    private static NativeImage bundled() throws IOException {
        try (InputStream input = Minecraft.getInstance().getResourceManager().open(BUNDLED)) { return decode(input); }
    }

    private static NativeImage decode(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new IOException("Image exceeds 32 MB");
        try (ImageInputStream info = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(info);
            if (!readers.hasNext()) throw new IOException("Unsupported image format");
            ImageReader reader = readers.next();
            try {
                reader.setInput(info, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || (long) width * height > 16777216)
                    throw new IOException("Image exceeds the supported dimensions");
            } finally { reader.dispose(); }
        }
        return NativeImage.read(new ByteArrayInputStream(bytes));
    }

    private static Path desktopPath() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) return null;
        try {
            char[] buffer = new char[32768];
            WindowsWallpaper api = Native.load("user32", WindowsWallpaper.class, W32APIOptions.UNICODE_OPTIONS);
            if (api.SystemParametersInfoW(0x0073, buffer.length, buffer, 0)) {
                int end = 0;
                while (end < buffer.length && buffer[end] != 0) end++;
                if (end > 0) {
                    Path path = Path.of(new String(buffer, 0, end));
                    if (Files.isRegularFile(path)) return path;
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            LOG.debug("Cannot query Windows wallpaper: {}", failure.toString());
        }
        String appData = System.getenv("APPDATA");
        if (appData == null) return null;
        Path cached = Path.of(appData).resolve("Microsoft/Windows/Themes/TranscodedWallpaper");
        return Files.isRegularFile(cached) ? cached : null;
    }

    private static void adopt(Loaded result) {
        Minecraft client = Minecraft.getInstance();
        DynamicTexture uploaded = null;
        try {
            int width = result.image().getWidth(), height = result.image().getHeight();
            uploaded = new DynamicTexture(result.image());
            uploaded.setFilter(true, false);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("stormdlc", "runtime/wallpaper_" + textureSequence++);
            client.getTextureManager().register(id, uploaded);
            Texture previous = texture;
            texture = new Texture(id, width, height);
            if (previous != null) client.getTextureManager().release(previous.id());
            status = result.message();
        } catch (RuntimeException failure) {
            if (uploaded != null) uploaded.close();
            else result.image().close();
            status = "Unable to upload wallpaper";
            LOG.warn("Wallpaper upload failed: {}", failure.toString());
        }
    }

    public static void release() {
        refresh();
        synchronized (COMPLETION_LOCK) {
            Loaded pending = completed.getAndSet(null);
            if (pending != null) pending.image().close();
        }
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(texture.id());
            texture = null;
        }
    }

    public static void shutdown() {
        stopped = true;
        generation++;
        WORKER.shutdownNow();
        release();
    }
}
