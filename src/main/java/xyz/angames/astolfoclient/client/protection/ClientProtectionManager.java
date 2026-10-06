package xyz.angames.astolfoclient.client.protection;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class ClientProtectionManager {
   private static final ClientProtectionManager INSTANCE = new ClientProtectionManager();
   private final List<ClientProtectionManager.CrashAlert> activeAlerts = new CopyOnWriteArrayList<>();
   private long lastSoundTime = 0L;
   private static final int ADVANCEMENT_QUEUE_LIMIT = 1600;
   private static final long ADVANCEMENT_QUIET_PERIOD_MS = 3000L;
   private final Queue<Packet<?>> pendingAdvancements = new ConcurrentLinkedQueue<>();
   private volatile long lastAdvancementPacketTime = 0L;

   public static ClientProtectionManager getInstance() {
      return INSTANCE;
   }

   public static void init() {
   }

   public List<ClientProtectionManager.CrashAlert> getActiveAlerts() {
      return this.activeAlerts;
   }

   public void onCrashBlocked(String title, String details) {
      long now = System.currentTimeMillis();
      if (now - this.lastSoundTime > 400L) {
         this.lastSoundTime = now;
         ModSounds.playCrashDetectionSound();
      }

      boolean hasDuplicate = false;

      for (ClientProtectionManager.CrashAlert alert : this.activeAlerts) {
         if (alert.title.equalsIgnoreCase(title) && now - alert.timestamp < 1500L) {
            hasDuplicate = true;
            break;
         }
      }

      if (!hasDuplicate) {
         this.activeAlerts.add(new ClientProtectionManager.CrashAlert(title, details, now));

         while (this.activeAlerts.size() > 3) {
            this.activeAlerts.remove(0);
         }
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.gui != null && mc.gui.getChat() != null) {
         mc.gui.getChat().addMessage(Component.literal("§c[Protection] §fBlocked crash exploit: §e" + title + " §7(" + details + ")"));
      }
   }

   public boolean isMaliciousPacket(Packet<?> packet) {
      if (packet == null) {
         return false;
      }

      if (packet instanceof ClientboundExplodePacket explosion) {
         Vec3 center = explosion.center();
         if (center == null
            || isInvalidDouble(center.x)
            || isInvalidDouble(center.y)
            || isInvalidDouble(center.z)
            || Math.abs(center.x) > 3.0E7
            || Math.abs(center.y) > 3.0E7
            || Math.abs(center.z) > 3.0E7) {
            this.onCrashBlocked("Explosion Crash", "Center coordinates outside world boundary");
            return true;
         }

         if (explosion.playerKnockback().isPresent()) {
            Vec3 kb = (Vec3)explosion.playerKnockback().get();
            if (kb == null
               || isInvalidDouble(kb.x)
               || isInvalidDouble(kb.y)
               || isInvalidDouble(kb.z)
               || Math.abs(kb.x) > 1000000.0
               || Math.abs(kb.y) > 1000000.0
               || Math.abs(kb.z) > 1000000.0) {
               this.onCrashBlocked("Explosion Crash", "Malformed knockback vector");
               return true;
            }
         }
      }

      if (packet instanceof ClientboundLevelParticlesPacket particle) {
         if (isInvalidDouble(particle.getX())
            || isInvalidDouble(particle.getY())
            || isInvalidDouble(particle.getZ())
            || Math.abs(particle.getX()) > 3.0E7
            || Math.abs(particle.getY()) > 3.0E7
            || Math.abs(particle.getZ()) > 3.0E7) {
            this.onCrashBlocked("Particle Exploit", "Coordinates out of bounds");
            return true;
         }

         if (particle.getCount() > 1000 || particle.getCount() < 0) {
            this.onCrashBlocked("Particle Exploit", "Invalid count: " + particle.getCount());
            return true;
         }

         if (Float.isNaN(particle.getMaxSpeed()) || Float.isInfinite(particle.getMaxSpeed()) || Math.abs(particle.getMaxSpeed()) > 1000.0F) {
            this.onCrashBlocked("Particle Exploit", "Malformed particle speed");
            return true;
         }

         if (Float.isNaN(particle.getXDist())
            || Float.isNaN(particle.getYDist())
            || Float.isNaN(particle.getZDist())
            || Math.abs(particle.getXDist()) > 1000.0F
            || Math.abs(particle.getYDist()) > 1000.0F
            || Math.abs(particle.getZDist()) > 1000.0F) {
            this.onCrashBlocked("Particle Exploit", "Malformed particle offset");
            return true;
         }
      }

      if (packet instanceof ClientboundUpdateAdvancementsPacket) {
         if (this.pendingAdvancements.size() >= 1600) {
            this.pendingAdvancements.poll();
            this.onCrashBlocked("Advancement Flood", "Queue exceeded limit (1600)");
         }

         this.pendingAdvancements.add(packet);
         this.lastAdvancementPacketTime = System.currentTimeMillis();
         return true;
      } else {
         if (packet instanceof ClientboundSetEntityMotionPacket vel) {
            double vx = Math.abs(vel.getXa() / 8000.0);
            double vy = Math.abs(vel.getYa() / 8000.0);
            double vz = Math.abs(vel.getZa() / 8000.0);
            if (isInvalidDouble(vx) || isInvalidDouble(vy) || isInvalidDouble(vz) || vx > 100000.0 || vy > 100000.0 || vz > 100000.0) {
               this.onCrashBlocked("Velocity Exploit", "Extreme entity velocity values");
               return true;
            }
         }

         if (packet instanceof ClientboundSoundPacket sound) {
            if (isInvalidDouble(sound.getX())
               || isInvalidDouble(sound.getY())
               || isInvalidDouble(sound.getZ())
               || Math.abs(sound.getX()) > 3.0E7
               || Math.abs(sound.getY()) > 3.0E7
               || Math.abs(sound.getZ()) > 3.0E7) {
               this.onCrashBlocked("Sound Exploit", "Coordinates out of bounds");
               return true;
            }

            if (Float.isNaN(sound.getVolume())
               || Float.isNaN(sound.getPitch())
               || sound.getVolume() < 0.0F
               || sound.getVolume() > 100.0F
               || sound.getPitch() < 0.0F
               || sound.getPitch() > 100.0F) {
               this.onCrashBlocked("Sound Exploit", "Invalid sound volume/pitch");
               return true;
            }
         }

         if (packet instanceof ClientboundAddEntityPacket spawn) {
            if (isInvalidDouble(spawn.getX())
               || isInvalidDouble(spawn.getY())
               || isInvalidDouble(spawn.getZ())
               || Math.abs(spawn.getX()) > 3.0E7
               || Math.abs(spawn.getY()) > 3.0E7
               || Math.abs(spawn.getZ()) > 3.0E7) {
               this.onCrashBlocked("Spawn Exploit", "Entity coordinates out of bounds");
               return true;
            }

            if (isInvalidDouble(spawn.getXa())
               || isInvalidDouble(spawn.getYa())
               || isInvalidDouble(spawn.getZa())
               || Math.abs(spawn.getXa()) > 100000.0
               || Math.abs(spawn.getYa()) > 100000.0
               || Math.abs(spawn.getZa()) > 100000.0) {
               this.onCrashBlocked("Spawn Exploit", "Entity spawned with extreme velocity");
               return true;
            }
         }

         if (packet instanceof ClientboundPlayerPositionPacket teleport) {
            PositionMoveRotation change = teleport.change();
            if (change != null) {
               Vec3 pos = change.position();
               Vec3 delta = change.deltaMovement();
               if (pos == null
                  || isInvalidDouble(pos.x)
                  || isInvalidDouble(pos.y)
                  || isInvalidDouble(pos.z)
                  || Math.abs(pos.x) > 3.0E7
                  || Math.abs(pos.y) > 3.0E7
                  || Math.abs(pos.z) > 3.0E7
                  || delta != null
                     && (
                        isInvalidDouble(delta.x)
                           || isInvalidDouble(delta.y)
                           || isInvalidDouble(delta.z)
                           || Math.abs(delta.x) > 1000000.0
                           || Math.abs(delta.y) > 1000000.0
                           || Math.abs(delta.z) > 1000000.0
                     )
                  || Float.isNaN(change.yRot())
                  || Float.isInfinite(change.yRot())
                  || Float.isNaN(change.xRot())
                  || Float.isInfinite(change.xRot())) {
                  String coordsStr = pos != null ? String.format("X: %.1f, Y: %.1f, Z: %.1f", pos.x, pos.y, pos.z) : "null";
                  this.onCrashBlocked("Teleport Crash", "Invalid Coordinates (" + coordsStr + ")");

                  try {
                     Minecraft mc = Minecraft.getInstance();
                     if (mc != null && mc.getConnection() != null) {
                        mc.getConnection().send(new ServerboundAcceptTeleportationPacket(teleport.id()));
                     }
                  } catch (Exception var9) {
                  }

                  return true;
               }
            }
         }

         if (packet instanceof ClientboundMoveVehiclePacket vehicleMove) {
            Vec3 pos = vehicleMove.position();
            if (pos == null
               || isInvalidDouble(pos.x)
               || isInvalidDouble(pos.y)
               || isInvalidDouble(pos.z)
               || Math.abs(pos.x) > 3.0E7
               || Math.abs(pos.y) > 3.0E7
               || Math.abs(pos.z) > 3.0E7
               || Float.isNaN(vehicleMove.yRot())
               || Float.isInfinite(vehicleMove.yRot())
               || Float.isNaN(vehicleMove.xRot())
               || Float.isInfinite(vehicleMove.xRot())) {
               this.onCrashBlocked("Vehicle Crash", "Invalid vehicle coordinates or rotation");
               return true;
            }
         }

         if (packet instanceof ClientboundTeleportEntityPacket entityPos) {
            PositionMoveRotation change = entityPos.change();
            if (change != null) {
               Vec3 pos = change.position();
               if (pos == null
                  || isInvalidDouble(pos.x)
                  || isInvalidDouble(pos.y)
                  || isInvalidDouble(pos.z)
                  || Math.abs(pos.x) > 3.0E7
                  || Math.abs(pos.y) > 3.0E7
                  || Math.abs(pos.z) > 3.0E7) {
                  this.onCrashBlocked("Entity Position Exploit", "Invalid coordinates for entity");
                  return true;
               }
            }
         }

         if (packet instanceof ClientboundEntityPositionSyncPacket entitySync) {
            PositionMoveRotation values = entitySync.values();
            if (values != null) {
               Vec3 pos = values.position();
               if (pos == null
                  || isInvalidDouble(pos.x)
                  || isInvalidDouble(pos.y)
                  || isInvalidDouble(pos.z)
                  || Math.abs(pos.x) > 3.0E7
                  || Math.abs(pos.y) > 3.0E7
                  || Math.abs(pos.z) > 3.0E7) {
                  this.onCrashBlocked("Entity Sync Exploit", "Invalid sync coordinates for entity");
                  return true;
               }
            }
         }

         return false;
      }
   }

   public void tick() {
      for (ClientProtectionManager.CrashAlert alert : this.activeAlerts) {
         if (alert.isExpired()) {
            this.activeAlerts.remove(alert);
         }
      }

      if (!this.pendingAdvancements.isEmpty() && System.currentTimeMillis() - this.lastAdvancementPacketTime >= 3000L) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getConnection() != null) {
            while (!this.pendingAdvancements.isEmpty()) {
               Packet<?> p = this.pendingAdvancements.poll();
               if (p != null) {
                  try {
                     ((net.minecraft.network.protocol.Packet)p).handle(mc.getConnection());
                  } catch (Exception var5) {
                  }
               }
            }
         }
      }
   }

   private static boolean isInvalidDouble(double val) {
      return Double.isNaN(val) || Double.isInfinite(val);
   }

   @Environment(EnvType.CLIENT)
   public static class CrashAlert {
      public final String title;
      public final String details;
      public final long timestamp;
      public float anim = 0.0F;

      public CrashAlert(String title, String details, long timestamp) {
         this.title = title;
         this.details = details;
         this.timestamp = timestamp;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() - this.timestamp > 4500L;
      }
   }
}
