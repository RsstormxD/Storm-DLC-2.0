package xyz.angames.astolfoclient.client.module.modules;

import dev.stormdlc.combat.BlockGrid;
import dev.stormdlc.combat.BlockPlacementModule;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.MultiSelectSetting;

public final class AutoTrapModule extends BlockPlacementModule {
    public final BooleanSetting obsidian = new BooleanSetting("Obsidian", true);
    public final BooleanSetting cryingObsidian = new BooleanSetting("Crying Obsidian", true);
    public final MultiSelectSetting blocks = new MultiSelectSetting("Allowed Blocks", "Choose blocks", obsidian, cryingObsidian);
    public final BooleanSetting roof = new BooleanSetting("Roof", true);
    public final BooleanSetting corners = new BooleanSetting("Corners", false);

    public AutoTrapModule() {
        super("AutoTrap", "Surround eligible targets using allowed hotbar blocks");
        addSettings(blocks, roof, corners);
    }

    @Override
    protected Set<Block> allowedBlocks() {
        Set<Block> allowed = new HashSet<>();
        if (obsidian.get()) allowed.add(Blocks.OBSIDIAN);
        if (cryingObsidian.get()) allowed.add(Blocks.CRYING_OBSIDIAN);
        return Set.copyOf(allowed);
    }

    @Override
    protected List<BlockPos> positions(Minecraft client) {
        return BlockGrid.trap(currentTarget.getBoundingBox(), client.player.getEyePosition(), roof.get(), corners.get());
    }
}
