package xyz.angames.astolfoclient.client.module.modules.render;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import dev.stormdlc.render.CosmeticParticles;
import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

public final class AmbientParticlesModule extends Module {
    public final ModeSetting style = new ModeSetting("Particle style", "Sparkles", "Sparkles", "Snow", "Cherry", "Ash", "Enchant");
    public final NumberSetting density = new NumberSetting("Particles per tick", 3, 1, 12, 1);
    public final NumberSetting radius = new NumberSetting("Spawn radius", 10, 3, 24, 1);
    public final NumberSetting height = new NumberSetting("Spawn height", 5, 1, 12, .5);
    public final NumberSetting size = new NumberSetting("Particle size", .7, .2, 2, .1);
    public final NumberSetting lifetime = new NumberSetting("Lifetime (seconds)", 3, .5, 6, .5);
    public final NumberSetting speed = new NumberSetting("Drift speed", .5, 0, 2, .1);
    public final BooleanSetting outdoors = new BooleanSetting("Outdoors only", true);
    public final BooleanSetting collision = new BooleanSetting("Particle collision", true);
    private final CosmeticParticles particles = new CosmeticParticles();
    private final Random random = new Random();
    public AmbientParticlesModule() { super("Ambient Particles", "Decorative snow, petals, ash and sparkles around you", Category.RENDER); }
    @Override public void onDisable() { particles.clear(); }
    @Override public void onTick() {
        if (!isEnabled()) return;
        particles.tick();
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        Vec3 center = mc.player.position();
        ParticleOptions effect = switch (style.get()) {
            case "Snow" -> ParticleTypes.SNOWFLAKE;
            case "Cherry" -> ParticleTypes.CHERRY_LEAVES;
            case "Ash" -> ParticleTypes.WHITE_ASH;
            case "Enchant" -> ParticleTypes.ENCHANT;
            default -> ParticleTypes.END_ROD;
        };
        for (int i = 0; i < density.getInt(); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 2 + random.nextDouble() * (radius.get() - 2);
            Vec3 pos = center.add(Math.cos(angle) * distance, .4 + random.nextDouble() * height.get(), Math.sin(angle) * distance);
            BlockPos block = BlockPos.containing(pos);
            if (!mc.level.hasChunkAt(block) || !mc.level.isEmptyBlock(block) || !mc.level.getFluidState(block).isEmpty()) continue;
            if (outdoors.get() && !mc.level.canSeeSky(block)) continue;
            double fall = style.is("Sparkles") || style.is("Enchant") ? .012 : -.035;
            Vec3 velocity = new Vec3((random.nextDouble() - .5) * .025, fall, (random.nextDouble() - .5) * .025).scale(speed.get());
            particles.emit(effect, pos, velocity, VisualColors.get(AmbientParticlesModule.class, i * 30L), size.getFloat(), (int)(lifetime.get()*20), 0, collision.get());
        }
    }
}
