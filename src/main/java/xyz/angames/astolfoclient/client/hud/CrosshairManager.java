package xyz.angames.astolfoclient.client.hud;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.SizeState;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.CrosshairModule;

@Environment(EnvType.CLIENT)
public class CrosshairManager {
   private final Minecraft client = Minecraft.getInstance();
   private final Color entityColor = new Color(255, 50, 50);

   public void render(GuiGraphics context) {
      if (this.client.player != null && this.client.options.getCameraType() == CameraType.FIRST_PERSON) {
         CrosshairModule module = (CrosshairModule)AstolfoclientClient.moduleManager.getModuleByName("Crosshair");
         if (module != null && module.isEnabled()) {
            Matrix4f matrix = context.pose().last().pose();
            float x = this.client.getWindow().getGuiScaledWidth() / 2.0F;
            float y = this.client.getWindow().getGuiScaledHeight() / 2.0F;
            float gap = module.getGap();
            if (module.hasDynamicGap()) {
               float cooldown = 1.0F - this.client.player.getAttackStrengthScale(0.0F);
               gap += 8.0F * cooldown * cooldown;
            }

            float thickness = module.getThickness();
            float length = module.getLength();
            Color color = module.usesEntityColor() && this.client.hitResult != null && this.client.hitResult.getType() == Type.ENTITY
               ? this.entityColor
               : Color.WHITE;
            color = new Color(xyz.angames.astolfoclient.client.config.VisualColors.resolve(module, color.getRGB(), 0L));
            Builder.rectangle()
               .size(new SizeState(thickness, length))
               .color(new QuadColorState(color))
               .build()
               .render(matrix, x - thickness / 2.0F, y - gap - length);
            Builder.rectangle().size(new SizeState(thickness, length)).color(new QuadColorState(color)).build().render(matrix, x - thickness / 2.0F, y + gap);
            Builder.rectangle()
               .size(new SizeState(length, thickness))
               .color(new QuadColorState(color))
               .build()
               .render(matrix, x - gap - length, y - thickness / 2.0F);
            Builder.rectangle().size(new SizeState(length, thickness)).color(new QuadColorState(color)).build().render(matrix, x + gap, y - thickness / 2.0F);
         }
      }
   }
}
