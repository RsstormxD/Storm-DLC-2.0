package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.CompiledShaderProgram;

@Environment(EnvType.CLIENT)
public class ShaderManager {
   public static CompiledShaderProgram ROUNDED_RECT_PROGRAM = null;
   public static CompiledShaderProgram ROUNDED_BORDER_PROGRAM = null;
}
