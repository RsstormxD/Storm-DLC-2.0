package xyz.angames.astolfoclient.client.mixin;

import dev.stormdlc.menu.MainMenuRenderer;
import dev.stormdlc.menu.MenuAppearanceScreen;
import dev.stormdlc.menu.MenuWallpaper;
import dev.stormdlc.menu.MenuLayout;
import dev.stormdlc.menu.GlassButtonRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule;
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
        if (MainMenuModule.INSTANCE == null || !MainMenuModule.INSTANCE.isEnabled()) return;
        MenuLayout.arrange(children(), width, height);
        for (int index = 0; index < 3; index++) {
            final int theme = index;
            var button = addRenderableWidget(Button.builder(Component.literal("Theme " + (index + 1)), pressed -> {
                MainMenuModule.INSTANCE.wallpaper.setValue(MainMenuModule.Wallpaper.values()[theme]);
                MenuWallpaper.refresh();
                MainMenuRenderer.save();
            }).bounds(width / 2 - 100 + index * 68, height - 33, 64, 19).build());
            GlassButtonRenderer.theme(button, index + 1);
        }
        var appearance = addRenderableWidget(Button.builder(Component.literal("Appearance"), button ->
            minecraft.setScreen(new MenuAppearanceScreen((Screen) (Object) this)))
            .bounds(10, 10, width < 440 ? 22 : 76, 21).build());
        appearance.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Menu appearance")));
        if (width < 440) GlassButtonRenderer.icon(appearance, GlassButtonRenderer.Icon.APPEARANCE);
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
        if (MainMenuRenderer.enabled()) MainMenuRenderer.clock(graphics, width, height, alpha);
        else logo.renderLogo(graphics, width, alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private void stormSplash(SplashRenderer splash, GuiGraphics graphics, int width, net.minecraft.client.gui.Font font, int color) {
        if (!MainMenuRenderer.enabled()) splash.render(graphics, width, font, color);
    }

}
