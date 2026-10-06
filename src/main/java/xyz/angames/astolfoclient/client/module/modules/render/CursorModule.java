package xyz.angames.astolfoclient.client.module.modules.render;

import dev.stormdlc.hud.CursorRenderer;
import net.minecraft.resources.ResourceLocation;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

public final class CursorModule extends Module {
    public enum Style {
        DUCK("Duck", "duck.gif"), BAT("Bat", "bat.gif");
        private final String label;
        private final ResourceLocation resource;
        Style(String label, String filename) {
            this.label = label;
            resource = ResourceLocation.fromNamespaceAndPath("stormdlc", "cursor/" + filename);
        }
        public ResourceLocation resource() { return resource; }
        @Override public String toString() { return label; }
    }

    public final EnumSetting<Style> style = new EnumSetting<>("Style", Style.DUCK);
    public final NumberSetting size = new NumberSetting("Size", 32, 12, 72, 2);
    public final NumberSetting offsetX = new NumberSetting("Offset X", 0, -48, 48, 1);
    public final NumberSetting offsetY = new NumberSetting("Offset Y", 6, -48, 72, 1);
    public final NumberSetting animationSpeed = new NumberSetting("Animation Speed", 1, 0.25, 3, 0.25);
    public final BooleanSetting smoothFollow = new BooleanSetting("Smooth Follow", false);
    public final BooleanSetting clickPulse = new BooleanSetting("Click Pulse", true);
    private final CursorRenderer renderer = new CursorRenderer();

    public CursorModule() {
        super("Cursor", "Animated duck or bat below the mouse cursor", Category.RENDER);
        addSettings(style, size, offsetX, offsetY, animationSpeed, smoothFollow, clickPulse);
    }

    public void renderCursor() { renderer.render(this); }
    public void reloadGraphics() { renderer.close(); }
    @Override public void onDisable() { renderer.close(); }
}
