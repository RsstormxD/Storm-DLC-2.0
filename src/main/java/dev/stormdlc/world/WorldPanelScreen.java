package dev.stormdlc.world;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
public final class WorldPanelScreen extends Screen {
    public WorldPanelScreen(){super(Component.literal("Storm DLC 2.0 - 3D panels"));}
    @Override public void render(GuiGraphics c,int x,int y,float delta){
        c.drawCenteredString(font,"3D panels: click to interact | scroll to browse | G / ESC to close",width/2,height-24,0xffd9e8ff);
    }
    @Override public void renderBackground(GuiGraphics c,int x,int y,float delta){}
    @Override public boolean mouseClicked(double x,double y,int button){WorldPanels.click(x,y,button);return true;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){WorldPanels.drag(x,y,button);return true;}
    @Override public boolean mouseReleased(double x,double y,int button){WorldPanels.release(x,y,button);return true;}
    @Override public boolean charTyped(char chr,int mods){return WorldPanels.character(chr,mods);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){WorldPanels.scroll(x,y,vertical);return true;}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_G){onClose();return true;}if(key==256){onClose();return true;}return WorldPanels.key(key,scan,mods) || super.keyPressed(key,scan,mods);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){if(AstolfoclientClient.configManager!=null)AstolfoclientClient.configManager.saveConfig("default");}
}
