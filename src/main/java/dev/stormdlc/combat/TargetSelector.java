package dev.stormdlc.combat;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.util.FriendsManager;

public final class TargetSelector {
    public record Filters(boolean players, boolean mobs, boolean animals, double range, double fov, boolean visibleOnly) {
        public Filters(boolean players, boolean mobs, boolean animals, double range, double fov) {
            this(players, mobs, animals, range, fov, true);
        }
    }
    private TargetSelector() {}

    public static LivingEntity select(Minecraft client, Filters filters, SortMode mode, UUID priority) {
        if (client.player == null || client.level == null) return null;
        Vec3 eye = client.player.getEyePosition();
        var candidates = client.level.getEntitiesOfClass(LivingEntity.class,
            client.player.getBoundingBox().inflate(filters.range()),
            entity -> valid(client, entity, filters));
        if (priority != null) for (LivingEntity entity : candidates) {
            if (entity.getUUID().equals(priority)) return entity;
        }
        Comparator<LivingEntity> distance = Comparator.comparingDouble(entity -> distanceSquared(eye, entity.getBoundingBox()));
        Comparator<LivingEntity> order = switch (mode) {
            case DISTANCE -> distance;
            case HEALTH -> Comparator.<LivingEntity>comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount()).thenComparing(distance);
            case ARMOR -> Comparator.comparingInt(LivingEntity::getArmorValue).thenComparing(distance);
        };
        return candidates.stream().min(order.thenComparingInt(LivingEntity::getId)).orElse(null);
    }

    public static boolean valid(Minecraft client, LivingEntity entity, Filters filters) {
        if (!canAct(client, entity)) return false;
        boolean allowed;
        if (entity instanceof Player) allowed = filters.players();
        else if (entity instanceof Animal) allowed = filters.animals();
        else allowed = entity instanceof Mob && filters.mobs();
        if (!allowed || entity.isInvisible() || filters.visibleOnly() && !client.player.hasLineOfSight(entity)) return false;
        Vec3 eye = client.player.getEyePosition();
        if (distanceSquared(eye, entity.getBoundingBox()) > filters.range() * filters.range()) return false;
        Vec3 direction = entity.getBoundingBox().getCenter().subtract(eye);
        if (direction.lengthSqr() < 1.0e-12) return true;
        double cosine = Mth.clamp(client.player.getLookAngle().dot(direction.normalize()), -1.0, 1.0);
        return Math.toDegrees(Math.acos(cosine)) <= filters.fov();
    }

    public static boolean canAct(Minecraft client, LivingEntity entity) {
        if (client == null || client.player == null || client.level == null || entity == null
            || entity == client.player || !client.player.isAlive() || client.player.isSpectator()
            || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()
            || entity.level() != client.level || client.level.getEntity(entity.getId()) != entity) return false;
        return !(entity instanceof Player player) || !FriendsManager.isFriend(player);
    }

    public static Vec3 closestPoint(Vec3 point, AABB box) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX),
            Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    public static double distanceSquared(Vec3 point, AABB box) {
        return point.distanceToSqr(closestPoint(point, box));
    }
}
