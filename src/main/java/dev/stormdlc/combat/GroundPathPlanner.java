package dev.stormdlc.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class GroundPathPlanner {
    private record Node(BlockPos cell, Vec3 point, double cost, double heuristic, Node parent) {}
    private static final int[][] DIRECTIONS = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

    public List<Vec3> plan(Minecraft client, LivingEntity target, Vec3 destination, double radius, boolean jump) {
        if (!TargetSelector.canAct(client, target) || !RotationMath.finite(destination)) return List.of();
        AABB targetBox = target.getBoundingBox().move(destination.subtract(target.position()));
        Vec3 start = client.player.position();
        BlockPos origin = BlockPos.containing(start);
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble((Node n) -> n.cost + n.heuristic)
            .thenComparingDouble(Node::heuristic).thenComparingInt(n -> n.cell.getX()).thenComparingInt(n -> n.cell.getZ()));
        Map<BlockPos, Node> best = new HashMap<>();
        Node root = new Node(origin, start, 0, heuristic(start, destination, radius), null), closest = null;
        open.add(root);
        best.put(origin, root);
        long deadline = System.nanoTime() + 2_500_000L;
        for (int visited = 0; !open.isEmpty() && visited < 192; visited++) {
            if (visited > 8 && System.nanoTime() >= deadline) break;
            Node node = open.poll();
            if (best.get(node.cell) != node) continue;
            if (node != root && (closest == null || node.heuristic < closest.heuristic)) closest = node;
            if (horizontalDistance(node.point, destination) <= radius && Math.abs(node.point.y - destination.y) <= 1.25
                && visibleFrom(client, node.point, targetBox))
                return reconstruct(node);
            for (int[] direction : DIRECTIONS) {
                int x = node.cell.getX() + direction[0], z = node.cell.getZ() + direction[1];
                if (Math.abs(x - origin.getX()) > 14 || Math.abs(z - origin.getZ()) > 14) continue;
                Vec3 next = standingPoint(client, x, z, node.point.y, jump);
                if (next == null) continue;
                boolean diagonal = direction[0] != 0 && direction[1] != 0;
                if (diagonal && (standingPoint(client, x, node.cell.getZ(), node.point.y, jump) == null
                    || standingPoint(client, node.cell.getX(), z, node.point.y, jump) == null)) continue;
                if (!clearEdge(client, node.point, next, jump)) continue;
                BlockPos cell = BlockPos.containing(next);
                double rise = next.y - node.point.y;
                double cost = node.cost + (diagonal ? Math.sqrt(2) : 1) + Math.max(0, rise) * .8 + Math.max(0, -rise) * .25;
                Node known = best.get(cell);
                if (known != null && known.cost <= cost) continue;
                Node candidate = new Node(cell, next, cost, heuristic(next, destination, radius), node);
                best.put(cell, candidate);
                open.add(candidate);
            }
        }
        return closest == null ? List.of() : reconstruct(closest);
    }

    private static double heuristic(Vec3 point, Vec3 destination, double radius) {
        return Math.max(0, horizontalDistance(point, destination) - radius) + Math.abs(point.y - destination.y) * .35;
    }

    private static boolean visibleFrom(Minecraft client, Vec3 feet, AABB targetBox) {
        Vec3 eye = feet.add(0, client.player.getEyeHeight(), 0);
        AABB inside = RotationMath.interior(targetBox);
        for (Vec3 point : new Vec3[]{TargetSelector.closestPoint(eye, inside), inside.getCenter(),
            new Vec3(inside.getCenter().x, inside.maxY, inside.getCenter().z)}) {
            HitResult hit = client.level.clip(new ClipContext(eye, point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) < 1.0e-6) return true;
        }
        return false;
    }

    public static double horizontalDistance(Vec3 a, Vec3 b) { return Math.hypot(a.x - b.x, a.z - b.z); }

    private static List<Vec3> reconstruct(Node end) {
        ArrayList<Vec3> reversed = new ArrayList<>();
        for (Node node = end; node.parent != null; node = node.parent) reversed.add(node.point);
        java.util.Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private static Vec3 standingPoint(Minecraft client, int x, int z, double currentY, boolean jump) {
        int base = (int) Math.floor(currentY);
        for (int supportY = base; supportY >= base - 2; supportY--) {
            BlockPos support = new BlockPos(x, supportY, z);
            if (!client.level.hasChunkAt(support) || !client.level.getWorldBorder().isWithinBounds(support)) continue;
            var state = client.level.getBlockState(support);
            if (hazard(state) || state.getFluidState().is(FluidTags.LAVA)) continue;
            var shape = state.getCollisionShape(client.level, support, CollisionContext.of(client.player));
            if (shape.isEmpty()) continue;
            double top = Double.NEGATIVE_INFINITY;
            for (AABB piece : shape.toAabbs())
                if (.5 >= piece.minX && .5 <= piece.maxX && .5 >= piece.minZ && .5 <= piece.maxZ)
                    top = Math.max(top, supportY + piece.maxY);
            double change = top - currentY;
            if (!Double.isFinite(top) || change > (jump ? 1.05 : .6) || change < -1.0) continue;
            Vec3 point = new Vec3(x + .5, top + .002, z + .5);
            if (clearBody(client, point) && supported(client, point)) return point;
        }
        return null;
    }

    private static boolean hazard(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.CACTUS) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
            || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)
            || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private static AABB body(Minecraft client, Vec3 feet) {
        double half = client.player.getBbWidth() / 2.0 + .035;
        return new AABB(feet.x - half, feet.y + .01, feet.z - half, feet.x + half,
            feet.y + client.player.getBbHeight() - .01, feet.z + half);
    }

    private static boolean clearBody(Minecraft client, Vec3 feet) {
        AABB box = body(client, feet);
        for (double x : new double[]{box.minX, box.maxX}) for (double z : new double[]{box.minZ, box.maxZ}) {
            BlockPos pos = BlockPos.containing(x, feet.y, z);
            if (!client.level.hasChunkAt(pos) || !client.level.getWorldBorder().isWithinBounds(pos)) return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = client.level.getBlockState(pos);
            if (hazard(state) || state.getFluidState().is(FluidTags.LAVA)) return false;
        }
        return client.level.noCollision(client.player, box);
    }

    private static boolean supported(Minecraft client, Vec3 feet) {
        int supported = 0;
        for (double dx : new double[]{-.18, .18}) for (double dz : new double[]{-.18, .18}) {
            BlockPos pos = BlockPos.containing(feet.x + dx, feet.y - .05, feet.z + dz);
            if (!client.level.hasChunkAt(pos)) return false;
            var state = client.level.getBlockState(pos);
            if (hazard(state) || !state.getFluidState().isEmpty()) return false;
            var shape = state.getCollisionShape(client.level, pos, CollisionContext.of(client.player));
            for (AABB piece : shape.toAabbs()) {
                AABB world = piece.move(pos);
                if (feet.x + dx >= world.minX && feet.x + dx <= world.maxX && feet.z + dz >= world.minZ
                    && feet.z + dz <= world.maxZ && world.maxY >= feet.y - .12 && world.maxY <= feet.y + .02) {
                    supported++;
                    break;
                }
            }
        }
        if (supported < 2) return false;
        BlockPos center = BlockPos.containing(feet.x, feet.y - .05, feet.z);
        var state = client.level.getBlockState(center);
        if (hazard(state) || !state.getFluidState().isEmpty()) return false;
        for (AABB piece : state.getCollisionShape(client.level, center, CollisionContext.of(client.player)).toAabbs()) {
            AABB world = piece.move(center);
            if (feet.x >= world.minX && feet.x <= world.maxX && feet.z >= world.minZ && feet.z <= world.maxZ
                && world.maxY >= feet.y - .12 && world.maxY <= feet.y + .02) return true;
        }
        return false;
    }

    private static boolean clearEdge(Minecraft client, Vec3 from, Vec3 to, boolean jump) {
        double rise = to.y - from.y;
        if (rise > .6 && !jump) return false;
        double travelY = Math.max(from.y, to.y);
        if (rise > .6 && !clearBody(client, new Vec3(from.x, travelY, from.z))) return false;
        for (int i = 1; i <= 4; i++) {
            double t = i / 4.0;
            Vec3 at = new Vec3(from.x + (to.x - from.x) * t, travelY, from.z + (to.z - from.z) * t);
            if (!clearBody(client, at)) return false;
        }
        return true;
    }

    public boolean safeDirection(Minecraft client, Vec3 direction, boolean cliffGuard) {
        if (client.player == null || client.level == null) return false;
        Vec3 start = client.player.position();
        Vec3 ahead = start.add(direction.normalize().scale(.55));
        if (!clearBody(client, ahead)) return false;
        if (!cliffGuard || !client.player.onGround()) return true;
        Vec3 standing = standingPoint(client, (int) Math.floor(ahead.x), (int) Math.floor(ahead.z), start.y, false);
        return standing != null && supported(client, new Vec3(ahead.x, standing.y, ahead.z));
    }

    public boolean reachable(Minecraft client, Vec3 from, Vec3 to, boolean jump) {
        return client.player != null && client.level != null && supported(client, to) && clearEdge(client, from, to, jump);
    }

    public boolean safeInput(Minecraft client, Vec3 direction, double stepY) {
        if (safeDirection(client, direction, true)) return true;
        if (stepY <= client.player.getY() + .05 || stepY > client.player.getY() + 1.1) return false;
        Vec3 ahead = client.player.position().add(direction.normalize().scale(.55));
        Vec3 surface = standingPoint(client, (int) Math.floor(ahead.x), (int) Math.floor(ahead.z), client.player.getY(), true);
        return surface != null && surface.y >= client.player.getY() - .1 && surface.y <= stepY + .1
            && supported(client, new Vec3(ahead.x, surface.y, ahead.z))
            && clearBody(client, new Vec3(ahead.x, stepY, ahead.z));
    }
}
