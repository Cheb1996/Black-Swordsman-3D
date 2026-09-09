package com.danil.blackswordsman;
/** Shared water tessellation. Tint stores depth and wave amplitude for the water shader. */
public final class WaterGeometry {
 public final float[] vertices;public final short[] indices;
 public WaterGeometry(int env,float x0,float z0,float size,int cells){vertices=new float[(cells+1)*(cells+1)*9];indices=new short[cells*cells*6];int n=0,k=0;for(int z=0;z<=cells;z++)for(int x=0;x<=cells;x++){float wx=x0+x*size/cells,wz=z0+z*size/cells,level=WaterField.level(env,wx,wz),d=WaterField.kind(env,wx,wz)==WaterField.DRY?-4:level-WorldLayout.height(env,wx,wz);float[] v={wx,level,wz,0,1,0,d,WaterField.kind(env,wx,wz)==WaterField.OCEAN?.12f:.025f,WaterField.coast(wx,wz)};for(float value:v)vertices[n++]=value;if(x<cells&&z<cells){int a=z*(cells+1)+x;for(int i:new int[]{a,a+cells+1,a+1,a+1,a+cells+1,a+cells+2})indices[k++]=(short)i;}}}
 public boolean wet(){for(int i=6;i<vertices.length;i+=9)if(vertices[i]>0)return true;return false;}
}
