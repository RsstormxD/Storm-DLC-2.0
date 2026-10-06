package dev.stormdlc.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import static org.lwjgl.opengl.GL33C.*;

/** Small OpenGL 3.2 renderer for the shaders supplied with the two original mods. */
public final class LegacyRenderer {
    private static final Map<String,Integer> PROGRAMS = new HashMap<>();
    private static int vao, vbo, ebo, snapshot;
    private static boolean stopping;
    public static boolean available(){return !stopping && org.lwjgl.glfw.GLFW.glfwGetCurrentContext()!=0;}
    public static void shutdown(){stopping=true;endWorld();}
    private static int snapshotW, snapshotH;
    private static long snapshotFrame = -1, frame;
    private static Matrix4f worldModel, worldProjection;
    private static final Deque<float[]> localClips=new ArrayDeque<>();
    public static void pushLocalClip(float x,float y,float right,float bottom){
        if(!localClips.isEmpty()){float[] p=localClips.peek();x=Math.max(x,p[0]);y=Math.max(y,p[1]);right=Math.min(right,p[2]);bottom=Math.min(bottom,p[3]);}
        localClips.push(new float[]{x,y,Math.max(x,right),Math.max(y,bottom)});
    }
    public static void popLocalClip(){if(!localClips.isEmpty())localClips.pop();}

