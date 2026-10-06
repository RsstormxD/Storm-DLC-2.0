package xyz.angames.astolfoclient.client.module.modules.render;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;
import dev.stormdlc.world.WorldPanels;
public final class Gui3DModule extends Module {
    public static Gui3DModule INSTANCE;
    public final BooleanSetting lock=new BooleanSetting("3D lock",true);
    public final NumberSetting distance=new NumberSetting("Distance",3,1,8,.1);
    public final NumberSetting size=new NumberSetting("Width (blocks)",2.8,1,6,.1);
    public final NumberSetting horizontal=new NumberSetting("Horizontal offset",0,-5,5,.1);
    public final NumberSetting vertical=new NumberSetting("Vertical offset",0,-3,3,.1);
    public final NumberSetting rotation=new NumberSetting("Panel angle",0,-60,60,1);
    public final ActionSetting interact=new ActionSetting("Interact (G)",()->WorldPanels.interact());
    public final ActionSetting reposition=new ActionSetting("Place in front",()->WorldPanels.resetGui());
    public Gui3DModule(){super("GUI 3D","Floating module menu. Press G for mouse control.",Category.RENDER);INSTANCE=this;}
    @Override public void onEnable(){WorldPanels.resetGui();}
}
