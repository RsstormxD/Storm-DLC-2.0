package dev.stormdlc.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

public final class ClientFeedback {
    public enum Tone {
        SUCCESS(0xff71e3ae), INFO(0xff72b8ff), WARNING(0xffffc56b), ERROR(0xffff7b87);
        private final int color;
        Tone(int color) { this.color = color; }
        public int color() { return color; }
    }

    public record Notice(String key, String title, String message, Tone tone, long created, long duration, int priority) {
        public long age() { return Math.max(0, now() - created); }
        public boolean expired() { return age() >= duration; }
        public float remaining() { return Math.max(0.0F, 1.0F - age() / (float) duration); }
    }

    public static final class Silence implements AutoCloseable {
        private boolean closed;
        private Silence() { suppressed++; }
        @Override public void close() {
            if (!closed) { closed = true; suppressed = Math.max(0, suppressed - 1); }
        }
    }

    private static final List<Notice> notices = new ArrayList<>(4);
    private static Notice reaction;
    private static int suppressed;
    private static boolean stopping;
    private static UUID playerId;
    private static ClientLevel level;
    private static long sessionStarted;
    private static float lastHealth;
    private static boolean lastFlight;
    private static long lastDamage;
    private static boolean lowHealth;

    private ClientFeedback() {}

    public static long now() { return System.nanoTime() / 1_000_000L; }
    public static Silence suppress() { return new Silence(); }
    public static void start() { stopping = false; clear(); }
    public static void stop() { stopping = true; clear(); }
    public static InterfaceModule settings() { return (InterfaceModule) ModuleManager.getModule(InterfaceModule.class); }

    public static void moduleToggled(String name, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) { client.execute(() -> moduleToggled(name, enabled)); return; }
        if (stopping || suppressed > 0 || client.player == null || client.level == null) return;
        InterfaceModule settings = settings();
        if (settings == null) return;
        String state = enabled ? "enabled" : "disabled";
        Notice notice = new Notice("module:" + name.toLowerCase(Locale.ROOT), name + " " + state,
            enabled ? "Module is now active" : "Module has been turned off", enabled ? Tone.SUCCESS : Tone.INFO,
            now(), Math.round(settings.notificationDuration.get() * 1000.0), 100);
        if (settings.notifications.get() && (settings.isEnabled() || name.equalsIgnoreCase("Interface"))) {
            notices.removeIf(item -> item.key().equals(notice.key()) || item.expired());
            if (notices.size() == 4) notices.remove(0);
            notices.add(notice);
        }
        if (settings.islandReactions.get()) reaction = notice;
    }

    public static void react(String key, String title, String message, Tone tone, long duration, int priority) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) { client.execute(() -> react(key, title, message, tone, duration, priority)); return; }
        InterfaceModule settings = settings();
        if (stopping || suppressed > 0 || settings == null || !settings.islandReactions.get()
            || client.player == null || client.level == null) return;
        if (reaction != null && !reaction.expired() && reaction.priority() > priority && reaction.age() < 1200L) return;
        reaction = new Notice(key, title, message, tone, now(), Math.max(800L, Math.min(8000L, duration)), priority);
    }

    public static List<Notice> notices() {
        notices.removeIf(Notice::expired);
        return List.copyOf(notices);
    }

    public static Notice reaction() {
        InterfaceModule settings = settings();
        if (settings == null || !settings.islandReactions.get() || reaction == null || reaction.expired()) {
            reaction = null;
            return null;
        }
        return reaction;
    }

    public static long sessionSeconds() { return sessionStarted == 0 ? 0 : Math.max(0, (now() - sessionStarted) / 1000L); }
    public static void dismissReaction() { reaction = null; }

    public static void tick(Minecraft client) {
        if (stopping) return;
        if (client.player == null || client.level == null) { clear(); return; }
        if (level != client.level || !client.player.getUUID().equals(playerId)) {
            clear();
            level = client.level;
            playerId = client.player.getUUID();
            sessionStarted = now();
            lastHealth = client.player.getHealth() + client.player.getAbsorptionAmount();
            lastFlight = client.player.isFallFlying();
            return;
        }
        notices.removeIf(Notice::expired);
        if (reaction != null && reaction.expired()) reaction = null;
        float health = client.player.getHealth() + client.player.getAbsorptionAmount();
        boolean critical = client.player.isAlive() && client.player.getHealth() <= 6.0F;
        if (critical && !lowHealth) react("health:low", "Low health", "Find cover and heal", Tone.WARNING, 3200L, 110);
        else if (health + 0.25F < lastHealth && now() - lastDamage > 1200L) {
            lastDamage = now();
            react("health:damage", "Damage taken", String.format(Locale.ROOT, "%.1f HP remaining", health), Tone.ERROR, 1600L, 70);
        }
        lowHealth = critical;
        lastHealth = health;
        boolean flight = client.player.isFallFlying();
        if (flight != lastFlight) react("elytra", flight ? "Elytra flight" : client.player.onGround() ? "Touchdown" : "Flight ended",
            flight ? "Flight mode engaged" : "Elytra flight ended", Tone.INFO, 2200L, 50);
        lastFlight = flight;
    }

    public static void clear() {
        notices.clear();
        reaction = null;
        playerId = null;
        level = null;
        sessionStarted = 0;
        lastHealth = 0.0F;
        lastFlight = lowHealth = false;
        lastDamage = 0;
    }
}
