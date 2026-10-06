package dev.stormdlc.menu;

import com.top1.client.SongIslandClient;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule;

public final class MenuAppearanceScreen extends Screen {
    private final Screen parent;
    private EditBox imagePath;
    private String message = "";
    private boolean error;
    private int startY;

    public MenuAppearanceScreen(Screen parent) {
        super(Component.literal("StormDLC menu"));
        this.parent = parent;
    }

    @Override protected void init() {
        MainMenuModule settings = MainMenuModule.INSTANCE;
        int x = width / 2 - 158;
        startY = Math.max(72, height / 2 - 74);
        addRenderableWidget(Button.builder(Component.literal("Background: " + settings.wallpaper.get()), button -> {
            settings.wallpaper.cycle();
            button.setMessage(Component.literal("Background: " + settings.wallpaper.get()));
            MenuWallpaper.refresh();
        }).bounds(x, startY, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Menu: " + (settings.isEnabled() ? "On" : "Off")), button -> {
            settings.toggle();
            button.setMessage(Component.literal("Menu: " + (settings.isEnabled() ? "On" : "Off")));
        }).bounds(x + 224, startY, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clock: " + (settings.clock.get() ? "On" : "Off")), button -> {
            settings.clock.toggle();
            button.setMessage(Component.literal("Clock: " + (settings.clock.get() ? "On" : "Off")));
        }).bounds(x, startY + 24, 156, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reload wallpaper"), button -> {
            MenuWallpaper.refresh();
            error = false;
            message = "";
        }).bounds(x + 160, startY + 24, 156, 20).build());
        addRenderableWidget(new AbstractSliderButton(x, startY + 48, 316, 20, Component.empty(), settings.dimming.get() / 80.0) {
            { updateMessage(); }
            @Override protected void updateMessage() {
                setMessage(Component.literal("Background dimming: " + Math.round(value * 80) + "%"));
            }
            @Override protected void applyValue() { settings.dimming.set(Math.round(value * 80)); }
        });
        imagePath = new EditBox(font, x, startY + 88, 220, 20, Component.literal("Path to wallpaper"));
        imagePath.setMaxLength(32768);
        imagePath.setHint(Component.literal("Paste a PNG / JPG path"));
        imagePath.setValue(MenuWallpaper.customPath());
        addRenderableWidget(imagePath);
        addRenderableWidget(Button.builder(Component.literal("Use image"), button -> {
            try {
                MenuWallpaper.selectCustom(imagePath.getValue());
                error = false;
                message = "";
                rebuildWidgets();
            } catch (IOException failure) {
                error = true;
                message = failure.getMessage();
            }
        }).bounds(x + 224, startY + 88, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(x, startY + 137, 316, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        MainMenuRenderer.background(graphics, width, height, false);
        SongIslandClient.fonts();
        IslandRender.drawRoundedRect(width / 2.0F - 173, startY - 38, 346, 207,
            20, 20, 20, 20, 0xea08080d);
        IslandRender.drawCenteredText(Fonts.medium(17), "Your StormDLC menu", width / 2.0F, startY - 26, 0xfff5f5fa);
        IslandRender.drawText(Fonts.regular(6), "Custom wallpaper: paste the full image path below",
            width / 2.0F - 157, startY + 76, 0xffb1b1bf);
        String status = message.isEmpty() ? MenuWallpaper.status() : message;
        IslandRender.drawWindowedText(Fonts.regular(6), status, width / 2.0F - 157, startY + 118,
            error ? 0xffff8794 : 0xff9fcab7, width / 2.0F - 158, width / 2.0F + 158, 5, 0);
        IslandRender.flush();
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() {
        if (AstolfoclientClient.configManager != null)
            AstolfoclientClient.configManager.saveConfig(AstolfoclientClient.configManager.getActiveProfile());
        minecraft.setScreen(parent);
    }
}
