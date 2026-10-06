package dev.stormdlc.combat;

public enum MovementMode {
    OFF("Off"), FOLLOW("Follow Target"), ORBIT("Target Strafe");
    private final String label;
    MovementMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
