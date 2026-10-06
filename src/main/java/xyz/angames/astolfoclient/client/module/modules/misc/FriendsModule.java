package xyz.angames.astolfoclient.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.gui.FriendsScreen;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ActionSetting;

public final class FriendsModule extends Module {
    public final ActionSetting manage = new ActionSetting("Manage Friends", () -> {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new FriendsScreen(client.screen));
    });

    public FriendsModule() {
        super("Friends", "Manage protected players by name or UUID", Category.FRIENDS);
        addSettings(manage);
    }
}
