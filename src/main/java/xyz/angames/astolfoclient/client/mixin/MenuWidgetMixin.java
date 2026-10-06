package xyz.angames.astolfoclient.client.mixin;

import dev.stormdlc.menu.GlassButtonRenderer;
import dev.stormdlc.menu.MainMenuRenderer;
import dev.stormdlc.menu.MenuAppearanceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractWidget.class)
public abstract class MenuWidgetMixin {
    @Shadow protected float alpha;
    @Shadow protected abstract void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void stormGlass(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var screen = Minecraft.getInstance().screen;
        if (widget instanceof AbstractButton && !(widget instanceof PlainTextButton)
            && (screen instanceof TitleScreen || screen instanceof MenuAppearanceScreen) && MainMenuRenderer.enabled())
            GlassButtonRenderer.render(widget, graphics, mouseX, mouseY, alpha);
        else renderWidget(graphics, mouseX, mouseY, delta);
    }
}
