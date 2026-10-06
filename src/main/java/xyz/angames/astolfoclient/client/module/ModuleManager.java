package xyz.angames.astolfoclient.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.module.modules.BoyKisserModule;
import xyz.angames.astolfoclient.client.module.modules.FullBrightModule;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;
import xyz.angames.astolfoclient.client.module.modules.JumpCircleModule;
import xyz.angames.astolfoclient.client.module.modules.KillAuraModule;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.module.modules.misc.AutoRespawnModule;
import xyz.angames.astolfoclient.client.module.modules.misc.CoordsHiderModule;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.module.modules.misc.PasswordHiderModule;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
import xyz.angames.astolfoclient.client.module.modules.render.AspectRatioModule;
import xyz.angames.astolfoclient.client.module.modules.render.BabyPlayerModule;
import xyz.angames.astolfoclient.client.module.modules.render.BlockOutlineModule;
import xyz.angames.astolfoclient.client.module.modules.render.CameraUtilsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ChinaHatModule;
import xyz.angames.astolfoclient.client.module.modules.render.CrosshairModule;
import xyz.angames.astolfoclient.client.module.modules.render.CubeParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;
import xyz.angames.astolfoclient.client.module.modules.render.DashTrailModule;
import xyz.angames.astolfoclient.client.module.modules.render.FireFliesModule;
import xyz.angames.astolfoclient.client.module.modules.render.HandPositionModule;
import xyz.angames.astolfoclient.client.module.modules.render.HitGlowModule;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.module.modules.render.ItemChamsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ItemPhysicsModule;
import xyz.angames.astolfoclient.client.module.modules.render.KillEffectModule;
import xyz.angames.astolfoclient.client.module.modules.render.LineGlyphsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ModelsModule;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.RagdollModule;
import xyz.angames.astolfoclient.client.module.modules.render.RainModule;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;
import xyz.angames.astolfoclient.client.module.modules.render.SwingAnimationModule;
import xyz.angames.astolfoclient.client.module.modules.render.TrailsModule;
import xyz.angames.astolfoclient.client.module.modules.render.TrajectoriesModule;
import xyz.angames.astolfoclient.client.module.modules.render.WingsModule;

@Environment(EnvType.CLIENT)
public class ModuleManager {
   public static ModuleManager INSTANCE;
   public static List<Module> modules = new java.util.concurrent.CopyOnWriteArrayList<>();
   private net.minecraft.client.multiplayer.ClientLevel tickLevel;
   private net.minecraft.client.player.LocalPlayer tickPlayer;
   private boolean wasAlive;
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("StormDLC/ModuleManager");
   public static final List<Class<? extends Module>> betaModules = new ArrayList<>();

   public static boolean isBeta(Module module) {
      return module == null ? false : betaModules.contains(module.getClass());
   }

