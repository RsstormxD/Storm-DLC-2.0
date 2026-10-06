package xyz.angames.astolfoclient.client.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.*;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.clickgui.*;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.GuiAppearanceModule;
import xyz.angames.astolfoclient.client.module.setting.Setting;

/** Appearance studio with theme presets, live material preview and grouped controls. */
public final class GuiAppearanceScreen extends Screen {
    private final Screen parent;
    private final GuiAppearanceModule appearance=GuiAppearanceModule.INSTANCE;
    private String tab="Layers";
    private final Module controlsModule=new Module("Appearance",Module.Category.RENDER){
        @Override public List<Setting> getSettings(){return settingsForTab();}
    };
    private final UniversalSettingsPanel panel=new UniversalSettingsPanel(controlsModule);
    private final List<Hit> hits=new ArrayList<>();
    private record Hit(float x,float y,float w,float h,Runnable click){}
    private float scroll,scale=1,wx,wy,px,py,pw,ph;
    private long last=System.nanoTime();
    public GuiAppearanceScreen(Screen parent){super(Component.literal("Appearance Studio"));this.parent=parent;}
    private List<Setting> settingsForTab(){
        if(appearance==null)return List.of();
        return switch(tab){
            case "Layout"->List.of(appearance.version,appearance.layout,appearance.density,appearance.radius,appearance.spacing,appearance.descriptions);
            case "Motion"->List.of(appearance.animations,appearance.speed,appearance.surfaceShine,appearance.enabledTint,appearance.accentGlow);
            default->List.of(appearance.theme,appearance.opacity,appearance.cardOpacity,appearance.layerDepth,appearance.shadowStrength,appearance.borders,appearance.borderStrength,appearance.blur,appearance.refraction);
        };
    }
    @Override protected void init(){last=System.nanoTime();}
    private static void rect(Matrix4f m,float x,float y,float w,float h,float r,Color color){Builder.rectangle().size(new SizeState(w,h)).radius(new QuadRadiusState(r)).color(color).build().render(m,x,y);}
    private static void text(Matrix4f m,String text,float x,float y,float size,Color color){GuiUtils.renderTextSafely(m,text,x,y,color,size);}
    private void button(Matrix4f m,String title,float x,float y,float w,float h,boolean selected,int mx,int my,Runnable action){
        boolean hover=!ModePopupState.isOpen()&&GuiUtils.isMouseOver(mx,my,x,y,w,h);
        Color accent=ThemeManager.getThemeColor();
        rect(m,x,y,w,h,6,GuiSkin.color(selected?accent.getRGB():0xc2d6f4,selected?.24f:hover?.14f:.06f));
        text(m,title,x+9,y+(h-8)/2,8,selected?Color.WHITE:GuiSkin.muted(1));
        hits.add(new Hit(x,y,w,h,action));
    }
    private void selectTab(String next){panel.cancelInteraction();tab=next;scroll=0;}
    @Override public void render(GuiGraphics c,int mouseX,int mouseY,float delta){
        scale=Math.max(.25f,Math.min(1.3f,Math.min((width-20)/680f,(height-20)/420f)));
        wx=(width/scale-680)/2;wy=(height/scale-420)/2;
        int mx=(int)(mouseX/scale),my=(int)(mouseY/scale);
        float dt=Math.min(.1f,(System.nanoTime()-last)/1e9f);last=System.nanoTime();hits.clear();
        c.fill(0,0,width,height,0xa3070b14);c.pose().pushPose();c.pose().scale(scale,scale,1);
        var m=c.pose().last().pose();Color accent=ThemeManager.getThemeColor();
        GuiSkin.window(m,wx,wy,680,420,1,accent,false);
        text(m,"Appearance Studio",wx+22,wy+19,18,Color.WHITE);
        text(m,"Shape your workspace. Every change is previewed live.",wx+23,wy+43,8,GuiSkin.muted(1));
        float lx=wx+16,ly=wy+70,lw=206;
        GuiSkin.sidebar(m,lx,ly,lw,294,1,accent);
        text(m,"THEMES",lx+12,ly+13,7,GuiSkin.muted(1));
        String[] themes={"Liquid Glass","Midnight","AMOLED","Aurora","Carbon","Slate"};
        for(int i=0;i<themes.length;i++){
            String theme=themes[i];float bx=lx+10+(i%2)*96,by=ly+30+(i/2)*34;
            button(m,theme,bx,by,90,27,appearance.theme.is(theme),mx,my,()->{appearance.setEnabled(true);appearance.version.set("New");appearance.theme.set(theme);panel.cancelInteraction();});
        }
        text(m,"LIVE PREVIEW",lx+12,ly+144,7,GuiSkin.muted(1));
        float cy=ly+164;
        GuiSkin.card(m,lx+10,cy,lw-20,64,1,accent,true,0);
        text(m,"Storm DLC 2.0",lx+23,cy+11,11,Color.WHITE);
        text(m,"A workspace with depth",lx+23,cy+28,7.5f,GuiSkin.muted(1));
        rect(m,lx+23,cy+47,28,10,4,GuiSkin.color(accent.getRGB(),.22f));
        text(m,"ACTIVE",lx+27,cy+50,5,accent);
        GuiSkin.settings(m,lx+13,cy+72,lw-26,38,1,accent);
        text(m,"Accent & surfaces",lx+24,cy+81,7.5f,GuiSkin.muted(1));
        rect(m,lx+24,cy+97,lw-50,3,1.5f,GuiSkin.color(0x627086,.3f));
        rect(m,lx+24,cy+97,(lw-50)*.65f,3,1.5f,accent);
        rect(m,lx+24+(lw-50)*.65f-3,cy+95,7,7,3.5f,Color.WHITE);
        float right=wx+237,rightW=427;
        String[] tabs={"Layers","Layout","Motion"};
        for(int i=0;i<tabs.length;i++){String name=tabs[i];button(m,name,right+i*93,wy+71,86,27,tab.equals(name),mx,my,()->selectTab(name));}
        button(m,appearance.version.is("Old")?"Old GUI":"New GUI",right+rightW-103,wy+71,103,27,true,mx,my,()->{appearance.version.set(appearance.version.is("New")?"Old":"New");appearance.setEnabled(true);selectTab("Layout");});
        px=right;py=wy+107;pw=rightW;ph=257;
        GuiSkin.surface(m,px,py,pw,ph,1,accent,1);panel.updateRows();scroll=Math.max(-Math.max(0,panel.getTotalHeight()-ph),Math.min(0,scroll));
        c.enableScissor((int)px,(int)py,(int)(px+pw),(int)(py+ph));
        panel.render(c,px,py+scroll,pw,1,ModePopupState.isOpen()?-1:mx,ModePopupState.isOpen()?-1:my,dt,accent);
        c.disableScissor();
        if(panel.getTotalHeight()>ph){float thumb=Math.max(22,ph*ph/panel.getTotalHeight()),maximum=panel.getTotalHeight()-ph;
            rect(m,px+pw-4,py+(ph-thumb)*(-scroll/maximum),2,thumb,1,GuiSkin.color(accent.getRGB(),.7f));}
        button(m,"Reset appearance",wx+18,wy+381,137,25,false,mx,my,()->{panel.cancelInteraction();appearance.resetAppearance();appearance.setEnabled(true);scroll=0;});
        text(m,"Settings are saved automatically",wx+174,wy+390,7.5f,GuiSkin.muted(1));
        button(m,"Done",wx+573,wy+379,88,29,true,mx,my,this::onClose);
        ModePopupState.renderActive(c,wx,wy,680,420,mx,my,dt,1,accent);
        c.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        float mx=(float)(x/scale),my=(float)(y/scale);
        if(ModePopupState.mouseClicked(mx,my,button,wx,wy,680,420))return true;
        if(button==0)for(var hit:hits)if(GuiUtils.isMouseOver(mx,my,hit.x,hit.y,hit.w,hit.h)){hit.click.run();return true;}
        if(GuiUtils.isMouseOver(mx,my,px,py,pw,ph))return panel.mouseClicked(mx,my,button,px,py+scroll,pw);
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return panel.mouseDragged(x/scale,y/scale,px,pw);}
    @Override public boolean mouseReleased(double x,double y,int b){panel.mouseReleased();return true;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(ModePopupState.mouseScrolled(x/scale,y/scale,vertical,wx,wy,680,420))return true;
        if(ModePopupState.isOpen())return true;
        if(GuiUtils.isMouseOver((float)(x/scale),(float)(y/scale),px,py,pw,ph))scroll+=vertical*30;return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==256&&ModePopupState.isOpen()){ModePopupState.close();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void removed(){panel.cancelInteraction();ModePopupState.close();AstolfoclientClient.configManager.saveConfig("default");}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
