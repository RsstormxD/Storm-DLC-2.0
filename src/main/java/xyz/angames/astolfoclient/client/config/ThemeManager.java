package xyz.angames.astolfoclient.client.config;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ThemeManager {
   public static Color themeColor = new Color(16738740);
   public static Color customColor1 = themeColor;
   public static Color customColor2 = themeColor;

   public static int getThemedColor() {
      return themeColor.getRGB();
   }

   public static int getThemedColor(long offset) {
      return themeColor.getRGB();
   }

   public static void setThemedColor(int rgb) {
      setThemeColor(new Color(rgb));
   }

   public static void setThemeColor(Color c) {
      if (c != null) {
         themeColor = c;
         customColor1 = c;
         customColor2 = c;
      }
   }

   public static Color getThemeColor() {
      return themeColor;
   }

   public static Color getCustomColor1() {
      return themeColor;
   }

   public static void setCustomColor1(Color c) {
      setThemeColor(c);
   }

   public static Color getCustomColor2() {
      return themeColor;
   }

   public static void setCustomColor2(Color c) {
      setThemeColor(c);
   }

   public static ThemeManager.Theme getCurrentTheme() {
      return ThemeManager.Theme.CUSTOM;
   }

   public static void setCurrentTheme(ThemeManager.Theme theme) {
   }

   public static void cycleTheme() {
   }

   public static String getCustomColor1Hex() {
      return String.format("#%06X", 16777215 & themeColor.getRGB());
   }

   public static String getCustomColor2Hex() {
      return getCustomColor1Hex();
   }

   public static void setCustomColors(String hex1, String hex2) {
      try {
         setThemeColor(Color.decode(hex1));
      } catch (Exception var3) {
      }
   }

   public static void setCustomColor(String hex) {
      try {
         setThemeColor(Color.decode(hex));
      } catch (Exception var2) {
      }
   }

   @Environment(EnvType.CLIENT)
   public enum Theme {
      CUSTOM;
   }
}
