package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
public class ChinaHatModule extends Module {
   public final xyz.angames.astolfoclient.client.module.setting.NumberSetting radius = new xyz.angames.astolfoclient.client.module.setting.NumberSetting("Hat radius", .65, .3, 1.2, .05);
   public final xyz.angames.astolfoclient.client.module.setting.NumberSetting height = new xyz.angames.astolfoclient.client.module.setting.NumberSetting("Hat height", .28, .05, .8, .01);
   public final xyz.angames.astolfoclient.client.module.setting.NumberSetting offset = new xyz.angames.astolfoclient.client.module.setting.NumberSetting("Vertical offset", 0, -.3, .5, .025);
   public final xyz.angames.astolfoclient.client.module.setting.NumberSetting spin = new xyz.angames.astolfoclient.client.module.setting.NumberSetting("Rotation speed", 80, -180, 180, 5);

   public ChinaHatModule() {
      super("ChinaHat", Module.Category.RENDER);
   }
}
