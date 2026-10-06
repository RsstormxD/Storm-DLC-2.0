package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
@Mixin(SkyRenderer.class)
public class SkyRenderingMixin {
    @ModifyArgs(method = "renderStars", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0, remap = false))
    private void stormStarTint(Args args) {
        var module = (AmbientsModule)ModuleManager.getModule(AmbientsModule.class);
        if (module == null || !module.isEnabled() || !module.coloredStars.get()) return;
        int color = module.starsRgb();
        args.set(0, (float)args.get(0) * (color >> 16 & 255)/255f);
        args.set(1, (float)args.get(1) * (color >> 8 & 255)/255f);
        args.set(2, (float)args.get(2) * (color & 255)/255f);
    }
}
