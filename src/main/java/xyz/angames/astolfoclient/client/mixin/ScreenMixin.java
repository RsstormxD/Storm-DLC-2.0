package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.util.IASAccountHelper;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public abstract class ScreenMixin {
   @org.spongepowered.asm.mixin.injection.Redirect(method = "renderWithTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
   private void stormAnimateScreen(Screen screen, net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY, float delta) {
      var animations = xyz.angames.astolfoclient.client.module.modules.render.UiAnimationsModule.INSTANCE;
      if (animations == null) screen.render(context, mouseX, mouseY, delta);
      else animations.render(screen, context, mouseX, mouseY, delta);
   }

   @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
   private void stormScreenOpened(CallbackInfo ci) {
      var animations = xyz.angames.astolfoclient.client.module.modules.render.UiAnimationsModule.INSTANCE;
      if (animations != null) animations.opened((Screen)(Object)this);
   }

   @Shadow
   protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T var1);

   @Inject(method = "init", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      IASAccountHelper.onScreenInit(screen, this::addRenderableWidget);
   }
}
