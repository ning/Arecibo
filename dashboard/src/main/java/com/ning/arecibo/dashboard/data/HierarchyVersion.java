package com.ning.arecibo.dashboard.data;

// Supported versions of the end point
public enum HierarchyVersion {
   v1;

   public static String getValueList() {
       StringBuilder sb = new StringBuilder();
       sb.append("{ ");
       for(HierarchyVersion version:HierarchyVersion.values()) {
           sb.append(version);
           sb.append(" ");
       }
       sb.append("}");

       return sb.toString();
   }
}
