package dev.sxmurxy.mre.msdf;
import com.google.gson.*;
import dev.stormdlc.render.LegacyRenderer;
import org.joml.Matrix4f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class MsdfFont {
    public record Glyph(int index,int unicode,float advance,JsonObject plane,JsonObject atlas) {}
    private final Map<Integer,Glyph> glyphs=new HashMap<>();
    private final Map<Long,Float> kerning=new HashMap<>();
    private ResourceLocation atlas;
    private float atlasWidth,atlasHeight,range,ascender;
    public static FontBuilder builder(){return new FontBuilder();}
    public static final class FontBuilder {
        private ResourceLocation atlas,data; private ToIntFunction<Glyph> mapper=Glyph::unicode;
        public FontBuilder name(String name){return this;}
        public FontBuilder atlas(String name){return atlas(ResourceLocation.fromNamespaceAndPath("mre","fonts/"+name+".png"));}
        public FontBuilder data(String name){return data(ResourceLocation.fromNamespaceAndPath("mre","fonts/"+name+".json"));}
        public FontBuilder atlas(ResourceLocation id){atlas=id;return this;}
        public FontBuilder data(ResourceLocation id){data=id;return this;}
        public FontBuilder glyphMapper(ToIntFunction<Glyph> m){mapper=m;return this;}
        public MsdfFont build(){
            try(var in=Minecraft.getInstance().getResourceManager().open(data)) {
                var json=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
                MsdfFont f=new MsdfFont();f.atlas=atlas;var a=json.getAsJsonObject("atlas");
                f.atlasWidth=number(a,"width");f.atlasHeight=number(a,"height");f.range=number(a,"distanceRange");f.ascender=number(json.getAsJsonObject("metrics"),"ascender");
                for(var item:json.getAsJsonArray("glyphs")){var g=item.getAsJsonObject();var glyph=new Glyph(g.has("index")?g.get("index").getAsInt():0,g.has("unicode")?g.get("unicode").getAsInt():0,number(g,"advance"),g.getAsJsonObject("planeBounds"),g.getAsJsonObject("atlasBounds"));f.glyphs.put(mapper.applyAsInt(glyph),glyph);}
                if(json.has("kerning"))for(var item:json.getAsJsonArray("kerning")){var k=item.getAsJsonObject();f.kerning.put(pair(k.get("unicode1").getAsInt(),k.get("unicode2").getAsInt()),number(k,"advance"));}
                return f;
            }catch(Exception e){throw new IllegalStateException("Cannot load font "+data,e);}
        }
    }
    private static long pair(int a,int b){return ((long)a<<32)|(b&0xffffffffL);}
    private static float number(JsonObject o,String k){return o.get(k).getAsFloat();}
    public float getWidth(String text,float size){if(text==null)return 0;float width=0;int prev=-1;for(int i=0;i<text.length();i++){int c=text.charAt(i);if(c==167){i++;continue;}Glyph g=glyphs.get(c);if(g==null)continue;width+=(g.advance+kerning.getOrDefault(pair(prev,c),0f))*size;prev=c;}return width;}
    public float getHeight(float size){return size;}
    public void draw(Matrix4f matrix,String text,float x,float y,float size,int color,float thickness){
        if(text==null || text.isEmpty())return;
        List<float[]> quads=new ArrayList<>();int prev=-1;
        for(int i=0;i<text.length();i++){
            int c=text.charAt(i);if(c==167){i++;continue;}Glyph g=glyphs.get(c);if(g==null)continue;
            x+=kerning.getOrDefault(pair(prev,c),0f)*size;
            if(g.plane!=null && g.atlas!=null){
                float l=number(g.plane,"left"),r=number(g.plane,"right"),t=number(g.plane,"top"),b=number(g.plane,"bottom");
                float al=number(g.atlas,"left"),ar=number(g.atlas,"right"),at=number(g.atlas,"top"),ab=number(g.atlas,"bottom");
                quads.add(LegacyRenderer.quad(x+l*size,y+(0.8f-t)*size,(r-l)*size,(t-b)*size,al/atlasWidth,1-at/atlasHeight,(ar-al)/atlasWidth,(at-ab)/atlasHeight));
            }
            x+=g.advance*size;prev=c;
        }
        float[] verts=new float[quads.size()*20];for(int i=0;i<quads.size();i++)System.arraycopy(quads.get(i),0,verts,i*20,20);
        LegacyRenderer.draw("mre:core/msdf_font",LegacyRenderer.texture(atlas),verts,LegacyRenderer.colors(color,quads.size()*4),matrix,Map.of("Range",new float[]{range},"Thickness",new float[]{thickness},"Smoothness",new float[]{0.5f}));
    }
}