    public static void beginWorld(Matrix4f model,Matrix4f projection){worldModel=model;worldProjection=projection;}
    public static Matrix4f worldModel(){return worldModel;}
    public static Matrix4f worldProjection(){return worldProjection;}
    public static void endWorld(){worldModel=null;worldProjection=null;localClips.clear();}
    public static void beginFrame() { frame++; }
    public static int texture(ResourceLocation id) { return Minecraft.getInstance().getTextureManager().getTexture(id).getId(); }
    private static String source(String path) throws Exception {
        int colon=path.indexOf(':');
        var id=ResourceLocation.fromNamespaceAndPath(path.substring(0,colon),"shaders/"+path.substring(colon+1));
        String s;
        try(var in=Minecraft.getInstance().getResourceManager().open(id)) { s=new String(in.readAllBytes(),StandardCharsets.UTF_8); }
        var matcher=Pattern.compile("#moj_import <([^>]+)>").matcher(s);
        StringBuffer out=new StringBuffer();
        while(matcher.find()) {
            String child=matcher.group(1); int i=child.indexOf(':');
            String include=source(child.substring(0,i)+":include/"+child.substring(i+1)).replaceAll("(?m)^#version.*$","");
            matcher.appendReplacement(out,Matcher.quoteReplacement(include));
        }
        matcher.appendTail(out); return out.toString();
    }
    private static int shader(String path,int type) throws Exception {
        int id=glCreateShader(type); glShaderSource(id,source(path)); glCompileShader(id);
        if(glGetShaderi(id,GL_COMPILE_STATUS)==0) { String log=glGetShaderInfoLog(id);glDeleteShader(id);throw new IllegalStateException(path+": "+log); }
        return id;
    }
    private static int program(String name) {
        return PROGRAMS.computeIfAbsent(name,key->{
            try {
                int vs=shader(key+".vsh",GL_VERTEX_SHADER), fs=shader(key+".fsh",GL_FRAGMENT_SHADER), p=glCreateProgram();
                glAttachShader(p,vs);glAttachShader(p,fs);
                glBindAttribLocation(p,0,"Position");glBindAttribLocation(p,1,"UV0");glBindAttribLocation(p,2,"Color");
                glLinkProgram(p);glDeleteShader(vs);glDeleteShader(fs);
                if(glGetProgrami(p,GL_LINK_STATUS)==0) throw new IllegalStateException(glGetProgramInfoLog(p));
                return p;
            } catch(Exception e) { throw new IllegalStateException("Storm DLC 2.0 shader "+key,e); }
        });
    }
    public static float[] quad(float x,float y,float w,float h,float u,float v,float uw,float vh) {
        return new float[]{x,y,0,u,v, x,y+h,0,u,v+vh, x+w,y+h,0,u+uw,v+vh, x+w,y,0,u+uw,v};
    }
    public static int[] colors(int c,int count) { int[] out=new int[count];Arrays.fill(out,c);return out; }
    public static int background() {
        if(!available())return 0;
        var fb=Minecraft.getInstance().getMainRenderTarget();
        if(snapshot==0) snapshot=glGenTextures();
        int old=glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D,snapshot);
        if(snapshotW!=fb.width || snapshotH!=fb.height) {
            snapshotW=fb.width;snapshotH=fb.height;
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,snapshotW,snapshotH,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
            snapshotFrame=-1;
        }
        if(snapshotFrame!=frame) { glCopyTexSubImage2D(GL_TEXTURE_2D,0,0,0,0,0,snapshotW,snapshotH);snapshotFrame=frame; }
        glBindTexture(GL_TEXTURE_2D,old);return snapshot;
    }
    public static void draw(String shader,int texture,float[] verts,int[] colors,Matrix4f model,Matrix4f projection,Map<String,float[]> uniforms,float[] scissor,boolean depth) {
        drawFull(shader,texture,verts,colors,model,projection,uniforms,scissor,depth,new Matrix4f());
    }
    private static void drawFull(String shader,int texture,float[] verts,int[] colors,Matrix4f model,Matrix4f projection,Map<String,float[]> uniforms,float[] scissor,boolean depth,Matrix4f localMatrix){
        if(verts.length==0 || !available())return;
        int oldProgram=glGetInteger(GL_CURRENT_PROGRAM), oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING), oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int active=glGetInteger(GL_ACTIVE_TEXTURE);glActiveTexture(GL_TEXTURE0);int oldTex=glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean blend=glIsEnabled(GL_BLEND), cull=glIsEnabled(GL_CULL_FACE), depthTest=glIsEnabled(GL_DEPTH_TEST), clip=glIsEnabled(GL_SCISSOR_TEST), depthMask=glGetBoolean(GL_DEPTH_WRITEMASK);
        int src=glGetInteger(GL_BLEND_SRC_RGB), dst=glGetInteger(GL_BLEND_DST_RGB), srcA=glGetInteger(GL_BLEND_SRC_ALPHA), dstA=glGetInteger(GL_BLEND_DST_ALPHA);
        int[] oldClip=new int[4];glGetIntegerv(GL_SCISSOR_BOX,oldClip);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            int p=program(shader);glUseProgram(p);
            if(vao==0){vao=glGenVertexArrays();vbo=glGenBuffers();ebo=glGenBuffers();}
            glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            int n=verts.length/5;
            ByteBuffer bytes=stack.malloc(n*24);
            for(int i=0;i<n;i++) { for(int j=0;j<5;j++)bytes.putFloat(verts[i*5+j]);int c=colors[i];bytes.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>>24)); }
            bytes.flip();glBufferData(GL_ARRAY_BUFFER,bytes,GL_STREAM_DRAW);
            IntBuffer indices=stack.mallocInt(n/4*6);
            for(int i=0;i<n;i+=4)indices.put(i).put(i+1).put(i+2).put(i+2).put(i+3).put(i);
            indices.flip();glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STREAM_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,24,0);glVertexAttribPointer(1,2,GL_FLOAT,false,24,12);glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,24,20);
            glEnableVertexAttribArray(0);glEnableVertexAttribArray(1);glEnableVertexAttribArray(2);
            glUniformMatrix4fv(glGetUniformLocation(p,"LocalMat"),false,localMatrix.get(stack.mallocFloat(16)));
            float[] localClip=worldModel!=null && !localClips.isEmpty()?localClips.peek():new float[]{-100000,-100000,100000,100000};
            glUniform4f(glGetUniformLocation(p,"PanelClip"),localClip[0],localClip[1],localClip[2],localClip[3]);
            glUniformMatrix4fv(glGetUniformLocation(p,"ModelViewMat"),false,model.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(glGetUniformLocation(p,"ProjMat"),false,projection.get(stack.mallocFloat(16)));
            glUniform4f(glGetUniformLocation(p,"ColorModulator"),1,1,1,1);
            glUniform1i(glGetUniformLocation(p,"Sampler0"),0);glUniform1i(glGetUniformLocation(p,"Outline"),0);
            glUniform1i(glGetUniformLocation(p,"FresnelInvert"),0);
            for(var entry:uniforms.entrySet()) {
                int loc=glGetUniformLocation(p,entry.getKey());float[] a=entry.getValue();
                if(entry.getKey().equals("FresnelInvert")){glUniform1i(loc,(int)a[0]);continue;}
                switch(a.length){case 1->glUniform1f(loc,a[0]);case 2->glUniform2f(loc,a[0],a[1]);case 3->glUniform3f(loc,a[0],a[1],a[2]);case 4->glUniform4f(loc,a[0],a[1],a[2],a[3]);}
            }
            if(texture!=0)glBindTexture(GL_TEXTURE_2D,texture);
            glEnable(GL_BLEND);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);glDisable(GL_CULL_FACE);
            if(depth)glEnable(GL_DEPTH_TEST);else glDisable(GL_DEPTH_TEST);glDepthMask(false);
            if(scissor!=null) {
                var window=Minecraft.getInstance().getWindow();double scale=window.getGuiScale();
                int x=(int)Math.floor(scissor[0]*scale), y=(int)Math.floor(window.getHeight()-(scissor[1]+scissor[3])*scale);
                int right=(int)Math.ceil((scissor[0]+scissor[2])*scale), top=(int)Math.ceil(window.getHeight()-scissor[1]*scale);
                if(clip){right=Math.min(right,oldClip[0]+oldClip[2]);top=Math.min(top,oldClip[1]+oldClip[3]);x=Math.max(x,oldClip[0]);y=Math.max(y,oldClip[1]);}
                glEnable(GL_SCISSOR_TEST);glScissor(x,y,Math.max(0,right-x),Math.max(0,top-y));
            }
            glDrawElements(GL_TRIANGLES,n/4*6,GL_UNSIGNED_INT,0);
        } finally {
            glUseProgram(oldProgram);glBindVertexArray(oldVao);glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindTexture(GL_TEXTURE_2D,oldTex);glActiveTexture(active);
            state(GL_BLEND,blend);state(GL_CULL_FACE,cull);state(GL_DEPTH_TEST,depthTest);glDepthMask(depthMask);
            glBlendFuncSeparate(src,dst,srcA,dstA);state(GL_SCISSOR_TEST,clip);glScissor(oldClip[0],oldClip[1],oldClip[2],oldClip[3]);
        }
    }
    public static void draw(String shader,int tex,float[] v,int[] c,Matrix4f matrix,Map<String,float[]> uniforms) {
        if(worldModel!=null){drawFull(shader,tex,v,c,new Matrix4f(worldModel).mul(matrix),worldProjection,uniforms,null,true,matrix);return;}
        draw(shader,tex,v,c,new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrix),RenderSystem.getProjectionMatrix(),uniforms,null,false);
    }
    private static void state(int cap,boolean enabled){if(enabled)glEnable(cap);else glDisable(cap);}
    public static void reload() {if(!available()){PROGRAMS.clear();return;}PROGRAMS.values().forEach(org.lwjgl.opengl.GL33C::glDeleteProgram);PROGRAMS.clear();}
}
