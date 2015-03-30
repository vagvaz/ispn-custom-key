package eu.leads.processor.common.infinispan;

import java.io.Serializable;

/**
 * Created by vagvaz on 3/30/15.
 */
public class CustomKey implements Comparable,Serializable {
   private String customString;

   public CustomKey(){
      customString = "";
   }
   public CustomKey(String string){
      this.customString =string;
   }
   public  CustomKey(Integer anInt){
      this.customString = anInt.toString();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CustomKey customKey = (CustomKey) o;

      return !(customString != null ? !customString.equals(customKey.customString) : customKey.customString != null);

   }


   @Override
   public int hashCode() {
      return  5;
   }

   public String getCustomString() {

      return customString;
   }

   public void setCustomString(String customString) {
      this.customString = customString;
   }

   public CustomKey(CustomKey other){
      this(other.getCustomString());

   }

   @Override
   public int compareTo(Object o) {
      if (this == o) return 0;
      if (o == null || getClass() != o.getClass()) return -1;

      CustomKey other = (CustomKey)o;
      return customString.compareTo(other.getCustomString());
   }
}
