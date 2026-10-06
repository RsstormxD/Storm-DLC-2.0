package xyz.angames.astolfoclient.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public final class DiscordAvatarManager {
    public record Profile(String username, String userId) {
        public boolean available() { return !username.isBlank() && !userId.isBlank(); }
    }
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "StormDLC-DiscordAvatar");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile Profile profile = new Profile("", "");
    private static volatile ResourceLocation avatarTexture;
    private static volatile String currentUrl = "";
    private static volatile long generation, lastAttempt;
    private static volatile boolean downloading, stopped;
    private static long sequence;

    private DiscordAvatarManager() {}
    public static Profile profile() { return profile; }

    public static void update(String username, String userId, String avatarHash) {
        if (stopped || userId == null || !userId.matches("[0-9]{1,24}")) return;
        profile = new Profile(username == null ? "" : username.strip(), userId);
        if (avatarHash != null && avatarHash.matches("[a-zA-Z0-9_]+"))
            fetch("https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png?size=128");
        else {
            try {
                long id = Long.parseUnsignedLong(userId);
                fetch("https://cdn.discordapp.com/embed/avatars/" + ((id >>> 22) % 6) + ".png");
            } catch (NumberFormatException ignored) {}
        }
    }

    public static synchronized void fetch(String url) {
        if (stopped || url == null || url.isBlank()) return;
        URI uri;
        try { uri = URI.create(url); }
        catch (IllegalArgumentException ignored) { return; }
        if (!"https".equals(uri.getScheme()) || !"cdn.discordapp.com".equals(uri.getHost())) return;
        if (!url.equals(currentUrl)) {
            currentUrl = url;
            generation++;
            downloading = false;
            lastAttempt = 0;
            Minecraft.getInstance().execute(DiscordAvatarManager::releaseTexture);
        }
        long now = System.nanoTime();
        if (downloading || avatarTexture != null || lastAttempt != 0 && now - lastAttempt < 15_000_000_000L) return;
        downloading = true;
        lastAttempt = now;
        long requestGeneration = generation;
        WORKER.execute(() -> download(uri, requestGeneration));
    }

    private static void download(URI uri, long requestGeneration) {
        HttpURLConnection connection = null;
        NativeImage image = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "StormDLC/2.0");
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != 200 || connection.getContentLengthLong() > 2_097_152) return;
            byte[] bytes;
            try (var input = connection.getInputStream()) { bytes = input.readNBytes(2_097_153); }
            if (bytes.length > 2_097_152) return;
            image = NativeImage.read(new ByteArrayInputStream(bytes));
            if (image.getWidth() > 512 || image.getHeight() > 512) return;
            NativeImage ready = image;
            image = null;
            Minecraft.getInstance().execute(() -> {
                if (stopped || requestGeneration != generation) { ready.close(); return; }
                DynamicTexture texture = null;
                try {
                    texture = new DynamicTexture(ready);
                    texture.setFilter(true, false);
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("stormdlc", "runtime/discord_avatar_" + sequence++);
                    Minecraft.getInstance().getTextureManager().register(id, texture);
                    releaseTexture();
                    avatarTexture = id;
                } catch (RuntimeException failure) {
                    if (texture != null) texture.close();
                    else ready.close();
                }
            });
        } catch (Exception ignored) {
        } finally {
            if (image != null) image.close();
            if (connection != null) connection.disconnect();
            if (requestGeneration == generation) downloading = false;
        }
    }

    public static ResourceLocation getAvatarTexture() {
        if (!stopped && avatarTexture == null && !currentUrl.isBlank()) fetch(currentUrl);
        return avatarTexture;
    }

    private static void releaseTexture() {
        ResourceLocation previous = avatarTexture;
        avatarTexture = null;
        if (previous != null) Minecraft.getInstance().getTextureManager().release(previous);
    }

    public static void shutdown() {
        stopped = true;
        generation++;
        WORKER.shutdownNow();
        releaseTexture();
    }
}
