package xyz.angames.astolfoclient.client.module.setting;

import java.awt.Color;

public final class ColorSetting extends NumberSetting {
    public ColorSetting(String name, int rgb) { super(name, rgb & 0xffffff, 0, 0xffffff, 1); }
    public Color color() { return new Color(getInt()); }
    public void setColor(Color color) { set(color.getRGB() & 0xffffff); }
}
