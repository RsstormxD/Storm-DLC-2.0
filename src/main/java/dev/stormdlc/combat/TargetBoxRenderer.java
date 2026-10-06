package dev.stormdlc.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TargetBoxRenderer {
    private TargetBoxRenderer() {}

    public static void draw(WorldRenderContext context, AABB box, int color) {
        PoseStack matrices = context.matrixStack();
        if (matrices == null || context.consumers() == null) return;
        Vec3 camera = context.camera().getPosition();
        matrices.pushPose();
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z);
            ShapeRenderer.renderLineBox(matrices, context.consumers().getBuffer(RenderType.lines()), box,
                ((color >>> 16) & 255) / 255.0F, ((color >>> 8) & 255) / 255.0F,
                (color & 255) / 255.0F, ((color >>> 24) & 255) / 255.0F);
        } finally { matrices.popPose(); }
    }
}
