package xyz.angames.astolfoclient.client.mixin;

import dev.stormdlc.menu.LoadingAnimation;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private float currentProgress;
    @Unique private final LoadingAnimation stormLoading = new LoadingAnimation();

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
    private int stormBlackBackground(IntSupplier supplier) { return 0xff000000; }

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIFFIIIIIII)V"))
    private void stormWordmark(GuiGraphics graphics, Function<ResourceLocation, RenderType> pipeline, ResourceLocation image,
        int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight,
        int textureWidth, int textureHeight, int color) {
        if (u < 0) stormLoading.wordmark(graphics, (color >>> 24) / 255.0F);
    }

    @Inject(method = "drawProgressBar", at = @At("HEAD"), cancellable = true)
    private void stormProgress(GuiGraphics graphics, int left, int top, int right, int bottom, float alpha, CallbackInfo ci) {
        stormLoading.progress(graphics, currentProgress, alpha);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void stormRelease(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (minecraft.getOverlay() != (Object) this) {
            graphics.flush();
            stormLoading.release();
        }
    }
}
