package xyz.angames.astolfoclient.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class GuiScaleSettings {
   public static final float[] SCALES = new float[]{0.35F, 0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 2.0F};
   public static final String[] SCALE_LABELS = new String[]{"35%", "50%", "75%", "100%", "125%", "150%", "200%"};
   private static float scale = 1.0F;

   public static float getScale() {
      return scale;
   }

   public static void setScale(float newScale) {
      scale = newScale;
   }

   public static String getScaleLabel() {
      return Math.round(scale * 100.0F) + "%";
   }

   public static int getSelectedIndex() {
      for (int i = 0; i < SCALES.length; i++) {
         if (Math.abs(SCALES[i] - scale) < 0.01F) {
            return i;
         }
      }

      return 3;
   }
}
