package xyz.angames.astolfoclient.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import xyz.angames.astolfoclient.client.render.models.CowModel;
import xyz.angames.astolfoclient.client.render.models.RabbitModel;

@Environment(EnvType.CLIENT)
public class CustomModelRenderer {
   private final RabbitModel rabbitModel = new RabbitModel();
   private final CowModel cowModel = new CowModel();
   private static final ResourceLocation RABBIT_TEXTURE = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/models/rabbit.png");
   private static final ResourceLocation AMOGUS_TEXTURE = ResourceLocation.fromNamespaceAndPath("astolfoclient", "textures/models/amogus.png");

   public void render(PlayerRenderState state, PoseStack matrices, MultiBufferSource vertexConsumers, int light, String mode, PlayerModel baseModel) {
      if (mode.equals("Rabbit")) {
         this.rabbitModel.setAngles(state, baseModel);
         VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.entityTranslucent(RABBIT_TEXTURE));
         this.rabbitModel.render(matrices, buffer, light);
      } else if (mode.equals("Cow")) {
         this.cowModel.render(matrices, vertexConsumers, state, light);
      } else if (mode.equals("Amogus")) {
      }
   }
}
