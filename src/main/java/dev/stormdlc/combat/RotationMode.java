package dev.stormdlc.combat;

public enum RotationMode {
    MINESTAR_V1, MINESTAR_V2, POLAR, AC_V2, HVH;
    @Override public String toString() {
        return switch (this) {
            case MINESTAR_V1 -> "MineStar V1";
            case MINESTAR_V2 -> "MineStar V2";
            case POLAR -> "Polar";
            case AC_V2 -> "AC V2";
            case HVH -> "HvH";
        };
    }
}
