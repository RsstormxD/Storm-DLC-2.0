package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(EnvType.CLIENT)
public class ModuleButton {
   public Module module;
   public float x;
   public float y;
   public float width;
   public float height;
   public boolean isVisible = false;
   public boolean isBinding = false;
   public boolean expanded = false;
   public boolean favorite = false;
   public float renderX = -9999.0F;
   public float renderY = -9999.0F;
   public float renderAlpha = 0.0F;
   public float renderScale = 0.94F;
   private float toggleAnim = 0.0F;
   private float hoverAnim = 0.0F;
   public final UniversalSettingsPanel settingsPanel;
   public static final float HEADER_HEIGHT = 24.0F;
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = ClickGuiIcons.SEMIBOLD_FONT;
   private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;
   private static final Color C_CARD_BG = new Color(6, 6, 8, 255);
   private static final Color C_CARD_BG_HOV = new Color(10, 10, 14, 255);
   private static final Color C_SWITCH_OFF = new Color(20, 20, 26, 255);

   public ModuleButton(Module module, float width) {
      this.module = module;
      this.width = width;
      this.settingsPanel = new UniversalSettingsPanel(module);
      this.toggleAnim = module.isEnabled() ? 1.0F : 0.0F;
      this.height = GuiSkin.header();
   }

   public float calculateHeight() {
      this.settingsPanel.updateRows();
      return GuiSkin.header() + (expanded ? this.settingsPanel.getTotalHeight() : 0.0F);
   }

