package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import xyz.angames.astolfoclient.client.config.SoundSettings;

@Environment(EnvType.CLIENT)
public class ModSounds {
   public static final ResourceLocation ENABLE_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "module_enable");
   public static final SoundEvent ENABLE_SOUND = SoundEvent.createVariableRangeEvent(ENABLE_ID);
   public static final ResourceLocation DISABLE_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "module_disable");
   public static final SoundEvent DISABLE_SOUND = SoundEvent.createVariableRangeEvent(DISABLE_ID);
   public static final ResourceLocation CRASH_DETECTION_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "crash_detection");
   public static final SoundEvent CRASH_DETECTION_SOUND = SoundEvent.createVariableRangeEvent(CRASH_DETECTION_ID);
   public static final ResourceLocation CRASH_DETECTION_HYPHEN_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "crash-detection");
   public static final SoundEvent CRASH_DETECTION_HYPHEN_SOUND = SoundEvent.createVariableRangeEvent(CRASH_DETECTION_HYPHEN_ID);
   public static final ResourceLocation GUI_OPEN_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "clickgui_open");
   public static final SoundEvent GUI_OPEN_SOUND = SoundEvent.createVariableRangeEvent(GUI_OPEN_ID);
   public static final ResourceLocation CATEGORY_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "clickgui_category");
   public static final SoundEvent CATEGORY_SOUND = SoundEvent.createVariableRangeEvent(CATEGORY_ID);
   public static final ResourceLocation MODULE_SELECT_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "clickgui_module");
   public static final SoundEvent MODULE_SELECT_SOUND = SoundEvent.createVariableRangeEvent(MODULE_SELECT_ID);
   public static final ResourceLocation SLIDER_MOVING_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "clickgui_slider");
   public static final SoundEvent SLIDER_MOVING_SOUND = SoundEvent.createVariableRangeEvent(SLIDER_MOVING_ID);
   public static final ResourceLocation SEARCH_CLICK_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "clickgui_search");
   public static final SoundEvent SEARCH_CLICK_SOUND = SoundEvent.createVariableRangeEvent(SEARCH_CLICK_ID);
   public static final ResourceLocation MODE_OPEN_ID = ResourceLocation.fromNamespaceAndPath("astolfoclient", "clickgui_mode_open");
   public static final SoundEvent MODE_OPEN_SOUND = SoundEvent.createVariableRangeEvent(MODE_OPEN_ID);
   private static long lastSliderSoundTime = 0L;

   public static void register() {
      registerSound(ENABLE_ID, ENABLE_SOUND);
      registerSound(DISABLE_ID, DISABLE_SOUND);
      registerSound(CRASH_DETECTION_ID, CRASH_DETECTION_SOUND);
      registerSound(CRASH_DETECTION_HYPHEN_ID, CRASH_DETECTION_HYPHEN_SOUND);
      registerSound(GUI_OPEN_ID, GUI_OPEN_SOUND);
      registerSound(CATEGORY_ID, CATEGORY_SOUND);
      registerSound(MODULE_SELECT_ID, MODULE_SELECT_SOUND);
      registerSound(SLIDER_MOVING_ID, SLIDER_MOVING_SOUND);
      registerSound(SEARCH_CLICK_ID, SEARCH_CLICK_SOUND);
      registerSound(MODE_OPEN_ID, MODE_OPEN_SOUND);
   }

   private static void registerSound(ResourceLocation id, SoundEvent sound) {
      if (!BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
         Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound);
      }
   }

   public static void playSound(SoundEvent sound, float volume) {
      if (SoundSettings.isSoundEnabled()) {
         float master = SoundSettings.getMasterVolume() / 100.0F;
         float vol = volume / 100.0F * master;
         if (!(vol <= 0.001F)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
               mc.execute(() -> {
                  try {
                     if (mc.getSoundManager() != null) {
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, vol));
                     }
                  } catch (Exception var4x) {
                  }
               });
            }
         }
      }
   }

   public static void playGuiOpen() {
      playSound(GUI_OPEN_SOUND, SoundSettings.getGuiOpenVolume());
   }

   public static void playCategoryChange() {
      playSound(CATEGORY_SOUND, SoundSettings.getCategoryVolume());
   }

   public static void playModuleSelect() {
      playSound(MODULE_SELECT_SOUND, SoundSettings.getModuleSelectVolume());
   }

   public static void playSliderMove() {
      long now = System.currentTimeMillis();
      if (now - lastSliderSoundTime >= 65L) {
         lastSliderSoundTime = now;
         playSound(SLIDER_MOVING_SOUND, SoundSettings.getSliderVolume());
      }
   }

   public static void playSearchClick() {
      playSound(SEARCH_CLICK_SOUND, SoundSettings.getSearchVolume());
   }

   public static void playModeOpen() {
      playSound(MODE_OPEN_SOUND, SoundSettings.getModeOpenVolume());
   }

   public static void playEnable() {
      playSound(ENABLE_SOUND, SoundSettings.getModuleToggleVolume());
   }

   public static void playDisable() {
      playSound(DISABLE_SOUND, SoundSettings.getModuleToggleVolume());
   }

   public static void playCrashDetectionSound() {
      playSound(CRASH_DETECTION_SOUND, 100.0F);
   }
}
