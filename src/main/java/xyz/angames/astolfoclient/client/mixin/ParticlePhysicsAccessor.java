package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Particle.class)
public interface ParticlePhysicsAccessor {
    @Accessor("gravity") void stormSetGravity(float gravity);
    @Accessor("hasPhysics") void stormSetCollision(boolean collision);
}
