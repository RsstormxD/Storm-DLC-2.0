package xyz.angames.astolfoclient.client.hud;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class BoyKisserManager {
   private final Minecraft client = Minecraft.getInstance();
   private final List<ResourceLocation> frames = new ArrayList<>();
   private int currentFrame = 0;
   private long lastFrameTime = 0L;
   private final int frameDelay = 100;
   private double x = 10.0;
   private double y = 50.0;
   private final double width = 64.0;
   private final double height = 64.0;
   private boolean isDragging = false;
   private double dragX;
   private double dragY;

   public BoyKisserManager() {
      int frameCount = 52;

      for (int i = 0; i < frameCount; i++) {
         this.frames.add(ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/gui/boikiser/boykisser_" + i + ".png"));
      }
   }

   public void render(GuiGraphics context, float delta) {
      long now = (long)(System.nanoTime() / 1000000.0);
      if (now - this.lastFrameTime > 100L) {
         this.currentFrame = (this.currentFrame + 1) % this.frames.size();
         this.lastFrameTime = now;
      }

      ResourceLocation currentTexture = this.frames.get(this.currentFrame);
      context.pose().pushPose();
      float scaleModifier = this.getScaleModifier();
      context.pose().translate((float)this.x, (float)this.y, 0.0F);
      context.pose().scale(scaleModifier, scaleModifier, 1.0F);
      context.pose().translate((float)(-this.x), (float)(-this.y), 0.0F);
      BufferSource provider = this.client.renderBuffers().bufferSource();
      Matrix4f matrix = context.pose().last().pose();
      VertexConsumer vertexConsumer = provider.getBuffer(RenderType.text(currentTexture));
      int light = 15728880;
      float x1 = (float)this.x;
      float y1 = (float)this.y;
      float x2 = (float)(this.x + 64.0);
      float y2 = (float)(this.y + 64.0);
      float z = 0.0F;
      vertexConsumer.addVertex(matrix, x1, y2, z).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(light);
      vertexConsumer.addVertex(matrix, x2, y2, z).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(light);
      vertexConsumer.addVertex(matrix, x2, y1, z).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(light);
      vertexConsumer.addVertex(matrix, x1, y1, z).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(light);
      provider.endBatch();
      context.pose().popPose();
   }

   public float getScaleModifier() {
      Minecraft mc = Minecraft.getInstance();
      double currentGuiScale = mc.getWindow().getGuiScale();
      if (currentGuiScale <= 0.0) {
         currentGuiScale = 2.0;
      }

      return (float)(2.0 / currentGuiScale);
   }

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      float scaleModifier = this.getScaleModifier();
      double effectiveW = 64.0 * scaleModifier;
      double effectiveH = 64.0 * scaleModifier;
      if (button == 0 && mouseX >= this.x && mouseX <= this.x + effectiveW && mouseY >= this.y && mouseY <= this.y + effectiveH) {
         this.isDragging = true;
         this.dragX = mouseX - this.x;
         this.dragY = mouseY - this.y;
         return true;
      } else {
         return false;
      }
   }

   public void onMouseDragged(double mouseX, double mouseY, int button) {
      if (button == 0 && this.isDragging) {
         float scaleModifier = this.getScaleModifier();
         float screenW = this.client.getWindow().getGuiScaledWidth();
         float screenH = this.client.getWindow().getGuiScaledHeight();
         float effectiveW = (float)(64.0 * scaleModifier);
         float effectiveH = (float)(64.0 * scaleModifier);
         this.x = Math.max(0.0, Math.min(Math.max(0.0F, screenW - effectiveW), mouseX - this.dragX));
         this.y = Math.max(0.0, Math.min(Math.max(0.0F, screenH - effectiveH), mouseY - this.dragY));
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.isDragging = false;
      }
   }

   public void onMouseReleased(int button) {
      if (button == 0) {
         this.isDragging = false;
      }
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getWidth() {
      return 64.0;
   }

   public double getHeight() {
      return 64.0;
   }
}
