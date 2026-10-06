package dev.stormdlc.world;

import com.top1.client.SongIslandClient;
import com.top1.client.music.LyricsFetcher;
import dev.stormdlc.render.LegacyRenderer;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.*;
import dev.sxmurxy.mre.msdf.MsdfFont;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import xyz.angames.astolfoclient.client.gui.clickgui.ClickGuiIcons;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.render.*;
import xyz.angames.astolfoclient.client.module.setting.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorldPanels {
    private static final Panel GUI=new Panel(580,380), MUSIC=new Panel(164,128);
    private static com.top1.client.island.MusicIsland worldIsland;
    private static Vector2f lastDrag;
    private static Module selected;
    private static int moduleScroll,settingScroll;
    private static final Matrix4f IDENTITY=new Matrix4f();
    private static Object world;
    private static final int ACCENT=0xff85bbff, GREEN=0xff1ed760;
    private static class Panel {
        float width,height;Vec3 anchor;Quaternionf rotation;
        Matrix4f model,inverse;float distance,size,offsetX,offsetY,angle;
        Panel(float width,float height){this.width=width;this.height=height;}
        void update(WorldRenderContext ctx,float d,float s,float x,float y,float a,boolean locked){
            var cam=ctx.camera();
            if(anchor==null || !locked || distance!=d || offsetX!=x || offsetY!=y || angle!=a){
                distance=d;size=s;offsetX=x;offsetY=y;angle=a;
                rotation=new Quaternionf(cam.rotation());
                var offset=new Vector3f(x,y,-d).rotate(rotation);
                anchor=cam.getPosition().add(offset.x,offset.y,offset.z);
                rotation.rotateY((float)Math.toRadians(a));
            }
            var rel=anchor.subtract(cam.getPosition());
            model=new Matrix4f().rotate(new Quaternionf(cam.rotation()).conjugate()).translate((float)rel.x,(float)rel.y,(float)rel.z)
                .rotate(rotation).scale(s/width,-s/width,s/width).translate(-width/2,-height/2,0);
            inverse=new Matrix4f(ctx.projectionMatrix()).mul(model).invert();
        }
        Vector2f hit(double x,double y){
            if(inverse==null)return null;
            var window=Minecraft.getInstance().getWindow();
            float nx=(float)(2*x/window.getGuiScaledWidth()-1),ny=(float)(1-2*y/window.getGuiScaledHeight());
            var near=new Vector3f(nx,ny,-1).mulProject(inverse);var far=new Vector3f(nx,ny,1).mulProject(inverse);
            float dz=far.z-near.z;if(Math.abs(dz)<1e-6)return null;
            float t=-near.z/dz;if(t<0 || t>1)return null;
            float px=near.x+(far.x-near.x)*t,py=near.y+(far.y-near.y)*t;
            return px>=0 && py>=0 && px<=width && py<=height?new Vector2f(px,py):null;
        }
    }
    public static void resetGui(){GUI.anchor=null;GUI.inverse=null;}
    public static void resetSpotify(){MUSIC.anchor=null;MUSIC.inverse=null;}
    public static void reset(){resetGui();resetSpotify();world=null;worldIsland=null;clickGui=null;dummyCtx=null;}
    public static boolean active(){return Gui3DModule.INSTANCE!=null && Gui3DModule.INSTANCE.isEnabled() || Spotify3DModule.INSTANCE!=null && Spotify3DModule.INSTANCE.isEnabled();}
    public static void interact(){var mc=Minecraft.getInstance();if(mc.level!=null && active())mc.setScreen(new WorldPanelScreen());}
    public static void render(WorldRenderContext ctx){
        var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null || mc.options.hideGui)return;
        if(world!=mc.level){reset();world=mc.level;}
        var gui=Gui3DModule.INSTANCE;var spotify=Spotify3DModule.INSTANCE;
        if(gui!=null && gui.isEnabled()){
            GUI.update(ctx,gui.distance.getFloat(),gui.size.getFloat(),gui.horizontal.getFloat(),gui.vertical.getFloat(),gui.rotation.getFloat(),gui.lock.get());
            LegacyRenderer.beginWorld(GUI.model,ctx.projectionMatrix());try{drawGui();}finally{LegacyRenderer.endWorld();}
        }else GUI.inverse=null;
        if(spotify!=null && spotify.isEnabled()){
            if(worldIsland==null){worldIsland=new com.top1.client.island.MusicIsland(SongIslandClient.tracker());worldIsland.rendering3D=true;}
            worldIsland.showLyrics=spotify.lyrics.get();MUSIC.width=172;MUSIC.height=worldIsland.surfaceHeight();
            MUSIC.update(ctx,spotify.distance.getFloat(),spotify.size.getFloat(),spotify.horizontal.getFloat(),spotify.vertical.getFloat(),spotify.rotation.getFloat(),spotify.lock.get());
            LegacyRenderer.beginWorld(MUSIC.model,ctx.projectionMatrix());try{drawMusic();}finally{LegacyRenderer.endWorld();}
        }else MUSIC.inverse=null;
    }
    private static void rect(float x,float y,float w,float h,float r,int color){Builder.rectangle().size(new SizeState(w,h)).radius(new QuadRadiusState(r)).color(color).build().render(IDENTITY,x,y);}
    private static MsdfFont font(){return ClickGuiIcons.MEDIUM_FONT.get();}
    private static void text(String s,float x,float y,float size,int color){Builder.text().font(font()).text(s).size(size).color(color).build().render(IDENTITY,x,y);}
    private static String fit(String s,float max,float size){if(s==null)return "";if(font().getWidth(s,size)<=max)return s;while(s.length()>0 && font().getWidth(s+"...",size)>max)s=s.substring(0,s.length()-1);return s+"...";}
    private static void image(ResourceLocation id,float x,float y,float w,float h){if(id!=null)Builder.texture().size(new SizeState(w,h)).radius(new QuadRadiusState(6)).texture(0,0,1,1,id).build().render(IDENTITY,x,y);}
    private static List<Setting> settings(){
        List<Setting> list=new ArrayList<>();if(selected!=null)flatten(selected.getSettings(),list);return list;
    }
    private static void flatten(List<Setting> input,List<Setting> out){for(Setting s:input){if(!s.isVisible())continue;if(s instanceof ConfigureSetting c)flatten(c.getSubSettings(),out);else if(s instanceof BooleanSetting || s instanceof NumberSetting || s instanceof ModeSetting || s instanceof EnumSetting<?> || s instanceof ActionSetting)out.add(s);}}
    private static xyz.angames.astolfoclient.client.gui.ClickGuiScreen clickGui;
    private static net.minecraft.client.gui.GuiGraphics dummyCtx;

    private static void drawGui(){
        if(clickGui == null) {
            clickGui = new xyz.angames.astolfoclient.client.gui.ClickGuiScreen();
            clickGui.init(Minecraft.getInstance(), 580, 380);
        }
        if(dummyCtx == null) {
            dummyCtx = new net.minecraft.client.gui.GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        }
        
        var p = getMouseHit(GUI);
        guiInput(()->clickGui.render(dummyCtx,p!=null?(int)p.x:-100,p!=null?(int)p.y:-100,Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)));
        cursor(GUI);
    }
    private static void guiInput(Runnable action){
        xyz.angames.astolfoclient.client.gui.ClickGuiScreen.rendering3D=true;
        try{action.run();}finally{xyz.angames.astolfoclient.client.gui.ClickGuiScreen.rendering3D=false;}
    }
    private static void drawMusic(){
        if(worldIsland==null)return;
        worldIsland.render();
        com.top1.client.island.render.IslandRender.flush();
        cursor(MUSIC);
    }
    private static Vector2f getMouseHit(Panel panel) {
        var mc=Minecraft.getInstance();if(!(mc.screen instanceof WorldPanelScreen))return null;
        var w=mc.getWindow();return panel.hit(mc.mouseHandler.xpos()*w.getGuiScaledWidth()/w.getScreenWidth(),mc.mouseHandler.ypos()*w.getGuiScaledHeight()/w.getScreenHeight());
    }
    private static void cursor(Panel panel){
        var hit=getMouseHit(panel);
        if(hit!=null){rect(hit.x-2,hit.y-2,4,4,2,0xffffffff);}
    }
    public static void click(double x,double y,int button){
        var pm = MUSIC.hit(x,y);
        if(pm != null && Spotify3DModule.INSTANCE.isEnabled()){
            var island = worldIsland;
            if(island != null) island.handleClick(pm.x, pm.y, button);
            return;
        }
        var pg = GUI.hit(x,y);
        if(pg != null && Gui3DModule.INSTANCE.isEnabled() && clickGui != null){
            lastDrag=pg;guiInput(()->clickGui.mouseClicked(pg.x, pg.y, button));
        }
    }
    public static void drag(double x,double y,int button){
        var pg=GUI.hit(x,y);
        if(pg!=null && clickGui!=null && lastDrag!=null){
            var old=lastDrag;guiInput(()->clickGui.mouseDragged(pg.x,pg.y,button,pg.x-old.x,pg.y-old.y));lastDrag=pg;
        }
    }
    public static void scroll(double x,double y,double amount){
        var pm = MUSIC.hit(x,y);
        if(pm != null && Spotify3DModule.INSTANCE.isEnabled()){
            var island = worldIsland;
            if(island != null) island.handleScroll(pm.x, pm.y, amount);
            return;
        }
        var pg = GUI.hit(x,y);
        if(pg != null && Gui3DModule.INSTANCE.isEnabled() && clickGui != null){
            guiInput(()->clickGui.mouseScrolled(pg.x, pg.y, 0, amount));
        }
    }
    public static void release(double x,double y,int button){
        if(clickGui!=null){var p=GUI.hit(x,y);guiInput(()->clickGui.mouseReleased(p==null?-100:p.x,p==null?-100:p.y,button));}lastDrag=null;
    }
    public static boolean key(int key,int scan,int mods){
        if(clickGui==null || !Gui3DModule.INSTANCE.isEnabled())return false;
        boolean[] handled={false};guiInput(()->handled[0]=clickGui.keyPressed(key,scan,mods));return handled[0];
    }
    public static boolean character(char chr,int mods){
        if(clickGui==null || !Gui3DModule.INSTANCE.isEnabled())return false;
        boolean[] handled={false};guiInput(()->handled[0]=clickGui.charTyped(chr,mods));return handled[0];
    }
}
