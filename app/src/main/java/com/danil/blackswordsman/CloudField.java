package com.danil.blackswordsman;
/** Periodic 3D density input, independent from the camera; no sky billboards. */
public final class CloudField {
 public static final int SIDE=32;public static final float FLOOR=68,CEILING=136;
 public static byte[] noise(){byte[] v=new byte[SIDE*SIDE*SIDE];java.util.Random r=new java.util.Random(80832);for(int i=0;i<v.length;i++)v[i]=(byte)r.nextInt(256);return v;}
 public static int steps(int quality){return quality>=3?32:quality==2?24:16;}
 public static float envelope(float y){float t=(y-FLOOR)/(CEILING-FLOOR);return t<=0||t>=1?0:Math.min(1,t*5)*Math.min(1,(1-t)*3);}
 private CloudField(){}
}
