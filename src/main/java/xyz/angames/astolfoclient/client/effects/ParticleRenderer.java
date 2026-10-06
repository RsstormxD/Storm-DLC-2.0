package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;
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
import java.awt.Color;
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
public class ParticleRenderer {
   private static final ResourceLocation BUBBLES_TEX = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/bubbles.png");
   private static final ResourceLocation STARS_TEX = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/stars.png");
   private static final ResourceLocation DOLLARS_TEX = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/dollars.png");
   private static final ResourceLocation HEART_TEX = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");
   private static final ResourceLocation SMOLESTAR_TEX = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/mini-star.png");
   private final ParticleManager manager;

   public ParticleRenderer(ParticleManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      ParticlesModule module = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
      if (module != null && module.isEnabled()) {
         List<Particle> allParticles = this.manager.getParticles();
         if (!allParticles.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            Tesselator tessellator = Tesselator.getInstance();

            for (ParticlesModule.ParticleType type : ParticlesModule.ParticleType.values()) {
               boolean bound = false;
               BufferBuilder buffer = null;

               for (Particle particle : allParticles) {
                  if (particle.type == type) {
                     long age = currentTime - particle.creationTime;
                     if (age <= particle.lifespan) {
                        if (!bound) {
                           RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
                           RenderSystem.setShaderTexture(0, this.getTextureForType(type));
                           buffer = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                           bound = true;
                        }

                        float progress = (float)age / (float)particle.lifespan;
                        float alpha = progress > 0.6F ? 1.0F - (progress - 0.6F) / 0.4F : 1.0F;
                        Color particleColor = particle.color != null ? particle.color : new Color(VisualColors.get(ParticlesModule.class, particle.creationTime));
                        particleColor = new Color(VisualColors.resolve(xyz.angames.astolfoclient.client.module.ModuleManager.getModule(ParticlesModule.class), particleColor.getRGB(), particle.creationTime));
                        float r = particleColor.getRed() / 255.0F;
                        float g = particleColor.getGreen() / 255.0F;
                        float b = particleColor.getBlue() / 255.0F;
                        double x = Mth.lerp(tickDelta, particle.prevPosition.x, particle.position.x);
                        double y = Mth.lerp(tickDelta, particle.prevPosition.y, particle.position.y);
                        double z = Mth.lerp(tickDelta, particle.prevPosition.z, particle.position.z);
                        PoseStack matrixStack = context.matrixStack();
                        matrixStack.pushPose();
                        matrixStack.translate(
                           x - context.camera().getPosition().x,
                           y - context.camera().getPosition().y,
                           z - context.camera().getPosition().z
                        );
                        matrixStack.mulPose(context.camera().rotation());
                        float finalScale = particle.scale / 2.5F;
                        matrixStack.scale(finalScale, finalScale, finalScale);
                        matrixStack.mulPose(Axis.ZP.rotationDegrees(particle.rotation + (float)age * 0.1F));
                        Matrix4f matrix = matrixStack.last().pose();
                        buffer.addVertex(matrix, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
                        buffer.addVertex(matrix, 0.5F, -0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
                        buffer.addVertex(matrix, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
                        buffer.addVertex(matrix, -0.5F, 0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
                        matrixStack.popPose();
                     }
                  }
               }

               if (bound && buffer != null) {
                  BufferUploader.drawWithShader(buffer.buildOrThrow());
               }
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private ResourceLocation getTextureForType(ParticlesModule.ParticleType type) {
      switch (type) {
         case BUBBLES:
            return BUBBLES_TEX;
         case DOLLARS:
            return DOLLARS_TEX;
         case HEART:
            return HEART_TEX;
         case SMOLESTAR:
            return SMOLESTAR_TEX;
         case STARS:
         default:
            return STARS_TEX;
      }
   }
}
