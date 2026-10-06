package xyz.angames.astolfoclient.client.mixin;

import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public class ClientWorldMixin {
   @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
   private void stormCloudColor(float tickDelta, CallbackInfoReturnable<Integer> cir) {
      var module = (AmbientsModule)xyz.angames.astolfoclient.client.module.ModuleManager.getModule(AmbientsModule.class);
      if (module == null || !module.isEnabled() || !module.coloredClouds.get()) return;
      int original = cir.getReturnValue(), color = module.cloudsRgb();
      float light = ((original >> 16 & 255)*.2126f + (original >> 8 & 255)*.7152f + (original & 255)*.0722f)/255f;
      float strength = module.cloudStrength.getFloat();
      int r = (int)((original >> 16 & 255)*(1-strength) + (color >> 16 & 255)*light*strength);
      int g = (int)((original >> 8 & 255)*(1-strength) + (color >> 8 & 255)*light*strength);
      int b = (int)((original & 255)*(1-strength) + (color & 255)*light*strength);
      cir.setReturnValue((original & 0xff000000) | r << 16 | g << 8 | b);
   }

   @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
   private void stormStarBrightness(float tickDelta, CallbackInfoReturnable<Float> cir) {
      var module = (AmbientsModule)xyz.angames.astolfoclient.client.module.ModuleManager.getModule(AmbientsModule.class);
      if (module != null && module.isEnabled()) cir.setReturnValue(Math.min(1, cir.getReturnValue() * module.starBrightness.getFloat()));
   }

   @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
   private void onGetSkyColor(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
      if (AstolfoclientClient.moduleManager != null) {
         AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
         if (ambients != null && ambients.isEnabled() && ambients.customSkybox.get()) {
            float strength = (float)ambients.skyboxStrength.get();
            if (strength > 0.0F) {
               Color themeColor = new Color(ambients.skyColor());
               int originalColor = (Integer)cir.getReturnValue();
               int origR = originalColor >> 16 & 0xFF;
               int origG = originalColor >> 8 & 0xFF;
               int origB = originalColor & 0xFF;
               int themeR = themeColor.getRed();
               int themeG = themeColor.getGreen();
               int themeB = themeColor.getBlue();
               int finalR = (int)(origR * (1.0F - strength) + themeR * strength);
               int finalG = (int)(origG * (1.0F - strength) + themeG * strength);
               int finalB = (int)(origB * (1.0F - strength) + themeB * strength);
               int finalColor = finalR << 16 | finalG << 8 | finalB;
               cir.setReturnValue(finalColor);
            }
         }
      }
   }
}
