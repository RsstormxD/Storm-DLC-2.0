package dev.stormdlc.combat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import xyz.angames.astolfoclient.client.mixin.KeyBindingAccessor;

public final class MovementControl {
    private final Map<KeyMapping, Boolean> ownedKeys = new IdentityHashMap<>();
    private LocalPlayer sprintPlayer;
    private boolean originalSprint;
    private int orbitSide = 1;
    private int lastCollisionTick = -20;

    private void force(KeyMapping key, boolean pressed) {
        ownedKeys.putIfAbsent(key, key.isDown());
        key.setDown(pressed);
    }

    private void release(KeyMapping key) {
        Boolean fallback = ownedKeys.remove(key);
        if (fallback == null) return;
        Minecraft client = Minecraft.getInstance();
        InputConstants.Key binding = ((KeyBindingAccessor) key).stormdlc$getKey();
        boolean pressed = fallback;
        if (binding.getType() == InputConstants.Type.SCANCODE) pressed = false;
        if (client.getWindow() != null) {
            long handle = client.getWindow().getWindow();
            if (binding.getType() == InputConstants.Type.KEYSYM && binding.getValue() != GLFW.GLFW_KEY_UNKNOWN)
                pressed = InputConstants.isKeyDown(handle, binding.getValue());
            else if (binding.getType() == InputConstants.Type.MOUSE)
                pressed = GLFW.glfwGetMouseButton(handle, binding.getValue()) == GLFW.GLFW_PRESS;
        }
        key.setDown(pressed);
    }

    public void update(Minecraft client, LivingEntity target, boolean lock, boolean sprint, double orbitDistance) {
        if (!TargetSelector.canAct(client, target)) { clear(); return; }
        boolean groundedMovement = !client.player.isFallFlying() && !client.player.isInWater()
            && !client.player.isInLava() && !client.player.isShiftKeyDown() && !client.player.getAbilities().flying;
        if (lock && groundedMovement) {
            if (client.player.horizontalCollision && client.player.tickCount - lastCollisionTick >= 6) {
                orbitSide = -orbitSide;
                lastCollisionTick = client.player.tickCount;
            }
            Vec3 delta = target.position().subtract(client.player.position());
            double distance = Math.hypot(delta.x, delta.z);
            Vec3 toward = new Vec3(delta.x, 0.0, delta.z).normalize();
            double advance = Math.max(-1.0, Math.min(1.0, (distance - orbitDistance) * 1.5));
            Vec3 desired = toward.scale(advance).add(-toward.z * orbitSide, 0.0, toward.x * orbitSide).normalize();
            Vec3 ahead = client.player.position().add(desired.scale(0.7));
            if (client.player.onGround() && !client.level.getBlockState(net.minecraft.core.BlockPos.containing(ahead).below()).blocksMotion()) {
                orbitSide = -orbitSide;
                desired = toward.scale(advance).add(-toward.z * orbitSide, 0.0, toward.x * orbitSide).normalize();
                Vec3 alternate = client.player.position().add(desired.scale(0.7));
                if (!client.level.getBlockState(net.minecraft.core.BlockPos.containing(alternate).below()).blocksMotion()) desired = Vec3.ZERO;
            }
            double yaw = Math.toRadians(client.player.getYRot());
            double forward = desired.x * -Math.sin(yaw) + desired.z * Math.cos(yaw);
            double strafe = desired.x * Math.cos(yaw) + desired.z * Math.sin(yaw);
            force(client.options.keyUp, forward > 0.25);
            force(client.options.keyDown, forward < -0.25);
            force(client.options.keyLeft, strafe > 0.25);
            force(client.options.keyRight, strafe < -0.25);
        } else {
            release(client.options.keyUp);
            release(client.options.keyDown);
            release(client.options.keyRight);
            release(client.options.keyLeft);
        }
        if (sprint && client.options.keyUp.isDown() && groundedMovement && !client.player.horizontalCollision
            && !client.player.isUsingItem() && (client.player.getFoodData().getFoodLevel() > 6 || client.player.getAbilities().mayfly)) {
            if (sprintPlayer != client.player) {
                releaseSprint();
                sprintPlayer = client.player;
                originalSprint = client.player.isSprinting();
            }
            force(client.options.keySprint, true);
            client.player.setSprinting(true);
        } else releaseSprint();
    }

    private void releaseSprint() {
        Minecraft client = Minecraft.getInstance();
        release(client.options.keySprint);
        if (sprintPlayer != null) {
            sprintPlayer.setSprinting(originalSprint || client.options.keySprint.isDown());
            sprintPlayer = null;
        }
    }

    public void clear() {
        releaseSprint();
        for (KeyMapping key : ownedKeys.keySet().toArray(KeyMapping[]::new)) release(key);
        orbitSide = 1;
        lastCollisionTick = -20;
    }
}
