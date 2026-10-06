package xyz.angames.astolfoclient.client.module;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.angames.astolfoclient.client.module.setting.ConfigureSetting;
import xyz.angames.astolfoclient.client.module.setting.MultiSelectSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public abstract class Module {
    private static final Logger LOG = LoggerFactory.getLogger("StormDLC/Modules");
    public xyz.angames.astolfoclient.client.config.VisualColors visualColors;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private final ArrayDeque<AutoCloseable> resources = new ArrayDeque<>();
    private volatile boolean enabled;
    private boolean transitioning;
    private boolean requestedEnabled;
    private int keyCode = -1;

    public Module(String name, Category category) { this(name, "", category); }

    public Module(String name, String description, Category category) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.category = Objects.requireNonNull(category, "category");
        registerSettings();
    }

    protected void registerSettings() {
        List<Setting> discovered = new ArrayList<>();
        for (Field field : getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(this);
                if (value instanceof Setting setting) discovered.add(setting);
            } catch (ReflectiveOperationException | RuntimeException failure) {
                LOG.debug("Cannot discover setting {} for {}", field.getName(), name, failure);
            }
        }
        Set<Setting> nested = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Setting> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Setting> pending = new ArrayDeque<>(settings);
        pending.addAll(discovered);
        while (!pending.isEmpty()) {
            Setting parent = pending.removeFirst();
            if (!visited.add(parent)) continue;
            List<? extends Setting> children = parent instanceof ConfigureSetting group ? group.getSubSettings()
                : parent instanceof MultiSelectSetting group ? group.getOptions() : List.of();
            for (Setting child : children) if (child != null) {
                nested.add(child);
                pending.addLast(child);
            }
        }
        settings.removeIf(nested::contains);
        for (Setting setting : discovered) if (!nested.contains(setting)) addSetting(setting);
    }

    public void addSetting(Setting setting) {
        if (setting != null && !settings.contains(setting)) settings.add(setting);
    }

    public void addSettings(Setting... values) {
        for (Setting setting : values) addSetting(setting);
    }

    public List<Setting> getSettings() { registerSettings(); return settings; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeyCode() { return keyCode; }
    public void setKeyCode(int value) { keyCode = value; }

    public void setEnabled(boolean next) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && !client.isSameThread()) {
            client.execute(() -> setEnabled(next));
            return;
        }
        requestedEnabled = next;
        if (transitioning) return;
        boolean previousState = enabled;
        transitioning = true;
        try {
            while (enabled != requestedEnabled) {
                enabled = requestedEnabled;
                if (enabled) {
                    try {
                        onEnable();
                        playToggleSound(true);
                    } catch (RuntimeException failure) {
                        LOG.error("Cannot enable module {}", name, failure);
                        enabled = requestedEnabled = false;
                        disableAndRelease();
                    }
                } else {
                    disableAndRelease();
                    playToggleSound(false);
                }
            }
        } finally {
            transitioning = false;
            if (previousState != enabled) dev.stormdlc.hud.ClientFeedback.moduleToggled(name, enabled);
        }
    }

    public void toggle() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && !client.isSameThread()) {
            client.execute(this::toggle);
            return;
        }
        setEnabled(!(transitioning ? requestedEnabled : enabled));
    }

    protected final <T extends AutoCloseable> T manage(T resource) {
        Objects.requireNonNull(resource, "resource");
        if (!enabled) {
            try { resource.close(); }
            catch (Exception failure) { LOG.warn("Cannot close inactive resource for {}", name, failure); }
            throw new IllegalStateException("Module resources require an enabled module");
        }
        resources.addFirst(resource);
        return resource;
    }

    private void disableAndRelease() {
        try { onDisable(); }
        catch (RuntimeException failure) { LOG.warn("Cannot clean module {}", name, failure); }
        finally {
            while (!resources.isEmpty()) {
                try { resources.removeFirst().close(); }
                catch (Exception failure) { LOG.warn("Cannot close resource for {}", name, failure); }
            }
        }
    }

    private void playToggleSound(boolean next) {
        try { if (next) ModSounds.playEnable(); else ModSounds.playDisable(); }
        catch (RuntimeException failure) { LOG.debug("Cannot play toggle sound for {}", name, failure); }
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onContextReset() {}
    public void onTick() {}
    public void onRender() {}
    public void onWorldRender(WorldRenderContext context) {}

    @Environment(EnvType.CLIENT)
    public enum Category { COMBAT, RENDER, FRIENDS, MISC }
}
