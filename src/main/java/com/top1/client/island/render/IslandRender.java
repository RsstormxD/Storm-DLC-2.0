package com.top1.client.island.render;
import com.top1.client.island.font.*;
import dev.stormdlc.render.LegacyRenderer;
import org.joml.Matrix4f;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
public final class IslandRender {
    private static final float SMOOTHNESS=0.5f;
    private static final List<Cmd> QUEUE=new ArrayList<>();
    private static final List<float[]> SCISSORS=new ArrayList<>();
    private static boolean needsSnapshot;
    private static final Deque<Float> OPACITY = new ArrayDeque<>();
    private record Cmd(String pipeline,int texture,float[] verts,int[] colors,float[] params,float[] scissor) {}
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("song-island",path);}
    public static void submitWorld(AbstractTexture texture,float[] verts,int[] colors,Matrix4f model,Matrix4f projection){
        var params=Map.of("SizeSmooth",new float[]{Fonts.MEDIUM.getAtlas().range(),.05f,.5f,0},"Radius",new float[4],"Extra",new float[4],"LocalClip",new float[4]);
        LegacyRenderer.draw("song-island:core/island_msdf",texture.getId(),verts,colors,model,projection,params,null,true);
    }
	public static void pushScissor(float x, float y, float width, float height) {
		SCISSORS.add(new float[] { x, y, width, height });
	}

	public static void popScissor() {
		if(!SCISSORS.isEmpty()) SCISSORS.remove(SCISSORS.size() - 1);
	}

	private static float[] scissor() {
		return SCISSORS.isEmpty() ? null : SCISSORS.get(SCISSORS.size() - 1);
	}

	private static void submit(String pipeline, int tex,
		float[] verts, int[] colors, float[] params) {
		if (!OPACITY.isEmpty()) {
			float opacity = OPACITY.peek();
			colors = colors.clone();
			for (int i = 0; i < colors.length; i++)
				colors[i] = (Math.round((colors[i] >>> 24) * opacity) << 24) | (colors[i] & 0xffffff);
		}
		QUEUE.add(new Cmd(pipeline, tex, verts, colors, params, scissor()));
	}

    public static void pushOpacity(float opacity) {
        OPACITY.push(Math.max(0, Math.min(1, opacity)) * (OPACITY.isEmpty() ? 1 : OPACITY.peek()));
    }

    public static void popOpacity() { if (!OPACITY.isEmpty()) OPACITY.pop(); }

    public static void drawGlass(float x, float y, float width, float height, float radius,
        float hover, float press, float pointerX, float pointerY, int color) {
        if (LegacyRenderer.worldModel() != null) return;
        needsSnapshot = true;
        var window = Minecraft.getInstance().getWindow();
        float sw = window.getGuiScaledWidth(), sh = window.getGuiScaledHeight();
        float u = x / sw, v = (sh - y - height) / sh, tw = width / sw, th = height / sh;
        float[] vertices = {x, y, 0, u, v + th, x, y + height, 0, u, v,
            x + width, y + height, 0, u + tw, v, x + width, y, 0, u + tw, v + th};
        submit("glass", 0, vertices, fill(color, 4),
            params(width, height, .75F, 1, radius, radius, radius, radius,
                hover, press, pointerX, pointerY));
    }

	private static int[] fill(int color, int vertices) {
		int[] colors = new int[vertices];
		java.util.Arrays.fill(colors, color);
		return colors;
	}

	public static void drawRoundedRect(float x, float y, float width, float height,
		float tl, float bl, float tr, float br, int argb) {
		float hp = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
		float vp = SMOOTHNESS / 2.0F + SMOOTHNESS;
		submit("shape", 0,
			quad(x - hp / 2.0F, y - vp / 2.0F, width + hp, height + vp), fill(argb, 4),
			params(width, height, SMOOTHNESS, 1.0F, tl, bl, tr, br, 0, 0, 0, 0));
	}

	public static void drawSquircle(float x, float y, float width, float height, float squirt,
		float tl, float bl, float tr, float br, int argb) {
		float hp = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
		float vp = SMOOTHNESS / 2.0F + SMOOTHNESS;
		submit("shape", 0,
			quad(x - hp / 2.0F, y - vp / 2.0F, width + hp, height + vp), fill(argb, 4),
			params(width, height, SMOOTHNESS, squirt,
				tl * squirt / 2.0F, bl * squirt / 2.0F, tr * squirt / 2.0F, br * squirt / 2.0F, 0, 0, 0, 0));
	}

	public static void drawTexture(ResourceLocation id, float x, float y, float width, float height, int argb) {
		AbstractTexture tex = texture(id);
		if(tex == null) return;
		submit("texture", tex.getId(),
			quad(x, y, width, height), fill(argb, 4),
			params(width, height, SMOOTHNESS, 1.0F, 0, 0, 0, 0, 0, 0, 0, 0));
	}

	public static void drawRoundedTexture(ResourceLocation id, float x, float y, float width, float height,
		float tl, float bl, float tr, float br, int argb) {
		AbstractTexture tex = texture(id);
		if(tex == null) return;
		float hp = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
		float vp = SMOOTHNESS / 2.0F + SMOOTHNESS;
		submit("texture", tex.getId(),
			quad(x - hp / 2.0F, y - vp / 2.0F, width + hp, height + vp), fill(argb, 4),
			params(width, height, SMOOTHNESS, 1.0F, tl, bl, tr, br, 0, 0, 0, 0));
	}

	public static void drawBlur(float x, float y, float width, float height, float blurRadius, float squirt,
		float tl, float bl, float tr, float br, int argb) {
		if(LegacyRenderer.worldModel()!=null)return;
		blurRadius /= 22.5F;
		if(blurRadius <= 0.0F) return;
		needsSnapshot = true;
		Minecraft mc = Minecraft.getInstance();
		float sw = mc.getWindow().getGuiScaledWidth();
		float sh = mc.getWindow().getGuiScaledHeight();
		float u = x / sw;
		float v = (sh - y - height) / sh;
		float tw = width / sw;
		float th = height / sh;
		float[] verts = new float[] {
			x, y, 0, u, v + th,
			x, y + height, 0, u, v,
			x + width, y + height, 0, u + tw, v,
			x + width, y, 0, u + tw, v + th
		};
		submit("blur", 0, verts, fill(argb, 4),
			params(width, height, 0.1F, squirt,
				tl * squirt / 2.0F, bl * squirt / 2.0F, tr * squirt / 2.0F, br * squirt / 2.0F,
				blurRadius, 0, 0, 0));
	}

	public static void drawText(Font font, String text, float x, float y, int argb) {
		text(font, text, x, y, argb, null, false, 0.0F, 0.0F, 0.0F, 0.0F);
	}

	public static void drawFadeoutText(Font font, String text, float x, float y, int argb,
		float fadeStart, float fadeEnd, float maxWidth) {
		drawWindowedText(font, text, x, y, argb, x, x + maxWidth, 7.0F, 0.0F);
	}

	public static void drawWindowedText(Font font, String text, float x, float y, int argb,
		float windowStart, float windowEnd, float fadeRight, float fadeLeft) {
		text(font, text, x, y, argb, null, true, windowStart, windowEnd, fadeRight, fadeLeft);
	}

	public static void drawKaraokeText(Font font, String text, float x, float y, int[] charColors,
		float windowStart, float windowEnd, float fadeRight, float fadeLeft) {
		text(font, text, x, y, 0xFFFFFFFF, charColors, true, windowStart, windowEnd, fadeRight, fadeLeft);
	}

	public static void drawCenteredText(Font font, String text, float x, float y, int argb) {
		text(font, text, x - font.width(text) / 2.0F, y, argb, null, false, 0.0F, 0.0F, 0.0F, 0.0F);
	}

	private static void text(Font font, String text, float x, float y, int argb, int[] charColors,
		boolean windowed, float windowStart, float windowEnd, float fadeRight, float fadeLeft) {
		if(text == null || text.isEmpty()) return;
		MsdfFont msdf = font.getFont();
		float size = font.getSize();
		float thickness = 0.05F;
		List<Integer> charIndices = charColors != null ? new ArrayList<>() : null;
		List<float[]> glyphs = msdf.buildGlyphs(text, size, thickness * 0.5F * size,
			x - 0.75F, y + size * 0.7F, charIndices);
		if(glyphs.isEmpty()) return;
		float[] verts = new float[glyphs.size() * 20];
		int o = 0;
		for(float[] q : glyphs){
			System.arraycopy(q, 0, verts, o, 20);
			o += 20;
		}
		int[] colors = new int[glyphs.size() * 4];
		for(int i = 0; i < glyphs.size(); i++){
			int color = argb;
			if(charColors != null){
				int index = charIndices.get(i);
				if(index >= 0 && index < charColors.length) color = charColors[index];
			}
			colors[i * 4] = color;
			colors[i * 4 + 1] = color;
			colors[i * 4 + 2] = color;
			colors[i * 4 + 3] = color;
		}
		AbstractTexture tex = msdf.getTexture();
		submit("msdf", tex.getId(), verts, colors,
			params(msdf.getAtlas().range(), thickness, 0.5F, 0.0F,
				windowStart, windowEnd, fadeRight, fadeLeft,
				0, 0, 0, windowed ? 1.0F : 0.0F));
	}

	private static AbstractTexture texture(ResourceLocation id) {
		try{
			return Minecraft.getInstance().getTextureManager().getTexture(id);
		}catch(Exception e){
			return null;
		}
	}

	private static float[] params(float a, float b, float c, float d,
		float r0, float r1, float r2, float r3,
		float e0, float e1, float e2, float e3) {
		return new float[] { a, b, c, d, r0, r1, r2, r3, e0, e1, e2, e3 };
	}

	private static float[] quad(float x, float y, float w, float h) {
		return new float[] {
			x, y, 0, 0, 0,
			x, y + h, 0, 0, 1,
			x + w, y + h, 0, 1, 1,
			x + w, y, 0, 1, 0
		};
	}


    public static void flush(){
        try {
            boolean world=LegacyRenderer.worldModel()!=null;
            Matrix4f model=world?LegacyRenderer.worldModel():new Matrix4f();
            var window=Minecraft.getInstance().getWindow();
            Matrix4f projection=world?LegacyRenderer.worldProjection():new Matrix4f().setOrtho(
                0, (float)(window.getWidth()/window.getGuiScale()),
                (float)(window.getHeight()/window.getGuiScale()), 0, -100, 100);
            int snapshot=needsSnapshot?LegacyRenderer.background():0;
            for(Cmd cmd:QUEUE){
                var uniforms=new HashMap<String,float[]>(Map.of("SizeSmooth",Arrays.copyOfRange(cmd.params,0,4),"Radius",Arrays.copyOfRange(cmd.params,4,8),"Extra",Arrays.copyOfRange(cmd.params,8,12)));
                uniforms.put("LocalClip",cmd.scissor==null?new float[]{-100000,-100000,200000,200000}:cmd.scissor);
                boolean background = cmd.pipeline.equals("blur") || cmd.pipeline.equals("glass");
                LegacyRenderer.draw("song-island:core/island_"+cmd.pipeline,background?snapshot:cmd.texture,cmd.verts,cmd.colors,model,projection,uniforms,world?null:cmd.scissor,world);
            }
        }finally{QUEUE.clear();SCISSORS.clear();needsSnapshot=false;}
    }

    public static void drawSkinHead(ResourceLocation skin, float x, float y, float size, float alpha) {
        AbstractTexture tex = texture(skin);
        if (tex == null) return;
        int tint = ((int) Math.max(0, Math.min(255, alpha)) << 24) | 0xffffff;
        float[] parameters = params(size, size, SMOOTHNESS, 1, 2, 2, 2, 2, 0, 0, 0, 0);
        for (int layer = 0; layer < 2; layer++) {
            float u1 = (layer == 0 ? 8.0F : 40.0F) / 64.0F, u2 = u1 + 8.0F / 64.0F;
            float v1 = 8.0F / 64.0F, v2 = 16.0F / 64.0F;
            float[] vertices = {x, y, 0, u1, v1, x, y + size, 0, u1, v2,
                x + size, y + size, 0, u2, v2, x + size, y, 0, u2, v1};
            submit("texture", tex.getId(), vertices, fill(tint, 4), parameters);
        }
    }
}
