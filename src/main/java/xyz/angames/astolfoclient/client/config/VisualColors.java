package xyz.angames.astolfoclient.client.config;

import java.util.Set;
import xyz.angames.astolfoclient.client.module.modules.render.Gui3DModule;
import xyz.angames.astolfoclient.client.module.modules.render.SongIslandModule;
import xyz.angames.astolfoclient.client.module.modules.render.Spotify3DModule;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.setting.ColorSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
import xyz.angames.astolfoclient.client.module.modules.render.BlockOutlineModule;
import xyz.angames.astolfoclient.client.module.modules.render.ChinaHatModule;
import xyz.angames.astolfoclient.client.module.modules.render.CrosshairModule;
import xyz.angames.astolfoclient.client.module.modules.render.CubeParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;
import xyz.angames.astolfoclient.client.module.modules.render.DashTrailModule;
import xyz.angames.astolfoclient.client.module.modules.render.FireFliesModule;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;
import xyz.angames.astolfoclient.client.module.modules.render.HitGlowModule;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.module.modules.JumpCircleModule;
import xyz.angames.astolfoclient.client.module.modules.render.KillEffectModule;
import xyz.angames.astolfoclient.client.module.modules.render.LineGlyphsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.RagdollModule;
import xyz.angames.astolfoclient.client.module.modules.render.RainModule;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.module.modules.render.TrailsModule;
import xyz.angames.astolfoclient.client.module.modules.render.TrajectoriesModule;
import xyz.angames.astolfoclient.client.module.modules.render.WingsModule;

/** Per-module color source. Original effect palettes remain available for old profiles. */
public final class VisualColors {
    private static final Set<Class<? extends Module>> SUPPORTED = Set.of(xyz.angames.astolfoclient.client.module.modules.render.AmbientParticlesModule.class, xyz.angames.astolfoclient.client.module.modules.render.SelfAuraModule.class, Gui3DModule.class, SongIslandModule.class, Spotify3DModule.class,
        AmbientsModule.class, BlockOutlineModule.class, ChinaHatModule.class, CrosshairModule.class, CubeParticlesModule.class, DamageIndicatorModule.class, DashTrailModule.class, FireFliesModule.class, HitEspModule.class, HitGlowModule.class, InterfaceModule.class, JumpCircleModule.class, KillEffectModule.class, LineGlyphsModule.class, ParticlesModule.class, RagdollModule.class, RainModule.class, ShaderHand.class, TargetEspModule.class, TrailsModule.class, TrajectoriesModule.class, WingsModule.class);
    private static final Set<Class<? extends Module>> EFFECT_PALETTES = Set.of(SongIslandModule.class, Spotify3DModule.class,
        CubeParticlesModule.class, DashTrailModule.class, RainModule.class,
        ParticlesModule.class, DamageIndicatorModule.class, CrosshairModule.class);
    public final ModeSetting source;
    public final ColorSetting custom = new ColorSetting("Custom color", 0x2575ff);

    private VisualColors(Module module) {
        boolean effect = EFFECT_PALETTES.contains(module.getClass());
        source = effect ? new ModeSetting("Color source", "Effect", "Client", "Custom", "Effect")
                        : new ModeSetting("Color source", "Client", "Client", "Custom");
        custom.setVisibility(() -> source.is("Custom"));
        module.getSettings().add(0, custom);
        module.getSettings().add(0, source);
    }
    public static void attach(Module module) {
        if (SUPPORTED.contains(module.getClass()) && module.visualColors == null) module.visualColors = new VisualColors(module);
    }
    public static boolean settingVisible(Module module, xyz.angames.astolfoclient.client.module.setting.Setting setting) {
        if (usesEffect(module)) return true;
        if (module instanceof CubeParticlesModule m && setting == m.colorMode) return false;
        if (module instanceof DashTrailModule m && setting == m.colorMode) return false;
        if (module instanceof CrosshairModule m && setting == m.useEntityColor) return false;
        if (module instanceof ParticlesModule m && setting == m.totemColor) return false;
        if (module instanceof RainModule m && (setting == m.colorMode || setting == m.customRed || setting == m.customGreen || setting == m.customBlue)) return false;
        return true;
    }
    public static boolean usesEffect(Module module) {
        return module == null || module.visualColors == null || module.visualColors.source.is("Effect");
    }
    public static int get(Class<? extends Module> type, long offset) {
        Module module = ModuleManager.getModule(type);
        if (module != null && module.visualColors != null && module.visualColors.source.is("Custom"))
            return module.visualColors.custom.color().getRGB();
        return ThemeManager.getThemedColor(offset);
    }
    public static int resolve(Module module, int original, long offset) {
        return usesEffect(module) ? original : get(module.getClass(), offset);
    }
}
