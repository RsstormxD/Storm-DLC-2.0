package xyz.angames.astolfoclient.client.gui.clickgui;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import java.awt.Color;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.module.modules.render.GuiAppearanceModule;

/** Shared materials for the desktop, navigation rail, cards and inset controls. */
public final class GuiSkin {
    private GuiSkin() {}

    public static boolean modern() {
        var settings = GuiAppearanceModule.INSTANCE;
        return settings != null && settings.isEnabled() && settings.version.is("New");
    }

    public static boolean list() {
        return modern() && GuiAppearanceModule.INSTANCE.layout.is("List");
    }

    public static float gap() {
        return modern() ? GuiAppearanceModule.INSTANCE.spacing.getFloat() : 9;
    }

    public static float header() {
        if (!modern()) return 24;
        var settings = GuiAppearanceModule.INSTANCE;
        return switch (settings.density.get()) {
            case "Compact" -> settings.descriptions.get() ? 44 : 34;
            case "Spacious" -> settings.descriptions.get() ? 60 : 44;
            default -> settings.descriptions.get() ? 52 : 38;
        };
    }

    public static float radius() {
        return modern() ? GuiAppearanceModule.INSTANCE.radius.getFloat() : 7;
    }

    public static boolean animations() {
        return !modern() || GuiAppearanceModule.INSTANCE.animations.get();
    }

    public static float motion(float delta) {
        return !animations() ? 1 : modern() ? delta * GuiAppearanceModule.INSTANCE.speed.getFloat() : delta;
    }

    public static Color foreground(float alpha) {
        return color(0xf2f5ff, alpha);
    }

    public static Color muted(float alpha) {
        return color(0x9baac1, alpha);
    }

