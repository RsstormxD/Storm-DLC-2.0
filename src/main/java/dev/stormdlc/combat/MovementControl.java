package dev.stormdlc.combat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import xyz.angames.astolfoclient.client.mixin.KeyBindingAccessor;

public final class MovementControl {
    public record Options(MovementMode mode, boolean sprint, double followDistance, double orbitDistance,
        double attackRange, double predictionTicks, boolean autoJump, boolean manualOverride) {}
    private final Map<KeyMapping, Boolean> ownedKeys = new IdentityHashMap<>();
    private final Map<Integer, Integer> scanCodes = new HashMap<>();
    private final GroundPathPlanner planner = new GroundPathPlanner();
    private LocalPlayer sprintPlayer;
    private boolean originalSprint, following;
    private int orbitSide = 1, lastCollisionTick = -20, pathIndex, nextPlanTick, lastSector = -1, lastJumpTick = -20;
    private double heading = Double.NaN;
    private Vec3 filteredVelocity = Vec3.ZERO, plannedGoal;
    private Vec3 nextWaypoint;
    private boolean stepUp;
    private List<Vec3> path = List.of();

    private void force(KeyMapping key, boolean pressed) {
        ownedKeys.putIfAbsent(key, key.isDown());
        key.setDown(pressed);
    }

    private boolean physical(Minecraft client, KeyMapping key, boolean fallback) {
        if (client.getWindow() == null) return fallback;
        InputConstants.Key binding = ((KeyBindingAccessor) key).stormdlc$getKey();
        long handle = client.getWindow().getWindow();
        if (binding.getType() == InputConstants.Type.KEYSYM && binding.getValue() != GLFW.GLFW_KEY_UNKNOWN)
            return InputConstants.isKeyDown(handle, binding.getValue());
        if (binding.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(handle, binding.getValue()) == GLFW.GLFW_PRESS;
        if (binding.getType() == InputConstants.Type.SCANCODE) {
            int code = scanCodes.computeIfAbsent(binding.getValue(), scan -> {
                for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++)
                    if (GLFW.glfwGetKeyScancode(keyCode) == scan) return keyCode;
                return GLFW.GLFW_KEY_UNKNOWN;
            });
            return code != GLFW.GLFW_KEY_UNKNOWN && InputConstants.isKeyDown(handle, code);
        }
        return fallback;
    }

    private void release(KeyMapping key) {
        Boolean fallback = ownedKeys.remove(key);
        if (fallback != null) key.setDown(physical(Minecraft.getInstance(), key, fallback));
    }

    private boolean manualInput(Minecraft client) {
        return physical(client, client.options.keyUp, false) || physical(client, client.options.keyDown, false)
            || physical(client, client.options.keyLeft, false) || physical(client, client.options.keyRight, false)
            || physical(client, client.options.keyShift, false);
    }

