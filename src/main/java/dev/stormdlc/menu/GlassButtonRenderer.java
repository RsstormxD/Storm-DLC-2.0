package dev.stormdlc.menu;

import com.top1.client.SongIslandClient;
import com.top1.client.island.Anim;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.lwjgl.glfw.GLFW;
import xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule;

public final class GlassButtonRenderer {
    public enum Icon { NONE, LANGUAGE, ACCESSIBILITY, APPEARANCE }
    private static final class Motion {
        final Anim hover = new Anim(240), press = new Anim(150);
        Icon icon = Icon.NONE;
        int theme;
    }
    private static final Map<AbstractWidget, Motion> MOTIONS = new WeakHashMap<>();

    private GlassButtonRenderer() {}

    public static void icon(AbstractWidget widget, Icon icon) {
        MOTIONS.computeIfAbsent(widget, ignored -> new Motion()).icon = icon;
    }

    public static void theme(AbstractWidget widget, int theme) {
        MOTIONS.computeIfAbsent(widget, ignored -> new Motion()).theme = theme;
    }

    public static void render(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        graphics.flush();
        SongIslandClient.fonts();
        Motion motion = MOTIONS.computeIfAbsent(widget, ignored -> new Motion());
        var window = Minecraft.getInstance().getWindow();
        boolean selected = motion.theme > 0 && MainMenuModule.INSTANCE.wallpaper.getValue().ordinal() == motion.theme - 1;
        motion.hover.update(widget.active && widget.isHoveredOrFocused() ? 1 : selected ? .55F : 0);
        motion.press.update(widget.active && widget.isHovered()
            && GLFW.glfwGetMouseButton(window.getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS ? 1 : 0);
        float hover = motion.hover.get(), press = motion.press.get();
        float inset = press * .8F;
        float x = widget.getX() + inset, y = widget.getY() - hover * .6F + inset;
        float width = widget.getWidth() - inset * 2, height = widget.getHeight() - inset * 2;
        int tint = MainMenuRenderer.accent();
        int opacity = Math.round(Math.max(0, Math.min(1, alpha)) * (widget.active ? 255 : 145));
        IslandRender.drawGlass(x, y, width, height, Math.min(12, height / 2), hover, press,
            (mouseX - x) / Math.max(1, width), (mouseY - y) / Math.max(1, height), (opacity << 24) | (tint & 0xffffff));
        int textColor = (opacity << 24) | (widget.active ? 0xf8f9fc : 0x989ba5);
        if (motion.icon != Icon.NONE) drawIcon(motion.icon, x + width / 2, y + height / 2, textColor);
        else {
            var font = Fonts.medium(height >= 26 ? 8.5F : 7.5F);
            String label = widget.getMessage().getString();
            float tx = x + Math.max(8, (width - font.width(label)) / 2);
            IslandRender.drawWindowedText(font, label, tx, y + (height - font.getSize()) / 2,
                textColor, x + 7, x + width - 7, 3, 0);
        }
        IslandRender.flush();
    }

    private static void drawIcon(Icon icon, float x, float y, int color) {
        if (icon == Icon.LANGUAGE) {
            IslandRender.drawRoundedRect(x - 6, y - 6, 12, 12, 6, 6, 6, 6, color);
            IslandRender.drawRoundedRect(x - 5, y - 5, 10, 10, 5, 5, 5, 5, 0xff16191f);
            IslandRender.drawRoundedRect(x - 2, y - 5, 4, 10, 2, 2, 2, 2, color);
            IslandRender.drawRoundedRect(x - 1, y - 4, 2, 8, 1, 1, 1, 1, 0xff16191f);
            IslandRender.drawRoundedRect(x - 5, y - .5F, 10, 1, .5F, .5F, .5F, .5F, color);
        } else if (icon == Icon.APPEARANCE) {
            IslandRender.drawRoundedRect(x - 5, y - 5, 10, 10, 3, 3, 3, 3, color);
            IslandRender.drawRoundedRect(x - 3.5F, y - 3.5F, 7, 7, 2, 2, 2, 2, 0xff16191f);
            IslandRender.drawRoundedRect(x - 1.4F, y - 1.4F, 2.8F, 2.8F, 1.4F, 1.4F, 1.4F, 1.4F, color);
        } else {
            IslandRender.drawRoundedRect(x - 1.7F, y - 6.5F, 3.4F, 3.4F, 1.7F, 1.7F, 1.7F, 1.7F, color);
            IslandRender.drawRoundedRect(x - 5, y - 1.5F, 10, 1.7F, .8F, .8F, .8F, .8F, color);
            IslandRender.drawRoundedRect(x - 1.2F, y - 2, 2.4F, 7, 1, 1, 1, 1, color);
            IslandRender.drawRoundedRect(x - 3, y + 3, 2, 3.5F, 1, 1, 1, 1, color);
            IslandRender.drawRoundedRect(x + 1, y + 3, 2, 3.5F, 1, 1, 1, 1, color);
        }
    }
}
