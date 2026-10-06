package dev.stormdlc.render;
import com.mojang.blaze3d.systems.RenderSystem;
import static org.lwjgl.opengl.GL33C.*;
public final class SkyRenderState implements AutoCloseable {
    private final int program=glGetInteger(GL_CURRENT_PROGRAM), vao=glGetInteger(GL_VERTEX_ARRAY_BINDING), vbo=glGetInteger(GL_ARRAY_BUFFER_BINDING);
    private final int active=glGetInteger(GL_ACTIVE_TEXTURE),depthFunc=glGetInteger(GL_DEPTH_FUNC);
    private final int srcRgb=glGetInteger(GL_BLEND_SRC_RGB),dstRgb=glGetInteger(GL_BLEND_DST_RGB),srcAlpha=glGetInteger(GL_BLEND_SRC_ALPHA),dstAlpha=glGetInteger(GL_BLEND_DST_ALPHA);
    private final int equationRgb=glGetInteger(GL_BLEND_EQUATION_RGB),equationAlpha=glGetInteger(GL_BLEND_EQUATION_ALPHA);
    private final boolean depth=glIsEnabled(GL_DEPTH_TEST),cull=glIsEnabled(GL_CULL_FACE),blend=glIsEnabled(GL_BLEND),write=glGetBoolean(GL_DEPTH_WRITEMASK);
    private final int texture;
    public SkyRenderState(){RenderSystem.activeTexture(GL_TEXTURE0);texture=glGetInteger(GL_TEXTURE_BINDING_2D);}
    @Override public void close(){
        glUseProgram(program);glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
        RenderSystem.activeTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);RenderSystem.bindTexture(texture);RenderSystem.activeTexture(active);
        RenderSystem.depthMask(write);RenderSystem.depthFunc(depthFunc);
        RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);glBlendEquationSeparate(equationRgb,equationAlpha);
        if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
        if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
        if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
    }
}
