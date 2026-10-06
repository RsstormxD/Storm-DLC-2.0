package xyz.angames.astolfoclient.client.module.modules;

import dev.stormdlc.combat.HotbarSlots;
import dev.stormdlc.combat.MovementControl;
import dev.stormdlc.combat.MovementMode;
import dev.stormdlc.combat.RotationMode;
import dev.stormdlc.combat.RotationMath;
import dev.stormdlc.combat.RotationPackets;
import dev.stormdlc.combat.SilentRotations;
import dev.stormdlc.combat.TargetBoxRenderer;
import dev.stormdlc.combat.TargetSelector;
import dev.stormdlc.combat.TargetingModule;
import dev.stormdlc.combat.TrajectoryPredictor;
import dev.stormdlc.combat.VisualRotations;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public final class KillAuraModule extends TargetingModule {
    public final NumberSetting reach = new NumberSetting("Attack Range", 3.6, 1.0, 6.0, 0.1);
    public final EnumSetting<RotationMode> rotationMode = new EnumSetting<>("Rotation Mode", RotationMode.MINESTAR_V1);
    public final NumberSetting rotationSpeed = new NumberSetting("Rotation speed", 0.45, 0.1, 1.0, 0.05);
    public final BooleanSetting visualRotations = new BooleanSetting("Visual Rotations", true);
    public final BooleanSetting elytraPredict = new BooleanSetting("Elytra Predict", false);
    public final NumberSetting predictionTicks = new NumberSetting("Prediction Ticks", 1.5, 0.0, 5.0, 0.1);
    public final BooleanSetting truePositionEsp = new BooleanSetting("True Position ESP", false);
    public final BooleanSetting autoSprint = new BooleanSetting("Auto Sprint", false);
    public final EnumSetting<MovementMode> movementMode = new EnumSetting<>("Movement", MovementMode.FOLLOW);
    public final NumberSetting followDistance = new NumberSetting("Follow Distance", 2.3, 0.8, 5.0, 0.1);
    public final NumberSetting movementPrediction = new NumberSetting("Movement Prediction", 1.5, 0.0, 4.0, 0.1);
    public final BooleanSetting autoJump = new BooleanSetting("Follow Auto Jump", true);
    public final BooleanSetting manualMovement = new BooleanSetting("Manual Movement Override", true);
    public final NumberSetting strafeDistance = new NumberSetting("Strafe Distance", 2.8, 1.0, 5.0, 0.1);
    public final BooleanSetting noEatAttack = new BooleanSetting("No Eat Attack", true);
    public final BooleanSetting criticalsSync = new BooleanSetting("Criticals Sync", false);
    public final BooleanSetting smartCriticals = new BooleanSetting("Smart Criticals", true);
    public final BooleanSetting elytraMode = new BooleanSetting("Elytra Boost", false);
    public final NumberSetting minSpeed = new NumberSetting("Min Speed", 0.85, 0.1, 2.5, 0.05);
    public final NumberSetting boostDelay = new NumberSetting("Firework Delay (ticks)", 10.0, 5.0, 40.0, 1.0);
    private final SilentRotations rotations = new SilentRotations();
    private final TrajectoryPredictor predictor = new TrajectoryPredictor();
    private final MovementControl movement = new MovementControl();
    private TrajectoryPredictor.Prediction prediction;
    private int nextBoostTick;

    public KillAuraModule() {
        super("KillAura", "Silent combat rotations, shared target filters and trajectory prediction");
        addSettings(reach, rotationMode, rotationSpeed, visualRotations, elytraPredict, predictionTicks,
            truePositionEsp, autoSprint, movementMode, followDistance, movementPrediction, autoJump, manualMovement,
            strafeDistance, noEatAttack, criticalsSync, smartCriticals,
            elytraMode, minSpeed, boostDelay);
        rotationSpeed.setVisibility(() -> rotationMode.getValue() != RotationMode.HVH);
        predictionTicks.setVisibility(elytraPredict::get);
        minSpeed.setVisibility(elytraMode::get);
        boostDelay.setVisibility(elytraMode::get);
        strafeDistance.setVisibility(() -> movementMode.getValue() == MovementMode.ORBIT);
        followDistance.setVisibility(() -> movementMode.getValue() == MovementMode.FOLLOW);
        movementPrediction.setVisibility(() -> movementMode.getValue() == MovementMode.FOLLOW);
        autoJump.setVisibility(() -> movementMode.getValue() == MovementMode.FOLLOW);
        manualMovement.setVisibility(() -> movementMode.getValue() != MovementMode.OFF);
        smartCriticals.setVisibility(criticalsSync::get);
    }

    @Override
    public void onDisable() {
        try { super.onDisable(); }
        finally {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.yHeadRot = player.yHeadRotO = player.getYRot();
                player.yBodyRot = player.yBodyRotO = player.getYRot();
            }
        }
    }

    @Override
    protected void onTargetLost() {
        rotations.clear();
        predictor.clear();
        prediction = null;
        nextBoostTick = 0;
        movement.clear();
        VisualRotations.clear(this);
    }

    @Override
    protected void onTargetChanged(LivingEntity previous, LivingEntity next) {
        rotations.retarget();
        predictor.clear();
        prediction = null;
        movement.clear();
        nextBoostTick = 0;
    }

    @Override
    protected boolean requiresLineOfSight() { return movementMode.getValue() != MovementMode.FOLLOW; }

    @Override
    public void onTick() {
        Minecraft client = Minecraft.getInstance();
        if (!selectTarget(client)) return;
        prediction = predictor.predict(client, currentTarget, elytraPredict.get(), predictionTicks.get());
        movement.update(client, currentTarget, new MovementControl.Options(movementMode.getValue(), autoSprint.get(),
            followDistance.get(), strafeDistance.get(), reach.get(), movementPrediction.get(), autoJump.get(), manualMovement.get()));
        var aimBox = prediction.box();
        var aimPoint = prediction.aimPoint();
        var actualBox = currentTarget.getBoundingBox();
        if (TargetSelector.distanceSquared(client.player.getEyePosition(), actualBox) <= reach.get() * reach.get()) {
            aimBox = actualBox;
            aimPoint = RotationMath.clampPoint(aimPoint, RotationMath.interior(actualBox));
        }
        SilentRotations.Angles angles = rotations.update(client, aimBox, aimPoint,
            rotationMode.getValue(), rotationSpeed.get());
        if (angles == null) {
            VisualRotations.clear(this);
            return;
        }
        if (visualRotations.get()) VisualRotations.update(this, client.player.getUUID(), angles);
        else VisualRotations.clear(this);
        boostElytra(client);
        attack(client);
    }

    private void boostElytra(Minecraft client) {
        if (!elytraMode.get() || !client.player.isFallFlying() || client.player.tickCount < nextBoostTick
            || client.player.getDeltaMovement().length() >= minSpeed.get()
            || TargetSelector.distanceSquared(client.player.getEyePosition(), currentTarget.getBoundingBox()) <= reach.get() * reach.get()
            || !TargetSelector.canAct(client, currentTarget)) return;
        int slot = HotbarSlots.find(client.player, stack -> stack.is(Items.FIREWORK_ROCKET) && !stack.isEmpty());
        if (slot < 0) return;
        var result = HotbarSlots.withSlot(client, slot, () -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
        nextBoostTick = client.player.tickCount + boostDelay.getInt();
        if (result.consumesAction()) client.player.swing(InteractionHand.MAIN_HAND);
    }

    private void attack(Minecraft client) {
        if (!TargetSelector.valid(client, currentTarget, filters()) || client.player.getAttackStrengthScale(0.5F) < 0.92F) return;
        if (noEatAttack.get() && client.player.isUsingItem() && !client.player.getUseItem().is(Items.SHIELD)) return;
        double range = reach.get();
        if (TargetSelector.distanceSquared(client.player.getEyePosition(), currentTarget.getBoundingBox()) > range * range) return;
        boolean canCritical = !client.player.isInWater() && !client.player.isInLava()
            && !client.player.onClimbable() && !client.player.isFallFlying() && !client.player.isPassenger();
        boolean waitForCritical = criticalsSync.get() && canCritical
            && (!smartCriticals.get() || client.options.keyJump.isDown() || !client.player.onGround());
        if (waitForCritical && !(client.player.fallDistance > 0.0F && !client.player.onGround()
            && client.player.getDeltaMovement().y < 0.0)) return;
        var attackAngles = rotations.forAttack(client, currentTarget.getBoundingBox(), range);
        if (attackAngles.isEmpty()) return;
        var player = client.player;
        var target = currentTarget;
        if (!TargetSelector.canAct(client, target)) { clearTarget(); return; }
        RotationPackets.with(client, attackAngles.get(), () -> {
            if (!TargetSelector.canAct(client, target)) return false;
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        });
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || !TargetSelector.canAct(client, currentTarget)) return;
        if (targetEsp.get()) TargetBoxRenderer.draw(context, currentTarget.getBoundingBox().inflate(0.015), 0xff5daeff);
        if (truePositionEsp.get() && prediction != null) TargetBoxRenderer.draw(context, prediction.box().inflate(0.025), 0xff63efbc);
    }
}
