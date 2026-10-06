package xyz.angames.astolfoclient.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import xyz.angames.astolfoclient.client.util.FriendsManager;

public final class FriendsScreen extends Screen {
    private final Screen parent;
    private EditBox input;
    private AutoCloseable subscription;
    private List<String> friends = List.of();
    private int page;
    private int rows;
    private String status = "";

    public FriendsScreen(Screen parent) {
        super(Component.literal("Storm DLC 2.0 — Friends"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (subscription == null) subscription = FriendsManager.subscribe(snapshot -> {
            if (minecraft != null && minecraft.screen == this) rebuild();
        });
        rebuild();
    }

    private void rebuild() {
        String value = input == null ? "" : input.getValue();
        clearWidgets();
        friends = FriendsManager.getFriends().stream().sorted().toList();
        rows = Math.max(1, Math.min(9, (height - 160) / 22));
        int pages = Math.max(1, (friends.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        int left = width / 2 - 150;
        input = addRenderableWidget(new EditBox(font, left, 42, 210, 20, Component.literal("Nick lub UUID")));
        input.setMaxLength(36);
        input.setValue(value);
        input.setHint(Component.literal("Nick lub UUID"));
        addRenderableWidget(Button.builder(Component.literal("Dodaj"), button -> {
            if (!FriendsManager.isValidIdentifier(input.getValue())) {
                status = "Wpisz poprawny nick albo UUID.";
                return;
            }
            boolean added = FriendsManager.addFriend(input.getValue());
            status = added ? "Dodano przyjaciela." : "Ten wpis już istnieje.";
            input.setValue("");
        }).bounds(left + 216, 42, 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Dodaj gracza z celownika"), button -> {
            if (minecraft != null && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player player) {
                FriendsManager.addFriend(player);
                status = "Dodano nick oraz UUID: " + player.getName().getString();
            } else status = "Najpierw wskaż gracza celownikiem.";
        }).bounds(left, 67, 300, 20).build());
        for (int row = 0; row < rows && page * rows + row < friends.size(); row++) {
            String identifier = friends.get(page * rows + row);
            addRenderableWidget(Button.builder(Component.literal("Usuń"), button -> {
                FriendsManager.removeFriend(identifier);
                status = "Usunięto wpis.";
            }).bounds(left + 242, 96 + row * 22, 58, 20).build());
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuild(); })
            .bounds(left, height - 50, 40, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuild(); })
            .bounds(left + 45, height - 50, 40, 20).build());
        next.active = page < pages - 1;
        addRenderableWidget(Button.builder(Component.literal("Wyczyść"), button -> {
            FriendsManager.clearFriends();
            status = "Wyczyszczono listę.";
        }).bounds(left + 90, height - 50, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Gotowe"), button -> onClose())
            .bounds(left + 185, height - 50, 115, 20).build());
        setInitialFocus(input);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xffedf4ff);
        int left = width / 2 - 150;
        for (int row = 0; row < rows && page * rows + row < friends.size(); row++) {
            String identifier = friends.get(page * rows + row);
            String visible = font.plainSubstrByWidth(identifier, 233);
            graphics.drawString(font, visible, left, 102 + row * 22, 0xff86dcb5);
        }
        graphics.drawCenteredString(font, "Wpisy: " + friends.size() + "  •  strona " + (page + 1), width / 2, height - 71, 0xffb8c7dc);
        graphics.drawCenteredString(font, status, width / 2, height - 23, 0xffb8c7dc);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public void removed() {
        if (subscription != null) {
            try { subscription.close(); }
            catch (Exception failure) { org.slf4j.LoggerFactory.getLogger("StormDLC/Friends").warn("Cannot close friends editor", failure); }
            subscription = null;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
