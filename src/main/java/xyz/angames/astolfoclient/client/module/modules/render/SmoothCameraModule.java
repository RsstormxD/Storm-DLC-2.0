package xyz.angames.astolfoclient.client.module.modules.render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

public final class SmoothCameraModule extends Module {
    public static SmoothCameraModule INSTANCE;
    public final NumberSetting response = new NumberSetting("Smoothing (ms)", 110, 20, 400, 10);
    public final BooleanSetting thirdPerson = new BooleanSetting("Third person only", false);
    public final BooleanSetting horizontal = new BooleanSetting("Smooth horizontal", true);
    public final BooleanSetting vertical = new BooleanSetting("Smooth vertical", true);
    private double pendingX, pendingY;
    private Object player;
    public SmoothCameraModule(){super("Smooth Camera","Cinematic mouse movement with adjustable smoothing",Category.RENDER);INSTANCE=this;}
    private void reset(){pendingX=0;pendingY=0;player=null;}
    @Override public void onEnable(){reset();}
    @Override public void onDisable(){reset();}
    @Override public void onTick(){var mc=Minecraft.getInstance();if(mc.player==null || mc.screen!=null)reset();}
    public void apply(LocalPlayer target,double x,double y,double elapsed){
        var mc=Minecraft.getInstance();
        if(!isEnabled() || mc.screen!=null || (thirdPerson.get() && mc.options.getCameraType().isFirstPerson())){
            reset();target.turn(x,y);return;
        }
        if(player!=target){reset();player=target;}
        double dt=Math.max(0.00001,Math.min(0.1,elapsed));
        double blend=1-Math.exp(-dt/(response.get()/1000.0));
        if(horizontal.get()){pendingX+=x;x=pendingX*blend;pendingX-=x;}else pendingX=0;
        if(vertical.get()){pendingY+=y;y=pendingY*blend;pendingY-=y;}else pendingY=0;
        target.turn(x,y);
    }
}
