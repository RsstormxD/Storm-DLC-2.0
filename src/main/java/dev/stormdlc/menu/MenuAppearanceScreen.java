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
import xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule;

public final class MenuAppearanceScreen extends Screen {
    private final Screen parent;
    private EditBox imagePath;
    private Button source;
    private String message = "";
    private boolean error;
    private int startY, panelWidth;

    public MenuAppearanceScreen(Screen parent) {
        super(Component.literal("Menu appearance"));
        this.parent = parent;
    }

    @Override protected void init() {
        MainMenuModule settings = MainMenuModule.INSTANCE;
        panelWidth = Math.min(316, width - 32);
        int x = (width - panelWidth) / 2, gap = 5, third = (panelWidth - gap * 2) / 3;
        startY = Math.max(64, (height - 175) / 2);
        for (int index = 0; index < 3; index++) {
            final int theme = index;
            var button = addRenderableWidget(Button.builder(Component.literal("Theme " + (index + 1)), pressed -> {
                settings.wallpaper.setValue(MainMenuModule.Wallpaper.values()[theme]);
                MenuWallpaper.refresh();
            }).bounds(x + index * (third + gap), startY, third, 23).build());
            GlassButtonRenderer.theme(button, index + 1);
        }
        int half = (panelWidth - gap) / 2;
        source = addRenderableWidget(Button.builder(Component.literal("Wallpaper: " + settings.wallpaper.get()), button -> {
            settings.wallpaper.cycle();
            button.setMessage(Component.literal("Wallpaper: " + settings.wallpaper.get()));
            MenuWallpaper.refresh();
        }).bounds(x, startY + 28, panelWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clock: " + (settings.clock.get() ? "On" : "Off")), button -> {
            settings.clock.toggle();
            button.setMessage(Component.literal("Clock: " + (settings.clock.get() ? "On" : "Off")));
        }).bounds(x, startY + 53, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reload wallpaper"), button -> {
            MenuWallpaper.refresh();
            error = false;
            message = "";
        }).bounds(x + half + gap, startY + 53, half, 20).build());
        addRenderableWidget(new AbstractSliderButton(x, startY + 78, panelWidth, 18, Component.empty(), settings.dimming.get() / 80.0) {
            { updateMessage(); }
            @Override protected void updateMessage() {
                setMessage(Component.literal("Dimming: " + Math.round(value * 80) + "%"));
            }
            @Override protected void applyValue() { settings.dimming.set(Math.round(value * 80)); }
        });
        imagePath = new EditBox(font, x, startY + 112, panelWidth - 85, 19, Component.literal("Path to wallpaper"));
        imagePath.setMaxLength(32768);
        imagePath.setHint(Component.literal("PNG / JPG path"));
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
        }).bounds(x + panelWidth - 80, startY + 112, 80, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(x, startY + 154, panelWidth, 21).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        source.setMessage(Component.literal("Wallpaper: " + MainMenuModule.INSTANCE.wallpaper.get()));
        MainMenuRenderer.background(graphics, width, height, false);
        SongIslandClient.fonts();
        float x = (width - panelWidth) / 2.0F;
        IslandRender.drawGlass(x - 12, startY - 30, panelWidth + 24, 207, 17, 0, 0, .5F, .5F, 0xffeeeeff);
        IslandRender.drawCenteredText(Fonts.medium(14), "Menu appearance", width / 2.0F, startY - 22, 0xfff5f5fa);
        IslandRender.drawText(Fonts.regular(6), "Your own wallpaper", x + 2, startY + 102, 0xffc5c5d0);
        String status = message.isEmpty() ? MenuWallpaper.status() : message;
        IslandRender.drawWindowedText(Fonts.regular(6), status, x + 2, startY + 141,
            error ? 0xffff8794 : 0xff9fcab7, x, x + panelWidth, 5, 0);
        IslandRender.flush();
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() {
        MainMenuRenderer.save();
        minecraft.setScreen(parent);
    }
}
