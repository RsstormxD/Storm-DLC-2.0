package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ActionSetting extends Setting {
   private final Runnable action;

   public ActionSetting(String name, Runnable action) {
      super(name);
      this.action = action;
   }

   public void run() {
      if (this.action != null) {
         this.action.run();
      }
   }
}
