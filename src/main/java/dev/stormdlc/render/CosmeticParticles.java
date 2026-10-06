package dev.stormdlc.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.mixin.ParticlePhysicsAccessor;

/** Bounded, local-only particles using vanilla depth-tested rendering. */
public final class CosmeticParticles {
    private final List<Particle> particles = new ArrayList<>();
    private Object world;
    public void tick() {
        Object current = Minecraft.getInstance().level;
        if (world != current) { clear(); world = current; }
        particles.removeIf(p -> !p.isAlive());
    }
    public void clear() { particles.forEach(Particle::remove); particles.clear(); }
    public void emit(ParticleOptions effect, Vec3 pos, Vec3 velocity, int color, float size, int lifetime, float gravity, boolean collision) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || particles.size() >= 768) return;
        Particle particle = mc.particleEngine.createParticle(effect, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        if (particle == null) return;
        particle.setParticleSpeed(velocity.x, velocity.y, velocity.z);
        particle.setColor((color >> 16 & 255)/255f, (color >> 8 & 255)/255f, (color & 255)/255f);
        particle.scale(size);
        particle.setLifetime(lifetime);
        var physics = (ParticlePhysicsAccessor)particle;
        physics.stormSetGravity(gravity);
        physics.stormSetCollision(collision);
        particles.add(particle);
    }
}