   public void render(GuiGraphics context, ClickGuiScreen parentGui, int mouseX, int mouseY, float alpha, float deltaTime) {
      float effectiveAlpha = alpha * this.renderAlpha;
      if (!(effectiveAlpha <= 0.02F)) {
         this.settingsPanel.updateRows();
         float settingsHeight = expanded ? this.settingsPanel.getTotalHeight() : 0.0F;
         this.height = GuiSkin.header() + settingsHeight;
         boolean en = this.module.isEnabled();
         this.toggleAnim = GuiUtils.animate(this.toggleAnim, en ? 1.0F : 0.0F, 16.0F, deltaTime);
         boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, GuiSkin.header());
         this.hoverAnim = GuiUtils.animate(this.hoverAnim, hov ? 1.0F : 0.0F, 14.0F, deltaTime);
         Color themeColor = ClickGuiScreen.accentColor(parentGui.rendering3D);
         context.pose().pushPose();
         if (this.renderScale < 0.999F) {
            float cx = this.x + this.width / 2.0F;
            float cy = this.y + this.height / 2.0F;
            context.pose().translate(cx, cy, 0.0F);
            context.pose().scale(this.renderScale, this.renderScale, 1.0F);
            context.pose().translate(-cx, -cy, 0.0F);
         }

         Matrix4f mx = context.pose().last().pose();
         GuiSkin.card(mx, this.x, this.y, this.width, this.height, effectiveAlpha, themeColor, en, hoverAnim);
         if (GuiSkin.modern()) {
            renderModernHeader(mx, themeColor, effectiveAlpha, mouseX, mouseY);
         } else {
            float cardCenterY = this.y + 12;
            float nameY = cardCenterY - 3.5F;
            GuiUtils.renderTextSafely(mx, this.module.getName(), this.x + 10.0F, nameY, GuiUtils.withAlpha(Color.WHITE, effectiveAlpha), 9.5F);
            float nameW = ((MsdfFont)SEMIBOLD_FONT.get()).getWidth(this.module.getName(), 9.5F);
            String keyText = "";
            if (this.isBinding) {
               keyText = "[...]";
            } else if (this.module.getKeyCode() != -1) {
               String kn = KeyUtils.getKeyName(this.module.getKeyCode());
               if (kn != null && !kn.isEmpty() && !kn.equalsIgnoreCase("UNKNOWN")) {
                  keyText = "[" + kn + "]";
               }
            }

            if (!keyText.isEmpty()) {
               Color keyCol = this.isBinding ? new Color(255, 180, 50) : new Color(130, 130, 150);
               float keyX = this.x + 10.0F + nameW + 5.0F;
               GuiUtils.renderTextSafely(mx, keyText, keyX, nameY + 0.5F, GuiUtils.withAlpha(keyCol, effectiveAlpha), 8.0F);
            }

            if (ModuleManager.isBeta(this.module)) {
               float badgeX = this.x + 10.0F + nameW + (keyText.isEmpty() ? 0.0F : ((MsdfFont)MEDIUM_FONT.get()).getWidth(keyText, 8.0F) + 5.0F) + 5.0F;
               float betaW = 20.0F;
               float betaH = 9.0F;
               Builder.rectangle()
                  .size(new SizeState(betaW, betaH))
                  .radius(new QuadRadiusState(2.0F))
                  .color(new QuadColorState(GuiUtils.withAlpha(new Color(255, 180, 0), effectiveAlpha * 0.85F)))
                  .build()
                  .render(mx, badgeX, cardCenterY - 4.5F);
               GuiUtils.renderTextSafely(mx, "BETA", badgeX + 2.5F, cardCenterY - 3.5F, GuiUtils.withAlpha(Color.BLACK, effectiveAlpha), 6.0F);
            }

            if (this.settingsPanel.hasSettings()) {
               GuiUtils.renderTextSafely(mx, expanded ? "-" : "+", this.x + this.width - 45.0F, nameY,
                  GuiUtils.withAlpha(expanded ? themeColor : new Color(145, 145, 160), effectiveAlpha), 10.0F);
            }
            float switchW = 22.0F;
            float switchH = 11.0F;
            float switchX = this.x + this.width - switchW - 10.0F;
            float switchY = cardCenterY - 5.5F;
            Color switchTrackColor = GuiUtils.interpolateColor(C_SWITCH_OFF, themeColor, this.toggleAnim);
            Builder.rectangle()
               .size(new SizeState(switchW, switchH))
               .radius(new QuadRadiusState(5.5F))
               .color(new QuadColorState(GuiUtils.withAlpha(switchTrackColor, effectiveAlpha)))
               .build()
               .render(mx, switchX, switchY);
            float knobSize = 8.0F;
            float knobX = switchX + 1.5F + (switchW - knobSize - 3.0F) * this.toggleAnim;
            float knobY = switchY + 1.5F;
            Builder.rectangle()
               .size(new SizeState(knobSize, knobSize))
               .radius(new QuadRadiusState(4.0F))
               .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, effectiveAlpha)))
               .build()
               .render(mx, knobX, knobY);
         }
         if (settingsHeight > 0.0F) {
            this.settingsPanel.render(context, this.x, this.y + GuiSkin.header(), this.width, effectiveAlpha, mouseX, mouseY, deltaTime, themeColor);
         }

         context.pose().popPose();
      }
   }

   private void renderModernHeader(Matrix4f matrix, Color accent, float alpha, int mouseX, int mouseY) {
      float header = GuiSkin.header();
      float titleY = y + (header < 38 ? 8 : 10);
      float toggleX = x + width - 35;
      float expandX = toggleX - 19;
      float nameWidth = Math.max(20, expandX - (x + 13) - 7);
      String name = fit(module.getName(), 10, nameWidth);
      GuiUtils.renderTextSafely(matrix, name, x + 13, titleY,
         GuiUtils.withAlpha(new Color(237, 244, 255), alpha), 10);

      float trackY = y + 8.5f;
      rect(matrix, toggleX, trackY, 24, 12, 6,
         GuiUtils.withAlpha(GuiUtils.interpolateColor(new Color(46, 56, 74), accent, toggleAnim), alpha));
      rect(matrix, toggleX + 2 + 12 * toggleAnim, trackY + 2, 8, 8, 4,
         GuiUtils.withAlpha(Color.WHITE, alpha));
      if (settingsPanel.hasSettings()) {
         boolean over = GuiUtils.isMouseOver(mouseX, mouseY, expandX - 1, y + 7, 16, 15);
         rect(matrix, expandX - 1, y + 7, 16, 15, 4,
            GuiUtils.withAlpha(expanded ? accent : Color.WHITE, alpha * (expanded ? .16f : over ? .12f : .045f)));
         Color arrow = GuiUtils.withAlpha(expanded ? accent : new Color(150, 169, 194), alpha);
         rect(matrix, expandX + 3.5f, y + 14, 6, 1, .5f, arrow);
         if (!expanded) rect(matrix, expandX + 6, y + 11.5f, 1, 6, .5f, arrow);
      }

      if (xyz.angames.astolfoclient.client.module.modules.render.GuiAppearanceModule.INSTANCE.descriptions.get()) {
         float descriptionY = y + Math.min(25, header - 23);
         String description = fitMedium(module.getDescription(), 7.2f, width - 26);
         GuiUtils.renderMediumTextSafely(matrix, description, x + 13, descriptionY,
            GuiUtils.withAlpha(new Color(147, 165, 190), alpha), 7.2f);
      }

      float footerY = y + header - 15;
      boolean pinHover = GuiUtils.isMouseOver(mouseX, mouseY, x + 11, footerY, 25, 12);
      rect(matrix, x + 11, footerY, 25, 12, 4,
         GuiUtils.withAlpha(favorite ? accent : Color.WHITE, alpha * (favorite ? .23f : pinHover ? .12f : .055f)));
      GuiUtils.renderMediumTextSafely(matrix, "PIN", x + 16, footerY + 3,
         GuiUtils.withAlpha(favorite ? accent : new Color(133, 151, 176), alpha), 6.3f);
      String status = module.isEnabled() ? "ACTIVE" : "DISABLED";
      if (ModuleManager.isBeta(module)) status += " / BETA";
      String key = isBinding ? "PRESS A KEY" : "MMB BIND";
      if (!isBinding && module.getKeyCode() != -1) {
         String keyName = KeyUtils.getKeyName(module.getKeyCode());
         if (keyName != null && !keyName.isEmpty() && !keyName.equalsIgnoreCase("UNKNOWN")) key = keyName;
      }
      float remaining = Math.max(20, width - 134);
      key = fitMedium(key, 6.3f, Math.min(remaining, 72));
      float keyWidth = MEDIUM_FONT.get().getWidth(key, 6.3f) + 10;
      float keyX = x + width - 11 - keyWidth;
      status = fitMedium(status, 6.3f, keyX - (x + 43) - 5);
      GuiUtils.renderMediumTextSafely(matrix, status, x + 43, footerY + 3,
         GuiUtils.withAlpha(module.isEnabled() ? accent : new Color(112, 131, 157), alpha), 6.3f);
      rect(matrix, keyX, footerY, keyWidth, 12, 4,
         GuiUtils.withAlpha(isBinding ? new Color(237, 173, 81) : Color.WHITE, alpha * (isBinding ? .17f : .04f)));
      GuiUtils.renderMediumTextSafely(matrix, key, keyX + 5, footerY + 3,
         GuiUtils.withAlpha(isBinding ? new Color(255, 210, 124) : new Color(127, 146, 172), alpha), 6.3f);
   }

   private static void rect(Matrix4f matrix, float x, float y, float w, float h, float radius, Color color) {
      if (w <= 0 || h <= 0 || color.getAlpha() == 0) return;
      Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color)).build().render(matrix, x, y);
   }

   private static String fit(String value, float size, float maxWidth) {
      if (value == null || value.isEmpty() || maxWidth <= 0) return "";
      if (SEMIBOLD_FONT.get().getWidth(value, size) <= maxWidth) return value;
      while (!value.isEmpty() && SEMIBOLD_FONT.get().getWidth(value + "...", size) > maxWidth)
         value = value.substring(0, value.length() - 1);
      return value.isEmpty() ? "" : value + "...";
   }

   private static String fitMedium(String value, float size, float maxWidth) {
      if (value == null || value.isEmpty() || maxWidth <= 0) return "";
      if (MEDIUM_FONT.get().getWidth(value, size) <= maxWidth) return value;
      while (!value.isEmpty() && MEDIUM_FONT.get().getWidth(value + "...", size) > maxWidth)
         value = value.substring(0, value.length() - 1);
      return value.isEmpty() ? "" : value + "...";
   }

   public boolean mouseClicked(float mouseX, float mouseY, int button) {
      if (GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, GuiSkin.header())) {
         if (button == 0 && GuiSkin.modern() && GuiUtils.isMouseOver(mouseX, mouseY, this.x + 11, this.y + GuiSkin.header() - 15, 25, 12)) {
            this.favorite = !this.favorite;
            return true;
         }
         if (button == 0 && GuiSkin.modern() && this.settingsPanel.hasSettings()
               && GuiUtils.isMouseOver(mouseX, mouseY, this.x + this.width - 55, this.y + 7, 16, 15)) {
            this.expanded = !this.expanded;
            this.settingsPanel.cancelInteraction();
            return true;
         }
         if (button == 0) {
            this.module.toggle();
            return true;
         }

         if (button == 1) {
            this.settingsPanel.updateRows();
            if (this.settingsPanel.hasSettings()) {
               expanded = !expanded;
               this.settingsPanel.cancelInteraction();
            }
            return true;
         }
         if (button == 2) {
            this.isBinding = !this.isBinding;
            return true;
         }
      }

      return expanded && this.settingsPanel.hasSettings() && GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y + GuiSkin.header(), this.width, this.settingsPanel.getTotalHeight())
         ? this.settingsPanel.mouseClicked(mouseX, mouseY, button, this.x, this.y + GuiSkin.header(), this.width)
         : false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      return expanded && this.settingsPanel.hasSettings() ? this.settingsPanel.mouseDragged(mouseX, mouseY, this.x, this.width) : false;
   }

   public void mouseReleased() {
      if (this.settingsPanel.hasSettings()) {
         this.settingsPanel.mouseReleased();
      }
   }

   public void keyPressed(int key) {
      if (expanded && this.settingsPanel.hasSettings()) {
         this.settingsPanel.keyPressed(key);
      }
   }
}
