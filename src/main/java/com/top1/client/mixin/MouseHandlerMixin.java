package com.top1.client.mixin;
import com.top1.client.SongIslandClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class) public class MouseHandlerMixin {
    @Inject(method="onPress",at=@At("HEAD"),cancellable=true) private void storm$button(long window,int button,int action,int modifiers,CallbackInfo ci){
        var mc=Minecraft.getInstance();if(window!=mc.getWindow().getWindow())return;
        if(action==1 && SongIslandClient.enabled() && SongIslandClient.island()!=null){
            double x=mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth(),y=mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight();
            if(SongIslandClient.island().handleClick(x,y,button))ci.cancel();
        }
    }
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true) private void storm$scroll(long window,double horizontal,double vertical,CallbackInfo ci){
        var mc=Minecraft.getInstance();if(window!=mc.getWindow().getWindow())return;
        if(SongIslandClient.enabled() && SongIslandClient.island()!=null){
            double x=mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth(),y=mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight();
            if(SongIslandClient.island().handleScroll(x,y,vertical))ci.cancel();
        }
    }
}
