package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(EnvType.CLIENT)
public class KeybindSetting extends Setting {
   private int key;

   public KeybindSetting(String name, int defaultKey) {
      super(name);
      this.key = defaultKey;
   }

   public int getKey() {
      return this.key;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public String getKeyName() {
      return this.key != 0 && this.key != -1 ? KeyUtils.getKeyName(this.key) : "None";
   }
}
