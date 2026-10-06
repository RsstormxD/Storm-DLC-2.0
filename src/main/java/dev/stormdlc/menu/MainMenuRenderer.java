package dev.stormdlc.menu;

import com.top1.client.SongIslandClient;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule;

public final class MainMenuRenderer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.ENGLISH);

    private MainMenuRenderer() {}

    public static boolean enabled() {
        Minecraft client = Minecraft.getInstance();
        return MainMenuModule.INSTANCE != null && MainMenuModule.INSTANCE.isEnabled()
            && client.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(
                "song-island", "fonts/msdf/medium.json")).isPresent();
    }

    public static int accent() {
        if (MainMenuModule.INSTANCE == null) return 0xffeeeeff;
        return switch (MainMenuModule.INSTANCE.wallpaper.getValue()) {
            case THEME_2 -> 0xffdfeaff;
            case THEME_3 -> 0xffede3ff;
            default -> 0xfff0f0f4;
        };
    }

    public static void background(GuiGraphics graphics, int width, int height, boolean controls) {
        graphics.flush();
        MenuWallpaper.Texture wallpaper = MenuWallpaper.texture();
        MenuWallpaper.Texture previous = MenuWallpaper.previous();
        float blend = MenuWallpaper.blend();
        if (previous != null && blend < 1) drawWallpaper(previous, width, height, 0xffffffff);
        drawWallpaper(wallpaper, width, height, previous == null ? 0xffffffff : tint(0xffffffff, blend));
        IslandRender.flush();
        MainMenuModule settings = MainMenuModule.INSTANCE;
        int opacity = (int) Math.round((settings == null ? 22 : settings.dimming.get()) * 2.55);
        graphics.fill(0, 0, width, height, opacity << 24);
        graphics.fillGradient(0, 0, width, height, 0x12000000, 0x65030408);
        graphics.flush();
    }

    private static void drawWallpaper(MenuWallpaper.Texture wallpaper, int width, int height, int color) {
        float scale = Math.max(width / (float) wallpaper.width(), height / (float) wallpaper.height());
        float imageWidth = wallpaper.width() * scale, imageHeight = wallpaper.height() * scale;
        IslandRender.drawTexture(wallpaper.id(), (width - imageWidth) / 2, (height - imageHeight) / 2,
            imageWidth, imageHeight, color);
    }

    public static void clock(GuiGraphics graphics, int width, int height, float alpha) {
        if (!MainMenuModule.INSTANCE.clock.get()) return;
        graphics.flush();
        SongIslandClient.fonts();
        LocalDateTime time = LocalDateTime.now();
        float fontSize = Math.max(30, Math.min(46, height * .11F));
        float controlsY = MenuLayout.controlsY();
        float y = Math.max(47, Math.min(height * .22F, controlsY - fontSize - 17));
        float center = width / 2.0F;
        String clock = time.format(TIME);
        IslandRender.drawCenteredText(Fonts.medium(fontSize), clock, center + .5F, y + 1, tint(0x8c000000, alpha));
        IslandRender.drawCenteredText(Fonts.medium(fontSize), clock, center, y, tint(0xfffcfcff, alpha));
        IslandRender.drawCenteredText(Fonts.regular(7), time.format(DATE), center, y + fontSize + 7,
            tint(0xffdedee8, alpha));
        IslandRender.flush();
    }

    public static void save() {
        var manager = xyz.angames.astolfoclient.client.AstolfoclientClient.configManager;
        if (manager != null) manager.saveConfig(manager.getActiveProfile());
    }

    public static int tint(int color, float alpha) {
        return (Math.round((color >>> 24) * Math.max(0, Math.min(1, alpha))) << 24) | (color & 0xffffff);
    }
}
