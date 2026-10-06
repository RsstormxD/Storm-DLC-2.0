package xyz.angames.astolfoclient.client.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.stormdlc.config.ClientPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FriendsManager {
    private static final Logger LOG = LoggerFactory.getLogger("StormDLC/Friends");
    private static final Gson GSON = new Gson();
    private static final AtomicReference<Set<String>> FRIENDS = new AtomicReference<>(Set.of());
    private static final CopyOnWriteArrayList<Consumer<Set<String>>> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Object FILE_LOCK = new Object();

    private FriendsManager() {}

    private static String normalize(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidIdentifier(String identifier) {
        String normalized = normalize(identifier);
        if (normalized.matches("[a-z0-9_]{1,16}")) return true;
        try { return UUID.fromString(normalized).toString().equals(normalized); }
        catch (IllegalArgumentException invalid) { return false; }
    }

    public static boolean isFriend(String identifier) {
        return identifier != null && FRIENDS.get().contains(normalize(identifier));
    }

    public static boolean isFriend(Player player) {
        return player != null && (isFriend(player.getName().getString()) || isFriend(player.getUUID().toString()));
    }

    public static Set<String> getFriends() { return FRIENDS.get(); }

    public static boolean addFriend(String identifier) {
        if (!isValidIdentifier(identifier)) return false;
        return mutate(values -> values.add(normalize(identifier)));
    }

    public static boolean addFriend(Player player) {
        if (player == null) return false;
        return mutate(values -> {
            boolean nameAdded = values.add(normalize(player.getName().getString()));
            return values.add(player.getUUID().toString()) || nameAdded;
        });
    }

    public static boolean removeFriend(String identifier) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null && client.isSameThread()) {
            for (Player player : client.level.players()) {
                if (normalize(player.getName().getString()).equals(normalize(identifier))
                    || player.getUUID().toString().equals(normalize(identifier))) return removeFriend(player);
            }
        }
        return mutate(values -> values.remove(normalize(identifier)));
    }

    public static boolean removeFriend(Player player) {
        if (player == null) return false;
        return mutate(values -> {
            boolean nameRemoved = values.remove(normalize(player.getName().getString()));
            return values.remove(player.getUUID().toString()) || nameRemoved;
        });
    }

    public static void clearFriends() { replaceAll(Set.of(), true); }

    public static void replaceAll(Collection<String> identifiers) { replaceAll(identifiers, true); }

    private static boolean mutate(java.util.function.Predicate<Set<String>> mutation) {
        while (true) {
            Set<String> before = FRIENDS.get();
            Set<String> next = new HashSet<>(before);
            if (!mutation.test(next)) return false;
            Set<String> snapshot = Set.copyOf(next);
            if (FRIENDS.compareAndSet(before, snapshot)) {
                save();
                notifyListeners(snapshot);
                return true;
            }
        }
    }

    private static void replaceAll(Collection<String> identifiers, boolean persist) {
        Set<String> normalized = new HashSet<>();
        if (identifiers != null) for (String identifier : identifiers) {
            if (isValidIdentifier(identifier)) normalized.add(normalize(identifier));
        }
        Set<String> snapshot = Set.copyOf(normalized);
        Set<String> before = FRIENDS.getAndSet(snapshot);
        if (!before.equals(snapshot)) {
            if (persist) save();
            notifyListeners(snapshot);
        }
    }

    public static AutoCloseable subscribe(Consumer<Set<String>> listener) {
        java.util.Objects.requireNonNull(listener, "listener");
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    private static void notifyListeners(Set<String> snapshot) {
        Minecraft client = Minecraft.getInstance();
        Runnable notification = () -> {
            for (Consumer<Set<String>> listener : LISTENERS) {
                try { listener.accept(snapshot); }
                catch (RuntimeException failure) { LOG.warn("Friend listener failed", failure); }
            }
        };
        if (client != null && !client.isSameThread()) client.execute(notification);
        else notification.run();
    }

    public static void load() {
        synchronized (FILE_LOCK) {
            Path file = ClientPaths.configDirectory().resolve("friends.json");
            if (!Files.isRegularFile(file)) return;
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                List<String> loaded = GSON.fromJson(reader, new TypeToken<List<String>>() {}.getType());
                replaceAll(loaded, false);
            } catch (IOException | RuntimeException failure) {
                LOG.warn("Cannot load friends from {}", file, failure);
            }
        }
    }

    public static void save() {
        synchronized (FILE_LOCK) {
            Path file = ClientPaths.configDirectory().resolve("friends.json");
            Path temporary = null;
            try {
                Files.createDirectories(file.getParent());
                temporary = Files.createTempFile(file.getParent(), "friends-", ".tmp");
                try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    GSON.toJson(FRIENDS.get().stream().sorted().toList(), writer);
                }
                try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException failure) {
                LOG.warn("Cannot save friends to {}", file, failure);
            } finally {
                if (temporary != null) try { Files.deleteIfExists(temporary); }
                catch (IOException failure) { LOG.debug("Cannot remove temporary friends file", failure); }
            }
        }
    }
}
