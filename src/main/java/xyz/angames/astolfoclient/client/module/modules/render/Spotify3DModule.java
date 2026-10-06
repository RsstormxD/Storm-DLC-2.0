package xyz.angames.astolfoclient.client.module.modules.render;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;
import dev.stormdlc.world.WorldPanels;
public final class Spotify3DModule extends Module {
    public static Spotify3DModule INSTANCE;
    public final BooleanSetting lock=new BooleanSetting("3D lock",true);
    public final NumberSetting distance=new NumberSetting("Distance",3,1,8,.1);
    public final NumberSetting size=new NumberSetting("Width (blocks)",1.8,.6,4,.1);
    public final NumberSetting horizontal=new NumberSetting("Horizontal offset",2.4,-5,5,.1);
    public final NumberSetting vertical=new NumberSetting("Vertical offset",0,-3,3,.1);
    public final NumberSetting rotation=new NumberSetting("Panel angle",0,-60,60,1);
    public final BooleanSetting lyrics=new BooleanSetting("Show lyrics",true);
    public final BooleanSetting spotifyOnly=new BooleanSetting("Prefer Spotify",true);
    public final ActionSetting interact=new ActionSetting("Interact (G)",()->WorldPanels.interact());
    public final ActionSetting reposition=new ActionSetting("Place in front",()->WorldPanels.resetSpotify());
    public Spotify3DModule(){super("Spotify 3D","Floating media player using Windows media sessions",Category.RENDER);INSTANCE=this;}
    @Override public void onEnable(){WorldPanels.resetSpotify();}
}
