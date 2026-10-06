package xyz.angames.astolfoclient.client.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;

@Environment(EnvType.CLIENT)
public class DamageIndicatorRenderer {
   private final DamageIndicatorManager manager;

   public DamageIndicatorRenderer(DamageIndicatorManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      DamageIndicatorModule module = (DamageIndicatorModule)AstolfoclientClient.moduleManager.getModuleByName("DamageIndicators");
      if (module != null && module.isEnabled()) {
         if (!this.manager.getParticles().isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            Font textRenderer = client.font;
            Camera camera = context.camera();
            PoseStack matrices = context.matrixStack();
            BufferSource vertexConsumers = client.renderBuffers().bufferSource();
            double camX = camera.getPosition().x;
            double camY = camera.getPosition().y;
            double camZ = camera.getPosition().z;
            float baseScale = (float)module.scale.get() * 0.02F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            for (DamageIndicatorManager.DamageParticle p : this.manager.getParticles()) {
               float alpha = 1.0F;
               int fadeTime = 12;
               if (p.maxAge - p.age <= fadeTime) {
                  float fadeProgress = (float)(p.maxAge - p.age) / fadeTime;
                  alpha = fadeProgress * fadeProgress * (3.0F - 2.0F * fadeProgress);
               }

               float popScale = 1.0F;
               float popDuration = 6.0F;
               if (p.age < popDuration) {
                  float t = p.age / popDuration;
                  float c1 = 1.70158F;
                  float c3 = c1 + 1.0F;
                  popScale = 1.0F + c3 * (float)Math.pow(t - 1.0F, 3.0) + c1 * (float)Math.pow(t - 1.0F, 2.0);
                  if (popScale < 0.0F) {
                     popScale = 0.0F;
                  }
               }

               int color = xyz.angames.astolfoclient.client.config.VisualColors.resolve(module, p.isCrit ? -65536 : -22016, 0L);
               int alphaHex = (int)(alpha * 255.0F) << 24;
               color = color & 16777215 | alphaHex;
               matrices.pushPose();
               matrices.translate(p.x - camX, p.y - camY, p.z - camZ);
               matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
               matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
               float currentScale = baseScale * (p.isCrit ? 1.3F : 1.0F) * popScale;
               matrices.scale(-currentScale, -currentScale, currentScale);
               Matrix4f positionMatrix = matrices.last().pose();
               Component text = Component.literal(p.text);
               float xOffset = -textRenderer.width(text) / 2.0F;
               textRenderer.drawInBatch(text, xOffset, 0.0F, color, true, positionMatrix, vertexConsumers, DisplayMode.NORMAL, 0, 15728880);
               matrices.popPose();
            }

            vertexConsumers.endBatch();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
         }
      }
   }
}
