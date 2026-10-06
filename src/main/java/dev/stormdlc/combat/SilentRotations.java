package dev.stormdlc.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SilentRotations {
    public record Angles(float yaw, float pitch) {
        public Angles {
            yaw = Float.isFinite(yaw) ? Mth.wrapDegrees(yaw) : 0;
            pitch = Float.isFinite(pitch) ? Mth.clamp(pitch, -90.0F, 90.0F) : 0;
        }

        public Vec3 direction() {
            double yawRadians = Math.toRadians(yaw), pitchRadians = Math.toRadians(pitch);
            double horizontal = Math.cos(pitchRadians);
            return new Vec3(-Math.sin(yawRadians) * horizontal, -Math.sin(pitchRadians), Math.cos(yawRadians) * horizontal);
        }
    }

    private final RotationMath.Axis yaw = new RotationMath.Axis();
    private final RotationMath.Axis pitch = new RotationMath.Axis();
    private Angles angles;
    private Angles lastDesired;
    private RotationMode previousMode;
    private Vec3 relativeAim;
    private long lastTick = Long.MIN_VALUE;
    private double yawTracking;
    private double pitchTracking;
    private double configuredSpeed = 0.45;

    public void clear() {
        angles = lastDesired = null;
        previousMode = null;
        relativeAim = null;
        lastTick = Long.MIN_VALUE;
        yawTracking = pitchTracking = 0.0;
        yaw.reset(0);
        pitch.reset(0);
    }

    public Angles angles() { return angles; }

    public void retarget() {
        relativeAim = null;
        lastDesired = null;
        lastTick = Long.MIN_VALUE;
        yawTracking = pitchTracking = 0;
        yaw.stop();
        pitch.stop();
    }

    public Angles update(Minecraft client, AABB box, Vec3 preferredPoint, RotationMode mode, double speed) {
        if (client.player == null || client.level == null || !client.player.isAlive()
            || !RotationMath.finite(box) || !RotationMath.finite(preferredPoint) || mode == null) {
            clear();
            return null;
        }
        long tick = client.player.tickCount;
        if (lastTick == tick && mode == previousMode) return angles;
        double dt = lastTick == Long.MIN_VALUE ? 0.05 : Mth.clamp((tick - lastTick) * 0.05, 0.05, 0.15);
        if (lastTick != Long.MIN_VALUE && (tick < lastTick || tick - lastTick > 3)) {
            clear();
            dt = 0.05;
        }
        lastTick = tick;
        configuredSpeed = Double.isFinite(speed) ? Mth.clamp(speed, 0.1, 1.0) : 0.45;
        if (angles == null) angles = new Angles(client.player.getYRot(), client.player.getXRot());
        if (previousMode != mode) {
            yaw.reset(angles.yaw());
            pitch.reset(angles.pitch());
            lastDesired = null;
            yawTracking = pitchTracking = 0.0;
            previousMode = mode;
        }
        Vec3 eye = client.player.getEyePosition();
        Vec3 point = choosePoint(client, box, preferredPoint, angles, mode).orElse(null);
        if (point == null || eye.distanceToSqr(point) < 1.0e-10) {
            yaw.stop();
            pitch.stop();
            lastDesired = null;
            yawTracking = pitchTracking = 0.0;
            return null;
        }
        relativeAim = RotationMath.relative(point, RotationMath.interior(box));
        Angles desired = calculate(eye, point);
        double horizontalSquared = (point.x - eye.x) * (point.x - eye.x) + (point.z - eye.z) * (point.z - eye.z);
        if (horizontalSquared < 1.0e-6) desired = new Angles(angles.yaw(), desired.pitch());
        trackDesired(desired, dt);
        double previousYaw = yaw.position(), previousPitch = pitch.position();
        double goalYaw = previousYaw + Mth.wrapDegrees(desired.yaw() - previousYaw);
        double goalPitch = desired.pitch();
        double angularError = Math.sqrt(RotationMath.angularDistance(new Angles((float) previousYaw, (float) previousPitch), desired));
        RotationMath.Response response = RotationMath.response(mode, configuredSpeed, angularError);
        switch (mode) {
            case MINESTAR_V1 -> {
                yaw.brake(goalYaw, yawTracking * 0.25, response, dt, false);
                pitch.brake(goalPitch, pitchTracking * 0.25, response.pitch(), dt, false);
            }
            case MINESTAR_V2 -> {
                yaw.spring(goalYaw, yawTracking * 0.7, response, dt);
                pitch.spring(goalPitch, pitchTracking * 0.7, response.pitch(), dt);
            }
            case POLAR -> {
                yaw.brake(goalYaw, yawTracking * 0.65, response, dt, false);
                pitch.brake(goalPitch, pitchTracking * 0.65, response.pitch(), dt, false);
                RotationMath.limitTurn(yaw, pitch, previousYaw, previousPitch, response.speed() * dt);
            }
            case AC_V2 -> {
                yaw.brake(goalYaw, yawTracking * 0.85, response, dt, true);
                pitch.brake(goalPitch, pitchTracking * 0.85, response.pitch(), dt, true);
                RotationMath.limitTurn(yaw, pitch, previousYaw, previousPitch, response.speed() * dt);
            }
            case HVH -> {
                yaw.direct(goalYaw);
                pitch.direct(goalPitch);
            }
        }
        yaw.wrap();
        pitch.clampPitch();
        double gcd = RotationMath.mouseStep(client);
        Angles previous = angles;
        angles = new Angles(previous.yaw() + RotationMath.quantize((float) Mth.wrapDegrees(yaw.position() - previous.yaw()), gcd),
            previous.pitch() + RotationMath.quantizePitch(pitch.position() - previous.pitch(), gcd, previous.pitch()));
        if (mode == RotationMode.HVH) {
            double range = eye.distanceTo(box.getCenter()) + box.getSize() + 1.0;
            angles = snapInside(client, previous, desired, box, range, 180.0).orElse(angles);
        }
        return angles;
    }

    private void trackDesired(Angles desired, double dt) {
        if (lastDesired != null) {
            double yawDelta = Mth.wrapDegrees(desired.yaw() - lastDesired.yaw());
            double pitchDelta = desired.pitch() - lastDesired.pitch();
            if (Math.abs(yawDelta) < 70.0 && Math.abs(pitchDelta) < 45.0) {
                double alpha = 1.0 - Math.exp(-dt * 14.0);
                yawTracking = Mth.lerp(alpha, yawTracking, Mth.clamp(yawDelta / dt, -1400.0, 1400.0));
                pitchTracking = Mth.lerp(alpha, pitchTracking, Mth.clamp(pitchDelta / dt, -900.0, 900.0));
            } else yawTracking = pitchTracking = 0.0;
        }
        lastDesired = desired;
    }

    /** Reprojection is used only for the attack packet; it never resets the continuous tracking state. */
    public Optional<Angles> forAttack(Minecraft client, AABB currentBox, double range) {
        if (angles == null || client.player == null || client.level == null || !RotationMath.finite(currentBox)
            || !Double.isFinite(range) || range <= 0.0) return Optional.empty();
        if (aligned(client, angles, currentBox, range)) return Optional.of(angles);
        Vec3 eye = client.player.getEyePosition();
        double distance = Math.max(0.1, eye.distanceTo(currentBox.getCenter()));
        double angularRadius = Math.toDegrees(Math.atan2(currentBox.getSize() * 0.5, distance));
        double baseCorrection = switch (previousMode) {
            case MINESTAR_V1 -> 8.0 + configuredSpeed * 12.0;
            case MINESTAR_V2 -> 4.0 + configuredSpeed * 8.0;
            case POLAR -> 3.0 + configuredSpeed * 8.0;
            case AC_V2 -> 4.0 + configuredSpeed * 10.0;
            case HVH -> 180.0;
        };
        double correction = previousMode == RotationMode.HVH ? 180.0 : Math.min(24.0, Math.max(baseCorrection, angularRadius * 0.65));
        Vec3 preferred = relativeAim == null ? currentBox.getCenter()
            : RotationMath.absolute(relativeAim, RotationMath.interior(currentBox));
        for (Vec3 point : candidates(client, currentBox, preferred, angles)) {
            Angles desired = calculate(eye, point);
            if (RotationMath.angularDistance(angles, desired) > correction * correction || !visible(client, point)) continue;
            Optional<Angles> snapped = snapInside(client, angles, desired, currentBox, range, correction);
            if (snapped.isPresent()) return snapped;
        }
        return Optional.empty();
    }

    private static Optional<Angles> snapInside(Minecraft client, Angles reference, Angles desired,
                                                AABB box, double range, double correction) {
        double step = RotationMath.mouseStep(client);
        long yawCount = Math.round(Mth.wrapDegrees(desired.yaw() - reference.yaw()) / step);
        long pitchCount = Math.round((desired.pitch() - reference.pitch()) / step);
        List<Angles> grid = new ArrayList<>(9);
        for (int horizontal = -1; horizontal <= 1; horizontal++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                double candidatePitch = reference.pitch() + (pitchCount + vertical) * step;
                if (candidatePitch < -90.0 || candidatePitch > 90.0) continue;
                Angles candidate = new Angles((float) (reference.yaw() + (yawCount + horizontal) * step), (float) candidatePitch);
                if (RotationMath.angularDistance(reference, candidate) <= correction * correction + 1.0e-6) grid.add(candidate);
            }
        }
        grid.sort(Comparator.comparingDouble(candidate -> RotationMath.angularDistance(desired, candidate)));
        for (Angles candidate : grid) if (aligned(client, candidate, box, range)) return Optional.of(candidate);
        return Optional.empty();
    }

    public static Angles calculate(Vec3 eye, Vec3 point) {
        Vec3 delta = point.subtract(eye);
        double horizontal = Math.hypot(delta.x, delta.z);
        return new Angles((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F,
            (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)));
    }

    public static boolean visible(Minecraft client, Vec3 point) {
        if (client.player == null || client.level == null || !RotationMath.finite(point)) return false;
        HitResult hit = client.level.clip(new ClipContext(client.player.getEyePosition(), point,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) < 1.0e-6;
    }

    private static List<Vec3> candidates(Minecraft client, AABB box, Vec3 preferred, Angles reference) {
        Vec3 eye = client.player.getEyePosition();
        AABB inside = RotationMath.interior(box);
        List<Vec3> samples = new ArrayList<>(32);
        samples.add(RotationMath.clampPoint(preferred, inside));
        samples.add(RotationMath.clampPoint(new Vec3(inside.getCenter().x, eye.y, inside.getCenter().z), inside));
        samples.add(TargetSelector.closestPoint(eye, inside));
        samples.add(inside.getCenter());
        if (reference != null) inside.clip(eye, eye.add(reference.direction()
            .scale(eye.distanceTo(inside.getCenter()) + inside.getSize() + 1.0)))
            .ifPresent(point -> samples.add(RotationMath.clampPoint(point, inside)));
        for (double x : new double[]{inside.minX, inside.getCenter().x, inside.maxX})
            for (double y : new double[]{inside.minY, inside.getCenter().y, inside.maxY})
                for (double z : new double[]{inside.minZ, inside.getCenter().z, inside.maxZ})
                    samples.add(new Vec3(x, y, z));
        samples.removeIf(point -> eye.distanceToSqr(point) < 1.0e-10);
        if (reference != null) samples.sort(Comparator
            .comparingDouble((Vec3 point) -> RotationMath.angularDistance(reference, calculate(eye, point)))
            .thenComparingDouble(point -> point.distanceToSqr(preferred)));
        else samples.sort(Comparator.comparingDouble(eye::distanceToSqr));
        return samples;
    }

    private Optional<Vec3> choosePoint(Minecraft client, AABB box, Vec3 preferred, Angles reference, RotationMode mode) {
        AABB inside = RotationMath.interior(box);
        Vec3 eye = client.player.getEyePosition();
        Vec3 preferredInside = RotationMath.clampPoint(preferred, inside);
        if (mode != RotationMode.HVH) {
            Vec3 heightPoint = RotationMath.clampPoint(new Vec3(inside.getCenter().x, eye.y, inside.getCenter().z), inside);
            Vec3 stable = new Vec3(preferredInside.x, Mth.lerp(0.55, heightPoint.y, preferredInside.y), preferredInside.z);
            if (relativeAim != null) {
                Vec3 previous = RotationMath.absolute(relativeAim, inside);
                Vec3 filtered = previous.lerp(stable, 0.32 + configuredSpeed * 0.30);
                if (visible(client, filtered) && eye.distanceToSqr(filtered) > 1.0e-10) return Optional.of(filtered);
            }
            if (visible(client, stable) && eye.distanceToSqr(stable) > 1.0e-10) return Optional.of(stable);
        }
        List<Vec3> samples = candidates(client, box, preferredInside, reference);
        if (mode != RotationMode.HVH && relativeAim != null) {
            Vec3 previous = RotationMath.absolute(relativeAim, inside);
            double diagonal = Math.max(1.0e-4, inside.getSize());
            samples.add(previous);
            samples.sort(Comparator.comparingDouble(point ->
                RotationMath.angularDistance(reference, calculate(eye, point))
                    + point.distanceToSqr(previous) / (diagonal * diagonal) * 8.0));
        }
        for (Vec3 point : samples) if (eye.distanceToSqr(point) > 1.0e-10 && visible(client, point)) return Optional.of(point);
        return Optional.empty();
    }

    public static Optional<Vec3> visiblePoint(Minecraft client, AABB box) {
        if (client.player == null || client.level == null || !RotationMath.finite(box)) return Optional.empty();
        for (Vec3 point : candidates(client, box, box.getCenter(), null)) if (visible(client, point)) return Optional.of(point);
        return Optional.empty();
    }

    public static boolean aligned(Minecraft client, Angles angles, AABB box, double range) {
        if (client.player == null || client.level == null || !RotationMath.finite(box)
            || !Double.isFinite(range) || range <= 0.0 || angles == null) return false;
        Vec3 eye = client.player.getEyePosition();
        if (box.contains(eye)) return true;
        Vec3 end = eye.add(angles.direction().scale(range));
        return box.clip(eye, end).filter(point -> visible(client, point)).isPresent();
    }
}
