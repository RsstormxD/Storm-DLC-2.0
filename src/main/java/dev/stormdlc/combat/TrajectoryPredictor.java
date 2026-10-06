package dev.stormdlc.combat;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TrajectoryPredictor {
    public record Prediction(AABB box, Vec3 offset, double ticks) {
        public Vec3 aimPoint() {
            return new Vec3((box.minX + box.maxX) * 0.5, box.minY + box.getYsize() * 0.72, (box.minZ + box.maxZ) * 0.5);
        }
    }
    private UUID sampledEntity;
    private Vec3 lastPosition;
    private long lastTick = Long.MIN_VALUE;
    private Vec3 sampledVelocity = Vec3.ZERO;

    public void clear() {
        sampledEntity = null;
        lastPosition = null;
        lastTick = Long.MIN_VALUE;
        sampledVelocity = Vec3.ZERO;
    }

    public Prediction predict(Minecraft client, LivingEntity target, boolean enabled, double configuredTicks) {
        AABB original = target.getBoundingBox();
        Vec3 velocity = target.getDeltaMovement();
        long tick = client.level.getGameTime();
        if (!target.getUUID().equals(sampledEntity)) {
            clear();
            sampledEntity = target.getUUID();
        }
        if (tick != lastTick) {
            if (lastPosition != null && lastTick != Long.MIN_VALUE && tick > lastTick && tick - lastTick <= 3) {
                Vec3 observed = target.position().subtract(lastPosition).scale(1.0 / (tick - lastTick));
                sampledVelocity = velocity.lengthSqr() < 1.0e-8 && observed.lengthSqr() < 16.0 ? observed : velocity;
            } else sampledVelocity = velocity;
            lastPosition = target.position();
            lastTick = tick;
        }
        if (!enabled || configuredTicks <= 0.0 || !(target.isFallFlying() || client.player.isFallFlying())) {
            return new Prediction(original, Vec3.ZERO, 0.0);
        }
        double ticks = Mth.clamp(configuredTicks, 0.0, 5.0) + latencyTicks(client, target);
        Vec3 offset = Vec3.ZERO;
        velocity = sampledVelocity;
        double remaining = ticks;
        while (remaining > 1.0e-6) {
            double step = Math.min(1.0, remaining);
            Vec3 nextOffset = offset.add(velocity.scale(step));
            AABB nextBox = original.move(nextOffset);
            if (!client.level.hasChunkAt(net.minecraft.core.BlockPos.containing(nextBox.getCenter()))
                || !client.level.noBlockCollision(target, nextBox)) break;
            offset = nextOffset;
            double horizontalDrag = target.isFallFlying() ? 0.99 : 0.91;
            double verticalDrag = 0.98;
            double gravity = 0.0;
            if (!target.onGround() && !target.isNoGravity()) {
                double pitch = Math.toRadians(target.getXRot());
                gravity = target.isFallFlying() ? -0.08 + 0.06 * Math.cos(pitch) * Math.cos(pitch) : -0.08;
            }
            velocity = new Vec3(velocity.x * Math.pow(horizontalDrag, step),
                (velocity.y + gravity * step) * Math.pow(verticalDrag, step),
                velocity.z * Math.pow(horizontalDrag, step));
            remaining -= step;
        }
        return new Prediction(original.move(offset), offset, ticks);
    }

    private static double latencyTicks(Minecraft client, LivingEntity target) {
        if (client.getConnection() == null) return 0.0;
        PlayerInfo ownInfo = client.getConnection().getPlayerInfo(client.player.getUUID());
        PlayerInfo targetInfo = client.getConnection().getPlayerInfo(target.getUUID());
        int ownPing = ownInfo == null ? 0 : Math.max(0, ownInfo.getLatency());
        int targetPing = targetInfo == null ? 0 : Math.max(0, targetInfo.getLatency());
        return Mth.clamp((ownPing + targetPing) / 100.0, 0.0, 2.0);
    }
}