    public static Color color(int rgb, float alpha) {
        return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255,
                Math.max(0, Math.min(255, Math.round(alpha * 255))));
    }

    private static int mix(int first, int second, float amount) {
        amount = Math.max(0, Math.min(1, amount));
        int r = Math.round((first >> 16 & 255) * (1 - amount) + (second >> 16 & 255) * amount);
        int g = Math.round((first >> 8 & 255) * (1 - amount) + (second >> 8 & 255) * amount);
        int b = Math.round((first & 255) * (1 - amount) + (second & 255) * amount);
        return r << 16 | g << 8 | b;
    }

    private static int base() {
        if (!modern()) return 0x060608;
        return switch (GuiAppearanceModule.INSTANCE.theme.get()) {
            case "AMOLED" -> 0x030406;
            case "Aurora" -> 0x111629;
            case "Liquid Glass" -> 0x111b29;
            case "Carbon" -> 0x141516;
            case "Slate" -> 0x232d3b;
            default -> 0x0c1220;
        };
    }

    private static float corner(float width, float height, float requested) {
        return Math.max(0, Math.min(requested, Math.min(width, height) / 2));
    }

    private static void fill(Matrix4f matrix, float x, float y, float width, float height,
                             float radius, Color tint) {
        if (width <= 0 || height <= 0 || tint.getAlpha() == 0) return;
        Builder.rectangle().size(new SizeState(width, height))
                .radius(new QuadRadiusState(corner(width, height, radius)))
                .color(tint).build().render(matrix, x, y);
    }

    private static void edge(Matrix4f matrix, float x, float y, float width, float height,
                             float radius, float thickness, Color tint) {
        if (width <= 0 || height <= 0 || tint.getAlpha() == 0) return;
        Builder.border().size(new SizeState(width, height))
                .radius(new QuadRadiusState(corner(width, height, radius)))
                .thickness(thickness).color(tint).build().render(matrix, x, y);
    }

    private static void gradient(Matrix4f matrix, float x, float y, float width, float height,
                                 float radius, Color top, Color bottom) {
        if (width <= 0 || height <= 0) return;
        Builder.rectangle().size(new SizeState(width, height))
                .radius(new QuadRadiusState(corner(width, height, radius)))
                .color(new QuadColorState(top, top, bottom, bottom)).build().render(matrix, x, y);
    }

    private static float depth() {
        return modern() ? GuiAppearanceModule.INSTANCE.layerDepth.getFloat() / 100f : 0;
    }

    private static void shadow(Matrix4f matrix, float x, float y, float width, float height,
                               float radius, float alpha, float spread) {
        if (!modern()) return;
        float strength = GuiAppearanceModule.INSTANCE.shadowStrength.getFloat() / 100f;
        if (strength <= 0 || depth() <= 0) return;
        // The silhouettes need no scene samples, so this material also works on 3D panels.
        for (int i = 5; i >= 1; i--) {
            float grow = spread * i / 5f * depth();
            fill(matrix, x - grow, y - grow + spread * .35f, width + grow * 2,
                    height + grow * 2, radius + grow,
                    color(0x000000, alpha * strength * (.028f + (5 - i) * .009f)));
        }
    }

    public static void window(Matrix4f matrix, float x, float y, float width, float height,
                               float alpha, Color accent, boolean world) {
        if (!modern()) {
            fill(matrix, x, y, width, height, 10, color(0, alpha));
            return;
        }
        var settings = GuiAppearanceModule.INSTANCE;
        float round = radius();
        shadow(matrix, x, y, width, height, round, alpha, 12);
        if (settings.theme.is("Liquid Glass") && !world) {
            Builder.liquidGlass().size(new SizeState(width, height))
                    .radius(new QuadRadiusState(corner(width, height, round)))
                    .blurRadius(settings.blur.getFloat())
                    .distortStrength(settings.refraction.getFloat() / 1500f)
                    .alpha(alpha * .86f, .10f)
                    .fresnel(3.5f, color(0xd3e6ff, 1), .14f,
                            settings.surfaceShine.get() ? .24f : .06f, false)
                    .build().render(matrix, x, y);
        }
        float opacity = settings.opacity.getFloat() / 100f;
        gradient(matrix, x, y, width, height, round,
                color(mix(base(), 0x354459, .15f), alpha * opacity),
                color(base(), alpha * Math.min(1, opacity + .08f)));
        if (settings.theme.is("Aurora")) {
            Builder.rectangle().size(new SizeState(width, height))
                    .radius(new QuadRadiusState(corner(width, height, round)))
                    .color(new QuadColorState(color(accent.getRGB(), alpha * .12f),
                            color(0x8071dd, alpha * .13f), color(0x192940, alpha * .04f),
                            color(0x4297ae, alpha * .07f)))
                    .build().render(matrix, x, y);
        }
        if (settings.surfaceShine.get()) {
            gradient(matrix, x + 1, y + 1, width - 2, Math.min(height * .38f, 100),
                    Math.max(0, round - 1), color(0xdceaff, alpha * .038f), color(0xdceaff, 0));
        }
        float border = settings.borderStrength.getFloat() / 100f;
        if (settings.borders.get()) {
            edge(matrix, x, y, width, height, round, .85f,
                    color(0xd1e0f4, alpha * border * .35f));
            edge(matrix, x + 1.5f, y + 1.5f, width - 3, height - 3,
                    Math.max(0, round - 1.5f), .5f, color(0x000000, alpha * .23f));
        }
    }

    /** Level 0 is inset, 1 is content, 2 is raised and 3 is a floating overlay. */
    public static void surface(Matrix4f matrix, float x, float y, float width, float height,
                                float alpha, Color accent, int level) {
        level = Math.max(0, Math.min(3, level));
        if (!modern()) {
            fill(matrix, x, y, width, height, 7, color(0x08090d, alpha));
            return;
        }
        var settings = GuiAppearanceModule.INSTANCE;
        float round = Math.max(3, radius() - 2);
        int tint = level == 0 ? mix(base(), 0x000000, .26f)
                : mix(base(), 0x647891, (.035f + level * .025f) * (.35f + depth()));
        float opacity = level == 0 ? .63f : .67f + level * .07f;
        if (level >= 2) shadow(matrix, x, y, width, height, round, alpha * .7f, 3 + level);
        gradient(matrix, x, y, width, height, round,
                color(mix(tint, 0x92a6c1, settings.surfaceShine.get() ? .025f : 0), alpha * opacity),
                color(tint, alpha * opacity));
        if (settings.borders.get()) {
            float strength = settings.borderStrength.getFloat() / 100f;
            edge(matrix, x, y, width, height, round, .6f,
                    color(level == 0 ? 0x000000 : 0xd5e2f6,
                            alpha * strength * (level == 0 ? .30f : .08f + level * .025f)));
        }
    }

    public static void sidebar(Matrix4f matrix, float x, float y, float width, float height,
                                float alpha, Color accent) {
        surface(matrix, x, y, width, height, alpha, accent, 0);
        if (modern() && GuiAppearanceModule.INSTANCE.surfaceShine.get()) {
            gradient(matrix, x + 1, y + 1, width - 2, Math.min(height, 84),
                    Math.max(3, radius() - 3), color(accent.getRGB(), alpha * .055f),
                    color(accent.getRGB(), 0));
        }
    }

    public static void settings(Matrix4f matrix, float x, float y, float width, float height,
                                 float alpha, Color accent) {
        if (height <= 0) return;
        surface(matrix, x, y, width, height, alpha, accent, 0);
        if (modern()) separator(matrix, x + 8, y, width - 16, alpha * .6f);
    }

    public static void separator(Matrix4f matrix, float x, float y, float width, float alpha) {
        fill(matrix, x, y, width, .65f, 0, color(0xc1d0e7, alpha * .12f));
    }

    public static void card(Matrix4f matrix, float x, float y, float width, float height,
                             float alpha, Color accent, boolean enabled, float hover) {
        if (!modern()) {
            fill(matrix, x, y, width, height, 7,
                    color(hover > .1f ? 0x0a0a0e : 0x060608, alpha));
            return;
        }
        var settings = GuiAppearanceModule.INSTANCE;
        hover = Math.max(0, Math.min(1, hover));
        float round = radius();
        float materialAlpha = settings.cardOpacity.getFloat() / 100f;
        int body = mix(base(), 0x68819c, .075f + depth() * .065f + hover * .065f);
        if (enabled && settings.enabledTint.get()) body = mix(body, accent.getRGB(), .095f);
        // The body is a raised plane, with a separate inset plane for the settings.
        float shadowAlpha = settings.shadowStrength.getFloat() / 100f * depth();
        fill(matrix, x, y + 2, width, height, round,
                color(0x000000, alpha * shadowAlpha * .25f));
        gradient(matrix, x, y, width, height, round,
                color(mix(body, 0xb2c5df, settings.surfaceShine.get() ? .026f : 0), alpha * materialAlpha),
                color(body, alpha * materialAlpha));
        if (enabled && settings.accentGlow.getFloat() > 0) {
            float glow = settings.accentGlow.getFloat() / 100f;
            gradient(matrix, x + 1, y + 1, width - 2, Math.min(height - 2, header()),
                    Math.max(0, round - 1), color(accent.getRGB(), alpha * glow * .13f),
                    color(accent.getRGB(), 0));
        }
        if (settings.borders.get()) {
            float strength = settings.borderStrength.getFloat() / 100f;
            edge(matrix, x, y, width, height, round, .7f,
                    color(enabled ? accent.getRGB() : 0xc6d6ed,
                            alpha * strength * (enabled ? .48f + hover * .14f : .13f + hover * .20f)));
        }
        if (enabled) {
            float marker = Math.min(18, header() - 14);
            fill(matrix, x + 1.5f, y + (header() - marker) / 2, 2, marker, 1,
                    color(accent.getRGB(), alpha * .95f));
        }
    }
}