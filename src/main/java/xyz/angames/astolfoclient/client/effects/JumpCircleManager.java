package xyz.angames.astolfoclient.client.effects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class JumpCircleManager {
   private final List<JumpCircle> circles = new CopyOnWriteArrayList<>();
   public static final long LIFESPAN = 1300L;

   public void addCircle(double x, double y, double z) {
      if (circles.size() >= 64) circles.remove(0);
      this.circles.add(new JumpCircle(x, y, z, 0L));
   }

   public void tick() {
      this.circles.removeIf(circle -> System.currentTimeMillis() - circle.creationTime > circle.lifespan + circle.delay);
   }

   public List<JumpCircle> getCircles() {
      return this.circles;
   }
}
