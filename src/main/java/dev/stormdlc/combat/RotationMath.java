package dev.stormdlc.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RotationMath {
    private RotationMath() {}

    public record Response(double time, double speed, double acceleration, double jerk) {
        public Response pitch() {
            return new Response(time * 1.08, speed * 0.72, acceleration * 0.72, jerk * 0.72);
        }
    }

    /** Continuous state is kept separately from mouse-step output, retaining sub-GCD movement. */
    public static final class Axis {
        private double position;
        private double velocity;
        private double acceleration;

        public double position() { return position; }
        public double velocity() { return velocity; }

        public void reset(double position) {
            this.position = Double.isFinite(position) ? position : 0.0;
            stop();
        }

        public void stop() { velocity = acceleration = 0.0; }
        public void wrap() { position = Mth.wrapDegrees(position); }
        public void clampPitch() {
            double clamped = Mth.clamp(position, -90.0, 90.0);
            if (clamped != position) stop();
            position = clamped;
        }

        public void direct(double goal) {
            position = goal;
            stop();
        }

        public void spring(double goal, double trackingVelocity, Response response, double dt) {
            double omega = 2.0 / response.time();
            double displacement = position - goal;
            double relativeVelocity = velocity - trackingVelocity;
            double coefficient = relativeVelocity + omega * displacement;
            double decay = Math.exp(-omega * dt);
            double nextPosition = goal + trackingVelocity * dt + (displacement + coefficient * dt) * decay;
            double nextVelocity = trackingVelocity + (relativeVelocity - omega * coefficient * dt) * decay;
            double nextAcceleration = (nextVelocity - velocity) / dt;
            if (Math.abs(nextVelocity) <= response.speed() && Math.abs(nextAcceleration) <= response.acceleration()) {
                double step = nextPosition - position;
                finish(goal, step, nextVelocity, nextAcceleration);
            } else advance(goal, nextVelocity, response, dt);
        }

        public void brake(double goal, double trackingVelocity, Response response, double dt, boolean minimumJerk) {
            double error = goal - position;
            if (Math.abs(error) < 1.0e-8) { position = goal; stop(); return; }
            double stoppingSpeed = Math.sqrt(2.0 * response.acceleration() * Math.abs(error));
            double wanted = error / response.time() + trackingVelocity;
            double envelope = Math.min(response.speed(), stoppingSpeed + Math.abs(trackingVelocity));
            wanted = Mth.clamp(wanted, -envelope, envelope);
            if (minimumJerk) {
                // Quintic blending reduces the controller gain near the final angle.
                double proximity = smooth01(Math.abs(error) / 18.0);
                wanted *= 0.64 + 0.36 * proximity;
            }
            advance(goal, wanted, response, dt);
        }

        private void advance(double goal, double wantedVelocity, Response response, double dt) {
            wantedVelocity = Mth.clamp(wantedVelocity, -response.speed(), response.speed());
            double wantedAcceleration = Mth.clamp((wantedVelocity - velocity) / dt,
                -response.acceleration(), response.acceleration());
            double jerkStep = response.jerk() * dt;
            double nextAcceleration = Mth.clamp(wantedAcceleration, acceleration - jerkStep, acceleration + jerkStep);
            double nextVelocity = Mth.clamp(velocity + nextAcceleration * dt, -response.speed(), response.speed());
            double step = (velocity + nextVelocity) * 0.5 * dt;
            finish(goal, step, nextVelocity, nextAcceleration);
        }

        private void finish(double goal, double step, double nextVelocity, double nextAcceleration) {
            double error = goal - position;
            if (!Double.isFinite(step) || !Double.isFinite(nextVelocity)) { reset(goal); return; }
            if (step * error >= 0.0 && Math.abs(step) >= Math.abs(error)) {
                position = goal;
                stop();
            } else {
                position += step;
                velocity = nextVelocity;
                acceleration = nextAcceleration;
            }
        }

        public void limit(double previous, double factor) {
            position = previous + (position - previous) * factor;
            velocity *= factor;
            acceleration *= factor;
        }
    }

    public static double smooth01(double value) {
        double t = Mth.clamp(value, 0.0, 1.0);
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    public static Response response(RotationMode mode, double configuredSpeed, double angularError) {
        double speed = Mth.clamp(configuredSpeed, 0.1, 1.0);
        return switch (mode) {
            case MINESTAR_V1 -> new Response(0.035 + 0.055 * (1.0 - speed),
                1100 + 1600 * speed, 28000 + 36000 * speed, Double.POSITIVE_INFINITY);
            case MINESTAR_V2 -> new Response(0.065 + 0.16 * (1.0 - speed),
                900 + 1400 * speed, 22000 + 30000 * speed, Double.POSITIVE_INFINITY);
            case POLAR -> new Response(0.065 + 0.085 * (1.0 - speed),
                650 + 1250 * speed, 14000 + 22000 * speed, 280000 + 440000 * speed);
            case AC_V2 -> {
                double acquisition = smooth01(angularError / 45.0);
                yield new Response((0.052 + 0.075 * (1.0 - speed)) * (1.2 - 0.3 * acquisition),
                    (820 + 1500 * speed) * (0.62 + 0.38 * acquisition),
                    19000 + 28000 * speed, 360000 + 560000 * speed);
            }
            case HVH -> new Response(0.05, 3600, 100000, Double.POSITIVE_INFINITY);
        };
    }

    public static double mouseStep(Minecraft client) {
        double sensitivity = Mth.clamp(client.options.sensitivity().get(), 0.0, 1.0);
        double scale = sensitivity * 0.6 + 0.2;
        return scale * scale * scale * 8.0 * 0.15;
    }

    public static float quantize(float delta, double step) {
        if (!Float.isFinite(delta) || !Double.isFinite(step) || step <= 0.0) return 0.0F;
        return (float) (Math.rint(delta / step) * step);
    }

    public static float quantizePitch(double delta, double step, float previousPitch) {
        double count = Math.rint(delta / step);
        count = Mth.clamp(count, Math.ceil((-90.0 - previousPitch) / step),
            Math.floor((90.0 - previousPitch) / step));
        return (float) (count * step);
    }

    public static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    public static boolean finite(AABB box) {
        return box != null && Double.isFinite(box.minX) && Double.isFinite(box.maxX)
            && Double.isFinite(box.minY) && Double.isFinite(box.maxY)
            && Double.isFinite(box.minZ) && Double.isFinite(box.maxZ)
            && box.getXsize() > 0 && box.getYsize() > 0 && box.getZsize() > 0;
    }

    public static AABB interior(AABB box) {
        double inset = Math.min(0.025, Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) * 0.15);
        return box.deflate(Math.max(0.0, inset));
    }

    public static Vec3 clampPoint(Vec3 point, AABB box) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY),
            Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    public static Vec3 relative(Vec3 point, AABB box) {
        return new Vec3((point.x - box.minX) / box.getXsize(), (point.y - box.minY) / box.getYsize(),
            (point.z - box.minZ) / box.getZsize());
    }

    public static Vec3 absolute(Vec3 relative, AABB box) {
        return new Vec3(Mth.lerp(relative.x, box.minX, box.maxX), Mth.lerp(relative.y, box.minY, box.maxY),
            Mth.lerp(relative.z, box.minZ, box.maxZ));
    }

    public static double angularDistance(SilentRotations.Angles from, SilentRotations.Angles to) {
        double cosine = Mth.clamp(from.direction().dot(to.direction()), -1.0, 1.0);
        double radians = Math.acos(cosine);
        double degrees = Math.toDegrees(radians);
        return degrees * degrees;
    }

    public static void limitTurn(Axis yaw, Axis pitch, double previousYaw, double previousPitch, double maximum) {
        double yawStep = yaw.position() - previousYaw;
        double pitchStep = pitch.position() - previousPitch;
        double length = Math.hypot(yawStep * Math.cos(Math.toRadians((previousPitch + pitch.position()) * 0.5)), pitchStep);
        if (length > maximum && length > 1.0e-8) {
            double factor = maximum / length;
            yaw.limit(previousYaw, factor);
            pitch.limit(previousPitch, factor);
        }
    }
}
