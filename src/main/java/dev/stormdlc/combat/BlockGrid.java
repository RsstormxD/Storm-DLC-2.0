package dev.stormdlc.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BlockGrid {
    private BlockGrid() {}

    public static List<BlockPos> trap(AABB box, Vec3 eye, boolean roof) {
        return trap(box, eye, roof, false);
    }

    public static List<BlockPos> trap(AABB box, Vec3 eye, boolean roof, boolean corners) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        int minX = Mth.floor(box.minX + 1.0e-5), maxX = Math.min(minX + 4, Mth.floor(box.maxX - 1.0e-5));
        int minZ = Mth.floor(box.minZ + 1.0e-5), maxZ = Math.min(minZ + 4, Mth.floor(box.maxZ - 1.0e-5));
        int feet = Mth.floor(box.minY + 1.0e-5);
        int roofY = Mth.ceil(box.maxY);
        int wallTop = Math.min(feet + 4, roof ? roofY : Mth.floor(box.maxY - 1.0e-5));
        for (int y = feet; y <= wallTop; y++) {
            for (int x = minX; x <= maxX; x++) {
                positions.add(new BlockPos(x, y, minZ - 1));
                positions.add(new BlockPos(x, y, maxZ + 1));
            }
            for (int z = minZ; z <= maxZ; z++) {
                positions.add(new BlockPos(minX - 1, y, z));
                positions.add(new BlockPos(maxX + 1, y, z));
            }
            if (corners) {
                positions.add(new BlockPos(minX - 1, y, minZ - 1));
                positions.add(new BlockPos(minX - 1, y, maxZ + 1));
                positions.add(new BlockPos(maxX + 1, y, minZ - 1));
                positions.add(new BlockPos(maxX + 1, y, maxZ + 1));
            }
        }
        if (roof && roofY <= feet + 4) {
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(x, roofY, z));
        }
        return sorted(positions, eye);
    }

    public static List<BlockPos> web(AABB box, Vec3 eye, boolean head) {
        return web(box, eye, true, head);
    }

    public static List<BlockPos> web(AABB box, Vec3 eye, boolean feetWeb, boolean headWeb) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        int minX = Mth.floor(box.minX + 1.0e-5), maxX = Math.min(minX + 4, Mth.floor(box.maxX - 1.0e-5));
        int minZ = Mth.floor(box.minZ + 1.0e-5), maxZ = Math.min(minZ + 4, Mth.floor(box.maxZ - 1.0e-5));
        int feet = Mth.floor(box.minY + 1.0e-5);
        int head = Math.max(feet, Math.min(feet + 3, Mth.floor(box.maxY - 1.0e-5)));
        for (int y = feet; y <= head; y++) {
            if (!(feetWeb && y == feet) && !(headWeb && y == head)) continue;
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(x, y, z));
        }
        return sorted(positions, eye);
    }

    private static List<BlockPos> sorted(Set<BlockPos> positions, Vec3 eye) {
        List<BlockPos> result = new ArrayList<>(positions);
        result.sort(Comparator.comparingInt((BlockPos position) -> position.getY())
            .thenComparingDouble(position -> eye.distanceToSqr(Vec3.atCenterOf(position))));
        return result;
    }
}
