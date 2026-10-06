package dev.stormdlc.hud;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public final class AnimatedGif implements AutoCloseable {
    private static final AtomicInteger ids = new AtomicInteger();
    private static final Logger LOG = LoggerFactory.getLogger("StormDLC/Cursor");
    private record Frame(ResourceLocation texture, long end) {}
    private record FrameInfo(int x, int y, int width, int height, int delay, String disposal, boolean transparent) {}
    private final List<Frame> frames = new ArrayList<>();
    private final int width;
    private final int height;
    private long duration;

    private AnimatedGif(int width, int height) { this.width = width; this.height = height; }
    public double aspectRatio() { return height / (double) width; }

    public ResourceLocation frame(long elapsed) {
        if (frames.isEmpty()) return null;
        long phase = Math.floorMod(elapsed, duration);
        for (Frame frame : frames) if (phase < frame.end()) return frame.texture();
        return frames.getLast().texture();
    }

    public static AnimatedGif load(ResourceLocation resource) throws IOException {
        Minecraft client = Minecraft.getInstance();
        var readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) throw new IOException("GIF decoder is unavailable");
        ImageReader reader = readers.next();
        AnimatedGif animation = null;
        try (InputStream input = client.getResourceManager().getResource(resource)
                .orElseThrow(() -> new IOException("Missing cursor animation: " + resource)).open();
             var imageInput = new MemoryCacheImageInputStream(input)) {
            reader.setInput(imageInput, false, false);
            Node stream = tree(reader.getStreamMetadata());
            Node descriptor = child(stream, "LogicalScreenDescriptor");
            int width = integer(descriptor, "logicalScreenWidth", reader.getWidth(0));
            int height = integer(descriptor, "logicalScreenHeight", reader.getHeight(0));
            int count = reader.getNumImages(true);
            if (width < 1 || height < 1 || width > 1024 || height > 1024 || count < 1 || count > 256
                || (long) width * height * count > 16_777_216L) throw new IOException("Cursor animation exceeds image limits");
            animation = new AnimatedGif(width, height);
            int background = background(stream);
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            BufferedImage restore = null;
            FrameInfo previous = null;
            int registration = ids.incrementAndGet();
            boolean transparentCanvas = false;
            for (int index = 0; index < count; index++) {
                Node metadata = tree(reader.getImageMetadata(index));
                FrameInfo info = frameInfo(metadata, reader.getWidth(index), reader.getHeight(index));
                if (index == 0) {
                    transparentCanvas = info.transparent();
                    clear(canvas, 0, 0, width, height, transparentCanvas ? 0 : background);
                } else if (previous != null) {
                    if (previous.disposal().equals("restoreToBackgroundColor"))
                        clear(canvas, previous.x(), previous.y(), previous.width(), previous.height(), transparentCanvas ? 0 : background);
                    else if (previous.disposal().equals("restoreToPrevious") && restore != null) canvas = restore;
                }
                restore = info.disposal().equals("restoreToPrevious") ? copy(canvas) : null;
                BufferedImage decoded = reader.read(index);
                Graphics2D graphics = canvas.createGraphics();
                try { graphics.setComposite(AlphaComposite.SrcOver); graphics.drawImage(decoded, info.x(), info.y(), null); }
                finally { graphics.dispose(); }
                ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath("stormdlc", "cursor/runtime/" + registration + "/" + index);
                animation.upload(client, textureId, canvas, info.delay());
                previous = info;
            }
            return animation;
        } catch (IOException | RuntimeException | Error failure) {
            if (animation != null) animation.close();
            throw failure;
        } finally { reader.dispose(); }
    }

    private void upload(Minecraft client, ResourceLocation id, BufferedImage canvas, int delay) {
        NativeImage image = new NativeImage(width, height, false);
        boolean ownedByTexture = false;
        try {
            int[] pixels = canvas.getRGB(0, 0, width, height, null, 0, width);
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setPixel(x, y, pixels[y * width + x]);
            DynamicTexture texture = new DynamicTexture(image);
            ownedByTexture = true;
            try {
                texture.setFilter(false, false);
                client.getTextureManager().register(id, texture);
            } catch (RuntimeException | Error failure) { texture.close(); throw failure; }
            duration += delay;
            frames.add(new Frame(id, duration));
        } finally { if (!ownedByTexture) image.close(); }
    }

    private static FrameInfo frameInfo(Node root, int width, int height) {
        Node image = child(root, "ImageDescriptor"), control = child(root, "GraphicControlExtension");
        int delay = integer(control, "delayTime", 10);
        delay = delay <= 0 ? 100 : Math.max(20, Math.min(65535, delay) * 10);
        return new FrameInfo(integer(image, "imageLeftPosition", 0), integer(image, "imageTopPosition", 0),
            integer(image, "imageWidth", width), integer(image, "imageHeight", height), delay,
            attribute(control, "disposalMethod", "none"), attribute(control, "transparentColorFlag", "FALSE").equalsIgnoreCase("TRUE"));
    }

    private static int background(Node stream) {
        Node table = child(stream, "GlobalColorTable");
        int index = integer(table, "backgroundColorIndex", 0);
        if (table != null) for (Node item = table.getFirstChild(); item != null; item = item.getNextSibling()) {
            if (item.getNodeName().equals("ColorTableEntry") && integer(item, "index", -1) == index)
                return 0xff000000 | integer(item, "red", 0) << 16 | integer(item, "green", 0) << 8 | integer(item, "blue", 0);
        }
        return 0;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try { graphics.setComposite(AlphaComposite.Src); graphics.drawImage(source, 0, 0, null); }
        finally { graphics.dispose(); }
        return result;
    }

    private static void clear(BufferedImage image, int x, int y, int width, int height, int argb) {
        Graphics2D graphics = image.createGraphics();
        try { graphics.setComposite(AlphaComposite.Src); graphics.setColor(new Color(argb, true)); graphics.fillRect(x, y, width, height); }
        finally { graphics.dispose(); }
    }

    private static Node tree(IIOMetadata metadata) {
        return metadata == null || metadata.getNativeMetadataFormatName() == null ? null
            : metadata.getAsTree(metadata.getNativeMetadataFormatName());
    }
    private static Node child(Node root, String name) {
        if (root != null) for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
            if (node.getNodeName().equals(name)) return node;
        return null;
    }
    private static String attribute(Node node, String name, String fallback) {
        Node value = node == null || node.getAttributes() == null ? null : node.getAttributes().getNamedItem(name);
        return value == null ? fallback : value.getNodeValue();
    }
    private static int integer(Node node, String name, int fallback) {
        try { return Integer.parseInt(attribute(node, name, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    @Override public void close() {
        for (Frame frame : frames) {
            try { Minecraft.getInstance().getTextureManager().release(frame.texture()); }
            catch (RuntimeException failure) { LOG.warn("Cannot release cursor frame {}", frame.texture(), failure); }
        }
        frames.clear();
        duration = 0;
    }
}
