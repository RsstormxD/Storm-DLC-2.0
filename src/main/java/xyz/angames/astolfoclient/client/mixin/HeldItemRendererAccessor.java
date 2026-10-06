package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(ItemInHandRenderer.class)
public interface HeldItemRendererAccessor {
   @Invoker("renderPlayerArm")
   void invokeRenderArmHoldingItem(PoseStack var1, MultiBufferSource var2, int var3, float var4, float var5, HumanoidArm var6);
}