   public ModuleManager() {
      for (Module previous : modules) previous.setEnabled(false);
      modules.clear();
      INSTANCE = this;
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.GuiAppearanceModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.Gui3DModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.Spotify3DModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.SongIslandModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.MainMenuModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.UiAnimationsModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.AmbientParticlesModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.SelfAuraModule());
      this.addModule(new AmbientsModule());
      this.addModule(new AspectRatioModule());
      this.addModule(new BabyPlayerModule());
      this.addModule(new BlockOutlineModule());
      this.addModule(new CameraUtilsModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.SmoothCameraModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.misc.DiscordRpcModule());
      this.addModule(new ChinaHatModule());
      this.addModule(new CrosshairModule());
      this.addModule(new CubeParticlesModule());
      this.addModule(new DamageIndicatorModule());
      this.addModule(new DashTrailModule());
      this.addModule(new FireFliesModule());
      this.addModule(new HandPositionModule());
      this.addModule(new HitGlowModule());
      this.addModule(new InterfaceModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.render.CursorModule());
      this.addModule(new ItemChamsModule());
      this.addModule(new ItemPhysicsModule());
      this.addModule(new KillEffectModule());
      this.addModule(new LineGlyphsModule());
      this.addModule(new ModelsModule());
      this.addModule(new NoRenderModule());
      this.addModule(new ParticlesModule());
      this.addModule(new RagdollModule());
      this.addModule(new RainModule());
      this.addModule(new ShaderHand());
      this.addModule(new SwingAnimationModule());
      this.addModule(new TrailsModule());
      this.addModule(new TrajectoriesModule());
      this.addModule(new WingsModule());
      this.addModule(new KillAuraModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.AutoTrapModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.AutoWebModule());
      this.addModule(new xyz.angames.astolfoclient.client.module.modules.misc.FriendsModule());
      this.addModule(new CoordsHiderModule());
      this.addModule(new NameProtectModule());
      this.addModule(new PasswordHiderModule());
      this.addModule(new AutoRespawnModule());
      this.addModule(new BoyKisserModule());
      this.addModule(new FullBrightModule());
      this.addModule(new HitEspModule());
      this.addModule(new JumpCircleModule());
      this.addModule(new TargetEspModule());
   }

   private void addModule(Module module) {
      xyz.angames.astolfoclient.client.config.VisualColors.attach(module);
      modules.add(module);
   }

   public void tick(Minecraft client) {
      boolean alive = client.player != null && client.player.isAlive();
      if (tickLevel != client.level || tickPlayer != client.player || wasAlive != alive) resetRuntimeState();
      tickLevel = client.level;
      tickPlayer = client.player;
      wasAlive = alive;
      for (Module module : modules) {
         if (!module.isEnabled()) continue;
         try { module.onTick(); }
         catch (RuntimeException failure) {
            LOG.error("Disabling {} after a tick failure", module.getName(), failure);
            module.setEnabled(false);
         }
      }
   }

   public void resetRuntimeState() {
      tickLevel = null;
      tickPlayer = null;
      wasAlive = false;
      dev.stormdlc.combat.BlockPlacementService.resetContext();
      for (Module module : modules) {
         if (!module.isEnabled()) continue;
         try { module.onContextReset(); }
         catch (RuntimeException failure) {
            LOG.warn("Cannot reset {}", module.getName(), failure);
            module.setEnabled(false);
         }
      }
   }

   public void renderWorld(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
      for (Module module : modules) {
         if (!module.isEnabled()) continue;
         try { module.onWorldRender(context); }
         catch (RuntimeException failure) {
            LOG.error("Disabling {} after a render failure", module.getName(), failure);
            module.setEnabled(false);
         }
      }
   }

   public void disableAll() {
      for (Module module : modules) module.setEnabled(false);
      resetRuntimeState();
   }

   public synchronized void registerDynamicModule(Module module) {
      if (module != null) {
         for (Module existing : modules) {
            if (existing != module && existing.getName().equalsIgnoreCase(module.getName())) existing.setEnabled(false);
         }
         modules.removeIf(m -> m.getName().equalsIgnoreCase(module.getName()));
         modules.add(module);
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.screen instanceof ClickGuiScreen clickGui) {
            clickGui.refreshModuleButtons();
         }
      }
   }

   public synchronized void unregisterDynamicModule(Module module) {
      if (module != null) {
         module.setEnabled(false);
         for (Module existing : modules) {
            if (existing != module && existing.getName().equalsIgnoreCase(module.getName())) existing.setEnabled(false);
         }
         modules.remove(module);
         modules.removeIf(m -> m.getName().equalsIgnoreCase(module.getName()));
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.screen instanceof ClickGuiScreen clickGui) {
            clickGui.refreshModuleButtons();
         }
      }
   }

   public List<Module> getModules() {
      return modules != null ? modules : Collections.emptyList();
   }

   public Module getModuleByName(String name) {
      if (name != null && name.equalsIgnoreCase("Song Island")) name = "Dynamic Island";
      if (modules != null && name != null) {
         for (Module module : modules) {
            if (module != null && module.getName() != null && module.getName().equalsIgnoreCase(name)) {
               return module;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static Module getModule(Class<? extends Module> clazz) {
      if (modules != null && clazz != null) {
         for (Module module : modules) {
            if (module != null && module.getClass() == clazz) {
               return module;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
