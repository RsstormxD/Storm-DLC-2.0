package xyz.angames.astolfoclient.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.ShaderProgram;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class RenderUtil {
   private static final RenderType ROUNDED_RECT_LAYER = createLayer("rounded_rect_layer", CoreShaders.POSITION_COLOR);
   private static final RenderType ROUNDED_BORDER_LAYER = createLayer("rounded_border_layer", CoreShaders.POSITION_COLOR);

   private static RenderType createLayer(String name, ShaderProgram programKey) {
      return RenderType.create(
         name,
         DefaultVertexFormat.POSITION_COLOR,
         Mode.QUADS,
         256,
         true,
         true,
         CompositeState.builder()
            .setShaderState(new ShaderStateShard(programKey))
            .setTransparencyState(new TransparencyStateShard("translucent_transparency", RenderSystem::enableBlend, RenderSystem::disableBlend))
            .createCompositeState(false)
      );
   }

   public static void drawRoundedRect(Matrix4f matrix, MultiBufferSource vertexConsumers, float x, float y, float width, float height, BorderRadius radius, int color) {
      if (ShaderManager.ROUNDED_RECT_PROGRAM != null) {
         if (ShaderManager.ROUNDED_RECT_PROGRAM.getUniform("Size") != null) {
            ShaderManager.ROUNDED_RECT_PROGRAM.getUniform("Size").set(width, height);
            ShaderManager.ROUNDED_RECT_PROGRAM
               .getUniform("Radius")
               .setMat2x2(radius.topLeft(), radius.bottomLeft(), radius.topRight(), radius.bottomRight());
         }

         draw(matrix, vertexConsumers.getBuffer(ROUNDED_RECT_LAYER), x, y, width, height, color);
      }
   }

   public static void drawRoundedRectOutline(
      Matrix4f matrix, MultiBufferSource vertexConsumers, float x, float y, float width, float height, BorderRadius radius, int color, float thickness
   ) {
      if (ShaderManager.ROUNDED_BORDER_PROGRAM != null) {
         if (ShaderManager.ROUNDED_BORDER_PROGRAM.getUniform("Size") != null) {
            ShaderManager.ROUNDED_BORDER_PROGRAM.getUniform("Size").set(width, height);
            ShaderManager.ROUNDED_BORDER_PROGRAM
               .getUniform("Radius")
               .setMat2x2(radius.topLeft(), radius.bottomLeft(), radius.topRight(), radius.bottomRight());
            ShaderManager.ROUNDED_BORDER_PROGRAM.getUniform("Thickness").set(thickness);
         }

         draw(matrix, vertexConsumers.getBuffer(ROUNDED_BORDER_LAYER), x, y, width, height, color);
      }
   }

   private static void draw(Matrix4f matrix, VertexConsumer buffer, float x, float y, float width, float height, int color) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      float pad = 3.0F;
      buffer.addVertex(matrix, x - pad, y - pad, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x - pad, y + height + pad, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x + width + pad, y + height + pad, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x + width + pad, y - pad, 0.0F).setColor(r, g, b, a);
   }
}
