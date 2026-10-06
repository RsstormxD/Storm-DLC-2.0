package dev.stormdlc.combat;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import xyz.angames.astolfoclient.client.mixin.ClientPlayerInteractionManagerAccessor;

public final class HotbarSlots {
    private HotbarSlots() {}

    public static int find(LocalPlayer player, Predicate<ItemStack> allowed) {
        int selected = player.getInventory().selected;
        if (selected >= 0 && selected < 9 && allowed.test(player.getInventory().getItem(selected))) return selected;
        for (int slot = 0; slot < 9; slot++) if (allowed.test(player.getInventory().getItem(slot))) return slot;
        return -1;
    }

    public static int findItem(LocalPlayer player, Predicate<ItemStack> allowed, boolean inventory) {
        int slot = find(player, allowed);
        if (slot >= 0 || !inventory || player.containerMenu != player.inventoryMenu
            || !player.containerMenu.getCarried().isEmpty()) return slot;
        for (int index = 9; index < 36; index++) if (allowed.test(player.getInventory().getItem(index))) return index;
        return -1;
    }

    public static InteractionResult withItem(Minecraft client, int source, Supplier<InteractionResult> interaction) {
        if (source < 0) return InteractionResult.FAIL;
        if (source < 9) return withSlot(client, source, interaction);
        var player = client.player;
        var level = client.level;
        var gameMode = client.gameMode;
        if (source > 35 || player == null || level == null || gameMode == null || client.getConnection() == null
            || player.containerMenu != player.inventoryMenu || !player.containerMenu.getCarried().isEmpty()) return InteractionResult.FAIL;
        var menu = player.inventoryMenu;
        int selected = player.getInventory().selected;
        boolean swapped = false;
        try {
            gameMode.handleInventoryMouseClick(menu.containerId, source, selected, ClickType.SWAP, player);
            swapped = true;
            return withSlot(client, selected, interaction);
        } finally {
            if (swapped && client.player == player && client.level == level && client.gameMode == gameMode
                && player.containerMenu == menu && menu.getCarried().isEmpty() && client.getConnection() != null
                && client.getConnection().getConnection().isConnected())
                gameMode.handleInventoryMouseClick(menu.containerId, source, selected, ClickType.SWAP, player);
        }
    }

    public static <T> T withSlot(Minecraft client, int slot, Supplier<T> interaction) {
        if (slot < 0 || slot > 8 || client.player == null || client.gameMode == null || client.level == null)
            throw new IllegalStateException("Hotbar interaction has no valid client context");
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        MultiPlayerGameMode gameMode = client.gameMode;
        int original = player.getInventory().selected;
        try {
            player.getInventory().selected = slot;
            ((ClientPlayerInteractionManagerAccessor) gameMode).invokeSyncSelectedSlot();
            return interaction.get();
        } finally {
            player.getInventory().selected = original;
            if (client.player == player && client.level == level && client.gameMode == gameMode && client.getConnection() != null)
                ((ClientPlayerInteractionManagerAccessor) gameMode).invokeSyncSelectedSlot();
        }
    }

    public static <T> T withSecondaryUse(Minecraft client, Supplier<T> interaction) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.getConnection() == null)
            throw new IllegalStateException("Secondary use has no valid client context");
        var connection = player.connection;
        var level = client.level;
        boolean previous = player.isShiftKeyDown();
        try {
            if (!previous) {
                player.setShiftKeyDown(true);
                connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
            }
            return interaction.get();
        } finally {
            player.setShiftKeyDown(previous);
            if (!previous && client.player == player && client.level == level && client.getConnection() == connection
                && connection.getConnection().isConnected()) {
                connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
            }
        }
    }
}
