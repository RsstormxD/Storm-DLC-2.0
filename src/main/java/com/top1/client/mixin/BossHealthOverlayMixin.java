package com.top1.client.mixin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BossHealthOverlay.class) public class BossHealthOverlayMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true) private void storm$boss(GuiGraphics ctx,CallbackInfo ci){if(com.top1.client.SongIslandClient.enabled() && com.top1.client.island.IslandSettings.hideBossbar)ci.cancel();}
}
