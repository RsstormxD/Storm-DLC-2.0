package dev.stormdlc.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import xyz.angames.astolfoclient.client.util.FriendsManager;

public final class BlockPlacementService {
    private static final Map<BlockPos, Long> RECENT = new HashMap<>();
    private static ClientLevel level;
    private static long budgetTick = Long.MIN_VALUE;
    private static int spent;

    private BlockPlacementService() {}

    public static void resetContext() {
        RECENT.clear();
        level = null;
        budgetTick = Long.MIN_VALUE;
        spent = 0;
    }

    public static boolean tryPlace(Minecraft client, LivingEntity target, BlockPos position, Set<Block> allowed, double range,
        boolean rotate, boolean inventorySwap) {
        if (client.gameMode == null || !TargetSelector.canAct(client, target) || allowed.isEmpty()) return false;
        if (level != client.level) {
            resetContext();
            level = client.level;
        }
        long tick = level.getGameTime();
        if (budgetTick != tick) {
            budgetTick = tick;
            spent = 0;
            RECENT.entrySet().removeIf(entry -> entry.getValue() <= tick);
        }
        if (spent >= 4 || RECENT.containsKey(position) || level.isOutsideBuildHeight(position)
            || !level.hasChunkAt(position) || !level.getWorldBorder().isWithinBounds(position)
            || !level.getBlockState(position).isAir()) return false;
        AABB occupied = new AABB(position).deflate(1.0e-5);
        if (!level.getEntitiesOfClass(Player.class, occupied, FriendsManager::isFriend).isEmpty()) return false;
        int slot = HotbarSlots.findItem(client.player, stack -> !stack.isEmpty()
            && stack.getItem() instanceof BlockItem item && allowed.contains(item.getBlock()), inventorySwap);
        if (slot < 0) return false;
        BlockItem item = (BlockItem) client.player.getInventory().getItem(slot).getItem();
        BlockState placed = item.getBlock().defaultBlockState();
        if (!placed.canSurvive(level, position) || !level.isUnobstructed(placed, position, CollisionContext.of(client.player))) return false;
        double effectiveRange = Math.min(range, client.player.blockInteractionRange());
        BlockHitResult hit = findSupport(client, position, effectiveRange);
        if (hit == null) return false;
        var stack = client.player.getInventory().getItem(slot);
        BlockPlaceContext placement = new BlockPlaceContext(client.player, InteractionHand.MAIN_HAND, stack, hit);
        if (!placement.canPlace() || !placement.getClickedPos().equals(position)) return false;
        if (!TargetSelector.canAct(client, target)) return false;
        spent++;
        RECENT.put(position.immutable(), tick + 3);
        InteractionResult result = HotbarSlots.withItem(client, slot, () -> HotbarSlots.withSecondaryUse(client, () -> {
            if (!TargetSelector.canAct(client, target) || !client.level.getBlockState(position).isAir()) return InteractionResult.FAIL;
            var held = client.player.getMainHandItem();
            if (!(held.getItem() instanceof BlockItem block) || !allowed.contains(block.getBlock())) return InteractionResult.FAIL;
            if (rotate) return RotationPackets.with(client, SilentRotations.calculate(client.player.getEyePosition(), hit.getLocation()),
                () -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
            return client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        }));
        if (result.consumesAction()) {
            RECENT.put(position.immutable(), tick + 10);
            client.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private static BlockHitResult findSupport(Minecraft client, BlockPos destination, double range) {
        Vec3 eye = client.player.getEyePosition();
        BlockHitResult best = null;
        double closest = range * range;
        for (Direction offset : Direction.values()) {
            BlockPos support = destination.relative(offset);
            if (!client.level.hasChunkAt(support)) continue;
            BlockState state = client.level.getBlockState(support);
            if (state.isAir() || state.getShape(client.level, support).isEmpty()) continue;
            Direction face = offset.getOpposite();
            for (AABB localShape : state.getShape(client.level, support).toAabbs()) {
                AABB shape = localShape.move(support);
                Vec3 center = shape.getCenter();
                double inset = Math.min(0.01, Math.min(shape.getXsize(), Math.min(shape.getYsize(), shape.getZsize())) * 0.1);
                Vec3 near = RotationMath.clampPoint(eye, shape.deflate(inset));
                for (Vec3 sample : new Vec3[]{center, near}) {
                    Vec3 point = switch (face) {
                        case DOWN -> new Vec3(sample.x, shape.minY, sample.z);
                        case UP -> new Vec3(sample.x, shape.maxY, sample.z);
                        case NORTH -> new Vec3(sample.x, sample.y, shape.minZ);
                        case SOUTH -> new Vec3(sample.x, sample.y, shape.maxZ);
                        case WEST -> new Vec3(shape.minX, sample.y, sample.z);
                        case EAST -> new Vec3(shape.maxX, sample.y, sample.z);
                    };
                    double distance = eye.distanceToSqr(point);
                    if (distance > closest) continue;
                    Vec3 end = point.subtract(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.001));
                    BlockHitResult actual = client.level.clip(new ClipContext(eye, end,
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
                    if (actual.getType() == HitResult.Type.MISS || !actual.getBlockPos().equals(support) || actual.getDirection() != face) continue;
                    best = actual;
                    closest = distance;
                }
            }
        }
        return best;
    }
}
