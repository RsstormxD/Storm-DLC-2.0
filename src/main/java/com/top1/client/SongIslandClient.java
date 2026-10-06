package com.top1.client;
import com.top1.client.island.*;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import com.top1.client.music.MusicTracker;
import dev.stormdlc.render.LegacyRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import xyz.angames.astolfoclient.client.module.modules.render.SongIslandModule;
public final class SongIslandClient implements ClientModInitializer {
    private static MusicIsland island; private static MusicTracker tracker; private static boolean ready, stopping;
    public static MusicIsland island(){return island;}
    public static MusicTracker tracker(){return tracker;}
    public static boolean enabled(){return SongIslandModule.INSTANCE!=null && SongIslandModule.INSTANCE.isEnabled();}
    public static void fonts(){if(!ready){Fonts.init();ready=true;}}
    @Override public void onInitializeClient(){
        tracker=new MusicTracker();island=new MusicIsland(tracker);
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener(){
            public net.minecraft.resources.ResourceLocation getFabricId(){return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("stormdlc","render_resources");}
            public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager){ready=false;net.minecraft.client.Minecraft.getInstance().execute(()->{
                LegacyRenderer.reload();
                var cursor=(xyz.angames.astolfoclient.client.module.modules.render.CursorModule)xyz.angames.astolfoclient.client.module.ModuleManager.getModule(xyz.angames.astolfoclient.client.module.modules.render.CursorModule.class);
                if(cursor!=null)cursor.reloadGraphics();
                dev.stormdlc.menu.MenuWallpaper.refresh();
            });}
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->{stopping=true;dev.stormdlc.menu.MenuWallpaper.shutdown();LegacyRenderer.shutdown();tracker.shutdown();});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{WorldLyrics.reset();if(island!=null)island.reset();dev.stormdlc.world.WorldPanels.reset();});
        WorldRenderEvents.LAST.register(context->{
            if(stopping)return;
            fonts();dev.stormdlc.world.WorldPanels.render(context);
            if(enabled() && (IslandSettings.worldText || IslandSettings.skyText)){fonts();WorldLyrics.capture(context);WorldLyrics.render(tracker);}
        });
    }
    public static void renderIsland(){
        if(island==null || stopping)return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.getOverlay()!=null || mc.getResourceManager().getResource(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("song-island","fonts/msdf/medium.json")).isEmpty())return;
        fonts();
        if(enabled())island.render();
        dev.stormdlc.hud.NotificationRenderer.render();
        var cursor=(xyz.angames.astolfoclient.client.module.modules.render.CursorModule)xyz.angames.astolfoclient.client.module.ModuleManager.getModule(xyz.angames.astolfoclient.client.module.modules.render.CursorModule.class);
        if(cursor!=null && cursor.isEnabled())cursor.renderCursor();
        IslandRender.flush();
    }
}
