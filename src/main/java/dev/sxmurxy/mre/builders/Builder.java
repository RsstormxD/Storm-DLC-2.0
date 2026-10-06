package dev.sxmurxy.mre.builders;
import dev.sxmurxy.mre.builders.states.*;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.stormdlc.render.LegacyRenderer;
import org.joml.Matrix4f;
import java.awt.Color;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public final class Builder {
    public static Draw rectangle(){return new Draw("rectangle");}
    public static Draw border(){return new Draw("border");}
    public static Draw blur(){return new Draw("blur");}
    public static Draw texture(){return new Draw("texture");}
    public static Draw text(){return new Draw("text");}
    public static Draw liquidGlass(){return new Draw("liquidglass");}
    public static final class Draw {
        private final String kind;private float width,height,size=8,u,v,uw=1,vh=1,thickness;
        private int texture;private MsdfFont font;private String text="";
        private QuadColorState colors=new QuadColorState(-1);private QuadRadiusState radius=new QuadRadiusState(0);
        private final Map<String,float[]> uniforms=new HashMap<>();
        Draw(String kind){this.kind=kind;smoothness(1);uniforms.put("GlobalAlpha",new float[]{1});uniforms.put("BaseAlpha",new float[]{.2f});uniforms.put("CornerSmoothness",new float[]{2});}
        public Draw size(SizeState s){width=s.width();height=s.height();return this;}
        public Draw size(float s){size=s;return this;}
        public Draw radius(QuadRadiusState r){radius=r;return this;}
        public Draw color(QuadColorState c){colors=c;return this;}
        public Draw color(Color c){return color(new QuadColorState(c));}
        public Draw color(int c){return color(new QuadColorState(c));}
        public Draw font(MsdfFont f){font=f;return this;}
        public Draw text(String t){text=t;return this;}
        public Draw thickness(float t){thickness=t;uniforms.put("Thickness",new float[]{t});return this;}
        public Draw smoothness(float s){uniforms.put("Smoothness",kind.equals("border")?new float[]{s,s}:new float[]{s});return this;}
        public Draw smoothness(float a,float b){uniforms.put("Smoothness",kind.equals("border")?new float[]{a,b}:new float[]{a});uniforms.put("CornerSmoothness",new float[]{b});return this;}
        public Draw blurRadius(float r){uniforms.put("BlurRadius",new float[]{r});return this;}
        public Draw alpha(float global,float base){uniforms.put("GlobalAlpha",new float[]{global});uniforms.put("BaseAlpha",new float[]{base});return this;}
        public Draw fresnel(float power,Color color,float alpha,float mix,boolean invert){return fresnel(power,color.getRGB(),alpha,mix,invert);}
        public Draw fresnel(float power,int color,float alpha,float mix,boolean invert){uniforms.put("FresnelPower",new float[]{power});uniforms.put("FresnelColor",new float[]{(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f});uniforms.put("FresnelAlpha",new float[]{alpha});uniforms.put("FresnelMix",new float[]{mix});uniforms.put("FresnelInvert",new float[]{invert?1:0});return this;}
        public Draw distortStrength(float f){uniforms.put("DistortStrength",new float[]{f});return this;}
        public Draw captureBackground(){return this;}
        public Draw texture(float u,float v,float w,float h,AbstractTexture t){return texture(u,v,w,h,t.getId());}
        public Draw texture(float u,float v,float w,float h,ResourceLocation t){return texture(u,v,w,h,LegacyRenderer.texture(t));}
        public Draw texture(float u,float v,float w,float h,int t){this.u=u;this.v=v;uw=w;vh=h;texture=t;return this;}
        public Draw build(){return this;}
        public void render(Matrix4f matrix,float x,float y){render(matrix,x,y,0);}
        public void render(Matrix4f matrix,float x,float y,float z){
            if(!LegacyRenderer.available())return;
            if(kind.equals("text")){if(font!=null)font.draw(matrix,text,x,y,size,colors.topLeft(),thickness);return;}
            if(width<=0 || height<=0)return;
            uniforms.put("Size",new float[]{width,height});uniforms.put("Radius",radius.values());
            if(kind.equals("blur")||kind.equals("liquidglass")){
                texture=LegacyRenderer.background();var w=Minecraft.getInstance().getWindow();
                var p1=matrix.transformPosition(x,y,0,new org.joml.Vector3f());var p2=matrix.transformPosition(x+width,y+height,0,new org.joml.Vector3f());
                u=p1.x/w.getGuiScaledWidth();v=1-p1.y/w.getGuiScaledHeight();uw=(p2.x-p1.x)/w.getGuiScaledWidth();vh=-(p2.y-p1.y)/w.getGuiScaledHeight();
            }
            float[] verts=LegacyRenderer.quad(x,y,width,height,u,v,uw,vh);for(int i=2;i<verts.length;i+=5)verts[i]=z;
            LegacyRenderer.draw("mre:core/"+kind,texture,verts,colors.values(),matrix,uniforms);
        }
    }
}
