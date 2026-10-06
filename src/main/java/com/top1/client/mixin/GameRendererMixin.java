package com.top1.client.mixin;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class) public class GameRendererMixin {
    @Inject(method="render",at=@At("HEAD")) private void storm$begin(DeltaTracker counter,boolean tick,CallbackInfo ci){dev.stormdlc.render.LegacyRenderer.beginFrame();}
    @Inject(method="render",at=@At("TAIL")) private void storm$island(DeltaTracker counter,boolean tick,CallbackInfo ci){com.top1.client.SongIslandClient.renderIsland();}
}
