package dev.sxmurxy.mre.builders.states;
import java.awt.Color;
public record QuadColorState(int topLeft,int topRight,int bottomLeft,int bottomRight) {
    public QuadColorState(int c){this(c,c,c,c);}
    public QuadColorState(Color c){this(c.getRGB());}
    public QuadColorState(Color a,Color b,Color c,Color d){this(a.getRGB(),b.getRGB(),c.getRGB(),d.getRGB());}
    public int[] values(){return new int[]{topLeft,bottomLeft,bottomRight,topRight};}
}
