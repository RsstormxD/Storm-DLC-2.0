package dev.sxmurxy.mre.builders.states;
public record QuadRadiusState(float topLeft,float topRight,float bottomLeft,float bottomRight) {
    public static final QuadRadiusState NO_ROUND = new QuadRadiusState(0);
    public QuadRadiusState(float r){this(r,r,r,r);}
    public float[] values(){return new float[]{topLeft,bottomLeft,topRight,bottomRight};}
}
