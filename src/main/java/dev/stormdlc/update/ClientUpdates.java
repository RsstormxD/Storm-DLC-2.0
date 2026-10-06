package dev.stormdlc.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.slf4j.LoggerFactory;

/** Reads public GitHub releases without downloading or installing anything. */
public final class ClientUpdates {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("StormDLC/Updates");
    private static final String REPOSITORY = readRepository();
    private static volatile Release latest;
    private static ScheduledExecutorService worker;
    private static String lastToastVersion = "", lastChatVersion = "";

    private ClientUpdates() {}

    public static String currentVersion() {
        return FabricLoader.getInstance().getModContainer("stormdlc")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString().split("\\+", 2)[0])
            .orElse("0.0.0");
    }

    public static String repositoryUrl() {
        return REPOSITORY.isEmpty() ? "https://github.com" : "https://github.com/" + REPOSITORY;
    }

    public static boolean isOutdated() {
        return latest != null;
    }

    public static synchronized void start(Minecraft client) {
        if (worker != null || REPOSITORY.isEmpty()) return;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "stormdlc-update-checker");
            thread.setDaemon(true);
            return thread;
        });
        worker.scheduleWithFixedDelay(() -> check(client, http), 0, 30, TimeUnit.MINUTES);
    }

    public static synchronized void stop() {
        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
    }

    private static String readRepository() {
        try (var stream = ClientUpdates.class.getResourceAsStream("/stormdlc-update.json")) {
            if (stream == null) return "";
            JsonObject config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            String repository = config.get("repository").getAsString().trim();
            return repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") ? repository : "";
        } catch (Exception failure) {
            LOG.debug("Cannot read update repository", failure);
            return "";
        }
    }

    private static void check(Minecraft client, HttpClient http) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPOSITORY + "/releases?per_page=30"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Storm-DLC/" + currentVersion())
                .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return;

            String minecraft = FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("1.21.4");
            Version installed = Version.parse(currentVersion());
            Version newest = installed;
            Release candidate = null;
            for (var element : JsonParser.parseString(response.body()).getAsJsonArray()) {
                JsonObject release = element.getAsJsonObject();
                if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) continue;
                String tag = release.get("tag_name").getAsString();
                if (tag.startsWith("v") || tag.startsWith("V")) tag = tag.substring(1);
                if (!tag.endsWith("+" + minecraft)) continue;
                String version = tag.substring(0, tag.indexOf('+'));
                Version available;
                try { available = Version.parse(version); } catch (Exception ignored) { continue; }
                if (available.compareTo(newest) <= 0) continue;
                String expectedJar = "storm-dlc-" + tag + ".jar";
                boolean hasJar = false;
                for (var asset : release.getAsJsonArray("assets")) {
                    if (asset.getAsJsonObject().get("name").getAsString().equals(expectedJar)) {
                        hasJar = true;
                        break;
                    }
                }
                if (!hasJar) continue;
                String page = release.get("html_url").getAsString();
                if (!page.startsWith(repositoryUrl() + "/releases/")) continue;
                newest = available;
                candidate = new Release(version, page);
            }
            latest = candidate;
            if (candidate != null) client.execute(() -> notifyToast(client));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            LOG.debug("Update check unavailable: {}", failure.toString());
        }
    }

    private static void notifyToast(Minecraft client) {
        Release release = latest;
        if (release == null || lastToastVersion.equals(release.version)) return;
        lastToastVersion = release.version;
        SystemToast.add(client.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
            Component.literal("Storm DLC: nowa wersja"), Component.literal("Dostępna wersja " + release.version + ". Link na czacie."));
    }

    public static void notifyPlayer(Minecraft client) {
        Release release = latest;
        if (release == null || client.player == null || lastChatVersion.equals(release.version)) return;
        lastChatVersion = release.version;
        client.player.displayClientMessage(Component.literal("[Storm DLC] Dostępna jest nowa wersja " + release.version + ". ")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal("[Pobierz]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, release.page))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Otwórz wydanie na GitHubie"))))), false);
    }

    private record Release(String version, String page) {}
}
