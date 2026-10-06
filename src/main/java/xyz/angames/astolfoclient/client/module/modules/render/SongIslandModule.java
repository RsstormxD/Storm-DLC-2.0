package xyz.angames.astolfoclient.client.module.modules.render;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;
import com.top1.client.SongIslandClient;
import com.top1.client.island.*;
import net.minecraft.client.Minecraft;

public final class SongIslandModule extends Module {
    public static SongIslandModule INSTANCE;
    public final BooleanSetting showMenus = new BooleanSetting("Show in menus", true);
    public final BooleanSetting worldText = new BooleanSetting("Lyrics 3D",false) {
        @Override public boolean isEnabled(){return IslandSettings.worldText;}
        @Override public boolean get(){return isEnabled();}
        @Override public void setEnabled(boolean b){IslandSettings.worldText=b;IslandSettings.save();WorldLyrics.reset();}
        @Override public void set(boolean b){setEnabled(b);}
        @Override public void toggle(){setEnabled(!isEnabled());}
    };
        public final BooleanSetting skyText = new BooleanSetting("Text in sky",false) {
        @Override public boolean isEnabled(){return IslandSettings.skyText;}
        @Override public boolean get(){return isEnabled();}
        @Override public void setEnabled(boolean b){IslandSettings.skyText=b;IslandSettings.save();WorldLyrics.reset();}
        @Override public void set(boolean b){setEnabled(b);}
        @Override public void toggle(){setEnabled(!isEnabled());}
    };
    public final BooleanSetting lock3D = new BooleanSetting("3D lock",true) {
        @Override public boolean isEnabled(){return IslandSettings.lock3D;}
        @Override public boolean get(){return isEnabled();}
        @Override public void setEnabled(boolean b){IslandSettings.lock3D=b;IslandSettings.save();WorldLyrics.reset();}
        @Override public void set(boolean b){setEnabled(b);}
        @Override public void toggle(){setEnabled(!isEnabled());}
    };
    public final ActionSetting placeText = new ActionSetting("Place text in front",WorldLyrics::reset);
    public final BooleanSetting hideBossbar = new BooleanSetting("Hide bossbar",false) {
        @Override public boolean isEnabled(){return IslandSettings.hideBossbar;}
        @Override public boolean get(){return isEnabled();}
        @Override public void setEnabled(boolean b){IslandSettings.hideBossbar=b;IslandSettings.save();}
        @Override public void set(boolean b){setEnabled(b);}
        @Override public void toggle(){setEnabled(!isEnabled());}
    };
    public final ActionSetting move = new ActionSetting("Move island",()->{
        if(SongIslandClient.island()!=null)Minecraft.getInstance().setScreen(new DragScreen(SongIslandClient.island()));
    });
    public final ActionSetting reset = new ActionSetting("Reset position",IslandSettings::resetPosition);
    public final ActionSetting earlier = new ActionSetting("Lyrics -0.1s",()->{if(SongIslandClient.tracker()!=null)SongIslandClient.tracker().adjustLyricsOffset(-.1f);});
    public final ActionSetting later = new ActionSetting("Lyrics +0.1s",()->{if(SongIslandClient.tracker()!=null)SongIslandClient.tracker().adjustLyricsOffset(.1f);});
    public final ActionSetting map = new ActionSetting("Map lyrics",()->{
        if(SongIslandClient.tracker()!=null){var screen=MapperScreen.create(SongIslandClient.tracker());if(screen!=null)Minecraft.getInstance().setScreen(screen);}
    });
    public final ActionSetting forget = new ActionSetting("Forget lyric mapping",()->{if(SongIslandClient.tracker()!=null)SongIslandClient.tracker().forgetMapping();});
    public SongIslandModule(){super("Dynamic Island","Player profile, synced lyrics, media controls and live reactions",Category.RENDER);INSTANCE=this;IslandSettings.load();setEnabled(true);}
    @Override public void onDisable(){WorldLyrics.reset();if(SongIslandClient.island()!=null)SongIslandClient.island().reset();}
}
