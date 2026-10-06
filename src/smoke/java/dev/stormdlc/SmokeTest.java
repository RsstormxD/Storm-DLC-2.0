package dev.stormdlc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.module.modules.render.*;
import dev.stormdlc.world.*;
import java.nio.file.Path;

/** Development-only runtime smoke test; requires Gradle -PstormDlcSmoke. */
public final class SmokeTest implements ClientModInitializer {
    private int stage,ticks,waitTicks,mediaClicks;
    private Object guiAnchor,musicAnchor;
    private final long start=System.currentTimeMillis();
    private void shot(Minecraft mc,String name)throws Exception{
        Path dir=Path.of("../smoke");java.nio.file.Files.createDirectories(dir);
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve(name+".png"));}
        System.out.println("STORM_SMOKE screenshot "+name);
    }
    private static Object field(Class<?> type,Object owner,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private void projectClick(Minecraft mc,String name,float x,float y)throws Exception{
        Object panel=field(WorldPanels.class,null,name);
        var projection=new org.joml.Matrix4f((org.joml.Matrix4f)field(panel.getClass(),panel,"inverse")).invert();
        var point=new org.joml.Vector3f(x,y,0).mulProject(projection);
        if(Math.abs(point.x)>1 || Math.abs(point.y)>1)throw new AssertionError("Panel outside viewport "+name+" "+point);
        WorldPanels.click((point.x+1)*mc.getWindow().getGuiScaledWidth()/2d,(1-point.y)*mc.getWindow().getGuiScaledHeight()/2d,0);
    }
    private void injectMedia()throws Exception{
        if(dev.redstones.mediaplayerinfo.MediaPlayerInfo.INSTANCE instanceof dev.redstones.mediaplayerinfo.worker.IsolatedWindowsMediaPlayerInfo bridge && !bridge.hasReadSessions())throw new AssertionError("Native worker did not return sessions");
        System.out.println("STORM_SMOKE native worker communication PASS");
        var tracker=com.top1.client.SongIslandClient.tracker();tracker.shutdown();
        var thread=(Thread)field(tracker.getClass(),tracker,"thread");thread.join(2000);
        var media=new dev.redstones.mediaplayerinfo.IMediaSession(){
            public String getOwner(){return "Spotify";}
            public dev.redstones.mediaplayerinfo.MediaInfo getMedia(){return new dev.redstones.mediaplayerinfo.MediaInfo("Storm DLC 2.0 test track","UI verification",new byte[0],30,180,true);}
            public void play(){}public void pause(){}public void playPause(){mediaClicks++;}public void stop(){}public void next(){}public void previous(){}public void swapCycle(){}public int getCycleType(){return 0;}
        };
        var f=tracker.getClass().getDeclaredField("session");f.setAccessible(true);f.set(tracker,media);
        f=tracker.getClass().getDeclaredField("lyrics");f.setAccessible(true);f.set(tracker,java.util.List.of(new com.top1.client.music.LyricsFetcher.LyricLine(0,"Storm DLC 2.0 - sky lyrics"),new com.top1.client.music.LyricsFetcher.LyricLine(150,"Next line")));
        f=tracker.getClass().getDeclaredField("clock");f.setAccessible(true);f.set(tracker,30f);
        f=tracker.getClass().getDeclaredField("lyricsSynced");f.setAccessible(true);f.set(tracker,true);
        f=tracker.getClass().getDeclaredField("playing");f.setAccessible(true);f.set(tracker,false);
    }
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            try{
                if(System.currentTimeMillis()-start>240000){System.err.println("STORM_SMOKE TIMEOUT stage="+stage);mc.stop();return;}
                if(mc.getOverlay()!=null)return;
                ticks++;
                if(stage==0 && ticks>60 && mc.screen!=null){
                    mc.options.framerateLimit().set(60);mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);
                    java.nio.file.Files.deleteIfExists(dev.stormdlc.config.ClientPaths.configDirectory().resolve("gui-navigation.json"));
                    mc.setScreen(new ClickGuiScreen());stage=1;ticks=0;
                }else if(stage==1 && ticks>60){
                    shot(mc,"01-normal-gui");
                    var buttons=(java.util.List<xyz.angames.astolfoclient.client.gui.clickgui.ModuleButton>)field(ClickGuiScreen.class,mc.screen,"allModuleButtons");
                    var card=buttons.stream().filter(b->b.module instanceof CrosshairModule).findFirst().orElseThrow();
                    boolean before=card.module.isEnabled();card.expanded=false;
                    if(card.calculateHeight()!=24)throw new AssertionError("Collapsed card height");
                    float sm=xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils.getScaleModifier(mc)*xyz.angames.astolfoclient.client.config.GuiScaleSettings.getScale();
                    mc.screen.mouseClicked((card.x+20)*sm,(card.y+12)*sm,1);
                    if(!card.expanded || card.module.isEnabled()!=before || card.isBinding)throw new AssertionError("RMB expansion changed toggle/bind");
                    card.module.visualColors.source.set("Custom");card.module.visualColors.custom.set(0x32baff);
                    stage=11;ticks=0;
                }else if(stage==11 && ticks>30){
                    shot(mc,"06-module-color-picker");
                    var buttons=(java.util.List<xyz.angames.astolfoclient.client.gui.clickgui.ModuleButton>)field(ClickGuiScreen.class,mc.screen,"allModuleButtons");
                    var card=buttons.stream().filter(b->b.module instanceof CrosshairModule).findFirst().orElseThrow();
                    int theme=xyz.angames.astolfoclient.client.config.ThemeManager.getThemedColor(0);
                    int old=card.module.visualColors.custom.getInt();
                    card.mouseClicked(card.x+100,card.y+90,0);card.mouseDragged(card.x+130,card.y+100,0,30,10);card.mouseReleased();
                    if(old==card.module.visualColors.custom.getInt() || theme!=xyz.angames.astolfoclient.client.config.ThemeManager.getThemedColor(0))throw new AssertionError("Custom picker did not edit independently");
                    card.mouseClicked(card.x+20,card.y+12,1);
                    if(card.expanded || card.calculateHeight()!=24)throw new AssertionError("RMB collapse");
                    card.mouseClicked(card.x+20,card.y+12,2);
                    if(!card.isBinding || card.expanded)throw new AssertionError("MMB binding");card.isBinding=false;
                    System.out.println("STORM_SMOKE expand/collapse, keybind and color picker PASS");
                    stage=2;ticks=0;CreateWorldScreen.testWorld(mc,new TitleScreen());
                }else if(stage==2 && mc.screen instanceof CreateWorldScreen screen && ticks>30){
                    screen.getUiState().setName("Storm DLC 2.0 smoke "+System.currentTimeMillis());
                    screen.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    for(var child:screen.children())if(child instanceof Button button && button.getMessage().getString().contains("Create New World")){stage=3;ticks=0;button.onPress();break;}
                }else if(stage==3 && mc.player!=null && mc.level!=null && ticks>100){
                    mc.player.setYRot(0);mc.player.setXRot(0);
                    injectMedia();
                    SongIslandModule.INSTANCE.skyText.set(true);SongIslandModule.INSTANCE.lock3D.set(true);
                    Gui3DModule.INSTANCE.horizontal.set(-1.1);Gui3DModule.INSTANCE.size.set(2);
                    Spotify3DModule.INSTANCE.horizontal.set(1.2);Spotify3DModule.INSTANCE.size.set(1.6);
                    Gui3DModule.INSTANCE.setEnabled(true);Spotify3DModule.INSTANCE.setEnabled(true);WorldPanels.reset();
                    mc.setScreen(new WorldPanelScreen());stage=4;ticks=0;
                }else if(stage==4 && ticks>100){
                    shot(mc,"02-world-panels");
                    Object gui=field(WorldPanels.class,null,"clickGui");
                    var buttons=(java.util.List<xyz.angames.astolfoclient.client.gui.clickgui.ModuleButton>)field(ClickGuiScreen.class,gui,"allModuleButtons");
                    var button=buttons.stream().filter(b->b.isVisible && !b.module.getName().contains("3D") && b.y>=36 && b.y<300).findFirst().orElseThrow();
                    boolean before=button.module.isEnabled();projectClick(mc,"GUI",button.x+20,button.y+12);
                    if(button.module.isEnabled()==before)throw new AssertionError("3D GUI click");button.module.setEnabled(before);
                    System.out.println("STORM_SMOKE 3D GUI click PASS");
                    projectClick(mc,"MUSIC",86,116);
                    if(mediaClicks!=1)throw new AssertionError("Spotify3D play/pause click: "+mediaClicks);
                    System.out.println("STORM_SMOKE 3D Spotify control PASS");
                    guiAnchor=field(field(WorldPanels.class,null,"GUI").getClass(),field(WorldPanels.class,null,"GUI"),"anchor");
                    musicAnchor=field(field(WorldPanels.class,null,"MUSIC").getClass(),field(WorldPanels.class,null,"MUSIC"),"anchor");
                    mc.player.setYRot(20);mc.player.setPos(mc.player.position().add(1,0,0));
                    stage=40;ticks=0;
                }else if(stage==40 && ticks>30){
                    if(!guiAnchor.equals(field(field(WorldPanels.class,null,"GUI").getClass(),field(WorldPanels.class,null,"GUI"),"anchor")))throw new AssertionError("GUI lock moved");
                    if(!musicAnchor.equals(field(field(WorldPanels.class,null,"MUSIC").getClass(),field(WorldPanels.class,null,"MUSIC"),"anchor")))throw new AssertionError("Spotify lock moved");
                    System.out.println("STORM_SMOKE both world locks PASS");shot(mc,"04-locked-panels");
                    mc.setScreen(new ClickGuiScreen());stage=5;ticks=0;
                }else if(stage==5 && ticks>60){
                    shot(mc,"03-gui-in-world");
                    for(var module:xyz.angames.astolfoclient.client.AstolfoclientClient.moduleManager.getModules()) {
                        if(module.visualColors==null) continue;
                        module.visualColors.source.set("Client");
                        if(xyz.angames.astolfoclient.client.config.VisualColors.get(module.getClass(),0)!=xyz.angames.astolfoclient.client.config.ThemeManager.getThemedColor(0))throw new AssertionError("Client color "+module.getName());
                        module.visualColors.source.set("Custom");module.visualColors.custom.set(0x12cdef);
                        if(xyz.angames.astolfoclient.client.config.VisualColors.get(module.getClass(),0)!=0xff12cdef)throw new AssertionError("Custom color "+module.getName());
                    }
                    String error=xyz.angames.astolfoclient.client.AstolfoclientClient.configManager.saveConfig("smoke-test");if(error!=null)throw new AssertionError(error);
                    for(var module:xyz.angames.astolfoclient.client.AstolfoclientClient.moduleManager.getModules()) if(module.visualColors!=null) {module.visualColors.source.set("Client");module.visualColors.custom.set(0);}
                    double size=Gui3DModule.INSTANCE.size.get();Gui3DModule.INSTANCE.size.set(4);
                    xyz.angames.astolfoclient.client.AstolfoclientClient.configManager.loadConfig("smoke-test");stage=6;ticks=0;
                }else if(stage==6 && ticks>20){
                    if(Gui3DModule.INSTANCE.size.get()!=2)throw new AssertionError("settings persistence");
                    System.out.println("STORM_SMOKE settings persistence PASS");
                    int colored=0;
                    for(var module:xyz.angames.astolfoclient.client.AstolfoclientClient.moduleManager.getModules()) if(module.visualColors!=null) {
                        if(!module.visualColors.source.is("Custom") || module.visualColors.custom.getInt()!=0x12cdef)throw new AssertionError("Color persistence "+module.getName());colored++;
                    }
                    if(colored!=25)throw new AssertionError("Missing color controls: "+colored);
                    System.out.println("STORM_SMOKE 25 visual color sources and config roundtrip PASS");
                    if(!java.nio.file.Files.isDirectory(xyz.angames.astolfoclient.client.gui.ConfigImportScreen.configFolder()))throw new AssertionError("Config picker folder");
                    var cm=xyz.angames.astolfoclient.client.AstolfoclientClient.configManager;
                    String json=cm.serializeCurrentConfig();String a=cm.importConfig(json),b=cm.importConfig(json);
                    if(a.equals(b))throw new AssertionError("Import replaced existing config");
                    boolean rejected=false;try{cm.importConfig("{\"modules\":null}");}catch(java.io.IOException e){rejected=true;}
                    if(!rejected)throw new AssertionError("Invalid config accepted");
                    if(!cm.loadConfig(a))throw new AssertionError("Imported config not loadable");
                    System.out.println("STORM_SMOKE config import, collision, rejection PASS");
                    if(!xyz.angames.astolfoclient.client.AstolfoclientClient.getInstance().getDiscordRpcManager().hasAcknowledgedActivity())throw new AssertionError("Discord activity not acknowledged");
                    System.out.println("STORM_SMOKE Discord activity accepted PASS");
                    mc.setScreen(new xyz.angames.astolfoclient.client.gui.ConfigImportScreen(new ClickGuiScreen(),name->{},false));stage=7;ticks=0;
                }else if(stage==7 && ticks>30){
                    shot(mc,"05-config-import");
                    var screen=new ClickGuiScreen();mc.setScreen(screen);
                    var cards=(java.util.List<xyz.angames.astolfoclient.client.gui.clickgui.ModuleButton>)field(ClickGuiScreen.class,screen,"allModuleButtons");
                    for(var card:cards) if(ClickGuiScreen.getRenderSubcategory(card.module)==ClickGuiScreen.RenderSubcategory.INTERFACE)card.expanded=true;
                    var category=ClickGuiScreen.class.getDeclaredField("activeRenderSubcategory");category.setAccessible(true);category.set(screen,ClickGuiScreen.RenderSubcategory.INTERFACE);
                    var offsets=(java.util.Map<ClickGuiScreen.NavTab,Float>)field(ClickGuiScreen.class,screen,"scrollOffsets");offsets.put(ClickGuiScreen.NavTab.RENDER,-120f);
                    mc.setScreen(null);var reopened=new ClickGuiScreen();mc.setScreen(reopened);
                    var restored=(java.util.List<xyz.angames.astolfoclient.client.gui.clickgui.ModuleButton>)field(ClickGuiScreen.class,reopened,"allModuleButtons");
                    for(var card:restored) if(ClickGuiScreen.getRenderSubcategory(card.module)==ClickGuiScreen.RenderSubcategory.INTERFACE && !card.expanded)throw new AssertionError("Expanded modules not restored");
                    if(field(ClickGuiScreen.class,reopened,"activeRenderSubcategory")!=ClickGuiScreen.RenderSubcategory.INTERFACE)throw new AssertionError("GUI category memory");
                    offsets=(java.util.Map<ClickGuiScreen.NavTab,Float>)field(ClickGuiScreen.class,reopened,"scrollOffsets");
                    if(offsets.get(ClickGuiScreen.NavTab.RENDER)!=-120f)throw new AssertionError("GUI scroll memory");
                    System.out.println("STORM_SMOKE GUI reopen position PASS");
                    stage=8;ticks=0;
                }else if(stage==8 && ticks>30){
                    var offsets=(java.util.Map<ClickGuiScreen.NavTab,Float>)field(ClickGuiScreen.class,mc.screen,"scrollOffsets");
                    if(Math.abs(offsets.get(ClickGuiScreen.NavTab.RENDER)+120)>1)throw new AssertionError("GUI position reset after rendering");
                    System.out.println("STORM_SMOKE GUI retained position after render PASS");
                    System.out.println("STORM_SMOKE ALL PASS");stage=9;mc.stop();
                }
            }catch(Throwable t){t.printStackTrace();System.err.println("STORM_SMOKE FAILED stage="+stage);stage=99;mc.stop();}
        });
    }
}
