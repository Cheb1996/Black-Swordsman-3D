package com.danil.blackswordsman;
import java.util.LinkedHashMap;
/** Shared-edge terrain patches; one draw call per patch instead of floating floor tiles. */
public final class TerrainRenderer {
 private final GameRenderer renderer;private int environment=-1;
 private final LinkedHashMap<Long,Mesh> meshes=new LinkedHashMap<Long,Mesh>(64,.75f,true);
 public TerrainRenderer(GameRenderer r){renderer=r;}
 public void draw(GameWorld w,int env,float r,float g,float b){if(environment!=env){release();environment=env;}int cx=(int)Math.floor(w.player.x/24),cz=(int)Math.floor(w.player.z/24),radius=w.qualityLevel==1?2:3;
  for(int z=cz-radius;z<=cz+radius;z++)for(int x=cx-radius;x<=cx+radius;x++){if(Math.abs(x*24)>GameWorld.OCEAN_HALF+48||Math.abs(z*24)>GameWorld.OCEAN_HALF+48)continue;long key=((long)x<<32)^(z&0xffffffffL);Mesh m=meshes.get(key);if(m==null){m=build(env,x,z);meshes.put(key,m);}renderer.draw(m,0,0,0,1,1,1,0,0,0,r,g,b,1,0,.91f,0);}
  while(meshes.size()>72){Long key=meshes.keySet().iterator().next();meshes.remove(key).release();}
 }
 private Mesh build(int env,int cx,int cz){int side=25;float[] v=new float[side*side*9];short[] ix=new short[24*24*6];int n=0,k=0;for(int z=0;z<side;z++)for(int x=0;x<side;x++){float wx=cx*24+x,wz=cz*24+z;float h=WorldLayout.height(env,wx,wz),nx=WorldLayout.height(env,wx-.25f,wz)-WorldLayout.height(env,wx+.25f,wz),nz=WorldLayout.height(env,wx,wz-.25f)-WorldLayout.height(env,wx,wz+.25f),l=(float)Math.sqrt(nx*nx+nz*nz+.25f);float road=Math.min(1,Math.abs(wx-WorldLayout.road(wz))/4);float[] f={wx,h,wz,nx/l,.5f/l,nz/l,1.1f-road*.15f+RegionLayout.desert(wx,wz)*1.5f+RegionLayout.mountains(wx,wz)*.5f,1.02f+road*.22f+RegionLayout.desert(wx,wz)*.9f+RegionLayout.mountains(wx,wz)*.45f,.87f+RegionLayout.desert(wx,wz)*.48f+RegionLayout.mountains(wx,wz)*.55f};for(float t:f)v[n++]=t;if(z<24&&x<24){int a=z*side+x;for(int t:new int[]{a,a+side,a+1,a+1,a+side,a+side+1})ix[k++]=(short)t;}}
 return new Mesh(v,ix,true);}
 public void release(){for(Mesh m:meshes.values())m.release();meshes.clear();}
}
