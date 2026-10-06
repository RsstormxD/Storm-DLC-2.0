package xyz.angames.astolfoclient.client.gui;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

public final class ConfigImportScreen extends Screen {
    private static final AtomicBoolean DIALOG_OPEN = new AtomicBoolean();
    private final Screen parent;
    private final Consumer<String> imported;
    private boolean autoBrowse;
    private EditBox path;
    private String status="Choose a JSON configuration or drop a file here.";
    private boolean error;
    public ConfigImportScreen(Screen parent, Consumer<String> imported) { this(parent, imported, true); }
    public ConfigImportScreen(Screen parent, Consumer<String> imported, boolean autoBrowse) {
        super(Component.literal("Import configuration")); this.parent=parent; this.imported=imported; this.autoBrowse=autoBrowse;
    }
    public static Path configFolder() throws java.io.IOException {
        return Files.createDirectories(dev.stormdlc.config.ClientPaths.configDirectory()
            .resolve("configs")).toAbsolutePath();
    }
    @Override protected void init(){
        String previous=path==null?"":path.getValue();
        path=new EditBox(font,width/2-170,height/2-35,340,20,Component.literal("JSON file path"));
        path.setMaxLength(4096);path.setValue(previous);addRenderableWidget(path);setInitialFocus(path);
        addRenderableWidget(Button.builder(Component.literal("Choose file..."),b->browse()).bounds(width/2-170,height/2-10,165,20).build());
        addRenderableWidget(Button.builder(Component.literal("Open configs folder"),b->{
            try { Util.getPlatform().openPath(configFolder()); } catch(Exception e) { fail(e); }
        }).bounds(width/2+5,height/2-10,165,20).build());
        addRenderableWidget(Button.builder(Component.literal("Import path"),b->importInput(path.getValue())).bounds(width/2-170,height/2+15,165,20).build());
        addRenderableWidget(Button.builder(Component.literal("Import clipboard"),b->importInput(minecraft.keyboardHandler.getClipboard())).bounds(width/2+5,height/2+15,165,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(width/2-80,height/2+90,160,20).build());
    }
    @Override public void tick() {
        super.tick();
        if(autoBrowse) { autoBrowse=false; browse(); }
    }
    private void browse() {
        if(!DIALOG_OPEN.compareAndSet(false,true)) { status="A file selection window is already open."; return; }
        final String folder;
        try { folder=configFolder().toString()+java.io.File.separator; }
        catch(Exception e) { DIALOG_OPEN.set(false); fail(e); return; }
        status="Choose a JSON file in the file selection window."; error=false;
        // The blocking native dialog runs off the game/render thread.
        Thread worker=new Thread(()->{
            String chosen=null; Throwable failure=null;
            try(MemoryStack stack=MemoryStack.stackPush()) {
                chosen=TinyFileDialogs.tinyfd_openFileDialog("Storm DLC 2.0 - import config", folder,
                    stack.pointers(stack.UTF8("*.json")), "JSON configuration", false);
            } catch(Throwable t) { failure=t; }
            finally { DIALOG_OPEN.set(false); }
            final String result=chosen; final Throwable problem=failure;
            minecraft.execute(()->{
                if(minecraft.screen!=this) return;
                if(problem!=null) { fail(problem); return; }
                if(result==null) { onClose(); return; }
                path.setValue(result); importInput(result);
            });
        },"Storm-config-picker");
        worker.setDaemon(true);worker.start();
    }
    private void fail(Throwable e) { error=true;status=e.getMessage()==null?"Cannot open file picker; use a path or drop a JSON file.":e.getMessage(); }
    private void importInput(String input){
        try{String name=AstolfoclientClient.configManager.importConfig(input);imported.accept(name);minecraft.setScreen(parent);}
        catch(Exception e){fail(e);}
    }
    @Override public void onFilesDrop(List<Path> files){if(files.size()==1)importInput(files.getFirst().toString());else{error=true;status="Drop one config file at a time.";}}
    @Override public void render(GuiGraphics c,int x,int y,float delta){
        renderBackground(c,x,y,delta);super.render(c,x,y,delta);
        c.drawCenteredString(font,title,width/2,height/2-75,0xff85bbff);
        c.drawCenteredString(font,font.plainSubstrByWidth(status,width-24),width/2,height/2+48,error?0xffff8080:0xffbbbbbb);
        c.drawCenteredString(font,"Imported profiles appear in Configs. Select Load to apply.",width/2,height/2+64,0xffbbbbbb);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
