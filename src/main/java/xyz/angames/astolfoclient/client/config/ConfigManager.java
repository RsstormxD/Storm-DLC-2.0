package xyz.angames.astolfoclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.hud.ArmorHudManager;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.KeybindSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(EnvType.CLIENT)
public class ConfigManager {
   private final Path mainDir;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private volatile String activeProfile = "default";
   public String getActiveProfile() { return activeProfile; }

   public ConfigManager() {
      this.mainDir = dev.stormdlc.config.ClientPaths.configDirectory();
   }

   public ConfigManager.ClientConfig buildCurrentConfig() {
      ConfigManager.ClientConfig config = new ConfigManager.ClientConfig();

      for (Module module : AstolfoclientClient.moduleManager.getModules()) {
         ConfigManager.ModuleData data = new ConfigManager.ModuleData();
         data.enabled = module.isEnabled();
         data.keyCode = module.getKeyCode();

         for (Setting setting : collectModuleSettings(module)) {
            Object valueToSave = this.getSettingValue(setting);
            if (valueToSave != null) {
               data.settings.put(setting.getName(), valueToSave);
            }
         }

         config.modules.put(module.getName(), data);
      }

      config.specialBinds.put("clickgui", AstolfoclientClient.clickGuiKeyCode);
      config.specialBinds.put("hudeditor", AstolfoclientClient.hudEditorKeyCode);
      this.saveHudPositions(config);
      config.theme = ThemeManager.getCurrentTheme().name();
      config.customColor1 = ThemeManager.getCustomColor1Hex();
      config.customColor2 = ThemeManager.getCustomColor2Hex();
      config.watermarkPosition = LogoRenderer.watermarkPosition;
      config.watermarkShowAvatar = LogoRenderer.showAvatar;
      List<String> enabledNames = new ArrayList<>();

      for (LogoRenderer.SectionType s : LogoRenderer.enabledSections) {
         enabledNames.add(s.name());
      }

      config.watermarkEnabledSections = enabledNames;
      List<String> orderNames = new ArrayList<>();

      for (LogoRenderer.SectionType s : LogoRenderer.getSectionOrder()) {
         orderNames.add(s.name());
      }

      config.watermarkSectionOrder = orderNames;
      config.armorHudLayout = ArmorHudManager.layout;
      config.armorHudWarningGlow = ArmorHudManager.warningGlow;
      config.soundMasterVolume = SoundSettings.getMasterVolume();
      config.soundGuiOpenVolume = SoundSettings.getGuiOpenVolume();
      config.soundCategoryVolume = SoundSettings.getCategoryVolume();
      config.soundModuleSelectVolume = SoundSettings.getModuleSelectVolume();
      config.soundSliderVolume = SoundSettings.getSliderVolume();
      config.soundSearchVolume = SoundSettings.getSearchVolume();
      config.soundModeOpenVolume = SoundSettings.getModeOpenVolume();
      config.soundModuleToggleVolume = SoundSettings.getModuleToggleVolume();
      config.guiScale = GuiScaleSettings.getScale();
      return config;
   }

   public String serializeCurrentConfig() {
      return this.gson.toJson(this.buildCurrentConfig());
   }

   /** Imports without replacing existing profiles. Loading stays an explicit GUI action. */
   public String importConfig(String input) throws IOException {
      String raw=input.strip(); String name="imported";
      if(!raw.startsWith("{")){
         if(raw.startsWith("\"") && raw.endsWith("\""))raw=raw.substring(1,raw.length()-1);
         Path source=Path.of(raw);
         if(!Files.isRegularFile(source) || Files.size(source)>1048576)throw new IOException("Select a JSON file smaller than 1 MB");
         name=source.getFileName().toString().replaceFirst("(?i)\\.json$", "");raw=Files.readString(source);
      }
      if(raw.length()>1048576)throw new IOException("Config is larger than 1 MB");
      try{
         var tree=com.google.gson.JsonParser.parseString(raw);
         if(!tree.isJsonObject() || !tree.getAsJsonObject().has("modules") || !tree.getAsJsonObject().get("modules").isJsonObject()
            || tree.getAsJsonObject().getAsJsonObject("modules").size()==0)throw new IllegalArgumentException();
         ClientConfig config=gson.fromJson(tree,ClientConfig.class);
         for(var data:config.modules.values())if(data==null || data.settings==null)throw new IllegalArgumentException();
         validateNumbers(tree);
         raw=gson.toJson(tree);
      }catch(Exception e){throw new IOException("Invalid Storm DLC 2.0 config: expected modules and settings",e);}
      name=name.replaceAll("[^a-zA-Z0-9_ -]","_").strip();
      if(name.isEmpty())name="imported";if(name.length()>60)name=name.substring(0,60);
      Path dir=mainDir.resolve("configs");Files.createDirectories(dir);
      String candidate=name;
      for(int n=1;;n++){
         try{Files.writeString(dir.resolve(candidate+".json"),raw,java.nio.charset.StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE_NEW);return candidate;}
         catch(java.nio.file.FileAlreadyExistsException e){candidate=name+"-"+n;}
      }
   }
   private static void validateNumbers(com.google.gson.JsonElement e){
      if(e.isJsonObject())e.getAsJsonObject().entrySet().forEach(v->validateNumbers(v.getValue()));
      else if(e.isJsonArray())e.getAsJsonArray().forEach(ConfigManager::validateNumbers);
      else if(e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && !Double.isFinite(e.getAsDouble()))throw new IllegalArgumentException();
   }
   private static String validName(String name){
      if(name==null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(":") || name.equals(".."))throw new IllegalArgumentException("Invalid config name");
      return name;
   }

