package dev.stormdlc.combat;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class VisualRotations {
    public record Frame(Object owner, UUID player, float previousYaw, float previousPitch, float yaw, float pitch, long tick) {
        public SilentRotations.Angles interpolate(float partialTick) {
            float delta = Mth.clamp(partialTick, 0.0F, 1.0F);
            return new SilentRotations.Angles(Mth.rotLerp(delta, previousYaw, yaw), Mth.lerp(delta, previousPitch, pitch));
        }
    }
    private static volatile Frame frame;
    private VisualRotations() {}

    public static void update(Object owner, UUID player, SilentRotations.Angles angles) {
        Minecraft client = Minecraft.getInstance();
        long tick = client.level == null ? 0 : client.level.getGameTime();
        Frame previous = frame;
        boolean same = previous != null && previous.owner() == owner && previous.player().equals(player);
        float oldYaw = same ? (previous.tick() == tick ? previous.previousYaw() : previous.yaw()) : angles.yaw();
        float oldPitch = same ? (previous.tick() == tick ? previous.previousPitch() : previous.pitch()) : angles.pitch();
        frame = new Frame(owner, player, oldYaw, oldPitch, angles.yaw(), angles.pitch(), tick);
    }

    public static Frame forEntity(LivingEntity entity) {
        Frame active = frame;
        Minecraft client = Minecraft.getInstance();
        return active != null && entity == client.player && entity.isAlive() && entity.getUUID().equals(active.player()) ? active : null;
    }

    public static void clear(Object owner) {
        Frame active = frame;
        if (active != null && active.owner() == owner) {
            frame = null;
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.yHeadRot = player.getYRot();
                player.yBodyRot = player.getYRot();
                player.yHeadRotO = player.getYRot();
                player.yBodyRotO = player.getYRot();
            }
        }
    }
}
