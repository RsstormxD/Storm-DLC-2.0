package dev.stormdlc.menu;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class LoadingAnimation {
    private static final String WORD = "StormDLC";
    private static final AtomicLong IDS = new AtomicLong();
    private static final int PADDING = 12;
    private final long started = System.nanoTime();
    private final int[] offsets = new int[WORD.length()], advances = new int[WORD.length()];
    private ResourceLocation texture;
    private int atlasWidth, atlasHeight, wordWidth;
    private float textAlpha = 1;

    public void wordmark(GuiGraphics graphics, float alpha) {
        ensureTexture();
        textAlpha = alpha;
        double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        float scale = Math.min(0.62F, Math.min(graphics.guiWidth() * .58F / wordWidth, graphics.guiHeight() * .14F / atlasHeight));
        float left = (graphics.guiWidth() - wordWidth * scale) / 2;
        float top = graphics.guiHeight() * .46F - atlasHeight * scale / 2;
        float travel = (float) ((seconds * .38) % 1.75 - .35);
        int advance = 0;
        for (int i = 0; i < WORD.length(); i++) {
            float enter = Mth.clamp((float) seconds / .65F - i * .035F, 0, 1);
            enter = 1 - (float) Math.pow(1 - enter, 4);
            float wave = (float) Math.sin(seconds * 2.6 - i * .38) * 1.2F;
            float shine = (float) Math.exp(-Math.pow(i / (float) WORD.length() - travel, 2) * 45);
            int shade = Math.round(195 + 60 * shine);
            int rgb = shade << 16 | shade << 8 | shade;
            float x = left + advance * scale - PADDING * scale;
            float y = top + wave + (1 - enter) * 8;
            int opacity = Math.round(255 * alpha * enter);
            drawGlyph(graphics, i, x - .8F, y, scale, Math.round(opacity * .10F) << 24 | 0xffffff);
            drawGlyph(graphics, i, x + .8F, y, scale, Math.round(opacity * .10F) << 24 | 0xffffff);
            drawGlyph(graphics, i, x, y, scale, opacity << 24 | rgb);
            advance += advances[i];
        }
    }

    public void progress(GuiGraphics graphics, float progress, float alpha) {
        int width = Math.min(190, Math.round(graphics.guiWidth() * .38F));
        int x = (graphics.guiWidth() - width) / 2, y = Math.round(graphics.guiHeight() * .64F);
        int opacity = Math.round(255 * alpha * textAlpha);
        graphics.fill(x, y, x + width, y + 2, Math.round(opacity * .15F) << 24 | 0xffffff);
        int filled = Math.round(width * Mth.clamp(progress, 0, 1));
        if (filled > 0) graphics.fill(x, y, x + filled, y + 2, opacity << 24 | 0xffffff);
        double elapsed = (System.nanoTime() - started) / 1_000_000_000.0;
        for (int i = 0; i < 3; i++) {
            float pulse = .3F + .7F * (float) Math.pow(.5 + .5 * Math.sin(elapsed * 3.5 - i * .65), 2);
            int dot = Math.round(opacity * pulse);
            int dx = graphics.guiWidth() / 2 - 9 + i * 8;
            graphics.fill(dx, y + 16, dx + 2, y + 18, dot << 24 | 0xffffff);
        }
    }

    private void drawGlyph(GuiGraphics graphics, int index, float x, float y, float scale, int color) {
        int glyphWidth = advances[index] + PADDING * 2;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.blit(RenderType::guiTextured, texture, 0, 0, offsets[index], 0,
            glyphWidth, atlasHeight, glyphWidth, atlasHeight, atlasWidth, atlasHeight, color);
        graphics.pose().popPose();
    }

    private void ensureTexture() {
        if (texture != null) return;
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 92);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        var measure = probe.createGraphics();
        measure.setFont(font);
        var metrics = measure.getFontMetrics();
        atlasHeight = metrics.getHeight() + PADDING * 2;
        int baseline = metrics.getAscent() + PADDING;
        for (int i = 0; i < WORD.length(); i++) {
            offsets[i] = atlasWidth;
            advances[i] = metrics.charWidth(WORD.charAt(i)) + 2;
            atlasWidth += advances[i] + PADDING * 2;
            wordWidth += advances[i];
        }
        measure.dispose();
        probe.flush();
        BufferedImage bitmap = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        var painter = bitmap.createGraphics();
        NativeImage image = null;
        DynamicTexture uploaded = null;
        try {
            painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            painter.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            painter.setFont(font);
            painter.setColor(Color.WHITE);
            for (int i = 0; i < WORD.length(); i++)
                painter.drawString(WORD.substring(i, i + 1), offsets[i] + PADDING, baseline);
            image = new NativeImage(atlasWidth, atlasHeight, false);
            for (int y = 0; y < atlasHeight; y++)
                for (int x = 0; x < atlasWidth; x++) image.setPixel(x, y, bitmap.getRGB(x, y));
            uploaded = new DynamicTexture(image);
            image = null;
            uploaded.setFilter(true, false);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("stormdlc", "runtime/loading_" + IDS.incrementAndGet());
            Minecraft.getInstance().getTextureManager().register(id, uploaded);
            texture = id;
            uploaded = null;
        } finally {
            if (uploaded != null) uploaded.close();
            if (image != null) image.close();
            painter.dispose();
            bitmap.flush();
        }
    }

    public void release() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(texture);
        texture = null;
    }
}
