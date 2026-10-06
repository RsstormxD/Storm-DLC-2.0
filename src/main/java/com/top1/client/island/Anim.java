package com.top1.client.island;

public class Anim {
    private double value, velocity;
    private float previous, target;
    private long duration, updated = System.nanoTime();

    public Anim(long duration) { this.duration = Math.max(1, duration); }

    public void update(float target) {
        if (!Float.isFinite(target)) return;
        long now = System.nanoTime();
        double dt = Math.max(0, Math.min(.1, (now - updated) / 1_000_000_000.0));
        updated = now;
        previous = (float) value;
        this.target = target;
        double omega = 6.0 / (duration / 1000.0);
        double offset = value - target;
        double term = velocity + omega * offset;
        double decay = Math.exp(-omega * dt);
        value = target + (offset + term * dt) * decay;
        velocity = (velocity - omega * term * dt) * decay;
        if (Math.abs(value - target) < .0001 && Math.abs(velocity) < .001) {
            value = target;
            velocity = 0;
        }
    }

    public float get() { return (float) value; }
    public void setDuration(long duration) { this.duration = Math.max(1, duration); }
    public float delta() { return (float) value - previous; }
    public void snap(float value) {
        if (!Float.isFinite(value)) return;
        this.value = this.target = this.previous = value;
        velocity = 0;
        updated = System.nanoTime();
    }
}
