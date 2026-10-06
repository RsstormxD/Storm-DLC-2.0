package xyz.angames.astolfoclient.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.stormdlc.combat.VisualRotations;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
public abstract class VisualRotationMixin {
    @WrapMethod(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V")
    private void stormdlc$visualRotation(LivingEntity entity, LivingEntityRenderState state, float partialTick, Operation<Void> original) {
        VisualRotations.Frame frame = VisualRotations.forEntity(entity);
        if (frame == null) {
            original.call(entity, state, partialTick);
            return;
        }
        float head = entity.yHeadRot, oldHead = entity.yHeadRotO;
        float body = entity.yBodyRot, oldBody = entity.yBodyRotO, oldYaw = entity.yRotO;
        var angles = frame.interpolate(partialTick);
        try {
            entity.yHeadRot = entity.yHeadRotO = angles.yaw();
            entity.yBodyRot = entity.yBodyRotO = angles.yaw();
            entity.yRotO = angles.yaw();
            original.call(entity, state, partialTick);
            state.bodyRot = angles.yaw();
            state.yRot = 0.0F;
            state.xRot = angles.pitch();
        } finally {
            entity.yHeadRot = head;
            entity.yHeadRotO = oldHead;
            entity.yBodyRot = body;
            entity.yBodyRotO = oldBody;
            entity.yRotO = oldYaw;
        }
    }
}
