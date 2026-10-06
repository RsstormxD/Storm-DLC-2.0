package dev.stormdlc.hud;

import com.top1.client.island.font.Font;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;

public final class IslandProfile {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private IslandProfile() {}

    private static String name(Minecraft client) {
        return client.player == null ? client.getUser().getName() : client.player.getGameProfile().getName();
    }

    private static ResourceLocation skin(Minecraft client) {
        return client.player == null ? DefaultPlayerSkin.get(client.getUser().getProfileId()).texture()
            : client.player.getSkin().texture();
    }

    public static void drawPill(Minecraft client, float x, float y, float width, float alpha) {
        IslandRender.drawSkinHead(skin(client), x + 9, y + 7, 18, alpha);
        IslandRender.drawWindowedText(Fonts.medium(8), name(client), x + 34, y + 8,
            color(0xfff5f5f7, alpha), x + 33, x + width - 46, 5, 0);
        IslandRender.drawWindowedText(Fonts.regular(5.5F), "Storm DLC 2.0  ·  " + client.getFps() + " FPS",
            x + 34, y + 21, color(0xff9999a4, alpha), x + 33, x + width - 43, 3, 0);
        IslandRender.drawText(Fonts.medium(7), LocalTime.now().format(CLOCK), x + width - 36, y + 9,
            color(0xffededf2, alpha));
        IslandRender.drawText(Fonts.regular(4.5F), client.player == null ? "MENU" : "IN GAME",
            x + width - 34, y + 21, color(0xff75daa8, alpha));
    }

    public static void drawExpanded(Minecraft client, float x, float y, float width, float alpha) {
        var player = client.player;
        Font title = Fonts.medium(10);
        Font text = Fonts.regular(7);
        Font small = Fonts.regular(6);
        IslandRender.drawSkinHead(skin(client), x + 14, y + 6, 28, alpha);
        IslandRender.drawWindowedText(title, name(client), x + 51, y + 9,
            color(0xfff5f5f7, alpha), x + 50, x + width - 14, 5, 0);
        IslandRender.drawText(text, "Storm DLC 2.0", x + 51, y + 25, color(0xffa9bad7, alpha));
        float column = (width - 36) / 3;
        if (player != null) {
            var info = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(player.getUUID());
            String ping = info == null ? "--" : Math.max(0, info.getLatency()) + " ms";
            stat(x + 14, y + 42, column, "HEALTH",
                String.format(Locale.ROOT, "%.1f HP", player.getHealth() + player.getAbsorptionAmount()), alpha);
            stat(x + 18 + column, y + 42, column, "ARMOR", Integer.toString(player.getArmorValue()), alpha);
            stat(x + 22 + 2 * column, y + 42, column, "PING", ping, alpha);
            float health = Mth.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0.0F, 1.0F);
            int healthColor = health <= 0.3F ? 0xffff7b87 : 0xff71e3ae;
            IslandRender.drawRoundedRect(x + 14, y + 70, width - 28, 2, 1, 1, 1, 1, color(0xff27272d, alpha));
            if (health > 0) IslandRender.drawRoundedRect(x + 14, y + 70, (width - 28) * health, 2,
                1, 1, 1, 1, color(healthColor, alpha));
        } else {
            stat(x + 14, y + 42, column, "VERSION", "1.21.4", alpha);
            stat(x + 18 + column, y + 42, column, "FPS", Integer.toString(client.getFps()), alpha);
            stat(x + 22 + 2 * column, y + 42, column, "TIME", LocalTime.now().format(CLOCK), alpha);
        }
        String profile = AstolfoclientClient.configManager == null ? "default" : AstolfoclientClient.configManager.getActiveProfile();
        IslandRender.drawWindowedText(text, "Profile: " + profile, x + 14, y + 80, color(0xffcdced8, alpha),
            x + 13, x + width - 14, 4, 0);
        long seconds = ClientFeedback.sessionSeconds();
        long count = ModuleManager.modules.stream().filter(Module::isEnabled).count();
        String status = player == null ? count + " modules active  ·  Ready to play"
            : String.format(Locale.ROOT, "%02d:%02d  ·  %d FPS  ·  %d active", seconds / 60, seconds % 60, client.getFps(), count);
        IslandRender.drawWindowedText(small, status, x + 14, y + 94, color(0xff8c8c98, alpha),
            x + 13, x + width - 14, 4, 0);
    }

    private static void stat(float x, float y, float width, String label, String value, float alpha) {
        IslandRender.drawRoundedRect(x, y, width, 23, 6, 6, 6, 6, color(0xff1d1d23, alpha));
        IslandRender.drawText(Fonts.regular(5), label, x + 6, y + 4, color(0xff858591, alpha));
        IslandRender.drawWindowedText(Fonts.medium(7), value, x + 6, y + 13, color(0xfff0f0f5, alpha),
            x + 5, x + width - 4, 3, 0);
    }

    private static int color(int rgb, float alpha) {
        return ((int) Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xffffff);
    }
}
