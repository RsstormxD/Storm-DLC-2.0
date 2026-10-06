package xyz.angames.astolfoclient.client.module.modules.render;

import dev.stormdlc.menu.MenuWallpaper;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ActionSetting;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

public final class MainMenuModule extends Module {
    public enum Wallpaper {
        DESKTOP("Windows wallpaper"), CUSTOM("Custom image"), BUNDLED("Storm wallpaper");
        private final String label;
        Wallpaper(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public static MainMenuModule INSTANCE;
    public final EnumSetting<Wallpaper> wallpaper = new EnumSetting<>("Background", Wallpaper.DESKTOP);
    public final BooleanSetting clock = new BooleanSetting("Clock", true);
    public final NumberSetting dimming = new NumberSetting("Background dimming", 32, 0, 80, 1);
    public final ActionSetting reload = new ActionSetting("Reload wallpaper", MenuWallpaper::refresh);

    public MainMenuModule() {
        super("Main Menu", "Personal wallpaper, StormDLC branding and a live clock", Category.RENDER);
        INSTANCE = this;
        addSettings(wallpaper, clock, dimming, reload);
        setEnabled(true);
    }

    @Override public void onEnable() { MenuWallpaper.refresh(); }
    @Override public void onDisable() { MenuWallpaper.release(); }
}
