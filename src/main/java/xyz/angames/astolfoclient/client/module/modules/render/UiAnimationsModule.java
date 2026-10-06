package xyz.angames.astolfoclient.client.module.modules.render;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

/** Cosmetic screen transitions; coordinates follow the last displayed frame. */
public final class UiAnimationsModule extends Module {
    public static UiAnimationsModule INSTANCE;
    public final BooleanSetting chat = new BooleanSetting("Animate chat", true);
    public final BooleanSetting inventory = new BooleanSetting("Animate containers", true);
    public final BooleanSetting menus = new BooleanSetting("Animate other menus", false);
    public final ModeSetting style = new ModeSetting("Opening style", "Slide", "Slide", "Zoom", "Slide and zoom");
    public final ModeSetting easing = new ModeSetting("Easing", "Cubic", "Cubic", "Smooth", "Linear");
    public final NumberSetting duration = new NumberSetting("Duration (ms)", 180, 80, 500, 10);
    public final NumberSetting distance = new NumberSetting("Slide distance", 16, 2, 50, 1);
    public final NumberSetting zoom = new NumberSetting("Zoom amount (%)", 4, 1, 12, 1);
    private final Map<Screen, Frame> frames = new WeakHashMap<>();

    public record Transform(float scale, float x, float y) {
        public static final Transform IDENTITY = new Transform(1, 0, 0);
        public double mouseX(double value) { return (value - x) / scale; }
        public double mouseY(double value) { return (value - y) / scale; }
    }
    private static final class Frame {
        final long opened = System.nanoTime();
        Transform displayed = Transform.IDENTITY;
    }
    public UiAnimationsModule() {
        super("UI Animations", "Animated opening of chat, containers and menus", Category.RENDER);
        INSTANCE = this;
        distance.setVisibility(() -> !style.is("Zoom"));
        zoom.setVisibility(() -> !style.is("Slide"));
    }
    @Override public void onDisable() { frames.clear(); }
    public void opened(Screen screen) { if (accepts(screen)) frames.put(screen, new Frame()); }
    private boolean accepts(Screen screen) {
        if (!isEnabled() || screen == null) return false;
        String name = screen.getClass().getName();
        // Storm panels have their own animation and world-space input mapping.
        if (name.startsWith("xyz.angames.") || name.startsWith("dev.stormdlc.")) return false;
        if (screen instanceof ChatScreen) return chat.get();
        if (screen instanceof AbstractContainerScreen<?>) return inventory.get();
        return menus.get() && name.startsWith("net.minecraft.client.gui.screen.");
    }
    public Transform inputTransform(Screen screen) {
        Frame frame = frames.get(screen);
        return accepts(screen) && frame != null ? frame.displayed : Transform.IDENTITY;
    }
    public void render(Screen screen, GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!accepts(screen)) { screen.render(context, mouseX, mouseY, delta); return; }
        Frame frame = frames.computeIfAbsent(screen, key -> new Frame());
        float progress = Math.min(1f, (System.nanoTime() - frame.opened) / (duration.getFloat() * 1_000_000f));
        float eased = switch (easing.get()) {
            case "Linear" -> progress;
            case "Smooth" -> progress * progress * (3 - 2 * progress);
            default -> 1 - (float)Math.pow(1 - progress, 3);
        };
        float remaining = 1 - eased;
        boolean isChat = screen instanceof ChatScreen;
        float scale = style.is("Slide") || isChat ? 1 : 1 - zoom.getFloat() / 100f * remaining;
        float slide = style.is("Zoom") && !isChat ? 0 : distance.getFloat() * remaining;
        Transform transform = new Transform(scale, screen.width * (1 - scale) / 2, screen.height * (1 - scale) / 2 + slide);
        frame.displayed = transform;
        context.pose().pushPose();
        try {
            context.pose().translate(transform.x(), transform.y(), 0);
            context.pose().scale(scale, scale, 1);
            screen.render(context, (int)Math.floor(transform.mouseX(mouseX)), (int)Math.floor(transform.mouseY(mouseY)), delta);
        } finally {
            context.pose().popPose();
        }
    }
}
