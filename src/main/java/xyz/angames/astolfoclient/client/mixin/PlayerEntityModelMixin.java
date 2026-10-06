package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
@Mixin(PlayerModel.class)
public abstract class PlayerEntityModelMixin {
   @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("TAIL"))
   private void swellBabyHead(PlayerRenderState state, CallbackInfo ci) {
      if (Minecraft.getInstance().player != null && state.id == Minecraft.getInstance().player.getId()) {
         Module babyMod = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer");
         PlayerModel model = (PlayerModel)(Object)this;
         if (babyMod != null && babyMod.isEnabled()) {
            model.head.xScale = 1.75F;
            model.head.yScale = 1.75F;
            model.head.zScale = 1.75F;
         } else {
            model.head.xScale = 1.0F;
            model.head.yScale = 1.0F;
            model.head.zScale = 1.0F;
         }
      }
   }
}
