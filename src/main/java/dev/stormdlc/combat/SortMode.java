package dev.stormdlc.combat;

public enum SortMode {
    DISTANCE, HEALTH, ARMOR;
    @Override public String toString() {
        return switch (this) { case DISTANCE -> "Distance"; case HEALTH -> "Health"; case ARMOR -> "Armor"; };
    }
}
