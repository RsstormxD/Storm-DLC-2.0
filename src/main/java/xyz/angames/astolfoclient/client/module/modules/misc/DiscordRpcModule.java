package xyz.angames.astolfoclient.client.module.modules.misc;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.DiscordRpcManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

public final class DiscordRpcModule extends Module {
    public static DiscordRpcModule INSTANCE;
    public final ModeSetting details = new ModeSetting("Details", "Activity", "Activity", "Modules", "Minimal");
    public final BooleanSetting elapsed = new BooleanSetting("Show elapsed time", true);
    public final BooleanSetting logo = new BooleanSetting("Show Storm logo", true);
    public final BooleanSetting spinningLogo = new BooleanSetting("Spinning logo", true);
    public final BooleanSetting server = new BooleanSetting("Show server address", false);
    public final BooleanSetting dimension = new BooleanSetting("Show dimension", true);
    public final ActionSetting reconnect = new ActionSetting("Reconnect to Discord",()->{var rpc=manager();if(rpc!=null && isEnabled()){rpc.stop();rpc.start();}});
    private int ticks;
    public DiscordRpcModule(){
        super("Discord RPC","Customizable Discord Rich Presence",Category.MISC);
        INSTANCE=this;
        this.addSettings(this.details, this.elapsed, this.logo, this.spinningLogo, this.server, this.dimension, this.reconnect);
        setEnabled(true);
    }
    private static DiscordRpcManager manager(){var app=AstolfoclientClient.getInstance();return app==null?null:app.getDiscordRpcManager();}
    @Override public void onDisable(){var rpc=manager();if(rpc!=null)rpc.stop();}
    @Override public String getDescription(){var rpc=manager();return "Discord: "+(rpc==null?"starting":rpc.status());}
    @Override public void onTick(){
        var rpc=manager();if(rpc==null)return;
        if(!isEnabled()){if(rpc.isRunning())rpc.stop();return;}
        if(!rpc.isRunning())rpc.start();
        if(++ticks%20!=0)return;
        var mc=Minecraft.getInstance();String state="Minecraft 1.21.4",text="Storm DLC 2.0";
        if(!details.is("Minimal")){
            if(mc.level==null)text="In the main menu";
            else if(details.is("Modules"))text=AstolfoclientClient.moduleManager.getModules().stream().filter(Module::isEnabled).count()+" modules enabled";
            else text=mc.isLocalServer()?"Exploring a singleplayer world":"Playing multiplayer";
            if(mc.level!=null && dimension.get())state += " | "+mc.level.dimension().location().getPath().replace('_',' ');
            if(server.get() && mc.getCurrentServer()!=null)text="Playing on "+mc.getCurrentServer().ip;
        }
        rpc.setPresence(text,state,elapsed.get(),logo.get(),spinningLogo.get());
    }
}
