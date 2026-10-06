package xyz.angames.astolfoclient.client.module.modules.render;

import dev.stormdlc.render.CosmeticParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

public final class SelfAuraModule extends Module {
    public final ModeSetting style = new ModeSetting("Aura style", "Orbit", "Orbit", "Helix", "Halo", "Flames");
    public final NumberSetting radius = new NumberSetting("Aura radius", .65, .3, 2, .05);
    public final NumberSetting height = new NumberSetting("Aura height", 1.8, .3, 3, .1);
    public final NumberSetting speed = new NumberSetting("Rotation speed", 1, .1, 3, .1);
    public final NumberSetting amount = new NumberSetting("Particles per tick", 3, 1, 10, 1);
    public final NumberSetting size = new NumberSetting("Particle size", .55, .2, 1.5, .05);
    public final NumberSetting lifetime = new NumberSetting("Trail lifetime (ticks)", 14, 4, 40, 1);
    public final BooleanSetting thirdPerson = new BooleanSetting("Third person only", true);
    public final BooleanSetting moving = new BooleanSetting("Only while moving", false);
    private final CosmeticParticles particles = new CosmeticParticles();
    private double phase;
    public SelfAuraModule() { super("Self Aura", "Decorative particles around your own player", Category.RENDER); }
    @Override public void onDisable() { particles.clear(); phase = 0; }
    @Override public void onTick() {
        if (!isEnabled()) return;
        particles.tick();
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if ((thirdPerson.get() && mc.options.getCameraType().isFirstPerson()) || mc.player.isInvisible()) { particles.clear(); return; }
        if (moving.get() && mc.player.getDeltaMovement().horizontalDistanceSqr() < .001) return;
        phase += .16 * speed.get();
        for (int i = 0; i < amount.getInt(); i++) {
            double angle = phase + i * Math.PI * 2 / amount.getInt();
            double y = style.is("Halo") ? height.get() + .25 : style.is("Helix") ? (Math.sin(angle)*.5+.5)*height.get() : style.is("Flames") ? .05 : height.get()*.55;
            Vec3 pos = mc.player.position().add(Math.cos(angle)*radius.get(), y, Math.sin(angle)*radius.get());
            Vec3 velocity = style.is("Flames") ? new Vec3(0, .035*speed.get(), 0) : Vec3.ZERO;
            particles.emit(style.is("Flames") ? ParticleTypes.FLAME : ParticleTypes.END_ROD, pos, velocity,
                VisualColors.get(SelfAuraModule.class, i*90L), size.getFloat(), lifetime.getInt(), 0, true);
        }
    }
}
