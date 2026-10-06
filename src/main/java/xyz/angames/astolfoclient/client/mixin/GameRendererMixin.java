package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.AspectRatioModule;
import xyz.angames.astolfoclient.client.module.modules.render.CameraUtilsModule;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private float zoom;
   @Shadow
   private float zoomX;
   @Shadow
   private float zoomY;

   @Shadow
   public abstract float getDepthFar();

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
      CameraUtilsModule camUtils = CameraUtilsModule.getInstance();
      if (camUtils != null && camUtils.isEnabled()) {
         float baseFov = (Float)cir.getReturnValue();
         cir.setReturnValue((float)camUtils.getAnimatedFov(baseFov));
      }
   }

   @Inject(method = "getProjectionMatrix", at = @At("HEAD"), cancellable = true)
   private void onGetBasicProjectionMatrix(float fov, CallbackInfoReturnable<Matrix4f> cir) {
      AspectRatioModule aspectRatioModule = AspectRatioModule.getInstance();
      if (aspectRatioModule != null && aspectRatioModule.isEnabled()) {
         float defaultAspect = (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight();
         float aspect = aspectRatioModule.getAspectRatio(defaultAspect);
         Matrix4f matrix4f = new Matrix4f();
         if (this.zoom != 1.0F) {
            matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
            matrix4f.scale(this.zoom, this.zoom, 1.0F);
         }

         matrix4f.perspective(fov * (float) (Math.PI / 180.0), aspect, 0.05F, this.getDepthFar());
         cir.setReturnValue(matrix4f);
      }
   }
}
