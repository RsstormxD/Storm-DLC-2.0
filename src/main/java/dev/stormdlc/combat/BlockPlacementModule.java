package dev.stormdlc.combat;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.KillAuraModule;

public abstract class BlockPlacementModule extends TargetingModule {
    public final NumberSetting placeRange = new NumberSetting("Place Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberSetting placeDelay = new NumberSetting("Place Delay (ticks)", 2.0, 1.0, 20.0, 1.0);
    public final NumberSetting blocksPerTick = new NumberSetting("Blocks Per Tick", 1.0, 1.0, 4.0, 1.0);
    public final BooleanSetting silentRotations = new BooleanSetting("Silent Rotations", true);
    public final BooleanSetting inventorySwap = new BooleanSetting("Inventory Swap", false);
    public final BooleanSetting auraTarget = new BooleanSetting("Use Aura Target", true);
    private long nextPlacement;

    protected BlockPlacementModule(String name, String description) {
        super(name, description);
        addSettings(placeRange, placeDelay, blocksPerTick, silentRotations, inventorySwap, auraTarget);
    }

    protected abstract Set<Block> allowedBlocks();
    protected abstract List<BlockPos> positions(Minecraft client);

    @Override
    protected void onTargetLost() { nextPlacement = 0; }

    @Override
    public final void onTick() {
        Minecraft client = Minecraft.getInstance();
        var aura = (KillAuraModule) ModuleManager.getModule(KillAuraModule.class);
        setPriorityTarget(auraTarget.get() && aura != null && aura.isEnabled() ? aura.getCurrentTarget() : null);
        if (!selectTarget(client)) return;
        long tick = client.level.getGameTime();
        if (tick < nextPlacement) return;
        Set<Block> allowed = allowedBlocks();
        if (allowed.isEmpty()) return;
        int placed = 0;
        for (BlockPos position : positions(client)) {
            if (!TargetSelector.valid(client, currentTarget, filters())) { clearTarget(); break; }
            if (BlockPlacementService.tryPlace(client, currentTarget, position, allowed, placeRange.get(), silentRotations.get(), inventorySwap.get())) {
                if (++placed >= blocksPerTick.getInt()) break;
            }
        }
        nextPlacement = tick + placeDelay.getInt();
    }

    @Override
    public final void onWorldRender(WorldRenderContext context) {
        if (isEnabled() && targetEsp.get() && TargetSelector.canAct(Minecraft.getInstance(), currentTarget))
            TargetBoxRenderer.draw(context, currentTarget.getBoundingBox().inflate(0.012), 0xffe4bd62);
    }
}
