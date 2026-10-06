package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ColorSetting;
import xyz.angames.astolfoclient.client.module.setting.ActionSetting;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ConfigureSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.KeybindSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.MultiSelectSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class UniversalSettingsPanel {
   private final Module module;
   private final List<Object> rows = new ArrayList<>();
   private static final float SLIDER_H = 23.0F;
   private static final float BOOL_H = 18.0F;
   private static final float MODE_H = 22.0F;
   private static final float MODE_ITEM_H = 15.0F;
   private static final float KEYBIND_H = 20.0F;
   private static final float ACTION_H = 22.0F;
   private static final Color C_LABEL_OFF = new Color(145, 145, 160, 255);
   private static final Color C_LABEL_ON = new Color(255, 255, 255, 255);
   private static final Color C_TRACK_BG = new Color(14, 14, 18, 255);
   private static final Color C_BOX_BG = new Color(10, 10, 14, 255);
   private static final Color C_SWITCH_OFF = new Color(20, 20, 26, 255);
   private static final Color C_POPUP_BG = new Color(6, 6, 8, 250);
   private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;
   private final Map<ColorSetting, ColorPickerComponent> colorPickers = new HashMap<>();
   private int draggingSlider = -1;
   private NumberSetting draggingSetting = null;
   private boolean isListening = false;
   private KeybindSetting listeningSetting = null;
   private final Map<BooleanSetting, Float> boolAnimMap = new HashMap<>();
   private final Map<NumberSetting, Float> sliderRatioAnimMap = new HashMap<>();
   private final Map<NumberSetting, Float> sliderHoverAnimMap = new HashMap<>();
   private final Map<ActionSetting, Float> actionHoverMap = new HashMap<>();

   public UniversalSettingsPanel(Module module) {
      this.module = module;
   }

   public void updateRows() {
      this.rows.clear();

      for (Setting baseSetting : this.module.getSettings()) {
         if (baseSetting != null && baseSetting.isVisible() && xyz.angames.astolfoclient.client.config.VisualColors.settingVisible(module, baseSetting)) {
            this.rows.add(baseSetting);
         }
      }
   }

   public boolean hasSettings() {
      for (Object row : this.rows) {
         if (row instanceof Setting) {
            return true;
         }
      }

      return false;
   }

   public float getTotalHeight() {
      if (this.rows.isEmpty()) {
         return 0.0F;
      }

      float h = GuiSkin.modern() ? 22.0F : 4.0F;

      for (Object r : this.rows) {
         h += this.rowH(r);
      }

      return h + (GuiSkin.modern() ? 8.0F : 4.0F);
   }

   public void render(GuiGraphics context, float x, float y, float width, float alpha, int mouseX, int mouseY, float deltaTime, Color themeColor) {
      if (GuiSkin.modern()) {
         renderModern(context, x, y, width, alpha, mouseX, mouseY, deltaTime, themeColor);
         return;
      }
      if (!(alpha <= 0.05F) && !this.rows.isEmpty()) {
         Matrix4f mx = context.pose().last().pose();

         for (Object row : this.rows) {
            if (row instanceof BooleanSetting bs) {
               float cur = this.boolAnimMap.getOrDefault(bs, bs.get() ? 1.0F : 0.0F);
               this.boolAnimMap.put(bs, GuiUtils.animate(cur, bs.get() ? 1.0F : 0.0F, 18.0F, deltaTime));
            }
         }

         Builder.rectangle()
            .size(new SizeState(width - 20.0F, 1.0F))
            .radius(new QuadRadiusState(0.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(24, 24, 32), alpha * 0.8F)))
            .build()
            .render(mx, x + 10.0F, y + 1.0F);
         float rowY = y + 5.0F;

         for (int i = 0; i < this.rows.size(); i++) {
            Object row = this.rows.get(i);
            if (row instanceof ColorSetting cs) {
               ColorPickerComponent picker = picker(cs);
               if (!picker.isDragging()) picker.loadColor();
               picker.x = x + 4; picker.y = rowY + 14; picker.width = width - 8; picker.height = 102;
               picker.animate(deltaTime);
               this.renderMediumText(mx, cs.getName(), x + 10, rowY + 2, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
               String hex = String.format("#%06X", cs.getInt());
               this.renderMediumText(mx, hex, x + width - 60, rowY + 2, GuiUtils.withAlpha(cs.color(), alpha), 8.5F);
               picker.render(mx, mouseX, mouseY, alpha);
            } else if (row instanceof NumberSetting ns) {
               this.renderSlider(mx, rowY, ns, themeColor, x, width, alpha, mouseX, mouseY, deltaTime, i == this.draggingSlider);
            } else if (row instanceof BooleanSetting bs) {
               this.renderBool(mx, rowY, bs, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
            } else if (row instanceof ModeSetting ms) {
               this.renderMode(mx, rowY, ms, ms.getName(), ms.get(), ms.getModes(), themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
            } else if (!(row instanceof EnumSetting<?> es)) {
               if (row instanceof KeybindSetting ks) {
                  this.renderKeybind(mx, rowY, ks, themeColor, x, width, alpha, mouseX, mouseY);
               } else if (row instanceof ActionSetting as) {
                  this.renderAction(mx, rowY, as, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
               } else if (row instanceof ConfigureSetting cs) {
                  this.renderConfigure(mx, rowY, cs, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
               } else if (row instanceof MultiSelectSetting mss) {
                  this.renderMultiSelect(mx, rowY, mss, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
               }
            } else {
               List<String> enumModes = new ArrayList<>();

               for (Enum<?> e : es.getValues()) {
                  enumModes.add(e.name());
               }

               this.renderMode(mx, rowY, es, es.getName(), es.getValue().name(), enumModes, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
            }

            rowY += this.rowH(row);
         }
      }
   }

   private void renderModern(GuiGraphics context, float x, float y, float width, float alpha,
                             int mouseX, int mouseY, float deltaTime, Color accent) {
      if (alpha <= .05f || rows.isEmpty()) return;
      Matrix4f matrix = context.pose().last().pose();
      GuiSkin.settings(matrix, x + 6, y + 2, width - 12, getTotalHeight() - 8, alpha, accent);
      modernText(matrix, "SETTINGS", x + 16, y + 9, new Color(130, 150, 180), 6.2f, alpha);
      String count = rows.size() + " OPTIONS";
      modernText(matrix, count, x + width - 16 - textWidth(count, 6.2f), y + 9,
         new Color(103, 125, 156), 6.2f, alpha);
      float rowY = y + 22;
      for (Object row : rows) {
         float rowHeight = rowH(row);
         float sx = x + 16, sw = width - 32;
         boolean hover = GuiUtils.isMouseOver(mouseX, mouseY, sx - 4, rowY, sw + 8, rowHeight);
         if (hover && !(row instanceof ColorSetting))
            modernRect(matrix, sx - 4, rowY, sw + 8, rowHeight - 2, 5,
               GuiUtils.withAlpha(Color.WHITE, alpha * .032f));
         if (row instanceof NumberSetting number) {
            renderModernSlider(matrix, rowY, number, accent, x, width, alpha, mouseX, mouseY, deltaTime);
         } else if (row instanceof BooleanSetting bool) {
            float animation = GuiUtils.animate(boolAnimMap.getOrDefault(bool, bool.get() ? 1f : 0f),
               bool.get() ? 1f : 0f, 18, deltaTime);
            boolAnimMap.put(bool, animation);
            Color label = GuiUtils.interpolateColor(new Color(156, 174, 199), new Color(231, 240, 252), animation);
            modernText(matrix, fitModern(bool.getName(), 8, sw - 38), sx, rowY + (rowHeight - 8) / 2,
               label, 8, alpha);
            float tx = sx + sw - 24, ty = rowY + (rowHeight - 12) / 2;
            modernRect(matrix, tx, ty, 24, 12, 6,
               GuiUtils.withAlpha(GuiUtils.interpolateColor(new Color(37, 48, 66), accent, animation), alpha));
            modernRect(matrix, tx + 2 + animation * 12, ty + 2, 8, 8, 4,
               GuiUtils.withAlpha(new Color(239, 246, 255), alpha));
         } else if (row instanceof ColorSetting color) {
            modernText(matrix, fitModern(color.getName(), 8, sw - 65), sx, rowY + 3,
               new Color(163, 182, 206), 8, alpha);
            String hex = String.format(java.util.Locale.ROOT, "#%06X", color.getInt());
            modernText(matrix, hex, sx + sw - textWidth(hex, 7.5f), rowY + 3,
               color.color(), 7.5f, alpha);
            ColorPickerComponent picker = picker(color);
            if (!picker.isDragging()) picker.loadColor();
            positionModernPicker(picker, x, rowY, width);
            picker.animate(deltaTime);
            picker.render(matrix, mouseX, mouseY, alpha);
         } else if (row instanceof KeybindSetting keybind) {
            boolean listening = isListening && listeningSetting == keybind;
            String key = listening ? "PRESS A KEY" : keybind.getKey() == 0 ? "UNBOUND" : keybind.getKeyName();
            key = fitModern(key, 7.5f, Math.min(85, sw * .5f));
            float pillWidth = Math.max(48, textWidth(key, 7.5f) + 16);
            float pillX = sx + sw - pillWidth, pillY = rowY + (rowHeight - 19) / 2;
            modernText(matrix, fitModern(keybind.getName(), 8, sw - pillWidth - 8), sx,
               rowY + (rowHeight - 8) / 2, new Color(160, 179, 204), 8, alpha);
            modernControl(matrix, pillX, pillY, pillWidth, 19, accent, alpha, hover, listening);
            modernText(matrix, key, pillX + (pillWidth - textWidth(key, 7.5f)) / 2, pillY + 6,
               listening ? new Color(255, 211, 139) : new Color(222, 234, 249), 7.5f, alpha);
         } else if (row instanceof ActionSetting action) {
            float animation = GuiUtils.animate(actionHoverMap.getOrDefault(action, 0f), hover ? 1 : 0, 15, deltaTime);
            actionHoverMap.put(action, animation);
            float bh = rowHeight - 6;
            modernControl(matrix, sx, rowY + 2, sw, bh, accent, alpha, hover, false);
            modernRect(matrix, sx, rowY + 2, sw, bh, 5,
               GuiUtils.withAlpha(accent, alpha * (.045f + animation * .075f)));
            String label = fitModern(action.getName(), 8, sw - 24);
            modernText(matrix, label, sx + 9, rowY + 2 + (bh - 8) / 2,
               hover ? accent : new Color(216, 231, 251), 8, alpha);
            modernText(matrix, ">", sx + sw - 12, rowY + 2 + (bh - 8) / 2,
               hover ? accent : new Color(134, 155, 183), 8, alpha);
         } else if (row instanceof ModeSetting mode) {
            renderModernSelector(matrix, mode, mode.getName(), mode.get(), mode.getModes().size() + " choices",
               ModePopupState.isSettingOpen(mode), x, rowY, width, alpha, mouseX, mouseY, accent);
         } else if (row instanceof EnumSetting<?> enumeration) {
            renderModernSelector(matrix, enumeration, enumeration.getName(), enumeration.getValue().name(),
               enumeration.getValues().length + " choices", ModePopupState.isSettingOpen(enumeration),
               x, rowY, width, alpha, mouseX, mouseY, accent);
         } else if (row instanceof ConfigureSetting configure) {
            renderModernSelector(matrix, configure, configure.getName(), configure.getButtonText(), "DETAILS",
               SubSettingsPopupState.isSettingOpen(configure), x, rowY, width, alpha, mouseX, mouseY, accent);
         } else if (row instanceof MultiSelectSetting multiple) {
            renderModernSelector(matrix, multiple, multiple.getName(), multiple.getButtonText(), "MULTI SELECT",
               MultiSelectPopupState.isSettingOpen(multiple), x, rowY, width, alpha, mouseX, mouseY, accent);
         }
         rowY += rowHeight;
      }
   }

   private void renderModernSlider(Matrix4f matrix, float y, NumberSetting number, Color accent,
                                   float x, float width, float alpha, int mouseX, int mouseY, float deltaTime) {
      float sx = x + 16, sw = width - 32, rh = rowH(number);
      double range = number.getMax() - number.getMin();
      float target = range <= 0 ? 0 : Mth.clamp((float)((number.get() - number.getMin()) / range), 0, 1);
      float value = GuiUtils.animate(sliderRatioAnimMap.getOrDefault(number, target), target, 22, deltaTime);
      sliderRatioAnimMap.put(number, value);
      boolean hover = draggingSetting == number || GuiUtils.isMouseOver(mouseX, mouseY, sx - 4, y + 14, sw + 8, rh - 14);
      float hoverAnimation = GuiUtils.animate(sliderHoverAnimMap.getOrDefault(number, 0f), hover ? 1 : 0, 16, deltaTime);
      sliderHoverAnimMap.put(number, hoverAnimation);
      String numberText = java.math.BigDecimal.valueOf(number.get()).setScale(3, java.math.RoundingMode.HALF_UP)
         .stripTrailingZeros().toPlainString();
      float valueWidth = Math.max(28, textWidth(numberText, 7.5f) + 10);
      modernText(matrix, fitModern(number.getName(), 8, sw - valueWidth - 8), sx, y + 3,
         new Color(169, 187, 211), 8, alpha);
      modernRect(matrix, sx + sw - valueWidth, y, valueWidth, 14, 4,
         GuiUtils.withAlpha(accent, alpha * (.09f + hoverAnimation * .055f)));
      modernText(matrix, numberText, sx + sw - valueWidth + (valueWidth - textWidth(numberText, 7.5f)) / 2,
         y + 3.5f, new Color(229, 241, 255), 7.5f, alpha);
      float trackY = y + rh - 11;
      modernRect(matrix, sx, trackY, sw, 3, 1.5f, GuiUtils.withAlpha(new Color(45, 60, 81), alpha));
      if (value > .001f) modernRect(matrix, sx, trackY, Math.max(1, sw * value), 3, 1.5f, GuiUtils.withAlpha(accent, alpha));
      float knobX = sx + sw * value;
      float knob = 6 + hoverAnimation * 2;
      if (hoverAnimation > .02f) modernRect(matrix, knobX - 6, trackY - 4.5f, 12, 12, 6,
         GuiUtils.withAlpha(accent, alpha * hoverAnimation * .16f));
      modernRect(matrix, knobX - knob / 2, trackY + 1.5f - knob / 2, knob, knob, knob / 2,
         GuiUtils.withAlpha(new Color(238, 247, 255), alpha));
   }

   private void renderModernSelector(Matrix4f matrix, Setting setting, String label, String value, String hint,
                                     boolean open, float x, float y, float width, float alpha,
                                     int mouseX, int mouseY, Color accent) {
      float sx = x + 16, sw = width - 32, boxY = y + 14, boxHeight = rowH(setting) - 18;
      float hintWidth = textWidth(hint, 5.7f);
      modernText(matrix, fitModern(label, 7.5f, sw - hintWidth - 10), sx, y + 1,
         new Color(153, 174, 202), 7.5f, alpha);
      modernText(matrix, hint, sx + sw - hintWidth, y + 2, new Color(99, 122, 153), 5.7f, alpha);
      boolean hover = GuiUtils.isMouseOver(mouseX, mouseY, sx, boxY, sw, boxHeight);
      modernControl(matrix, sx, boxY, sw, boxHeight, accent, alpha, hover, open);
      modernText(matrix, fitModern(value, 8, sw - 28), sx + 8, boxY + (boxHeight - 8) / 2,
         open || hover ? accent : new Color(222, 233, 248), 8, alpha);
      modernText(matrix, open ? "-" : "+", sx + sw - 14, boxY + (boxHeight - 9) / 2,
         open || hover ? accent : new Color(128, 149, 180), 9, alpha);
   }

   private static void modernControl(Matrix4f matrix, float x, float y, float width, float height,
                                     Color accent, float alpha, boolean hover, boolean active) {
      modernRect(matrix, x, y, width, height, 5,
         GuiUtils.withAlpha(active ? new Color(35, 51, 74) : hover ? new Color(30, 42, 61) : new Color(21, 30, 46), alpha * .9f));
      Builder.border().size(new SizeState(width, height)).radius(new QuadRadiusState(5)).thickness(.6f)
         .color(new QuadColorState(GuiUtils.withAlpha(active || hover ? accent : new Color(159, 183, 215),
            alpha * (active ? .52f : hover ? .29f : .12f)))).build().render(matrix, x, y);
   }

   private boolean mouseClickedModern(double mx, double my, int button, float x, float y, float width) {
      float rowY = y + 22, sx = x + 16, sw = width - 32;
      for (int index = 0; index < rows.size(); index++) {
         Object row = rows.get(index);
         float rh = rowH(row);
         if (row instanceof ColorSetting color) {
            ColorPickerComponent picker = picker(color);
            positionModernPicker(picker, x, rowY, width);
            if (picker.mouseClicked(mx, my, button)) return true;
         } else if (row instanceof KeybindSetting keybind && GuiUtils.isMouseOver((float)mx, (float)my, sx, rowY, sw, rh)) {
            if (button == 1 || button == 2) keybind.setKey(0);
            else if (button == 0) { isListening = true; listeningSetting = keybind; }
            return true;
         } else if (button == 0) {
            if (row instanceof NumberSetting number && GuiUtils.isMouseOver((float)mx, (float)my, sx - 4, rowY + 14, sw + 8, rh - 14)) {
               draggingSlider = index;
               draggingSetting = number;
               applySlider(mx, x, width, number);
               return true;
            } else if (row instanceof BooleanSetting bool && GuiUtils.isMouseOver((float)mx, (float)my, sx - 4, rowY, sw + 8, rh)) {
               bool.toggle();
               return true;
            } else if (row instanceof ActionSetting action && GuiUtils.isMouseOver((float)mx, (float)my, sx, rowY + 2, sw, rh - 6)) {
               action.run();
               return true;
            } else if (GuiUtils.isMouseOver((float)mx, (float)my, sx, rowY + 14, sw, rh - 18)) {
               if (row instanceof ModeSetting mode) {
                  ModSounds.playModeOpen();
                  ModePopupState.open(mode, mode.getName(), mode.getModes(), sx, rowY + 14, sw, rh - 18);
                  return true;
               } else if (row instanceof EnumSetting<?> enumeration) {
                  List<String> choices = new ArrayList<>();
                  for (Enum<?> value : enumeration.getValues()) choices.add(value.name());
                  ModSounds.playModeOpen();
                  ModePopupState.open(enumeration, enumeration.getName(), choices, sx, rowY + 14, sw, rh - 18);
                  return true;
               } else if (row instanceof ConfigureSetting configure) {
                  ModSounds.playModeOpen();
                  SubSettingsPopupState.open(configure, configure.getName(), configure.getSubSettings(), sx, rowY + 14, sw, rh - 18);
                  return true;
               } else if (row instanceof MultiSelectSetting multiple) {
                  ModSounds.playModeOpen();
                  MultiSelectPopupState.open(multiple, multiple.getName(), multiple.getOptions(), sx, rowY + 14, sw, rh - 18);
                  return true;
               }
            }
         }
         rowY += rh;
      }
      return false;
   }

   private static void positionModernPicker(ColorPickerComponent picker, float x, float y, float width) {
      picker.x = x + 10; picker.y = y + 18; picker.width = width - 20; picker.height = 105;
   }

   private static float modernRowScale() {
      var appearance = xyz.angames.astolfoclient.client.module.modules.render.GuiAppearanceModule.INSTANCE;
      return Mth.clamp(GuiSkin.header() / (appearance.descriptions.get() ? 52f : 38f), .85f, 1.2f);
   }

   private static void modernRect(Matrix4f matrix, float x, float y, float width, float height, float radius, Color color) {
      if (width <= 0 || height <= 0 || color.getAlpha() <= 0) return;
      Builder.rectangle().size(new SizeState(width, height)).radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color)).build().render(matrix, x, y);
   }

   private void modernText(Matrix4f matrix, String text, float x, float y, Color color, float size, float alpha) {
      renderMediumText(matrix, text, x, y, GuiUtils.withAlpha(color, alpha), size);
   }

   private static float textWidth(String text, float size) {
      return text == null ? 0 : MEDIUM_FONT.get().getWidth(text, size);
   }

   private static String fitModern(String value, float size, float maxWidth) {
      if (value == null || value.isEmpty() || maxWidth <= 0) return "";
      if (textWidth(value, size) <= maxWidth) return value;
      while (!value.isEmpty() && textWidth(value + "...", size) > maxWidth)
         value = value.substring(0, value.length() - 1);
      return value.isEmpty() ? "" : value + "...";
   }

   private void renderSlider(
      Matrix4f mx, float rowY, NumberSetting ns, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime, boolean isDragging
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float val = (float)ns.get();
      float targetRatio = Mth.clamp((val - (float)ns.getMin()) / ((float)ns.getMax() - (float)ns.getMin()), 0.0F, 1.0F);
      float animRatio = this.sliderRatioAnimMap.getOrDefault(ns, targetRatio);
      animRatio = GuiUtils.animate(animRatio, targetRatio, 20.0F, deltaTime);
      this.sliderRatioAnimMap.put(ns, animRatio);
      boolean isHov = isDragging || GuiUtils.isMouseOver(mouseX, mouseY, sx, rowY, sw, 23.0F);
      float hovAnim = this.sliderHoverAnimMap.getOrDefault(ns, 0.0F);
      hovAnim = GuiUtils.animate(hovAnim, isHov ? 1.0F : 0.0F, 14.0F, deltaTime);
      this.sliderHoverAnimMap.put(ns, hovAnim);
      String valStr = String.format(ns.getIncrement() > 0.0 && ns.getIncrement() < 1.0 ? "%.2f" : "%.1f", val);
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      float textY = rowY + 1.5F;
      Color labelCol = GuiUtils.interpolateColor(C_LABEL_OFF, Color.WHITE, hovAnim * 0.4F);
      this.renderMediumText(mx, ns.getName(), sx, textY, GuiUtils.withAlpha(labelCol, alpha), 8.5F);
      float valW = medFont.getWidth(valStr, 8.5F);
      this.renderMediumText(mx, valStr, sx + sw - valW, textY, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
      float trackY = rowY + 13.5F;
      float trackH = 3.0F;
      Builder.rectangle()
         .size(new SizeState(sw, trackH))
         .radius(new QuadRadiusState(1.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(C_TRACK_BG, alpha)))
         .build()
         .render(mx, sx, trackY);
      if (animRatio > 0.001F) {
         Builder.rectangle()
            .size(new SizeState(sw * animRatio, trackH))
            .radius(new QuadRadiusState(1.5F))
            .color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha)))
            .build()
            .render(mx, sx, trackY);
      }

      float knobX = sx + sw * animRatio;
      float knobY = trackY + trackH / 2.0F;
      float knobSize = 6.5F + 1.5F * hovAnim;
      Builder.rectangle()
         .size(new SizeState(knobSize, knobSize))
         .radius(new QuadRadiusState(knobSize / 2.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
         .build()
         .render(mx, knobX - knobSize / 2.0F, knobY - knobSize / 2.0F);
   }

   private void renderBool(Matrix4f mx, float rowY, BooleanSetting bs, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      boolean on = bs.get();
      float anim = this.boolAnimMap.getOrDefault(bs, on ? 1.0F : 0.0F);
      anim = GuiUtils.animate(anim, on ? 1.0F : 0.0F, 18.0F, deltaTime);
      this.boolAnimMap.put(bs, anim);
      Color labelColor = GuiUtils.interpolateColor(C_LABEL_OFF, C_LABEL_ON, anim);
      this.renderMediumText(mx, bs.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(labelColor, alpha), 8.5F);
      float switchW = 18.0F;
      float switchH = 9.0F;
      float switchX = sx + sw - switchW;
      float switchY = rowY + 4.0F;
      Color trackColor = GuiUtils.interpolateColor(C_SWITCH_OFF, themeColor, anim);
      Builder.rectangle()
         .size(new SizeState(switchW, switchH))
         .radius(new QuadRadiusState(4.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(trackColor, alpha)))
         .build()
         .render(mx, switchX, switchY);
      float thumbSize = 7.0F;
      float thumbX = switchX + 1.0F + (switchW - thumbSize - 2.0F) * anim;
      float thumbY = switchY + 1.0F;
      Builder.rectangle()
         .size(new SizeState(thumbSize, thumbSize))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
         .build()
         .render(mx, thumbX, thumbY);
   }

   private void renderMode(
      Matrix4f mx,
      float rowY,
      Setting setting,
      String name,
      String cur,
      List<String> modes,
      Color themeColor,
      float x,
      float w,
      float alpha,
      int mouseX,
      int mouseY,
      float deltaTime
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, name, sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean isOpen = ModePopupState.isSettingOpen(setting);
      String displayStr = cur + " >";
      float strW = medFont != null ? medFont.getWidth(displayStr, 7.5F) : 30.0F;
      float pillW = Math.max(strW + 10.0F, 38.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean pillHov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = isOpen ? new Color(28, 28, 38, 255) : (pillHov ? new Color(20, 20, 26, 255) : C_BOX_BG);
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      Color textColor = isOpen ? themeColor : (pillHov ? themeColor : new Color(220, 220, 235));
      float textX = pillX + (pillW - strW) / 2.0F;
      this.renderMediumText(mx, displayStr, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 7.5F);
   }

   private void renderKeybind(Matrix4f mx, float rowY, KeybindSetting ks, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, ks.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean listening = this.isListening && this.listeningSetting == ks;
      String cur = listening ? "..." : (ks.getKey() == 0 ? "NONE" : ks.getKeyName());
      float curW = medFont.getWidth(cur, 8.0F);
      float pillW = Math.max(curW + 10.0F, 30.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = listening ? new Color(50, 25, 20, 255) : (hov ? new Color(24, 24, 32, 255) : C_BOX_BG);
      Color textColor = listening ? new Color(255, 200, 80) : Color.WHITE;
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      float textX = pillX + (pillW - curW) / 2.0F;
      this.renderMediumText(mx, cur, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 8.0F);
   }

   private void renderAction(
      Matrix4f mx, float rowY, ActionSetting as, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime
   ) {
      float bx = x + 10.0F;
      float bw = w - 20.0F;
      float bh = 18.0F;
      boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, bx, rowY + 2.0F, bw, bh);
      float hovA = this.actionHoverMap.getOrDefault(as, 0.0F);
      hovA = GuiUtils.animate(hovA, hov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.actionHoverMap.put(as, hovA);
      Color btnBg = GuiUtils.interpolateColor(C_BOX_BG, new Color(26, 26, 36), hovA);
      Builder.rectangle()
         .size(new SizeState(bw, bh))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(btnBg, alpha)))
         .build()
         .render(mx, bx, rowY + 2.0F);
      String label = as.getName();
      float tw = ((MsdfFont)MEDIUM_FONT.get()).getWidth(label, 8.5F);
      float tx = bx + (bw - tw) / 2.0F;
      this.renderMediumText(mx, label, tx, rowY + 5.5F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
   }

   private void renderConfigure(
      Matrix4f mx, float rowY, ConfigureSetting cs, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, cs.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean isOpen = SubSettingsPopupState.isSettingOpen(cs);
      String displayStr = cs.getButtonText() + " >";
      float strW = medFont != null ? medFont.getWidth(displayStr, 7.5F) : 38.0F;
      float pillW = Math.max(strW + 10.0F, 44.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean pillHov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = isOpen ? new Color(28, 28, 38, 255) : (pillHov ? new Color(20, 20, 26, 255) : C_BOX_BG);
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      Color textColor = isOpen ? themeColor : (pillHov ? themeColor : new Color(220, 220, 235));
      float textX = pillX + (pillW - strW) / 2.0F;
      this.renderMediumText(mx, displayStr, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 7.5F);
   }

   private void renderMultiSelect(
      Matrix4f mx, float rowY, MultiSelectSetting mss, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, mss.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean isOpen = MultiSelectPopupState.isSettingOpen(mss);
      String displayStr = mss.getButtonText() + " >";
      float strW = medFont != null ? medFont.getWidth(displayStr, 7.5F) : 38.0F;
      float pillW = Math.max(strW + 10.0F, 42.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean pillHov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = isOpen ? new Color(28, 28, 38, 255) : (pillHov ? new Color(20, 20, 26, 255) : C_BOX_BG);
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      Color textColor = isOpen ? themeColor : (pillHov ? themeColor : new Color(220, 220, 235));
      float textX = pillX + (pillW - strW) / 2.0F;
      this.renderMediumText(mx, displayStr, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 7.5F);
   }

   private void renderMediumText(Matrix4f matrix, String text, float tx, float ty, Color color, float size) {
      if (text != null && !text.isEmpty() && color.getAlpha() > 4) {
         try {
            Builder.text().font((MsdfFont)MEDIUM_FONT.get()).text(text).color(color).size(size).build().render(matrix, tx, ty);
         } catch (Exception var8) {
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int button, float startX, float startY, float w) {
      if (this.isListening) {
         if (this.listeningSetting != null) {
            this.listeningSetting.setKey(-(button + 100));
         }

         this.isListening = false;
         this.listeningSetting = null;
         return true;
      } else {
         if (GuiSkin.modern()) return mouseClickedModern(mx, my, button, startX, startY, w);
         float rowY = startY + 5.0F;

         for (int i = 0; i < this.rows.size(); i++) {
            Object row = this.rows.get(i);
            float rh = this.rowH(row);
            if (row instanceof ColorSetting cs) {
               ColorPickerComponent picker = picker(cs);
               picker.x = startX + 4; picker.y = rowY + 14; picker.width = w - 8; picker.height = 102;
               if (picker.mouseClicked(mx, my, button)) return true;
            } else if (row instanceof NumberSetting ns) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY, w - 20.0F, 23.0F)) {
                  this.draggingSlider = i;
                  this.draggingSetting = ns;
                  this.applySlider(mx, startX, w, ns);
                  return true;
               }
            } else if (row instanceof BooleanSetting bs) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY, w - 20.0F, 18.0F)) {
                  bs.toggle();
                  return true;
               }
            } else if (row instanceof ModeSetting ms) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = ms.get() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 30.0F;
               float pillW = Math.max(strW + 10.0F, 38.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  ModSounds.playModeOpen();
                  ModePopupState.open(ms, ms.getName(), ms.getModes(), pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            } else if (row instanceof EnumSetting<?> es) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = es.getValue().name() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 30.0F;
               float pillW = Math.max(strW + 10.0F, 38.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  List<String> enumModes = new ArrayList<>();

                  for (Enum<?> e : es.getValues()) {
                     enumModes.add(e.name());
                  }

                  ModSounds.playModeOpen();
                  ModePopupState.open(es, es.getName(), enumModes, pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            } else if (row instanceof KeybindSetting ks) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY, w - 20.0F, 20.0F)) {
                  if (button != 1 && button != 2) {
                     this.isListening = true;
                     this.listeningSetting = ks;
                  } else {
                     ks.setKey(0);
                  }

                  return true;
               }
            } else if (row instanceof ActionSetting as) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY + 2.0F, w - 20.0F, 18.0F)) {
                  as.run();
                  return true;
               }
            } else if (row instanceof ConfigureSetting cs) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = cs.getButtonText() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 38.0F;
               float pillW = Math.max(strW + 10.0F, 44.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  ModSounds.playModeOpen();
                  SubSettingsPopupState.open(cs, cs.getName(), cs.getSubSettings(), pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            } else if (row instanceof MultiSelectSetting mss) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = mss.getButtonText() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 38.0F;
               float pillW = Math.max(strW + 10.0F, 42.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  ModSounds.playModeOpen();
                  MultiSelectPopupState.open(mss, mss.getName(), mss.getOptions(), pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            }

            rowY += rh;
         }

         return false;
      }
   }

   public boolean mouseDragged(double mx, double my, float startX, float w) {
      for (var picker : colorPickers.values()) if (picker.isDragging()) { picker.update(mx, my); return true; }
      if (this.draggingSetting != null && this.rows.contains(this.draggingSetting)) {
         this.applySlider(mx, startX, w, this.draggingSetting);
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased() {
      this.draggingSlider = -1;
      this.draggingSetting = null;
      colorPickers.values().forEach(ColorPickerComponent::mouseReleased);
   }

   public void keyPressed(int key) {
      if (this.isListening && this.listeningSetting != null) {
         this.listeningSetting.setKey(key != 256 && key != 261 && key != 259 ? key : 0);
         this.isListening = false;
         this.listeningSetting = null;
      }
   }

   private ColorPickerComponent picker(ColorSetting setting) {
      return colorPickers.computeIfAbsent(setting, key -> new ColorPickerComponent(key.getName(), key::color, key::setColor));
   }

   public void cancelInteraction() {
      mouseReleased();
      isListening = false;
      listeningSetting = null;
      for (Object row : rows) {
         if (row instanceof Setting setting) {
            if (ModePopupState.isSettingOpen(setting)) ModePopupState.close();
            if (SubSettingsPopupState.isSettingOpen(setting)) SubSettingsPopupState.close();
            if (MultiSelectPopupState.isSettingOpen(setting)) MultiSelectPopupState.close();
         }
      }
   }

   public boolean isListening() {
      return this.isListening;
   }

   public int settingCount() {
      return this.rows.size();
   }

   private float rowH(Object row) {
      if (GuiSkin.modern()) {
         if (row instanceof ColorSetting) return 132;
         float size = row instanceof NumberSetting ? 36 : row instanceof BooleanSetting ? 28
            : row instanceof KeybindSetting || row instanceof ActionSetting ? 30 : 42;
         return size * modernRowScale();
      }
      if (row instanceof ColorSetting) {
         return 120.0F;
      } else if (row instanceof NumberSetting) {
         return 23.0F;
      } else if (row instanceof BooleanSetting) {
         return 18.0F;
      } else if (row instanceof KeybindSetting) {
         return 20.0F;
      } else if (row instanceof ActionSetting) {
         return 22.0F;
      } else if (row instanceof ConfigureSetting) {
         return 22.0F;
      } else if (row instanceof MultiSelectSetting) {
         return 22.0F;
      } else {
         return !(row instanceof ModeSetting) && !(row instanceof EnumSetting) ? 22.0F : 22.0F;
      }
   }

   private void applySlider(double mx, float startX, float w, NumberSetting ns) {
      float inset = GuiSkin.modern() ? 16.0F : 10.0F;
      float sw = Math.max(1, w - inset * 2);
      float ratio = (float)Mth.clamp((mx - (startX + inset)) / sw, 0.0, 1.0);
      double range = ns.getMax() - ns.getMin();
      double rawVal = ns.getMin() + ratio * range;
      double inc = ns.getIncrement();
      if (inc > 0.0) {
         rawVal = Math.round(rawVal / inc) * inc;
      }

      double oldVal = ns.get();
      double newVal = Mth.clamp(rawVal, ns.getMin(), ns.getMax());
      if (Double.compare(oldVal, newVal) != 0) {
         ns.set(newVal);
         ModSounds.playSliderMove();
      }
   }
}
