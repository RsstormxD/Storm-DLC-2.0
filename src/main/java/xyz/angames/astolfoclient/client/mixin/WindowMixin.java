package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.PackResources;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Window.class)
public class WindowMixin {
   private boolean iconsSet = false;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.setCustomIcons();
   }

   @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
   private void onSetIcon(PackResources resourcePack, IconSet icons, CallbackInfo ci) {
      ci.cancel();
      if (!this.iconsSet) {
         this.setCustomIcons();
         this.iconsSet = true;
      }
   }

   private void setCustomIcons() {
      Window window = (Window)(Object)this;
      long handle = window.getWindow();
      String[] paths = new String[]{
         "/assets/astolfoclient/textures/gui/logo_black_32.png",
         "/assets/astolfoclient/textures/gui/logo_black_64.png",
         "/assets/astolfoclient/textures/gui/logo_black_big.png"
      };
      List<ByteBuffer> buffers = new ArrayList<>();
      Buffer iconBuffer = null;

      try {
         for (String path : paths) {
            try (InputStream is = WindowMixin.class.getResourceAsStream(path)) {
               if (is != null) {
                  NativeImage image = NativeImage.read(is);
                  if (image != null) {
                     int w = image.getWidth();
                     int h = image.getHeight();
                     ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);

                     for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                           int argb = image.getPixel(x, y);
                           buf.put((byte)(argb >> 16 & 0xFF));
                           buf.put((byte)(argb >> 8 & 0xFF));
                           buf.put((byte)(argb & 0xFF));
                           buf.put((byte)(argb >> 24 & 0xFF));
                        }
                     }

                     buf.flip();
                     image.close();
                     if (iconBuffer == null) {
                        iconBuffer = GLFWImage.malloc(paths.length);
                     }

                     iconBuffer.position(buffers.size());
                     iconBuffer.width(w);
                     iconBuffer.height(h);
                     iconBuffer.pixels(buf);
                     buffers.add(buf);
                  }
               }
            }
         }

         if (iconBuffer != null) {
            iconBuffer.position(0);
            iconBuffer.limit(buffers.size());
            GLFW.glfwSetWindowIcon(handle, iconBuffer);
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (iconBuffer != null) {
            iconBuffer.free();
         }

         for (ByteBuffer buf : buffers) {
            MemoryUtil.memFree(buf);
         }
      }
   }
}
