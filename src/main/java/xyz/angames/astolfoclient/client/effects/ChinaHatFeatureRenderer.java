package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.config.VisualColors;
import xyz.angames.astolfoclient.client.module.modules.render.ChinaHatModule;
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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
public class ChinaHatFeatureRenderer extends RenderLayer<PlayerRenderState, PlayerModel> {
   public static Player currentlyRenderingPlayer;
   private static final float RADIUS = 0.65F;
   private static final float HEIGHT = 0.28F;
   private static final int SEGMENTS = 40;
   private static final ResourceLocation BLOOM_TEXTURE = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/effects/dashtrail/dashbloom.png");
   private float spinAngle = 0.0F;
   private long lastRenderTime = 0L;

   public ChinaHatFeatureRenderer(RenderLayerParent<PlayerRenderState, PlayerModel> context) {
      super(context);
   }

   public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, PlayerRenderState state, float limbAngle, float limbDistance) {
      Minecraft mc = Minecraft.getInstance();
      ChinaHatModule module = (ChinaHatModule)AstolfoclientClient.moduleManager.getModuleByName("ChinaHat");
      if (module != null && module.isEnabled() && mc.player != null) {
         if (currentlyRenderingPlayer != null && currentlyRenderingPlayer.getId() == mc.player.getId()) {
            if (!state.isInvisible) {
               long now = System.currentTimeMillis();
               if (this.lastRenderTime != 0L) {
                  this.spinAngle = this.spinAngle + (float)(now - this.lastRenderTime) / 1000.0F * module.spin.getFloat();
                  if (this.spinAngle > 360.0F) {
                     this.spinAngle -= 360.0F;
                  }
               }

               this.lastRenderTime = now;
               matrices.pushPose();
               ((PlayerModel)this.getParentModel()).head.translateAndRotate(matrices);
               matrices.translate(0.0F, -0.4F - module.offset.getFloat(), 0.0F);
               matrices.scale(module.radius.getFloat() / .65f, -module.height.getFloat() / .28f, module.radius.getFloat() / .65f);
               matrices.mulPose(Axis.YP.rotationDegrees(this.spinAngle));
               Matrix4f matrix = matrices.last().pose();
               Tesselator tessellator = Tesselator.getInstance();
               if (vertexConsumers instanceof BufferSource immediate) {
                  immediate.endBatch();
               }

               RenderSystem.enableBlend();
               RenderSystem.disableCull();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
               CompiledShaderProgram shader = RenderSystem.setShader(AstolfoclientClient.CHINA_HAT_SHADER);
               if (shader != null) {
                  float timeSecs = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
                  if (shader.getUniform("uTime") != null) {
                     shader.getUniform("uTime").set(timeSecs);
                  }

                  if (shader.getUniform("uResolution") != null) {
                     shader.getUniform("uResolution").set(1.0F, 1.0F);
                  }

                  int themeRgb = VisualColors.get(ChinaHatModule.class, (int)(now / 10L));
                  float tr = (themeRgb >> 16 & 0xFF) / 255.0F;
                  float tg = (themeRgb >> 8 & 0xFF) / 255.0F;
                  float tb = (themeRgb & 0xFF) / 255.0F;
                  if (shader.getUniform("uThemeColor") != null) {
                     shader.getUniform("uThemeColor").set(tr, tg, tb);
                  }
               }

               Color cInner = new Color(VisualColors.get(ChinaHatModule.class, (int)(now / 10L)));
               Color cOuter = new Color(VisualColors.get(ChinaHatModule.class, (int)(now / 10L) + 60));
               int optimizedLayers = 16;
               BufferBuilder bufBody = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

               for (int layer = 1; layer < optimizedLayers; layer++) {
                  float t0 = (float)(layer - 1) / (optimizedLayers - 1);
                  float t1 = (float)layer / (optimizedLayers - 1);
                  float r0 = 0.65F * t0;
                  float h0 = 0.28F * (1.0F - t0);
                  float r1 = 0.65F * t1;
                  float h1 = 0.28F * (1.0F - t1);
                  Color c0 = this.lerpColor(cInner, cOuter, this.smoothstep(t0));
                  Color c1 = this.lerpColor(cInner, cOuter, this.smoothstep(t1));
                  float a0 = 1.0F;
                  float a1 = 1.0F;

                  for (int i = 0; i < 40; i++) {
                     double ang0 = (Math.PI * 2) * i / 40.0;
                     double ang1 = (Math.PI * 2) * (i + 1) / 40.0;
                     float st = i / 40.0F;
                     Color sc0 = this.lerpColor(c0, cOuter, st * 0.25F);
                     Color sc1 = this.lerpColor(c1, cOuter, st * 0.25F);
                     float u0_0 = (float)Math.cos(ang0) * t0 * 0.5F + 0.5F;
                     float v0_0 = (float)Math.sin(ang0) * t0 * 0.5F + 0.5F;
                     float u1_0 = (float)Math.cos(ang1) * t0 * 0.5F + 0.5F;
                     float v1_0 = (float)Math.sin(ang1) * t0 * 0.5F + 0.5F;
                     float u1_1 = (float)Math.cos(ang1) * t1 * 0.5F + 0.5F;
                     float v1_1 = (float)Math.sin(ang1) * t1 * 0.5F + 0.5F;
                     float u0_1 = (float)Math.cos(ang0) * t1 * 0.5F + 0.5F;
                     float v0_1 = (float)Math.sin(ang0) * t1 * 0.5F + 0.5F;
                     bufBody.addVertex(matrix, (float)Math.cos(ang0) * r0, h0, (float)Math.sin(ang0) * r0)
                        .setUv(u0_0, v0_0)
                        .setColor(sc0.getRed() / 255.0F, sc0.getGreen() / 255.0F, sc0.getBlue() / 255.0F, a0);
                     bufBody.addVertex(matrix, (float)Math.cos(ang1) * r0, h0, (float)Math.sin(ang1) * r0)
                        .setUv(u1_0, v1_0)
                        .setColor(sc0.getRed() / 255.0F, sc0.getGreen() / 255.0F, sc0.getBlue() / 255.0F, a0);
                     bufBody.addVertex(matrix, (float)Math.cos(ang1) * r1, h1, (float)Math.sin(ang1) * r1)
                        .setUv(u1_1, v1_1)
                        .setColor(sc1.getRed() / 255.0F, sc1.getGreen() / 255.0F, sc1.getBlue() / 255.0F, a1);
                     bufBody.addVertex(matrix, (float)Math.cos(ang0) * r1, h1, (float)Math.sin(ang0) * r1)
                        .setUv(u0_1, v0_1)
                        .setColor(sc1.getRed() / 255.0F, sc1.getGreen() / 255.0F, sc1.getBlue() / 255.0F, a1);
                  }
               }

               BufferUploader.drawWithShader(bufBody.buildOrThrow());
               float r = cOuter.getRed() / 255.0F;
               float g = cOuter.getGreen() / 255.0F;
               float b = cOuter.getBlue() / 255.0F;
               float rimW = 0.035F;
               float innerR = 0.65F - rimW;
               float innerH = 0.28F * (rimW / 0.65F);
               float innerT = (0.65F - rimW) / 0.65F;
               BufferBuilder bufRim = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

               for (int i = 0; i < 40; i++) {
                  double a0 = (Math.PI * 2) * i / 40.0;
                  double a1 = (Math.PI * 2) * (i + 1) / 40.0;
                  float u0_in = (float)Math.cos(a0) * innerT * 0.5F + 0.5F;
                  float v0_in = (float)Math.sin(a0) * innerT * 0.5F + 0.5F;
                  float u1_in = (float)Math.cos(a1) * innerT * 0.5F + 0.5F;
                  float v1_in = (float)Math.sin(a1) * innerT * 0.5F + 0.5F;
                  float u1_out = (float)Math.cos(a1) * 0.5F + 0.5F;
                  float v1_out = (float)Math.sin(a1) * 0.5F + 0.5F;
                  float u0_out = (float)Math.cos(a0) * 0.5F + 0.5F;
                  float v0_out = (float)Math.sin(a0) * 0.5F + 0.5F;
                  bufRim.addVertex(matrix, (float)Math.cos(a0) * innerR, innerH, (float)Math.sin(a0) * innerR)
                     .setUv(u0_in, v0_in)
                     .setColor(r, g, b, 1.0F);
                  bufRim.addVertex(matrix, (float)Math.cos(a1) * innerR, innerH, (float)Math.sin(a1) * innerR)
                     .setUv(u1_in, v1_in)
                     .setColor(r, g, b, 1.0F);
                  bufRim.addVertex(matrix, (float)Math.cos(a1) * 0.65F, 0.0F, (float)Math.sin(a1) * 0.65F)
                     .setUv(u1_out, v1_out)
                     .setColor(r, g, b, 1.0F);
                  bufRim.addVertex(matrix, (float)Math.cos(a0) * 0.65F, 0.0F, (float)Math.sin(a0) * 0.65F)
                     .setUv(u0_out, v0_out)
                     .setColor(r, g, b, 1.0F);
               }

               BufferUploader.drawWithShader(bufRim.buildOrThrow());
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
               RenderSystem.disableCull();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(false);
               r = cOuter.getRed() / 255.0F;
               g = cOuter.getGreen() / 255.0F;
               b = cOuter.getBlue() / 255.0F;
               RenderSystem.setShader(CoreShaders.POSITION_COLOR);
               BufferBuilder glowRimBuf = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               innerR = 0.598F;
               innerH = 0.68899995F;
               innerT = 0.7F;
               float outerAlpha = 0.0F;

               for (int i = 0; i < 40; i++) {
                  double a0 = (Math.PI * 2) * i / 40.0;
                  double a1 = (Math.PI * 2) * (i + 1) / 40.0;
                  float x0_in = (float)Math.cos(a0) * innerR;
                  float z0_in = (float)Math.sin(a0) * innerR;
                  float x1_in = (float)Math.cos(a1) * innerR;
                  float z1_in = (float)Math.sin(a1) * innerR;
                  float x0_out = (float)Math.cos(a0) * innerH;
                  float z0_out = (float)Math.sin(a0) * innerH;
                  float x1_out = (float)Math.cos(a1) * innerH;
                  float z1_out = (float)Math.sin(a1) * innerH;
                  glowRimBuf.addVertex(matrix, x0_in, 0.005F, z0_in).setColor(r, g, b, innerT);
                  glowRimBuf.addVertex(matrix, x1_in, 0.005F, z1_in).setColor(r, g, b, innerT);
                  glowRimBuf.addVertex(matrix, x1_out, 0.001F, z1_out).setColor(r, g, b, outerAlpha);
                  glowRimBuf.addVertex(matrix, x0_out, 0.001F, z0_out).setColor(r, g, b, outerAlpha);
               }

               BufferUploader.drawWithShader(glowRimBuf.buildOrThrow());
               RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
               RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
               BufferBuilder bloomBaseBuf = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
               float bSize = 0.663F;
               bloomBaseBuf.addVertex(matrix, -bSize, 0.005F, bSize).setUv(0.0F, 1.0F).setColor(r, g, b, 0.4F);
               bloomBaseBuf.addVertex(matrix, bSize, 0.005F, bSize).setUv(1.0F, 1.0F).setColor(r, g, b, 0.4F);
               bloomBaseBuf.addVertex(matrix, bSize, 0.005F, -bSize).setUv(1.0F, 0.0F).setColor(r, g, b, 0.4F);
               bloomBaseBuf.addVertex(matrix, -bSize, 0.005F, -bSize).setUv(0.0F, 0.0F).setColor(r, g, b, 0.4F);
               BufferUploader.drawWithShader(bloomBaseBuf.buildOrThrow());
               matrices.popPose();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   private Color lerpColor(Color a, Color b, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return new Color(
         (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
         (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
         (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t)
      );
   }

   private float smoothstep(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }
}
