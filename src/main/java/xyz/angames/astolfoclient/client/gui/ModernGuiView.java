package xyz.angames.astolfoclient.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.*;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.GuiScaleSettings;
import xyz.angames.astolfoclient.client.gui.clickgui.*;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.GuiAppearanceModule;
import java.awt.Color;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Independent layout and input coordinates for the new GUI. The classic GUI remains intact. */
final class ModernGuiView {
    private final ClickGuiScreen owner;
    private final List<ModuleButton> cards;
    private final Minecraft mc=Minecraft.getInstance();
    private final List<Hit> controls=new ArrayList<>();
    private final Map<String,Float> hover=new HashMap<>();
    private final Map<String,Float> positions=new HashMap<>();
    private final ColorPickerComponent accentPicker=new ColorPickerComponent("Client accent");
    private List<ModuleButton> visible=List.of();
    private List<String> configs=new ArrayList<>();
    private String category="All modules",query="",sort="Default",selectedConfig="default",newName="",status="";
    private boolean searching,typingName,enabledOnly,pinnedOnly,palette;
    private float scroll,targetScroll,maxScroll,configScroll,scale=1,x,y,w=720,h=440,side=156,contentX,contentY,contentW,contentH,alpha;
    private long last=System.nanoTime(),noticeTime;
    private float dt;
    private int mouseX,mouseY;
    private Color accent;
    private ModuleButton draggingCard;
    private boolean draggingScroll;
    private float scrollbarStart,scrollbarGrab;
    private record Hit(String id,float x,float y,float w,float h,Runnable action) {
        boolean contains(float mx,float my){return GuiUtils.isMouseOver(mx,my,x,y,w,h);}
    }
    ModernGuiView(ClickGuiScreen owner,List<ModuleButton> cards){this.owner=owner;this.cards=cards;reloadConfigs();}
    private void reloadConfigs(){configs=new ArrayList<>(AstolfoclientClient.configManager.getLocalConfigs());configs.sort(String.CASE_INSENSITIVE_ORDER);}
    boolean textFocused(){return searching||typingName;}
    boolean modalOpen(){return palette||ModePopupState.isOpen()||SubSettingsPopupState.isOpen()||MultiSelectPopupState.isOpen();}
    void init(){last=System.nanoTime();alpha=0;reloadConfigs();clearTransient();}
    private void clearTransient(){draggingCard=null;draggingScroll=false;for(var card:cards)card.settingsPanel.cancelInteraction();}
    void removed(){clearTransient();palette=false;searching=false;typingName=false;}
    void save(JsonObject json){
        var state=new JsonObject();state.addProperty("category",category);state.addProperty("query",query);state.addProperty("sort",sort);
        state.addProperty("enabledOnly",enabledOnly);state.addProperty("pinnedOnly",pinnedOnly);state.addProperty("scroll",scroll);state.addProperty("profile",selectedConfig);
        var pins=new JsonArray();for(var c:cards)if(c.favorite)pins.add(c.module.getName());state.add("pins",pins);json.add("modern",state);
    }
    void restore(JsonObject json){
        if(!json.has("modern"))return;var s=json.getAsJsonObject("modern");
        if(s.has("category"))category=s.get("category").getAsString();if(s.has("query"))query=s.get("query").getAsString();
        if(s.has("sort"))sort=s.get("sort").getAsString();if(s.has("profile"))selectedConfig=s.get("profile").getAsString();
        if(s.has("enabledOnly"))enabledOnly=s.get("enabledOnly").getAsBoolean();if(s.has("pinnedOnly"))pinnedOnly=s.get("pinnedOnly").getAsBoolean();
        if(s.has("scroll")){float n=s.get("scroll").getAsFloat();if(Float.isFinite(n))scroll=targetScroll=Math.min(0,n);}
        if(s.has("pins"))for(var name:s.getAsJsonArray("pins"))for(var c:cards)if(c.module.getName().equals(name.getAsString()))c.favorite=true;
    }
    void render(GuiGraphics context,int rawX,int rawY,float delta){
        boolean world=ClickGuiScreen.rendering3D;
        w=world?580:720;h=world?380:440;side=world?130:156;
        scale=world?1:Math.min(GuiUtils.getScaleModifier(mc)*GuiScaleSettings.getScale(),Math.min((owner.width-24)/w,(owner.height-24)/h));
        scale=Math.max(.25f,scale);x=world?0:(owner.width/scale-w)/2;y=world?0:(owner.height/scale-h)/2;
        mouseX=(int)(rawX/scale);mouseY=(int)(rawY/scale);dt=Math.min(.1f,Math.max(.0001f,(System.nanoTime()-last)/1e9f));last=System.nanoTime();
        alpha=GuiUtils.animate(alpha,1,14,dt);accent=ClickGuiScreen.accentColor(world);controls.clear();
        if(!world)context.fill(0,0,owner.width,owner.height,((int)(90*alpha)<<24)|0x030712);
        context.pose().pushPose();context.pose().scale(scale,scale,1);Matrix4f matrix=context.pose().last().pose();
        GuiSkin.window(matrix,x,y,w,h,alpha,accent,world);
        GuiSkin.sidebar(matrix,x+7,y+7,side-7,h-14,alpha,accent);
        drawNavigation(matrix);
        contentX=x+side+12;contentW=w-side-26;
        drawHeader(matrix);
        contentY=y+108;contentH=h-145;
        GuiSkin.surface(matrix,contentX-5,contentY-5,contentW+10,contentH+10,alpha,accent,1);
        if(category.equals("Configs"))drawConfigs(context,matrix);else drawCards(context,matrix);
        drawFooter(matrix);
        if(palette)drawPalette(matrix);
        ModePopupState.renderActive(context,x,y,w,h,mouseX,mouseY,dt,alpha,accent);
        SubSettingsPopupState.renderActive(context,x,y,w,h,mouseX,mouseY,dt,alpha,accent);
        MultiSelectPopupState.renderActive(context,x,y,w,h,mouseX,mouseY,dt,alpha,accent);
        context.pose().popPose();
    }
    private static Color tint(Color c,float a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.max(0,Math.min(255,(int)(a*255))));}
    private void text(Matrix4f m,String s,float px,float py,float size,Color color){GuiUtils.renderTextSafely(m,s,px,py,tint(color,alpha),size);}
    private String fit(String value,float width,float size){
        var font=ClickGuiIcons.SEMIBOLD_FONT.get();if(font.getWidth(value,size)<=width)return value;
        while(!value.isEmpty()&&font.getWidth(value+"...",size)>width)value=value.substring(0,value.length()-1);return value+"...";
    }
    private void rect(Matrix4f m,float px,float py,float width,float height,float radius,Color color){Builder.rectangle().size(new SizeState(width,height)).radius(new QuadRadiusState(radius)).color(color).build().render(m,px,py);}
    private void button(Matrix4f m,String id,String label,float px,float py,float width,float height,boolean selected,Runnable action){
        boolean over=!modalOpen()&&GuiUtils.isMouseOver(mouseX,mouseY,px,py,width,height);
        float progress=GuiUtils.animate(hover.getOrDefault(id,0f),over?1:0,16,dt);hover.put(id,progress);
        rect(m,px,py,width,height,6,tint(selected?accent:new Color(180,198,226),alpha*(selected?.20f:.035f+.075f*progress)));
        if(selected)Builder.border().size(new SizeState(width,height)).radius(new QuadRadiusState(6)).thickness(.7f).color(tint(accent,alpha*.36f)).build().render(m,px,py);
        text(m,fit(label,width-16,8),px+8,py+(height-8)/2,8,selected?Color.WHITE:new Color(186,199,218));
        controls.add(new Hit(id,px,py,width,height,action));
    }
    private void drawNavigation(Matrix4f m){
        Builder.texture().size(new SizeState(33,33)).texture(0,0,1,1,ResourceLocation.fromNamespaceAndPath("stormdlc","logo.png")).color(tint(Color.WHITE,alpha)).build().render(m,x+18,y+20);
        text(m,"Storm",x+59,y+24,12,Color.WHITE);text(m,"DLC 2.0",x+60,y+41,7,new Color(143,164,198));
        text(m,"WORKSPACE",x+20,y+78,6.5f,new Color(119,138,167));
        String[] names={"All modules","Combat","Friends","World","Player","PvP","Interface","Miscellaneous","Configs"};
        for(int i=0;i<names.length;i++){
            String name=names[i];float row=y+94+i*(ClickGuiScreen.rendering3D?22:29);
            button(m,"nav-"+name,name,x+15,row,side-27,ClickGuiScreen.rendering3D?20:25,category.equals(name),()->changeCategory(name));
            if(!name.equals("Configs")){
                long n=cards.stream().filter(card->categoryMatches(card,name)).count();
                text(m,String.valueOf(n),x+side-31,row+9,6.5f,new Color(116,137,168));
            }
        }
        float bottom=y+h-66;
        button(m,"appearance","Appearance",x+15,bottom,side-27,25,false,()->mc.setScreen(new GuiAppearanceScreen(mc.screen)));
        button(m,"hud","Edit HUD",x+15,bottom+30,side-27,23,false,()->mc.setScreen(new HudEditorScreen()));
    }
    private void changeCategory(String next){
        positions.put(category,scroll);category=next;scroll=targetScroll=positions.getOrDefault(next,0f);clearTransient();typingName=false;
        for(var c:cards){c.renderX=-9999;c.renderY=-9999;c.renderAlpha=0;}
        if(next.equals("Configs"))reloadConfigs();
    }
    private void drawHeader(Matrix4f m){
        float top=y+19;float searchWidth=ClickGuiScreen.rendering3D?142:200;
        text(m,category,contentX+1,top+2,17,Color.WHITE);
        long enabled=cards.stream().filter(c->c.module.isEnabled()).count();
        text(m,category.equals("Configs")?"Save a setup. Make it yours.":enabled+" active  /  "+cards.size()+" modules",contentX+2,top+25,8,new Color(136,156,187));
        float sx=x+w-searchWidth-17,sy=y+18;
        GuiSkin.surface(m,sx,sy,searchWidth,31,alpha,accent,2);
        String value=query.isEmpty()?(searching?"Type a module name":"Search modules  / "):query+(searching&&System.currentTimeMillis()%1000<500?"|":"");
        text(m,fit(value,searchWidth-28,8.5f),sx+11,sy+11,8.5f,query.isEmpty()?new Color(133,154,184):Color.WHITE);
        controls.add(new Hit("search",sx,sy,searchWidth-23,31,()->{searching=true;typingName=false;}));
        text(m,"x",sx+searchWidth-16,sy+11,9,new Color(163,182,209));
        controls.add(new Hit("clear-search",sx+searchWidth-22,sy,22,31,()->{query="";searching=false;targetScroll=scroll=0;}));
        float row=y+65;
        if(!category.equals("Configs")){
            button(m,"filter-all","All",contentX,row,37,25,!enabledOnly&&!pinnedOnly,()->{enabledOnly=false;pinnedOnly=false;scroll=targetScroll=0;});
            button(m,"filter-enabled","Enabled",contentX+43,row,63,25,enabledOnly,()->{enabledOnly=!enabledOnly;scroll=targetScroll=0;});
            button(m,"filter-pins","Pinned",contentX+112,row,56,25,pinnedOnly,()->{pinnedOnly=!pinnedOnly;scroll=targetScroll=0;});
            button(m,"sort","Sort: "+sort,contentX+contentW-163,row,106,25,false,()->{sort=switch(sort){case "Default"->"A-Z";case "A-Z"->"Enabled";default->"Default";};clearTransient();});
            button(m,"layout",GuiSkin.list()?"List":"Grid",contentX+contentW-51,row,51,25,false,()->{GuiAppearanceModule.INSTANCE.layout.set(GuiSkin.list()?"Grid":"List");clearTransient();});
        }else{
            button(m,"import","Import config",contentX,row,106,25,false,()->mc.setScreen(new ConfigImportScreen(mc.screen,name->{selectedConfig=name;reloadConfigs();notice("Imported "+name);} )));
            button(m,"folder","Open folder",contentX+113,row,96,25,false,()->{try{net.minecraft.Util.getPlatform().openPath(ConfigImportScreen.configFolder());}catch(Exception e){notice("Cannot open folder");}});
        }
    }
    private boolean categoryMatches(ModuleButton card,String name){
        if(name.equals("All modules"))return true;
        if(name.equals("Combat"))return card.module.getCategory()==Module.Category.COMBAT;
        if(name.equals("Friends"))return card.module.getCategory()==Module.Category.FRIENDS;
        if(name.equals("Miscellaneous"))return card.module.getCategory()==Module.Category.MISC;
        if(card.module.getCategory()!=Module.Category.RENDER)return false;
        return switch(name){
            case "World"->ClickGuiScreen.getRenderSubcategory(card.module)==ClickGuiScreen.RenderSubcategory.WORLD;
            case "Player"->ClickGuiScreen.getRenderSubcategory(card.module)==ClickGuiScreen.RenderSubcategory.PLAYER;
            case "PvP"->ClickGuiScreen.getRenderSubcategory(card.module)==ClickGuiScreen.RenderSubcategory.PVP;
            case "Interface"->ClickGuiScreen.getRenderSubcategory(card.module)==ClickGuiScreen.RenderSubcategory.INTERFACE;
            default->false;
        };
    }
    private void drawCards(GuiGraphics context,Matrix4f m){
        String needle=query.toLowerCase(Locale.ROOT).trim();
        List<ModuleButton> matches=new ArrayList<>();
        for(var card:cards){card.isVisible=false;
            if((needle.isEmpty()?categoryMatches(card,category):card.module.getName().toLowerCase(Locale.ROOT).contains(needle))
                &&(!enabledOnly||card.module.isEnabled())&&(!pinnedOnly||card.favorite))matches.add(card);
        }
        if(sort.equals("A-Z"))matches.sort(Comparator.comparing(c->c.module.getName(),String.CASE_INSENSITIVE_ORDER));
        if(sort.equals("Enabled"))matches.sort(Comparator.<ModuleButton,Boolean>comparing(c->!c.module.isEnabled()).thenComparing(c->c.module.getName()));
        visible=matches;int columns=GuiSkin.list()?1:2;float gap=GuiSkin.gap(),cardW=(contentW-16-gap*(columns-1))/columns;
        float[] ends=new float[columns];
        for(int i=0;i<matches.size();i++)ends[i%columns]+=matches.get(i).calculateHeight()+gap;
        float total=0;for(float end:ends)total=Math.max(total,end-gap);
        maxScroll=Math.max(0,total-(contentH-12));targetScroll=Math.max(-maxScroll,Math.min(0,targetScroll));scroll=GuiUtils.animate(scroll,targetScroll,18,dt);
        float[] rows=new float[columns];Arrays.fill(rows,contentY+6+scroll);
        ClickGuiScreen.panelScissor(context,(int)contentX,(int)contentY,(int)(contentX+contentW),(int)(contentY+contentH));
        for(int i=0;i<matches.size();i++){
            var card=matches.get(i);int col=i%columns;float tx=contentX+6+col*(cardW+gap),ty=rows[col];card.width=cardW;
            float ch=card.calculateHeight();rows[col]+=ch+gap;
            if(card.renderX<=-9000){card.renderX=tx;card.renderY=ty;}
            card.renderX=GuiUtils.animate(card.renderX,tx,22,dt);card.renderY=GuiUtils.animate(card.renderY,ty,22,dt);
            card.x=card.renderX;card.y=card.renderY;card.height=ch;card.renderAlpha=GuiUtils.animate(card.renderAlpha,1,18,dt);card.renderScale=1;
            card.isVisible=card.y+ch>contentY&&card.y<contentY+contentH;
            if(card.isVisible)card.render(context,owner,modalOpen()?-100:mouseX,modalOpen()?-100:mouseY,alpha,dt);
        }
        ClickGuiScreen.endPanelScissor(context);
        if(matches.isEmpty()){
            text(m,pinnedOnly?"No pinned modules":"No modules found",contentX+22,contentY+42,14,Color.WHITE);
            text(m,pinnedOnly?"Use PIN on a module card to keep it here.":"Clear the search or choose another filter.",contentX+22,contentY+66,8,new Color(142,163,193));
        }
        if(maxScroll>0){
            float track=contentH-12,thumb=Math.max(24,track*(contentH/Math.max(contentH,total)));
            float pos=contentY+6+(track-thumb)*(-scroll/maxScroll);
            rect(m,contentX+contentW-3,contentY+6,2,track,1,tint(Color.WHITE,.04f*alpha));
            rect(m,contentX+contentW-4,pos,3,thumb,1.5f,tint(accent,.65f*alpha));
            final float thumbTop=pos,thumbHeight=thumb;
            controls.add(new Hit("scrollbar",contentX+contentW-9,contentY,13,contentH,()->{draggingScroll=true;scrollbarStart=track-thumbHeight;scrollbarGrab=mouseY>=thumbTop&&mouseY<=thumbTop+thumbHeight?mouseY-thumbTop:thumbHeight/2;updateScrollbar(mouseY);}));
        }
    }
    private void drawFooter(Matrix4f m){
        text(m,"LMB toggle   /   RMB settings   /   MMB bind",contentX,y+h-20,7,new Color(117,141,173));
        if(System.currentTimeMillis()-noticeTime<4500)text(m,fit(status,contentW-8,8),contentX,contentY+contentH-17,8,accent);
        else if(!category.equals("Configs"))button(m,"collapse","Collapse all",contentX+contentW-90,y+h-28,90,21,false,()->{for(var c:cards){c.expanded=false;c.settingsPanel.cancelInteraction();}scroll=targetScroll=0;});
        button(m,"accent","Accent",x+side-72,y+77,53,13,false,()->{palette=true;accentPicker.loadColor();});
    }
    private void drawPalette(Matrix4f m){
        float px=x+side+18,py=y+62;
        GuiSkin.window(m,px,py,214,174,1,accent,ClickGuiScreen.rendering3D);
        text(m,"Client accent",px+12,py+12,11,Color.WHITE);text(m,"Click outside to close",px+12,py+153,7,new Color(142,163,193));
        accentPicker.x=px+6;accentPicker.y=py+30;accentPicker.width=202;accentPicker.height=116;accentPicker.animate(dt);accentPicker.render(m,mouseX,mouseY,1);
    }
    private void notice(String value){status=value;noticeTime=System.currentTimeMillis();}
    private void drawConfigs(GuiGraphics context,Matrix4f m){
        for(var card:cards)card.isVisible=false;
        float left=contentX+7,top=contentY+8,leftW=contentW*.47f,right=left+leftW+12,rightW=contentW-leftW-26;
        text(m,"SAVED PROFILES",left+7,top+6,7,new Color(135,157,189));
        float listY=top+27,listH=contentH-44;
        configScroll=Math.max(-Math.max(0,configs.size()*31-listH),Math.min(0,configScroll));
        ClickGuiScreen.panelScissor(context,(int)left,(int)listY,(int)(left+leftW),(int)(listY+listH));
        for(int i=0;i<configs.size();i++){
            String name=configs.get(i);float ry=listY+i*31+configScroll;
            if(ry+27>=listY&&ry<=listY+listH){
                button(m,"config-"+name,fit(name,leftW-22,8),left,ry,leftW,27,selectedConfig.equals(name),()->{selectedConfig=name;typingName=false;});
            }
        }
        ClickGuiScreen.endPanelScissor(context);
        GuiSkin.surface(m,right,top,rightW,contentH-12,alpha,accent,2);
        text(m,"PROFILE",right+12,top+12,7,new Color(135,157,189));
        text(m,fit(selectedConfig,rightW-24,13),right+12,top+31,13,Color.WHITE);
        button(m,"load","Load profile",right+10,top+58,rightW-20,27,true,()->notice(AstolfoclientClient.configManager.loadConfig(selectedConfig)?"Loaded "+selectedConfig:"Could not load profile"));
        button(m,"save","Save current setup",right+10,top+92,rightW-20,27,false,()->{String error=AstolfoclientClient.configManager.saveConfig(selectedConfig);notice(error==null?"Saved "+selectedConfig:error);reloadConfigs();});
        text(m,"NEW PROFILE",right+12,top+143,7,new Color(135,157,189));
        rect(m,right+10,top+160,rightW-20,27,5,tint(Color.WHITE,.055f*alpha));
        text(m,fit(newName.isEmpty()?"Enter a name...":newName+(typingName?"|":""),rightW-40,8),right+19,top+169,8,newName.isEmpty()?new Color(134,153,181):Color.WHITE);
        controls.add(new Hit("name",right+10,top+160,rightW-20,27,()->{typingName=true;searching=false;}));
        button(m,"create","Create profile",right+10,top+194,rightW-20,27,false,this::createConfig);
        if(configs.isEmpty())text(m,"No saved profiles yet",left+7,listY+10,8,new Color(138,158,188));
    }
    private void createConfig(){
        String name=newName.trim();if(name.isEmpty()){notice("Enter a profile name first");typingName=true;return;}
        if(configs.contains(name)){notice("A profile with this name already exists");return;}
        String error=AstolfoclientClient.configManager.saveConfig(name);if(error==null){selectedConfig=name;newName="";typingName=false;reloadConfigs();notice("Created "+name);}else notice(error);
    }
    boolean click(double rawX,double rawY,int button){
        float mx=(float)(rawX/scale),my=(float)(rawY/scale);mouseX=(int)mx;mouseY=(int)my;
        if(ModePopupState.mouseClicked(mx,my,button,x,y,w,h)||SubSettingsPopupState.mouseClicked(mx,my,button,x,y,w,h)||MultiSelectPopupState.mouseClicked(mx,my,button,x,y,w,h))return true;
        if(palette){if(accentPicker.mouseClicked(mx,my,button))return true;palette=false;accentPicker.mouseReleased();return true;}
        if(button==0)for(int i=controls.size()-1;i>=0;i--){var hit=controls.get(i);if(hit.contains(mx,my)){
            if(hit.id.startsWith("config-")&&(my<contentY+35||my>contentY+contentH-9))continue;
            hit.action.run();return true;
        }}
        searching=false;typingName=false;
        if(!category.equals("Configs")&&GuiUtils.isMouseOver(mx,my,contentX,contentY,contentW,contentH)){
            for(var card:visible)if(card.isVisible&&GuiUtils.isMouseOver(mx,my,card.x,card.y,card.width,card.height)){
                if(button==2)for(var other:cards)if(other!=card)other.isBinding=false;
                if(card.mouseClicked(mx,my,button)){draggingCard=card;return true;}
            }
        }
        return true;
    }
    private void updateScrollbar(float mouse){if(scrollbarStart>0)scroll=targetScroll=-Math.max(0,Math.min(1,(mouse-(contentY+6)-scrollbarGrab)/scrollbarStart))*maxScroll;}
    boolean drag(double rawX,double rawY,int button,double dx,double dy){
        float mx=(float)(rawX/scale),my=(float)(rawY/scale);
        if(palette&&accentPicker.isDragging()){accentPicker.update(mx,my);return true;}
        if(draggingScroll){updateScrollbar(my);return true;}
        if(SubSettingsPopupState.mouseDragged(mx,my,button,x,y,w,h))return true;
        return draggingCard!=null&&draggingCard.mouseDragged(mx,my,button,dx/scale,dy/scale);
    }
    boolean release(double rawX,double rawY,int button){
        accentPicker.mouseReleased();for(var card:cards)card.mouseReleased();draggingCard=null;draggingScroll=false;
        SubSettingsPopupState.mouseReleased(rawX/scale,rawY/scale,button);return true;
    }
    boolean scroll(double rawX,double rawY,double amount){
        float mx=(float)(rawX/scale),my=(float)(rawY/scale);
        if(ModePopupState.mouseScrolled(mx,my,amount,x,y,w,h)||SubSettingsPopupState.mouseScrolled(mx,my,amount,x,y,w,h)||MultiSelectPopupState.mouseScrolled(mx,my,amount,x,y,w,h))return true;
        if(modalOpen())return true;
        if(GuiUtils.isMouseOver(mx,my,contentX,contentY,contentW,contentH)){
            if(category.equals("Configs"))configScroll+=(float)amount*27;else targetScroll+=(float)amount*33;
        }return true;
    }
    boolean character(char chr){
        if(chr<' '||chr==127)return false;
        if(searching&&query.length()<64){query+=chr;targetScroll=scroll=0;return true;}
        if(typingName&&newName.length()<40){newName+=chr;return true;}return false;
    }
    boolean key(int key,int scan,int mods){
        if(key==256&&modalOpen()){palette=false;accentPicker.mouseReleased();ModePopupState.close();SubSettingsPopupState.close();MultiSelectPopupState.close();return true;}
        for(var card:cards)if(card.isBinding){card.module.setKeyCode(key==256||key==261||key==259?-1:key);card.isBinding=false;return true;}
        for(var card:cards)if(card.settingsPanel.isListening()){card.keyPressed(key);return true;}
        if((mods&2)!=0&&key==70||key==47&&!typingName&&!searching){searching=true;typingName=false;return true;}
        if(searching||typingName){
            if(key==256){searching=false;typingName=false;return true;}
            if(key==257){if(typingName)createConfig();searching=false;return true;}
            if(key==259){if(searching&&!query.isEmpty()){query=query.substring(0,query.length()-1);targetScroll=scroll=0;}else if(typingName&&!newName.isEmpty())newName=newName.substring(0,newName.length()-1);return true;}
            if((mods&2)!=0&&key==65){if(searching)query="";else newName="";return true;}
            return true;
        }
        if(key==256||key==260||key==344||key==AstolfoclientClient.clickGuiKeyCode){owner.onClose();return true;}
        return false;
    }
}
