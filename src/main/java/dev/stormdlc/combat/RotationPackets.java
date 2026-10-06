package dev.stormdlc.combat;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class RotationPackets {
    private RotationPackets() {}

    public static <T> T with(Minecraft client, SilentRotations.Angles angles, Supplier<T> action) {
        var player = client.player;
        var level = client.level;
        var connection = client.getConnection();
        if (player == null || level == null || connection == null || !connection.getConnection().isConnected())
            throw new IllegalStateException("Silent interaction has no connected player");
        if (!Float.isFinite(angles.yaw()) || !Float.isFinite(angles.pitch()))
            throw new IllegalArgumentException("Rotation must be finite");
        connection.send(new ServerboundMovePlayerPacket.Rot(angles.yaw(), angles.pitch(), player.onGround(), player.horizontalCollision));
        try {
            return action.get();
        } finally {
            if (client.player == player && client.level == level && client.getConnection() == connection
                && connection.getConnection().isConnected())
                connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround(), player.horizontalCollision));
        }
    }
}
