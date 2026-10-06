package dev.stormdlc.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.events.GuiEventListener;

public final class MenuLayout {
    private static float controlsY = 100;
    private MenuLayout() {}
    public static float controlsY() { return controlsY; }

    public static void arrange(List<? extends GuiEventListener> children, int width, int height) {
        List<AbstractWidget> primary = new ArrayList<>(), secondary = new ArrayList<>(), icons = new ArrayList<>();
        for (var child : children) {
            if (!(child instanceof Button button) || child instanceof PlainTextButton) continue;
            if (button.getWidth() >= 150) primary.add(button);
            else if (button.getWidth() > 24) secondary.add(button);
            else icons.add(button);
        }
        primary.sort(Comparator.comparingInt(AbstractWidget::getY));
        secondary.sort(Comparator.comparingInt(AbstractWidget::getX));
        icons.sort(Comparator.comparingInt(AbstractWidget::getX));
        int rows = primary.size() + (secondary.isEmpty() ? 0 : 1);
        int gap = height < 300 ? 5 : 7;
        int buttonHeight = Math.max(18, Math.min(30, (height - 145 - Math.max(0, rows - 1) * gap) / Math.max(1, rows)));
        int groupHeight = rows * buttonHeight + Math.max(0, rows - 1) * gap;
        int y = Math.max(92, Math.min(height - 43 - groupHeight, Math.round((height - groupHeight) * .56F)));
        int panelWidth = Math.min(236, width - 76);
        int x = (width - panelWidth) / 2;
        controlsY = y;
        for (var button : primary) {
            button.setRectangle(panelWidth, buttonHeight, x, y);
            y += buttonHeight + gap;
        }
        int count = secondary.size();
        int part = count == 0 ? panelWidth : (panelWidth - (count - 1) * gap) / count;
        for (int index = 0; index < count; index++)
            secondary.get(index).setRectangle(part, buttonHeight, x + index * (part + gap), y);
        for (int index = 0; index < icons.size(); index++) {
            var button = icons.get(index);
            int bx = index == 0 ? x - buttonHeight - gap : x + panelWidth + gap + (index - 1) * (buttonHeight + gap);
            button.setRectangle(buttonHeight, buttonHeight, bx, y);
            GlassButtonRenderer.icon(button, index == 0 ? GlassButtonRenderer.Icon.LANGUAGE : GlassButtonRenderer.Icon.ACCESSIBILITY);
        }
    }
}
