package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.*;

@Environment(EnvType.CLIENT)
public class HandPositionModule extends Module {
   public final NumberSetting mainSize = new NumberSetting("Main uniform size", 1, .25, 2, .05);
   public final NumberSetting offSize = new NumberSetting("Off uniform size", 1, .25, 2, .05);
   public final BooleanSetting mirrorHands = new BooleanSetting("Mirror main to offhand", false);
   public final ModeSetting preset = new ModeSetting("Hand preset", "Default", "Default", "Compact", "Cinematic");
   public final ActionSetting applyPreset = new ActionSetting("Apply hand preset", this::applyPreset);
   public final ActionSetting resetHands = new ActionSetting("Reset hand transforms", this::resetTransforms);

   public final NumberSetting mainPosX = new NumberSetting("Main Pos X", 0.0, -3.0, 3.0, 0.05);
   public final NumberSetting mainPosY = new NumberSetting("Main Pos Y", 0.0, -3.0, 3.0, 0.05);
   public final NumberSetting mainPosZ = new NumberSetting("Main Pos Z", 0.0, -3.0, 3.0, 0.05);
   public final NumberSetting mainRotX = new NumberSetting("Main Rot X", 0.0, -180.0, 180.0, 1.0);
   public final NumberSetting mainRotY = new NumberSetting("Main Rot Y", 0.0, -180.0, 180.0, 1.0);
   public final NumberSetting mainRotZ = new NumberSetting("Main Rot Z", 0.0, -180.0, 180.0, 1.0);
   public final NumberSetting mainScaleX = new NumberSetting("Main Scale X", 1.0, 0.1, 3.0, 0.05);
   public final NumberSetting mainScaleY = new NumberSetting("Main Scale Y", 1.0, 0.1, 3.0, 0.05);
   public final NumberSetting mainScaleZ = new NumberSetting("Main Scale Z", 1.0, 0.1, 3.0, 0.05);
   public final NumberSetting offPosX = new NumberSetting("Off Pos X", 0.0, -3.0, 3.0, 0.05);
   public final NumberSetting offPosY = new NumberSetting("Off Pos Y", 0.0, -3.0, 3.0, 0.05);
   public final NumberSetting offPosZ = new NumberSetting("Off Pos Z", 0.0, -3.0, 3.0, 0.05);
   public final NumberSetting offRotX = new NumberSetting("Off Rot X", 0.0, -180.0, 180.0, 1.0);
   public final NumberSetting offRotY = new NumberSetting("Off Rot Y", 0.0, -180.0, 180.0, 1.0);
   public final NumberSetting offRotZ = new NumberSetting("Off Rot Z", 0.0, -180.0, 180.0, 1.0);
   public final NumberSetting offScaleX = new NumberSetting("Off Scale X", 1.0, 0.1, 3.0, 0.05);
   public final NumberSetting offScaleY = new NumberSetting("Off Scale Y", 1.0, 0.1, 3.0, 0.05);
   public final NumberSetting offScaleZ = new NumberSetting("Off Scale Z", 1.0, 0.1, 3.0, 0.05);

   public HandPositionModule() {
      super("HandPosition", "Changes the position, rotation, and scale of held items.", Module.Category.RENDER);
      this.addSettings(mainSize, offSize, mirrorHands, preset, applyPreset, resetHands);
      offSize.setVisibility(() -> !mirrorHands.get());
      for (NumberSetting value : new NumberSetting[]{offPosX, offPosY, offPosZ, offRotX, offRotY, offRotZ, offScaleX, offScaleY, offScaleZ})
         value.setVisibility(() -> !mirrorHands.get());
      this.addSettings(
         this.mainPosX,
         this.mainPosY,
         this.mainPosZ,
         this.mainRotX,
         this.mainRotY,
         this.mainRotZ,
         this.mainScaleX,
         this.mainScaleY,
         this.mainScaleZ,
         this.offPosX,
         this.offPosY,
         this.offPosZ,
         this.offRotX,
         this.offRotY,
         this.offRotZ,
         this.offScaleX,
         this.offScaleY,
         this.offScaleZ
      );
   }

   private float getFloat(NumberSetting setting) {
      return Double.valueOf(setting.get()).floatValue();
   }

   public float[] getMainHandPos() {
      return new float[]{this.getFloat(this.mainPosX), this.getFloat(this.mainPosY), this.getFloat(this.mainPosZ)};
   }

   public float[] getMainHandRot() {
      return new float[]{this.getFloat(this.mainRotX), this.getFloat(this.mainRotY), this.getFloat(this.mainRotZ)};
   }

   public float[] getMainHandScale() {
      return new float[]{this.getFloat(this.mainScaleX)*mainSize.getFloat(), this.getFloat(this.mainScaleY)*mainSize.getFloat(), this.getFloat(this.mainScaleZ)*mainSize.getFloat()};
   }

   public float[] getOffHandPos() {
      if(mirrorHands.get()) { float[] p=getMainHandPos();p[0]=-p[0];return p;}
      return new float[]{this.getFloat(this.offPosX), this.getFloat(this.offPosY), this.getFloat(this.offPosZ)};
   }

   public float[] getOffHandRot() {
      if(mirrorHands.get()) { float[] p=getMainHandRot();p[1]=-p[1];p[2]=-p[2];return p;}
      return new float[]{this.getFloat(this.offRotX), this.getFloat(this.offRotY), this.getFloat(this.offRotZ)};
   }

   public float[] getOffHandScale() {
      if(mirrorHands.get())return getMainHandScale();
      return new float[]{this.getFloat(this.offScaleX)*offSize.getFloat(), this.getFloat(this.offScaleY)*offSize.getFloat(), this.getFloat(this.offScaleZ)*offSize.getFloat()};
   }
   private void resetTransforms() {
      for(NumberSetting value:new NumberSetting[]{mainPosX,mainPosY,mainPosZ,mainRotX,mainRotY,mainRotZ,offPosX,offPosY,offPosZ,offRotX,offRotY,offRotZ})value.set(0);
      for(NumberSetting value:new NumberSetting[]{mainScaleX,mainScaleY,mainScaleZ,offScaleX,offScaleY,offScaleZ,mainSize,offSize})value.set(1);
      mirrorHands.set(false);
   }
   private void applyPreset() {
      resetTransforms();
      if(preset.is("Compact")){mainSize.set(.75);offSize.set(.75);mainPosY.set(-.15);offPosY.set(-.15);}
      if(preset.is("Cinematic")){mainSize.set(.9);offSize.set(.9);mainPosX.set(.15);mainPosY.set(-.2);mainRotZ.set(-8);mirrorHands.set(true);}
   }
}
