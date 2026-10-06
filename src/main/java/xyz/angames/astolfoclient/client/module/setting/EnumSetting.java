package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EnumSetting<T extends Enum<T>> extends Setting {
   private T value;
   private final T[] values;

   public EnumSetting(String name, T value) {
      super(name);
      this.value = value;
      this.values = value.getDeclaringClass().getEnumConstants();
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T value) {
      java.util.Objects.requireNonNull(value, "value");
      this.value = value;
   }

   public void setByName(String name) {
      for (T e : this.values) {
         if (name != null && (e.name().equalsIgnoreCase(name)
               || e.name().replace('_', ' ').equalsIgnoreCase(name)
               || e.toString().equalsIgnoreCase(name))) {
            this.value = e;
            return;
         }
      }
   }

   public T[] getValues() {
      return this.values;
   }

   public void cycle() {
      int index = this.value.ordinal();
      int nextIndex = (index + 1) % this.values.length;
      this.value = this.values[nextIndex];
   }

   public String get() {
      return this.value.toString();
   }
}