    public void update(Minecraft client, LivingEntity target, Options options) {
        if (!TargetSelector.canAct(client, target) || client.screen != null || client.isPaused()) { clear(); return; }
        var player = client.player;
        boolean groundedMovement = !player.isFallFlying() && !player.isInWater() && !player.isInLava()
            && !player.isShiftKeyDown() && !player.getAbilities().flying && !player.isPassenger() && !player.onClimbable();
        if (!groundedMovement || options.mode() != MovementMode.OFF && options.manualOverride() && manualInput(client)) { clear(); return; }
        stepUp = false;
        nextWaypoint = null;
        Vec3 desired = switch (options.mode()) {
            case FOLLOW -> follow(client, target, options);
            case ORBIT -> orbit(client, target, options.orbitDistance());
            case OFF -> Vec3.ZERO;
        };
        if (options.mode() == MovementMode.OFF || desired.lengthSqr() < 1.0e-8) releaseDirections(client);
        else applyDirection(client, desired);
        if (options.sprint() && client.options.keyUp.isDown() && !player.horizontalCollision && !player.isUsingItem()
            && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly)
            && (options.mode() != MovementMode.FOLLOW || GroundPathPlanner.horizontalDistance(player.position(), target.position()) > options.followDistance() + 1.2)) {
            if (sprintPlayer != player) {
                releaseSprint();
                sprintPlayer = player;
                originalSprint = player.isSprinting();
            }
            force(client.options.keySprint, true);
            player.setSprinting(true);
        } else releaseSprint();
    }

    private Vec3 follow(Minecraft client, LivingEntity target, Options options) {
        var player = client.player;
        Vec3 origin = player.position(), delta = target.position().subtract(origin);
        Vec3 toward = new Vec3(delta.x, 0, delta.z).normalize();
        Vec3 velocity = target.getDeltaMovement();
        if (!RotationMath.finite(velocity) || velocity.horizontalDistanceSqr() > 4) velocity = Vec3.ZERO;
        filteredVelocity = filteredVelocity.scale(.65).add(velocity.multiply(.35, 0, .35));
        double stopDistance = Mth.clamp(options.followDistance(), .8, Math.max(.8, options.attackRange() - .45));
        double distance = Math.hypot(delta.x, delta.z);
        double closing = Math.max(0, player.getDeltaMovement().subtract(filteredVelocity).dot(toward));
        double brake = Math.min(.65, closing * 2.8);
        boolean visible = player.hasLineOfSight(target);
        if (following && distance <= stopDistance + brake && Math.abs(delta.y) < 1.4 && visible) following = false;
        else if (!following && (distance > stopDistance + .3 || Math.abs(delta.y) >= 1.4 || !visible)) following = true;
        if (!following) {
            path = List.of();
            pathIndex = 0;
            release(client.options.keyJump);
            return Vec3.ZERO;
        }
        Vec3 goal = target.position().add(filteredVelocity.scale(Mth.clamp(options.predictionTicks(), 0, 4)));
        int tick = player.tickCount;
        boolean moved = plannedGoal == null || GroundPathPlanner.horizontalDistance(plannedGoal, goal) > 1.0;
        if (tick >= nextPlanTick || moved && tick >= nextPlanTick - 3) {
            path = planner.plan(client, target, goal, Math.max(.65, stopDistance - .15), options.autoJump());
            pathIndex = 0;
            plannedGoal = goal;
            nextPlanTick = tick + 6;
        }
        while (pathIndex < path.size() && GroundPathPlanner.horizontalDistance(origin, path.get(pathIndex)) < .3
            && Math.abs(origin.y - path.get(pathIndex).y) < .5) pathIndex++;
        if (pathIndex >= path.size()) { release(client.options.keyJump); return Vec3.ZERO; }
        Vec3 waypoint = path.get(pathIndex), offset = waypoint.subtract(origin);
        nextWaypoint = waypoint;
        stepUp = offset.y > .05 && offset.y <= 1.1;
        boolean jumping = options.autoJump() && offset.y > .55 && offset.y <= 1.1
            && offset.horizontalDistanceSqr() < 1.8 && player.onGround();
        if (!planner.reachable(client, origin, waypoint, options.autoJump())) {
            nextPlanTick = tick + 1;
            path = List.of();
            release(client.options.keyJump);
            return Vec3.ZERO;
        }
        if (jumping && tick - lastJumpTick >= 6) {
            force(client.options.keyJump, true);
            lastJumpTick = tick;
        } else release(client.options.keyJump);
        Vec3 raw = new Vec3(offset.x, 0, offset.z).normalize();
        double desiredHeading = Math.atan2(raw.x, raw.z);
        if (!Double.isFinite(heading)) heading = desiredHeading;
        double difference = Math.toRadians(Mth.wrapDegrees((float) Math.toDegrees(desiredHeading - heading)));
        heading += Mth.clamp(difference, -.7, .7) * .75;
        Vec3 smoothed = new Vec3(Math.sin(heading), 0, Math.cos(heading));
        if (offset.y > .5 || !planner.safeDirection(client, smoothed, true)) {
            heading = desiredHeading;
            smoothed = raw;
        }
        return smoothed;
    }

    private Vec3 orbit(Minecraft client, LivingEntity target, double radius) {
        var player = client.player;
        if (player.horizontalCollision && player.tickCount - lastCollisionTick >= 6) {
            orbitSide = -orbitSide;
            lastCollisionTick = player.tickCount;
        }
        Vec3 delta = target.position().subtract(player.position());
        Vec3 toward = new Vec3(delta.x, 0, delta.z).normalize();
        double advance = Mth.clamp((Math.hypot(delta.x, delta.z) - radius) * 1.5, -1, 1);
        Vec3 desired = toward.scale(advance).add(-toward.z * orbitSide, 0, toward.x * orbitSide).normalize();
        if (!planner.safeDirection(client, desired, true)) {
            orbitSide = -orbitSide;
            desired = toward.scale(advance).add(-toward.z * orbitSide, 0, toward.x * orbitSide).normalize();
            if (!planner.safeDirection(client, desired, true)) desired = Vec3.ZERO;
        }
        release(client.options.keyJump);
        return desired;
    }

    private void applyDirection(Minecraft client, Vec3 desired) {
        double yaw = Math.toRadians(client.player.getYRot());
        double forward = -desired.x * Math.sin(yaw) + desired.z * Math.cos(yaw);
        double strafe = desired.x * Math.cos(yaw) + desired.z * Math.sin(yaw);
        double angle = Math.atan2(strafe, forward);
        int sector = Math.floorMod((int) Math.round(angle / (Math.PI / 4)), 8);
        if (lastSector >= 0) {
            double oldAngle = lastSector * Math.PI / 4;
            double separation = Math.abs(Math.toRadians(Mth.wrapDegrees((float) Math.toDegrees(angle - oldAngle))));
            if (separation < Math.PI / 8 + .055) sector = lastSector;
        }
        lastSector = sector;
        Vec3 actual = worldDirection(yaw, sector);
        if (!planner.safeInput(client, actual, stepUp && nextWaypoint != null ? nextWaypoint.y : client.player.getY())) {
            boolean found = false;
            for (int alternate : new int[]{Math.floorMod(sector + 1, 8), Math.floorMod(sector - 1, 8)}) {
                Vec3 candidate = worldDirection(yaw, alternate);
                if (candidate.dot(desired) > .55 && planner.safeInput(client, candidate, client.player.getY())) {
                    sector = alternate;
                    found = true;
                    break;
                }
            }
            if (!found) {
                release(client.options.keyUp);
                release(client.options.keyDown);
                release(client.options.keyLeft);
                release(client.options.keyRight);
                if (!stepUp) release(client.options.keyJump);
                lastSector = -1;
                nextPlanTick = client.player.tickCount + 1;
                return;
            }
        }
        lastSector = sector;
        force(client.options.keyUp, sector == 0 || sector == 1 || sector == 7);
        force(client.options.keyDown, sector == 3 || sector == 4 || sector == 5);
        force(client.options.keyLeft, sector == 1 || sector == 2 || sector == 3);
        force(client.options.keyRight, sector == 5 || sector == 6 || sector == 7);
    }

    private void releaseDirections(Minecraft client) {
        release(client.options.keyUp);
        release(client.options.keyDown);
        release(client.options.keyLeft);
        release(client.options.keyRight);
        release(client.options.keyJump);
        lastSector = -1;
    }

    private static Vec3 worldDirection(double yaw, int sector) {
        double angle = sector * Math.PI / 4, forward = Math.cos(angle), strafe = Math.sin(angle);
        return new Vec3(-Math.sin(yaw) * forward + Math.cos(yaw) * strafe, 0,
            Math.cos(yaw) * forward + Math.sin(yaw) * strafe);
    }

    private void releaseSprint() {
        Minecraft client = Minecraft.getInstance();
        release(client.options.keySprint);
        if (sprintPlayer != null) {
            sprintPlayer.setSprinting(originalSprint || sprintPlayer == client.player && client.options.keySprint.isDown());
            sprintPlayer = null;
        }
    }

    public void clear() {
        releaseSprint();
        for (KeyMapping key : ownedKeys.keySet().toArray(KeyMapping[]::new)) release(key);
        following = false;
        path = List.of();
        pathIndex = nextPlanTick = 0;
        plannedGoal = null;
        nextWaypoint = null;
        stepUp = false;
        filteredVelocity = Vec3.ZERO;
        heading = Double.NaN;
        lastSector = -1;
        orbitSide = 1;
        lastJumpTick = lastCollisionTick = -20;
    }
}
