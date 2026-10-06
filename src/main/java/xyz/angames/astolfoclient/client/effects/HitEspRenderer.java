package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;

@Environment(EnvType.CLIENT)
public class HitEspRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/hit_effect.png");
   private final HitEspManager manager;

   public HitEspRenderer(HitEspManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      HitEspModule hitEspModule = (HitEspModule)AstolfoclientClient.moduleManager.getModuleByName("HitESP");
      if (hitEspModule != null && hitEspModule.isEnabled()) {
         List<HitEspEffect> validEffects = this.manager.getEffects();
         if (!validEffects.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            long currentTime = System.currentTimeMillis();
            double tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
            int rgb = VisualColors.get(HitEspModule.class, 0L);
            float r = (rgb >> 16 & 0xFF) / 255.0F;
            float g = (rgb >> 8 & 0xFF) / 255.0F;
            float b = (rgb & 0xFF) / 255.0F;
            float a = 1.0F;
            float baseScale = hitEspModule.size.getFloat();
            float rotSpeed = hitEspModule.rotationSpeed.getFloat();
            float spawnAnimDuration = hitEspModule.spawnAnimDuration.getFloat();
            float leaveTime = hitEspModule.leaveTime.getFloat();
            float fadeTime = hitEspModule.fadeTime.getFloat();
            boolean doExplosion = hitEspModule.doExplosion.get();
            boolean drewAnything = false;

            for (HitEspEffect effect : validEffects) {
               PoseStack matrixStack = context.matrixStack();
               long age = currentTime - effect.creationTime;
               if (!effect.isShattered) {
                  float textureRotation = (float)age * rotSpeed * effect.rotationDirection;
                  float scale = baseScale;
                  float alpha = 1.0F;
                  if ((float)age < spawnAnimDuration) {
                     float p = (float)age / spawnAnimDuration;
                     float ease = (float)(Math.sin(-20.420352248333657 * (p + 1.0)) * Math.pow(2.0, -10.0 * p) + 1.0);
                     scale = baseScale * ease;
                     alpha = Math.min(1.0F, p * 2.5F);
                  }

                  if (!doExplosion && (float)age >= leaveTime) {
                     float fadeProgress = ((float)age - leaveTime) / fadeTime;
                     alpha = Math.max(0.0F, 1.0F - fadeProgress);
                  }

                  alpha *= a;
                  if (alpha > 0.01F) {
                     matrixStack.pushPose();
                     matrixStack.translate(
                        effect.position.x - context.camera().getPosition().x,
                        effect.position.y - context.camera().getPosition().y,
                        effect.position.z - context.camera().getPosition().z
                     );
                     matrixStack.mulPose(effect.orientation);
                     matrixStack.mulPose(Axis.ZP.rotation(textureRotation));
                     matrixStack.scale(scale, scale, scale);
                     Matrix4f matrix = matrixStack.last().pose();
                     buffer.addVertex(matrix, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
                     buffer.addVertex(matrix, 0.5F, -0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
                     buffer.addVertex(matrix, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
                     buffer.addVertex(matrix, -0.5F, 0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
                     matrixStack.popPose();
                     drewAnything = true;
                  }
               } else {
                  float groundLifespan = hitEspModule.groundLifespan.getFloat();
                  boolean shrinkOnGround = hitEspModule.shrinkOnGround.get();
                  float gridSize = hitEspModule.gridSize.getFloat();
                  float baseShardScale = baseScale / gridSize * 0.93F;

                  for (HitEspEffect.Shard shard : effect.shards) {
                     float alpha = 1.0F;
                     float shardScale = baseShardScale;
                     if (shard.onGround) {
                        long timeOnGround = currentTime - shard.groundHitTime;
                        float deathProgress = (float)timeOnGround / groundLifespan;
                        alpha = 1.0F - deathProgress;
                        if (alpha < 0.0F) {
                           alpha = 0.0F;
                        }

                        if (shrinkOnGround) {
                           float shrinkEase = 1.0F - (float)Math.pow(deathProgress, 2.0);
                           shardScale *= Math.max(0.0F, shrinkEase);
                        }
                     }

                     alpha *= a;
                     if (alpha > 0.01F && shardScale > 0.01F) {
                        double renderX = Mth.lerp(tickDelta, shard.prevPos.x, shard.pos.x);
                        double renderY = Mth.lerp(tickDelta, shard.prevPos.y, shard.pos.y);
                        double renderZ = Mth.lerp(tickDelta, shard.prevPos.z, shard.pos.z);
                        float renderRotX = (float)Mth.lerp(tickDelta, shard.prevRotX, shard.rotX);
                        float renderRotY = (float)Mth.lerp(tickDelta, shard.prevRotY, shard.rotY);
                        float renderRotZ = (float)Mth.lerp(tickDelta, shard.prevRotZ, shard.rotZ);
                        matrixStack.pushPose();
                        matrixStack.translate(
                           renderX - context.camera().getPosition().x,
                           renderY - context.camera().getPosition().y,
                           renderZ - context.camera().getPosition().z
                        );
                        matrixStack.mulPose(Axis.XP.rotationDegrees(renderRotX));
                        matrixStack.mulPose(Axis.YP.rotationDegrees(renderRotY));
                        matrixStack.mulPose(Axis.ZP.rotationDegrees(renderRotZ));
                        matrixStack.scale(shardScale, shardScale, shardScale);
                        Matrix4f matrix = matrixStack.last().pose();
                        buffer.addVertex(matrix, -0.5F, -0.5F, 0.0F).setUv(shard.u1, shard.v2).setColor(r, g, b, alpha);
                        buffer.addVertex(matrix, 0.5F, -0.5F, 0.0F).setUv(shard.u2, shard.v2).setColor(r, g, b, alpha);
                        buffer.addVertex(matrix, 0.5F, 0.5F, 0.0F).setUv(shard.u2, shard.v1).setColor(r, g, b, alpha);
                        buffer.addVertex(matrix, -0.5F, 0.5F, 0.0F).setUv(shard.u1, shard.v1).setColor(r, g, b, alpha);
                        matrixStack.popPose();
                        drewAnything = true;
                     }
                  }
               }
            }

            if (!drewAnything) {
               Matrix4f dummy = context.matrixStack().last().pose();
               buffer.addVertex(dummy, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0, 0, 0, 0);
               buffer.addVertex(dummy, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0, 0, 0, 0);
               buffer.addVertex(dummy, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0, 0, 0, 0);
               buffer.addVertex(dummy, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0, 0, 0, 0);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }
}
