package xyz.angames.astolfoclient.client.module.modules;

import dev.stormdlc.combat.BlockGrid;
import dev.stormdlc.combat.BlockPlacementModule;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

public final class AutoWebModule extends BlockPlacementModule {
    public final BooleanSetting head = new BooleanSetting("Head Web", true);
    public final BooleanSetting feet = new BooleanSetting("Feet Web", true);
    public final BooleanSetting groundedOnly = new BooleanSetting("Only Grounded", false);
    public final BooleanSetting predictMovement = new BooleanSetting("Predict Movement", false);
    public final NumberSetting predictionTicks = new NumberSetting("Web Prediction Ticks", 1.0, 0.0, 3.0, 0.1);

    public AutoWebModule() {
        super("AutoWeb", "Place cobwebs in eligible targets' occupied cells");
        addSettings(feet, head, groundedOnly, predictMovement, predictionTicks);
        predictionTicks.setVisibility(predictMovement::get);
    }

    @Override
    protected Set<Block> allowedBlocks() { return Set.of(Blocks.COBWEB); }

    @Override
    protected List<BlockPos> positions(Minecraft client) {
        if (groundedOnly.get() && !currentTarget.onGround()) return List.of();
        var box = currentTarget.getBoundingBox();
        if (predictMovement.get()) {
            var velocity = currentTarget.getDeltaMovement().scale(predictionTicks.get());
            if (velocity.lengthSqr() > 1.0) velocity = velocity.normalize();
            var predicted = box.move(velocity);
            if (client.level.noBlockCollision(currentTarget, predicted)) box = predicted;
        }
        return BlockGrid.web(box, client.player.getEyePosition(), feet.get(), head.get());
    }
}
