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
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH);

    private MainMenuRenderer() {}

    public static boolean enabled() {
        Minecraft client = Minecraft.getInstance();
        return MainMenuModule.INSTANCE != null && MainMenuModule.INSTANCE.isEnabled()
            && client.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(
                "song-island", "fonts/msdf/medium.json")).isPresent();
    }

    public static void background(GuiGraphics graphics, int width, int height, boolean controls) {
        graphics.flush();
        MenuWallpaper.Texture wallpaper = MenuWallpaper.texture();
        float scale = Math.max(width / (float) wallpaper.width(), height / (float) wallpaper.height());
        float imageWidth = wallpaper.width() * scale, imageHeight = wallpaper.height() * scale;
        IslandRender.drawTexture(wallpaper.id(), (width - imageWidth) / 2, (height - imageHeight) / 2,
            imageWidth, imageHeight, 0xffffffff);
        IslandRender.flush();
        MainMenuModule settings = MainMenuModule.INSTANCE;
        int opacity = (int) Math.round((settings == null ? 32 : settings.dimming.get()) * 2.55);
        graphics.fill(0, 0, width, height, opacity << 24);
        graphics.fillGradient(0, 0, width, height, 0x30000610, 0x7000050c);
        graphics.flush();
        if (controls) {
            float y = height / 4.0F + 40;
            IslandRender.drawRoundedRect(width / 2.0F - 112, y, 224, 116,
                18, 18, 18, 18, 0x85060810);
            IslandRender.flush();
        }
    }

    public static void brand(GuiGraphics graphics, int width, int height, float alpha) {
        graphics.flush();
        SongIslandClient.fonts();
        float y = Math.max(51, height / 4.0F - 32);
        float center = width / 2.0F;
        IslandRender.drawRoundedRect(center - 113, y - 10, 226, 59, 18, 18, 18, 18, tint(0x99000000, alpha));
        IslandRender.drawCenteredText(Fonts.medium(32), "StormDLC", center, y, tint(0xffffffff, alpha));
        String subtitle = "2.0  ·  Minecraft 1.21.4";
        if (width < 440 && MainMenuModule.INSTANCE.clock.get())
            subtitle += "  ·  " + LocalDateTime.now().format(TIME);
        IslandRender.drawCenteredText(Fonts.regular(7.5F), subtitle, center, y + 36, tint(0xffc5c5d1, alpha));
        IslandRender.flush();
    }

    public static void clock(GuiGraphics graphics, int width, float alpha) {
        if (!MainMenuModule.INSTANCE.clock.get() || width < 440) return;
        graphics.flush();
        SongIslandClient.fonts();
        LocalDateTime time = LocalDateTime.now();
        float x = width - 94;
        IslandRender.drawRoundedRect(x, 8, 84, 37, 13, 13, 13, 13, tint(0xbc050509, alpha));
        IslandRender.drawCenteredText(Fonts.medium(16), time.format(TIME), x + 42, 14, tint(0xfff6f6fa, alpha));
        IslandRender.drawCenteredText(Fonts.regular(5.8F), time.format(DATE), x + 42, 33, tint(0xffa4a4b0, alpha));
        IslandRender.flush();
    }

    private static int tint(int color, float alpha) {
        return (Math.round((color >>> 24) * Math.max(0, Math.min(1, alpha))) << 24) | (color & 0xffffff);
    }
}