   public String saveConfig(String configName) {
      String jsonOutput = this.serializeCurrentConfig();

      try {
         Path configsDir = this.mainDir.resolve("configs");
         if (!Files.exists(configsDir)) {
            Files.createDirectories(configsDir);
         }

         File configFile = configsDir.resolve(validName(configName) + ".json").toFile();

         try (FileWriter writer = new FileWriter(configFile, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
         }

         return null;
      } catch (Exception e) {
         return e.getMessage() != null ? e.getMessage() : "Unknown error occurred while saving config.";
      }
   }

   public boolean loadConfig(String configName) {
      try {
         Path configsDir = this.mainDir.resolve("configs");
         File configFile = configsDir.resolve(validName(configName) + ".json").toFile();
         if (!configFile.exists()) {
            return false;
         }

         String jsonInput = Files.readString(configFile.toPath());
         if (jsonInput != null && !jsonInput.isEmpty()) {
            Type type = (new TypeToken<ConfigManager.ClientConfig>() {}).getType();
            ConfigManager.ClientConfig config = (ConfigManager.ClientConfig)this.gson.fromJson(jsonInput, type);
            if (config == null) {
               return false;
            }

            Minecraft.getInstance().execute(() -> {
               this.applyModuleData(config);
               activeProfile = validName(configName);
               if (config.specialBinds != null) {
                  AstolfoclientClient.clickGuiKeyCode = config.specialBinds.getOrDefault("clickgui", 260);
                  AstolfoclientClient.hudEditorKeyCode = config.specialBinds.getOrDefault("hudeditor", 79);
               }

               this.applyHudPositions(config);
               if (config.theme != null) {
                  try {
                     ThemeManager.setCurrentTheme(ThemeManager.Theme.valueOf(config.theme));
                  } catch (Exception var9) {
                  }
               }

               if (config.customColor1 != null && config.customColor2 != null) {
                  ThemeManager.setCustomColors(config.customColor1, config.customColor2);
               }

               if (config.watermarkPosition != null) {
                  LogoRenderer.watermarkPosition = config.watermarkPosition;
               }

               LogoRenderer.showAvatar = config.watermarkShowAvatar;
               if (config.watermarkEnabledSections != null && !config.watermarkEnabledSections.isEmpty()) {
                  LogoRenderer.enabledSections.clear();
                  LogoRenderer.enabledSections.add(LogoRenderer.SectionType.BRAND);

                  for (String sName : config.watermarkEnabledSections) {
                     try {
                        LogoRenderer.enabledSections.add(LogoRenderer.SectionType.valueOf(sName));
                     } catch (Exception var8) {
                     }
                  }
               }

               if (config.watermarkSectionOrder != null && !config.watermarkSectionOrder.isEmpty()) {
                  List<LogoRenderer.SectionType> loadedOrder = new ArrayList<>();

                  for (String sName : config.watermarkSectionOrder) {
                     try {
                        loadedOrder.add(LogoRenderer.SectionType.valueOf(sName));
                     } catch (Exception var7x) {
                     }
                  }

                  if (!loadedOrder.contains(LogoRenderer.SectionType.BRAND)) {
                     loadedOrder.add(0, LogoRenderer.SectionType.BRAND);
                  }

                  for (LogoRenderer.SectionType secType : LogoRenderer.SectionType.values()) {
                     if (!loadedOrder.contains(secType)) {
                        loadedOrder.add(secType);
                     }
                  }

                  LogoRenderer.setSectionOrder(loadedOrder);
               }

               if (config.armorHudLayout != null) {
                  ArmorHudManager.layout = config.armorHudLayout;
               }

               if (config.armorHudWarningGlow != null) {
                  ArmorHudManager.warningGlow = config.armorHudWarningGlow;
               }

               if (config.soundMasterVolume != null) {
                  SoundSettings.setMasterVolume(config.soundMasterVolume);
               }

               if (config.soundGuiOpenVolume != null) {
                  SoundSettings.setGuiOpenVolume(config.soundGuiOpenVolume);
               }

               if (config.soundCategoryVolume != null) {
                  SoundSettings.setCategoryVolume(config.soundCategoryVolume);
               }

               if (config.soundModuleSelectVolume != null) {
                  SoundSettings.setModuleSelectVolume(config.soundModuleSelectVolume);
               }

               if (config.soundSliderVolume != null) {
                  SoundSettings.setSliderVolume(config.soundSliderVolume);
               }

               if (config.soundSearchVolume != null) {
                  SoundSettings.setSearchVolume(config.soundSearchVolume);
               }

               if (config.soundModeOpenVolume != null) {
                  SoundSettings.setModeOpenVolume(config.soundModeOpenVolume);
               }

               if (config.soundModuleToggleVolume != null) {
                  SoundSettings.setModuleToggleVolume(config.soundModuleToggleVolume);
               }

               if (config.guiScale != null) {
                  GuiScaleSettings.setScale(config.guiScale);
               }
               dev.stormdlc.hud.ClientFeedback.react("profile", "Profile loaded", activeProfile,
                   dev.stormdlc.hud.ClientFeedback.Tone.SUCCESS, 2500, 90);
            });
            return true;
         } else {
            return false;
         }
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   public List<String> getCloudConfigs() {
      return this.getLocalConfigs();
   }

   public List<String> getLocalConfigs() {
      List<String> list = new ArrayList<>();

      try {
         Path configsDir = this.mainDir.resolve("configs");
         if (Files.exists(configsDir)) {
            try (Stream<Path> stream = Files.list(configsDir)) {
               stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                  String name = p.getFileName().toString();
                  list.add(name.substring(0, name.length() - 5));
               });
            }
         }
      } catch (Exception var8) {
      }

      return list;
   }

   public boolean deleteConfig(String configName) {
      try {
         Path configsDir = this.mainDir.resolve("configs");
         File configFile = configsDir.resolve(validName(configName) + ".json").toFile();
         if (configFile.exists()) {
            return configFile.delete();
         }
      } catch (Exception var4) {
      }

      return false;
   }

   private static List<Setting> collectModuleSettings(Module module) {
      List<Setting> result = new ArrayList<>();
      java.util.Set<Setting> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
      java.util.ArrayDeque<Setting> queue = new java.util.ArrayDeque<>(module.getSettings());
      while (!queue.isEmpty()) {
         Setting setting = queue.removeFirst();
         if (!visited.add(setting)) continue;
         if (setting instanceof xyz.angames.astolfoclient.client.module.setting.ConfigureSetting group) {
            for (Setting child : group.getSubSettings()) if (child != null) queue.addLast(child);
         } else if (setting instanceof xyz.angames.astolfoclient.client.module.setting.MultiSelectSetting group) {
            for (Setting child : group.getOptions()) if (child != null) queue.addLast(child);
         } else result.add(setting);
      }
      return result;
   }

   private Object getSettingValue(Setting setting) {
      if (setting instanceof BooleanSetting s) {
         return s.get();
      } else if (setting instanceof NumberSetting s) {
         return s.get();
      } else if (setting instanceof ModeSetting s) {
         return s.get();
      } else if (setting instanceof EnumSetting<?> s) {
         return s.getValue().name();
      } else {
         return setting instanceof KeybindSetting s ? s.getKey() : null;
      }
   }

   private void saveHudPositions(ConfigManager.ClientConfig config) {
      for (Field field : AstolfoclientClient.class.getDeclaredFields()) {
         if (Modifier.isStatic(field.getModifiers())) {
            try {
               Object instance = field.get(null);
               if (instance != null) {
                  Field xF = instance.getClass().getDeclaredField("x");
                  Field yF = instance.getClass().getDeclaredField("y");
                  config.hudPositions.put(field.getName() + "_x", xF.getDouble(instance));
                  config.hudPositions.put(field.getName() + "_y", yF.getDouble(instance));
               }
            } catch (Exception var9) {
            }
         }
      }
   }

   private void applyModuleData(ConfigManager.ClientConfig config) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && !client.isSameThread()) {
         client.execute(() -> applyModuleData(config));
         return;
      }
      if (config.modules == null) return;
      try (var silence = dev.stormdlc.hud.ClientFeedback.suppress()) {
      for (Entry<String, ConfigManager.ModuleData> entry : config.modules.entrySet()) {
         Module module = AstolfoclientClient.moduleManager.getModuleByName(entry.getKey());
         ConfigManager.ModuleData data = entry.getValue();
         if (module == null || data == null) continue;
         module.setEnabled(false);
         module.setKeyCode(data.keyCode);
         if (data.settings != null) for (Setting setting : collectModuleSettings(module)) {
            String key = setting.getName();
            if (key.equals("Spinning logo") && !data.settings.containsKey(key)) key = "Spinning logo info";
            if (key.equals("Elytra Boost") && !data.settings.containsKey(key)) key = "Elytra Mode";
            if (key.equals("Target Strafe") && !data.settings.containsKey(key)) key = "Target Lock";
            if (!data.settings.containsKey(key)) continue;
            String value = String.valueOf(data.settings.get(key));
            try {
               if (setting instanceof BooleanSetting booleanSetting) booleanSetting.set(Boolean.parseBoolean(value));
               else if (setting instanceof NumberSetting numberSetting) numberSetting.set(Double.parseDouble(value));
               else if (setting instanceof ModeSetting modeSetting) modeSetting.set(value);
               else if (setting instanceof EnumSetting<?> enumSetting) {
                  if (key.equals("Rotation Mode") && value.equalsIgnoreCase("MineStar V3")) value = "MineStar V2";
                  enumSetting.setByName(value);
               } else if (setting instanceof KeybindSetting keybindSetting) keybindSetting.setKey((int) Double.parseDouble(value));
            } catch (RuntimeException failure) {
               org.slf4j.LoggerFactory.getLogger("StormDLC/Config").debug("Ignoring invalid setting {} for {}", key, module.getName(), failure);
            }
         }
         module.setEnabled(data.enabled);
      }
      }
   }

   private void applyHudPositions(ConfigManager.ClientConfig config) {
      if (config.hudPositions != null) {
         for (Field field : AstolfoclientClient.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
               try {
                  Object instance = field.get(null);
                  if (instance != null) {
                     String xKey = field.getName() + "_x";
                     String yKey = field.getName() + "_y";
                     if (config.hudPositions.containsKey(xKey)) {
                        float valX = config.hudPositions.get(xKey).floatValue();

                        try {
                           instance.getClass().getField("x").setFloat(instance, valX);
                        } catch (Exception e) {
                           instance.getClass().getMethod("setX", float.class).invoke(instance, valX);
                        }
                     }

                     if (config.hudPositions.containsKey(yKey)) {
                        float valY = config.hudPositions.get(yKey).floatValue();

                        try {
                           instance.getClass().getField("y").setFloat(instance, valY);
                        } catch (Exception e) {
                           instance.getClass().getMethod("setY", float.class).invoke(instance, valY);
                        }
                     }
                  }
               } catch (Exception var13) {
               }
            }
         }
      }
   }

   public void saveFriends() {
      xyz.angames.astolfoclient.client.util.FriendsManager.save();
   }

   public void loadFriends() {
      xyz.angames.astolfoclient.client.util.FriendsManager.load();
   }

   @Environment(EnvType.CLIENT)
   private static class ClientConfig {
      Map<String, ConfigManager.ModuleData> modules = new HashMap<>();
      Map<String, Integer> specialBinds = new HashMap<>();
      Map<String, Double> hudPositions = new HashMap<>();
      String theme;
      String customColor1;
      String customColor2;
      String watermarkPosition = "TOP_LEFT";
      boolean watermarkShowAvatar = true;
      List<String> watermarkEnabledSections = new ArrayList<>();
      List<String> watermarkSectionOrder = new ArrayList<>();
      String armorHudLayout = "VERTICAL";
      Boolean armorHudWarningGlow = true;
      Float soundMasterVolume;
      Float soundGuiOpenVolume;
      Float soundCategoryVolume;
      Float soundModuleSelectVolume;
      Float soundSliderVolume;
      Float soundSearchVolume;
      Float soundModeOpenVolume;
      Float soundModuleToggleVolume;
      Float guiScale;
   }

   @Environment(EnvType.CLIENT)
   private static class ModuleData {
      boolean enabled;
      int keyCode;
      Map<String, Object> settings = new HashMap<>();
   }
}
