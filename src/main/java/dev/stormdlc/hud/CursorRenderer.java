package dev.stormdlc.hud;

import com.top1.client.island.render.IslandRender;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.angames.astolfoclient.client.module.modules.render.CursorModule;

public final class CursorRenderer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger("StormDLC/Cursor");
    private AnimatedGif animation;
    private CursorModule.Style loaded;
    private CursorModule.Style failed;
    private Screen screen;
    private double x;
    private double y;
    private double animationTime;
    private long lastFrame;
    private long clicked;
    private boolean pressed;

    public void render(CursorModule module) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() || client.screen == null || client.mouseHandler.isMouseGrabbed() || client.getOverlay() != null) {
            screen = null;
            lastFrame = 0;
            pressed = false;
            return;
        }
        CursorModule.Style style = module.style.getValue();
        if (loaded != style) {
            closeAnimation();
            if (failed == style) return;
            try { animation = AnimatedGif.load(style.resource()); loaded = style; animationTime = 0; }
            catch (IOException | RuntimeException failure) { failed = style; LOG.warn("Cannot load {} cursor animation", style, failure); }
        }
        if (animation == null) return;
        double mouseX = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();
        long now = System.nanoTime();
        double elapsed = lastFrame == 0 ? 0 : Math.max(0, (now - lastFrame) / 1.0e9);
        double dt = Math.min(elapsed, 0.1);
        lastFrame = now;
        if (screen != client.screen || !module.smoothFollow.get()) { x = mouseX; y = mouseY; }
        else {
            double factor = 1 - Math.exp(-20 * dt);
            x += (mouseX - x) * factor;
            y += (mouseY - y) * factor;
        }
        screen = client.screen;
        animationTime += elapsed * 1000 * module.animationSpeed.get();
        boolean button = GLFW.glfwGetMouseButton(client.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (button && !pressed) clicked = now;
        pressed = button;
        double phase = clicked == 0 ? 1 : Mth.clamp((now - clicked) / 240_000_000.0, 0, 1);
        double pulse = module.clickPulse.get() ? Math.sin(phase * Math.PI) * 0.16 : 0;
        float width = (float) (module.size.get() * (1 + pulse));
        float height = (float) (width * animation.aspectRatio());
        float drawX = (float) Mth.clamp(x - width / 2 + module.offsetX.get(), 1, client.getWindow().getGuiScaledWidth() - width - 1);
        float drawY = (float) Mth.clamp(y + module.offsetY.get(), 1, client.getWindow().getGuiScaledHeight() - height - 1);
        var texture = animation.frame((long) animationTime);
        if (texture != null) IslandRender.drawTexture(texture, drawX, drawY, width, height, 0xffffffff);
    }

    private void closeAnimation() {
        if (animation != null) animation.close();
        animation = null;
        loaded = null;
    }

    @Override public void close() {
        closeAnimation();
        failed = null;
        screen = null;
        lastFrame = clicked = 0;
        animationTime = 0;
        pressed = false;
    }
}
