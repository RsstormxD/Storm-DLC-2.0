package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.shaders.Uniform;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(CompiledShaderProgram.class)
public interface ShaderProgramAccessor {
   @Accessor("uniformsByName")
   Map<String, Uniform> getUniformsByName();
}
