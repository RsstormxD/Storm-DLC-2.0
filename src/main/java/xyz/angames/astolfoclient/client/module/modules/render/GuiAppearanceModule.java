package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;

public final class GuiAppearanceModule extends Module {
    public static GuiAppearanceModule INSTANCE;

    public final ModeSetting version = new ModeSetting("GUI version", "Old", "Old", "New");
    public final ModeSetting theme = new ModeSetting("Theme", "Liquid Glass",
            "Liquid Glass", "Midnight", "AMOLED", "Aurora", "Carbon", "Slate");
    public final ModeSetting layout = new ModeSetting("Layout", "Grid", "Grid", "List");
    public final ModeSetting density = new ModeSetting("Card density", "Comfortable",
            "Compact", "Comfortable", "Spacious");
    public final NumberSetting opacity = new NumberSetting("Panel opacity", 86, 25, 100, 1);
    public final NumberSetting cardOpacity = new NumberSetting("Card opacity", 91, 35, 100, 1);
    public final NumberSetting layerDepth = new NumberSetting("Layer depth", 70, 0, 100, 5);
    public final NumberSetting shadowStrength = new NumberSetting("Soft shadows", 70, 0, 100, 5);
    public final NumberSetting borderStrength = new NumberSetting("Border intensity", 65, 0, 100, 5);
    public final NumberSetting accentGlow = new NumberSetting("Active card glow", 35, 0, 100, 5);
    public final NumberSetting blur = new NumberSetting("Glass blur", 10, 0, 18, 1);
    public final NumberSetting refraction = new NumberSetting("Glass refraction", 5, 0, 24, 1);
    public final NumberSetting radius = new NumberSetting("Corner radius", 11, 0, 18, 1);
    public final NumberSetting spacing = new NumberSetting("Card spacing", 10, 4, 18, 1);
    public final BooleanSetting descriptions = new BooleanSetting("Module descriptions", true);
    public final BooleanSetting borders = new BooleanSetting("Accent borders", true);
    public final BooleanSetting surfaceShine = new BooleanSetting("Surface highlights", true);
    public final BooleanSetting enabledTint = new BooleanSetting("Tint active modules", true);
    public final BooleanSetting animations = new BooleanSetting("Animations", true);
    public final NumberSetting speed = new NumberSetting("Animation speed", 1, 0.5, 2, 0.1);

    public GuiAppearanceModule() {
        super("GUI Appearance", "Desktop layout, layered materials and interface personalization", Category.RENDER);
        INSTANCE = this;
        for (Setting setting : new Setting[] {theme, layout, density, opacity, cardOpacity,
                layerDepth, shadowStrength, borderStrength, accentGlow, radius, spacing,
                descriptions, borders, surfaceShine, enabledTint, animations}) {
            setting.setVisibility(() -> version.is("New"));
        }
        blur.setVisibility(() -> version.is("New") && theme.is("Liquid Glass"));
        refraction.setVisibility(() -> version.is("New") && theme.is("Liquid Glass"));
        speed.setVisibility(() -> version.is("New") && animations.get());
        borderStrength.setVisibility(() -> version.is("New") && borders.get());
        shadowStrength.setVisibility(() -> version.is("New") && layerDepth.get() > 0);
        setEnabled(true);
    }

    public void resetAppearance() {
        version.set("Old");
        theme.set("Liquid Glass");
        layout.set("Grid");
        density.set("Comfortable");
        opacity.set(86);
        cardOpacity.set(91);
        layerDepth.set(70);
        shadowStrength.set(70);
        borderStrength.set(65);
        accentGlow.set(35);
        blur.set(10);
        refraction.set(5);
        radius.set(11);
        spacing.set(10);
        descriptions.set(true);
        borders.set(true);
        surfaceShine.set(true);
        enabledTint.set(true);
        animations.set(true);
        speed.set(1);
    }
}