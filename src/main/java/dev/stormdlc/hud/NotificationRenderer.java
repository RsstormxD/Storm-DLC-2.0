package dev.stormdlc.hud;

import com.top1.client.island.Anim;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class NotificationRenderer {
    private static final class Toast {
        ClientFeedback.Notice notice, previous;
        final Anim visibility = new Anim(300), y = new Anim(380), width = new Anim(320);
        final Anim text = new Anim(220), progress = new Anim(180);
        Toast(ClientFeedback.Notice notice, float bottom) {
            this.notice = notice;
            y.snap(bottom + 8);
            width.snap(142);
            progress.snap(1);
            text.snap(1);
        }
        void update(ClientFeedback.Notice next) {
            if (!notice.title().equals(next.title()) || !notice.message().equals(next.message()) || notice.tone() != next.tone()) {
                previous = notice;
                text.snap(0);
            }
            notice = next;
        }
    }
    private static final Map<String, Toast> TOASTS = new LinkedHashMap<>();
    private static long epoch = Long.MIN_VALUE;

    private NotificationRenderer() {}

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        var settings = ClientFeedback.settings();
        if (settings == null || !settings.notifications.get() || client.player == null || client.options.hideGui) {
            TOASTS.clear();
            return;
        }
        if (epoch != ClientFeedback.epoch()) { TOASTS.clear(); epoch = ClientFeedback.epoch(); }
        float bottom = client.getWindow().getGuiScaledHeight() - 78.0F;
        float right = client.getWindow().getGuiScaledWidth() - 9.0F;
        var active = new HashSet<String>();
        for (var notice : ClientFeedback.notices()) {
            active.add(notice.key());
            Toast toast = TOASTS.get(notice.key());
            if (toast == null) TOASTS.put(notice.key(), new Toast(notice, bottom));
            else if (toast.notice != notice) toast.update(notice);
        }
        TOASTS.entrySet().removeIf(entry -> entry.getValue().notice.age() > entry.getValue().notice.duration() + 1200);
        while (TOASTS.size() > 10) TOASTS.remove(TOASTS.keySet().iterator().next());
        var ordered = new ArrayList<>(TOASTS.values());
        for (int index = ordered.size() - 1; index >= 0; index--) {
            Toast toast = ordered.get(index);
            boolean alive = active.contains(toast.notice.key());
            toast.visibility.update(alive ? 1 : 0);
            float visibility = Mth.clamp(toast.visibility.get(), 0, 1);
            toast.y.update(bottom);
            float targetWidth = Math.min(210, Math.max(142, Math.max(Fonts.medium(7.5F).width(toast.notice.title()),
                Fonts.regular(6).width(toast.notice.message())) + 38));
            toast.width.update(targetWidth);
            toast.text.update(1);
            toast.progress.update(toast.notice.remaining());
            float width = toast.width.get(), y = toast.y.get(), x = right - width + (1 - visibility) * 20;
            if (visibility > .002F) draw(toast, x, y, width, visibility);
            bottom -= 38 * visibility;
        }
        TOASTS.entrySet().removeIf(entry -> !active.contains(entry.getKey()) && entry.getValue().visibility.get() < .003F);
    }

    private static void draw(Toast toast, float x, float y, float width, float visibility) {
        int alpha = Math.round(visibility * 255);
        float blend = Mth.clamp(toast.text.get(), 0, 1);
        int accent = toast.notice.tone().color();
        if (toast.previous != null) accent = mix(toast.previous.tone().color(), accent, blend);
        IslandRender.drawRoundedRect(x - .6F, y - .6F, width + 1.2F, 33.2F, 11, 11, 11, 11,
            color(accent, alpha * .34F));
        IslandRender.drawBlur(x, y, width, 32, 65, 2, 10, 10, 10, 10, color(0xffffffff, alpha));
        IslandRender.drawRoundedRect(x, y, width, 32, 10, 10, 10, 10, color(0xff090b12, alpha * .93F));
        IslandRender.drawRoundedRect(x + 8, y + 10, 3, 12, 1.5F, 1.5F, 1.5F, 1.5F, color(accent, alpha));
        IslandRender.pushScissor(x + 15, y + 3, width - 23, 26);
        try {
            if (toast.previous != null && blend < .998F) {
                IslandRender.pushOpacity(visibility * (1 - blend));
                try { content(toast.previous, x, y - blend * 4, width); }
                finally { IslandRender.popOpacity(); }
            }
            IslandRender.pushOpacity(visibility * blend);
            try { content(toast.notice, x, y + (1 - blend) * 4, width); }
            finally { IslandRender.popOpacity(); }
        } finally { IslandRender.popScissor(); }
        IslandRender.drawRoundedRect(x + 10, y + 30, (width - 20) * Mth.clamp(toast.progress.get(), 0, 1), .9F,
            .45F, .45F, .45F, .45F, color(accent, alpha * .65F));
        if (blend >= .998F) toast.previous = null;
    }

    private static void content(ClientFeedback.Notice notice, float x, float y, float width) {
        IslandRender.drawWindowedText(Fonts.medium(7.5F), notice.title(), x + 17, y + 6, 0xfff5f6fc,
            x + 16, x + width - 8, 5, 0);
        IslandRender.drawWindowedText(Fonts.regular(6), notice.message(), x + 17, y + 18, 0xffaeb5c8,
            x + 16, x + width - 8, 5, 0);
    }

    private static int mix(int a, int b, float t) {
        return 0xff000000 | Math.round(Mth.lerp(t, a >> 16 & 255, b >> 16 & 255)) << 16
            | Math.round(Mth.lerp(t, a >> 8 & 255, b >> 8 & 255)) << 8 | Math.round(Mth.lerp(t, a & 255, b & 255));
    }

    private static int color(int color, float alpha) { return (Math.round(Mth.clamp(alpha, 0, 255)) << 24) | (color & 0xffffff); }
}
