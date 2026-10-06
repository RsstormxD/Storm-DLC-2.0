package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class MouseMixin {
   @org.spongepowered.asm.mixin.injection.Redirect(method="turnPlayer", at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
   private void stormSmoothLook(net.minecraft.client.player.LocalPlayer player,double x,double y,double elapsed) {
      var module=xyz.angames.astolfoclient.client.module.modules.render.SmoothCameraModule.INSTANCE;
      if(module==null)player.turn(x,y);else module.apply(player,x,y,elapsed);
   }
   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
   private void stormmouseClicked(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
      stormTransformInput(args, false);
   }
   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(DDI)Z"))
   private void stormmouseReleased(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
      stormTransformInput(args, false);
   }
   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
   private void stormmouseScrolled(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
      stormTransformInput(args, false);
   }
   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
   private void stormmouseMoved(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
      stormTransformInput(args, false);
   }
   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"))
   private void stormmouseDragged(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
      stormTransformInput(args, true);
   }
   @org.spongepowered.asm.mixin.Unique
   private static void stormTransformInput(org.spongepowered.asm.mixin.injection.invoke.arg.Args args, boolean drag) {
      var animations = xyz.angames.astolfoclient.client.module.modules.render.UiAnimationsModule.INSTANCE;
      if (animations == null) return;
      var transform = animations.inputTransform(Minecraft.getInstance().screen);
      args.set(0, transform.mouseX((double)args.get(0)));
      args.set(1, transform.mouseY((double)args.get(1)));
      if (drag) {
         args.set(3, (double)args.get(3) / transform.scale());
         args.set(4, (double)args.get(4) / transform.scale());
      }
   }

   @Inject(method = "onPress", at = @At("HEAD"))
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (action == 1 && client.screen == null && AstolfoclientClient.moduleManager != null) {
         int mappedKey = -(button + 100);

         for (Module module : AstolfoclientClient.moduleManager.getModules()) {
            if (module.getKeyCode() == mappedKey) {
               module.toggle();
            }
         }
      }
   }
}
