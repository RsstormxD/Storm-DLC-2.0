package xyz.angames.astolfoclient.client.mixin;

import dev.stormdlc.menu.MainMenuRenderer;
import dev.stormdlc.menu.MenuAppearanceScreen;
import dev.stormdlc.menu.MenuWallpaper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow private float panoramaFade;

    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void stormMenuOpened(CallbackInfo ci) {
        MenuWallpaper.refresh();
        addRenderableWidget(Button.builder(Component.literal("Wallpaper"), button ->
            minecraft.setScreen(new MenuAppearanceScreen((Screen) (Object) this)))
            .bounds(10, width < 440 ? height - 34 : 10, 76, 20).build());
    }

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void stormBackground(GuiGraphics graphics, float delta, CallbackInfo ci) {
        if (MainMenuRenderer.enabled()) {
            MainMenuRenderer.background(graphics, width, height, true);
            ci.cancel();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private void stormBrand(LogoRenderer logo, GuiGraphics graphics, int width, float alpha) {
        if (MainMenuRenderer.enabled()) MainMenuRenderer.brand(graphics, width, height, alpha);
        else logo.renderLogo(graphics, width, alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private void stormSplash(SplashRenderer splash, GuiGraphics graphics, int width, net.minecraft.client.gui.Font font, int color) {
        if (!MainMenuRenderer.enabled()) splash.render(graphics, width, font, color);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void stormClock(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MainMenuRenderer.enabled()) MainMenuRenderer.clock(graphics, width, panoramaFade);
    }
}
