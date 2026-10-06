package xyz.angames.astolfoclient.client.module.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MultiSelectSetting extends Setting {
   private final String buttonText;
   private final List<BooleanSetting> options = new ArrayList<>();

   public MultiSelectSetting(String name, String buttonText, BooleanSetting... options) {
      super(name);
      this.buttonText = buttonText;
      if (options != null) {
         this.options.addAll(Arrays.asList(options));
      }
   }

   public MultiSelectSetting(String name, String buttonText, List<BooleanSetting> options) {
      super(name);
      this.buttonText = buttonText;
      if (options != null) {
         this.options.addAll(options);
      }
   }

   public String getButtonText() {
      return this.buttonText;
   }

   public List<BooleanSetting> getOptions() {
      return this.options;
   }

   public int getSelectedCount() {
      int count = 0;

      for (BooleanSetting b : this.options) {
         if (b.get()) {
            count++;
         }
      }

      return count;
   }
}
