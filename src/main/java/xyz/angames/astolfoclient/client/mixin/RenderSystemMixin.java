package xyz.angames.astolfoclient.client.mixin;

import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;

@Environment(EnvType.CLIENT)
@Mixin(RenderSystem.class)
public class RenderSystemMixin {
   @ModifyVariable(method = "setShaderFog", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private static FogParameters modifyFog(FogParameters originalFog) {
      if (AstolfoclientClient.moduleManager == null) {
         return originalFog;
      }

      var mc=net.minecraft.client.Minecraft.getInstance();
      if(mc.level==null || mc.player==null || mc.gameRenderer.getMainCamera().getFluidInCamera()!=net.minecraft.world.level.material.FogType.NONE
         || mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
         || mc.player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))return originalFog;
      AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
      if (ambients != null && ambients.isEnabled() && ambients.customFog.get()) {
         float start = originalFog.start();
         float end = originalFog.end();
         float r = originalFog.red();
         float g = originalFog.green();
         float b = originalFog.blue();
         float a = originalFog.alpha();
         start = (float)ambients.fogStart.get();
         end = Math.max(start + .1f,(float)ambients.fogEnd.get());
         if (ambients.fogColorEnabled.get() && (ambients.themeSync.get() || ambients.separateColors.get())) {
            float strength = (float)ambients.fogStrength.get();
            if (strength > 0.0F) {
               Color themeColor = new Color(ambients.fogColor());
               float fr = themeColor.getRed() / 255.0F;
               float fg = themeColor.getGreen() / 255.0F;
               float fb = themeColor.getBlue() / 255.0F;
               r = r * (1.0F - strength) + fr * strength;
               g = g * (1.0F - strength) + fg * strength;
               b = b * (1.0F - strength) + fb * strength;
            }
         }

         var shape = switch (ambients.fogShape.get()) {
            case "Sphere" -> com.mojang.blaze3d.shaders.FogShape.SPHERE;
            case "Cylinder" -> com.mojang.blaze3d.shaders.FogShape.CYLINDER;
            default -> originalFog.shape();
         };
         return new FogParameters(start, end, shape, r, g, b, a * ambients.fogOpacity.getFloat());
      } else {
         return originalFog;
      }
   }
}
