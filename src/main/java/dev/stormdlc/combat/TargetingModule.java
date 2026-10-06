package dev.stormdlc.combat;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.util.FriendsManager;

public abstract class TargetingModule extends Module {
    public final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    public final BooleanSetting targetMobs = new BooleanSetting("Target Mobs", false);
    public final BooleanSetting targetAnimals = new BooleanSetting("Target Animals", false);
    public final BooleanSetting targetEsp = new BooleanSetting("Target ESP", true);
    public final NumberSetting searchRange = new NumberSetting("Search Range", 8.0, 1.0, 32.0, 0.1);
    public final NumberSetting fov = new NumberSetting("FOV", 180.0, 15.0, 180.0, 5.0);
    public final EnumSetting<SortMode> sortMode = new EnumSetting<>("Sort Mode", SortMode.DISTANCE);
    public final BooleanSetting stableTarget = new BooleanSetting("Stable Target", true);
    protected LivingEntity currentTarget;
    private UUID priority;
    private ClientLevel level;
    private LocalPlayer player;

    protected TargetingModule(String name, String description) {
        super(name, description, Category.COMBAT);
        addSettings(searchRange, fov, sortMode, stableTarget, targetPlayers, targetMobs, targetAnimals, targetEsp);
    }

    public final LivingEntity getCurrentTarget() { return currentTarget; }

    public final void setPriorityTarget(LivingEntity entity) { priority = entity == null ? null : entity.getUUID(); }

    protected final TargetSelector.Filters filters() {
        return new TargetSelector.Filters(targetPlayers.get(), targetMobs.get(), targetAnimals.get(), searchRange.get(), fov.get());
    }

    @Override
    public void onEnable() {
        onContextReset();
        manage(FriendsManager.subscribe(snapshot -> {
            if (currentTarget instanceof Player selected && FriendsManager.isFriend(selected)) clearTarget();
        }));
    }

    @Override
    public void onDisable() {
        priority = null;
        onContextReset();
    }

    @Override
    public void onContextReset() {
        clearTarget();
        level = null;
        player = null;
    }

    protected final void clearTarget() {
        currentTarget = null;
        onTargetLost();
    }

    protected void onTargetLost() {}
    protected void onTargetChanged(LivingEntity previous, LivingEntity next) { onTargetLost(); }

    protected final boolean selectTarget(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null || client.gameMode == null
            || client.isPaused() || client.screen != null || !client.player.isAlive() || client.player.isSpectator()) {
            onContextReset();
            return false;
        }
        if (level != client.level || player != client.player) {
            clearTarget();
            level = client.level;
            player = client.player;
        }
        LivingEntity next = TargetSelector.select(client, filters(), sortMode.getValue(), priority);
        if (stableTarget.get() && priority == null && currentTarget != null && next != null
            && next != currentTarget && TargetSelector.valid(client, currentTarget, filters())) {
            double currentDistance = Math.sqrt(TargetSelector.distanceSquared(client.player.getEyePosition(), currentTarget.getBoundingBox()));
            double nextDistance = Math.sqrt(TargetSelector.distanceSquared(client.player.getEyePosition(), next.getBoundingBox()));
            boolean closeScores = switch (sortMode.getValue()) {
                case DISTANCE -> currentDistance <= nextDistance + 0.35;
                case HEALTH -> currentTarget.getHealth() + currentTarget.getAbsorptionAmount()
                    <= next.getHealth() + next.getAbsorptionAmount() + 1.0F && currentDistance <= nextDistance + 0.5;
                case ARMOR -> currentTarget.getArmorValue() == next.getArmorValue() && currentDistance <= nextDistance + 0.35;
            };
            if (closeScores) next = currentTarget;
        }
        if (currentTarget != next) {
            LivingEntity previous = currentTarget;
            if (previous != null && next != null) {
                currentTarget = next;
                onTargetChanged(previous, next);
            } else {
                clearTarget();
                currentTarget = next;
            }
        }
        return currentTarget != null;
    }
}
