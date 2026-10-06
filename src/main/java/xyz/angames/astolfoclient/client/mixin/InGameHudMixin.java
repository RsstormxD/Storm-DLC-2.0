package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class InGameHudMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (AstolfoclientClient.moduleManager != null
         && AstolfoclientClient.moduleManager.getModuleByName("Interface") instanceof InterfaceModule iface
         && iface.isEnabled()
         && iface.effectHud.get()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      Module crosshairModule = AstolfoclientClient.moduleManager.getModuleByName("Crosshair");
      if (crosshairModule != null && crosshairModule.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
   private void onRenderHotbar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
      if (hotbarModule != null && hotbarModule.isEnabled()) {
         ci.cancel();
      } else {
         float offset = LogoRenderer.getHotbarYOffset();
         if (offset > 0.01F) {
            context.pose().pushPose();
            context.pose().translate(0.0F, -offset, 0.0F);
         }
      }
   }

   @Inject(method = "renderItemHotbar", at = @At("RETURN"))
   private void onAfterRenderHotbar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
      if (hotbarModule == null || !hotbarModule.isEnabled()) {
         float offset = LogoRenderer.getHotbarYOffset();
         if (offset > 0.01F) {
            context.pose().popPose();
         }
      }
   }

   @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
   private void onBeforeRenderStatusBars(GuiGraphics context, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().pushPose();
         context.pose().translate(0.0F, -offset, 0.0F);
      }
   }

   @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
   private void onAfterRenderStatusBars(GuiGraphics context, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().popPose();
      }
   }

   @Inject(method = "renderExperienceBar", at = @At("HEAD"))
   private void onBeforeRenderExperienceBar(GuiGraphics context, int x, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().pushPose();
         context.pose().translate(0.0F, -offset, 0.0F);
      }
   }

   @Inject(method = "renderExperienceBar", at = @At("RETURN"))
   private void onAfterRenderExperienceBar(GuiGraphics context, int x, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().popPose();
      }
   }

   @Inject(method = "renderExperienceLevel", at = @At("HEAD"))
   private void onBeforeRenderExperienceLevel(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().pushPose();
         context.pose().translate(0.0F, -offset, 0.0F);
      }
   }

   @Inject(method = "renderExperienceLevel", at = @At("RETURN"))
   private void onAfterRenderExperienceLevel(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().popPose();
      }
   }

   @Inject(method = "renderSelectedItemName", at = @At("HEAD"))
   private void onBeforeRenderHeldItemTooltip(GuiGraphics context, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().pushPose();
         context.pose().translate(0.0F, -offset, 0.0F);
      }
   }

   @Inject(method = "renderSelectedItemName", at = @At("RETURN"))
   private void onAfterRenderHeldItemTooltip(GuiGraphics context, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      if (offset > 0.01F) {
         context.pose().popPose();
      }
   }
}
