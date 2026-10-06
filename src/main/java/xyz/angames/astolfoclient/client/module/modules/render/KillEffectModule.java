package xyz.angames.astolfoclient.client.module.modules.render;

import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import dev.stormdlc.render.CosmeticParticles;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

public class KillEffectModule extends Module {
    public final ModeSetting mode = new ModeSetting("Mode", "Zap", "Zap", "Thanos", "Confetti", "Fountain", "Shockwave");
    public final NumberSetting count = new NumberSetting("Burst particles", 64, 12, 180, 4);
    public final NumberSetting scatter = new NumberSetting("Scatter", 1, .2, 3, .1);
    public final NumberSetting gravity = new NumberSetting("Particle gravity", .35, 0, 1.5, .05);
    public final NumberSetting lifetime = new NumberSetting("Particle lifetime (seconds)", 2, .5, 5, .25);
    public final NumberSetting size = new NumberSetting("Particle size", .65, .2, 2, .05);
    public final BooleanSetting collision = new BooleanSetting("Particle collision", true);
    public final BooleanSetting rainbow = new BooleanSetting("Rainbow confetti", false);
    private final CosmeticParticles particles = new CosmeticParticles();
    private final Random random = new Random();
    public KillEffectModule() {
        super("KillEffect", "Cosmetic effects after a confirmed kill", Category.RENDER);
        for (Setting setting : new Setting[]{count, scatter, gravity, lifetime, size, collision}) setting.setVisibility(this::isParticleMode);
        rainbow.setVisibility(() -> mode.is("Confetti"));
    }
    public boolean isParticleMode() { return mode.is("Confetti") || mode.is("Fountain") || mode.is("Shockwave"); }
    public void burst(Vec3 position) {
        if (!isEnabled() || !isParticleMode()) return;
        for (int i=0; i<count.getInt(); i++) {
            double angle = i * Math.PI * 2 / count.getInt();
            Vec3 velocity;
            if (mode.is("Shockwave")) velocity = new Vec3(Math.cos(angle)*.19, .025, Math.sin(angle)*.19);
            else if (mode.is("Fountain")) velocity = new Vec3((random.nextDouble()-.5)*.1, .2+random.nextDouble()*.13, (random.nextDouble()-.5)*.1);
            else velocity = new Vec3((random.nextDouble()-.5)*.3, .08+random.nextDouble()*.18, (random.nextDouble()-.5)*.3);
            int color = mode.is("Confetti") && rainbow.get() ? java.awt.Color.HSBtoRGB(i/(float)count.getInt(), .8f, 1) : VisualColors.get(KillEffectModule.class, i*15L);
            particles.emit(ParticleTypes.END_ROD, position.add(0, mode.is("Shockwave") ? .1 : .7, 0), velocity.scale(scatter.get()), color, size.getFloat(), (int)(lifetime.get()*20), gravity.getFloat(), collision.get());
        }
    }
    @Override public void onTick() {
        if (!isEnabled()) return;
        particles.tick();
    }
    @Override public void onEnable() {
        if (AstolfoclientClient.killEffectManager != null) AstolfoclientClient.killEffectManager.clear();
    }
    @Override public void onDisable() {
        particles.clear();
        if (AstolfoclientClient.killEffectManager != null) AstolfoclientClient.killEffectManager.clear();
    }
}
