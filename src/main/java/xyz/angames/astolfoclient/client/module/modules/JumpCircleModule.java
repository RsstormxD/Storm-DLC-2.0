package xyz.angames.astolfoclient.client.module.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class JumpCircleModule extends Module {
   public final NumberSetting expansion = new NumberSetting("Expansion time (ms)", 400, 100, 1500, 25);
   public final NumberSetting fade = new NumberSetting("Fade time (ms)", 900, 200, 2500, 25);
   public final NumberSetting ringWidth = new NumberSetting("Ring width", .12, .02, .45, .01);
   public final NumberSetting glow = new NumberSetting("Glow intensity", 1, .1, 2, .1);
   public final NumberSetting rotation = new NumberSetting("Rotation multiplier", 1, -3, 3, .1);
   public final NumberSetting radius = new NumberSetting("Radius", 3.2, 1.0, 6.0, 0.1);
   public final BooleanSetting distortion = new BooleanSetting("Distortion", true);
   public final NumberSetting distortionStrength = new NumberSetting("Strength", 1.0, 0.2, 2.5, 0.1) {
      @Override
      public boolean isVisible() {
         return JumpCircleModule.this.distortion.get();
      }
   };
   public final BooleanSetting chromatic = new BooleanSetting("Chromatic", true) {
      @Override
      public boolean isVisible() {
         return JumpCircleModule.this.distortion.get();
      }
   };
   public final BooleanSetting texture = new BooleanSetting("Texture", true);
   public final ModeSetting style = new ModeSetting("Style", "Client", "Client", "Large", "Slim", "Ring", "Double Ring") {
      @Override
      public boolean isVisible() {
         return JumpCircleModule.this.texture.get();
      }
   };

   public JumpCircleModule() {
      super("JumpCircle", Module.Category.RENDER);
      ringWidth.setVisibility(() -> texture.get() && (style.is("Ring") || style.is("Double Ring")));
      this.addSetting(this.radius);
      this.addSetting(this.distortion);
      this.addSetting(this.distortionStrength);
      this.addSetting(this.chromatic);
      this.addSetting(this.texture);
      this.addSetting(this.style);
   }
}
