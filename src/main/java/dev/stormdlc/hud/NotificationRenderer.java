package dev.stormdlc.hud;

import com.top1.client.island.font.Font;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class NotificationRenderer {
    private NotificationRenderer() {}

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        var settings = ClientFeedback.settings();
        if (settings == null || !settings.notifications.get() || client.player == null || client.options.hideGui) return;
        List<ClientFeedback.Notice> notices = ClientFeedback.notices();
        Font title = Fonts.medium(7.5F);
        Font text = Fonts.regular(6.0F);
        float bottom = client.getWindow().getGuiScaledHeight() - 45.0F;
        float right = client.getWindow().getGuiScaledWidth() - 8.0F;
        for (int index = notices.size() - 1; index >= 0; index--) {
            var notice = notices.get(index);
            float enter = ease(Math.min(1.0F, notice.age() / 220.0F));
            float leave = ease(Math.min(1.0F, (notice.duration() - notice.age()) / 260.0F));
            float visibility = Math.min(enter, leave);
            float width = Math.min(184.0F, Math.max(138.0F, title.width(notice.title()) + 31.0F));
            float x = right - width + (1.0F - visibility) * (width + 12.0F);
            float y = bottom - 33.0F;
            int alpha = (int) (visibility * 255.0F);
            int accent = color(notice.tone().color(), alpha);
            IslandRender.drawSquircle(x - 1, y - 1, width + 2, 34, 5, 8, 8, 8, 8,
                color(notice.tone().color(), alpha * 0.14F));
            IslandRender.drawBlur(x, y, width, 32, 32, 4, 7, 7, 7, 7, color(0xffffffff, alpha));
            IslandRender.drawSquircle(x, y, width, 32, 5, 7, 7, 7, 7, color(0xff12151c, alpha * 0.94F));
            IslandRender.drawRoundedRect(x + 7, y + 8, 3, 16, 1.5F, 1.5F, 1.5F, 1.5F, accent);
            IslandRender.drawWindowedText(title, notice.title(), x + 16, y + 6, color(0xffffffff, alpha),
                x + 15, x + width - 7, 5, 0);
            IslandRender.drawWindowedText(text, notice.message(), x + 16, y + 18, color(0xffb5c0d0, alpha),
                x + 15, x + width - 7, 5, 0);
            IslandRender.drawRoundedRect(x + 8, y + 29, (width - 16) * notice.remaining(), 1,
                0.5F, 0.5F, 0.5F, 0.5F, color(notice.tone().color(), alpha * 0.7F));
            bottom -= 38;
        }
    }

    private static float ease(float value) { return value * value * (3.0F - 2.0F * value); }
    private static int color(int argb, float alpha) { return ((int) Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xffffff); }
}
