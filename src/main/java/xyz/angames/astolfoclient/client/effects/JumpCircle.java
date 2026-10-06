package xyz.angames.astolfoclient.client.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class JumpCircle {
   public final long creationTime = System.currentTimeMillis();
   public final double x;
   public final double y;
   public final double z;
   public final long delay;
   public final long expansion;
   public final long lifespan;

   public float growth(long age) { return 1 - (float)Math.pow(1 - Math.min(1, Math.max(0, age)/(float)expansion), 3); }
   public float alpha(long age) { return 1 - Math.max(0, age - expansion)/(float)(lifespan - expansion); }

   public JumpCircle(double x, double y, double z, long delay) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.delay = delay;
      var module = (xyz.angames.astolfoclient.client.module.modules.JumpCircleModule)xyz.angames.astolfoclient.client.module.ModuleManager.getModule(xyz.angames.astolfoclient.client.module.modules.JumpCircleModule.class);
      this.expansion = module == null ? 400 : module.expansion.getInt();
      this.lifespan = this.expansion + (module == null ? 900 : module.fade.getInt());
   }
}
