package com.danil.blackswordsman;
import java.io.*;
/** Baked meshes: bounded nearby plants, animated bend and wind, no per-frame mesh construction. */
public final class FloraRenderer {
 private final Mesh[][] meshes=new Mesh[48][2];private final android.content.res.AssetManager assets;private final GameRenderer painter;
 public FloraRenderer(android.content.res.AssetManager a,GameRenderer p){assets=a;painter=p;}
 private Mesh mesh(int id,int lod){if(meshes[id][lod]!=null)return meshes[id][lod];try{DataInputStream in=new DataInputStream(new BufferedInputStream(assets.open("flora/"+id+"-"+lod+".bin")));float[] v=new float[in.readInt()];short[] ix=new short[in.readInt()];for(int i=0;i<v.length;i++)v[i]=in.readFloat();for(int i=0;i<ix.length;i++)ix[i]=in.readShort();in.close();return meshes[id][lod]=new Mesh(v,ix,true);}catch(IOException e){throw new IllegalStateException("Missing flora",e);}}
 public void draw(GameWorld w,float time){int max=w.qualityLevel==3?420:w.qualityLevel==2?290:180,count=0;float range=w.qualityLevel==3?32:26;for(FloraWorld.Plant p:w.flora.plants){float dx=p.x-w.player.x,dz=p.z-w.player.z,d2=dx*dx+dz*dz;if(d2>range*range||!w.flora.visible(p))continue;boolean magic=p.species>=24&&p.species<32;if(count++>max&&!magic)continue;float sway=(float)Math.sin(time*1.6+p.x*.7+p.z*.5)*(p.species>=32?5:1.4f),q=p.scale;int lod=d2<100?1:0;float glow=magic?.65f:p.species==42||p.species==46?.45f:w.mushrooms.active(MushroomEffects.LANTERN)&&p.species<32?.7f:0;painter.surface(p.species<32?SurfaceLibrary.FUNGUS:SurfaceLibrary.LEAF);painter.draw(mesh(p.species,lod),p.x,p.y,p.z,q,q*(1-p.bend*.2f),q,sway+p.bend*38,p.yaw,p.bend*12,1,1,1,1,.02f,.72f,glow);}painter.surface(-1);}
 public void release(){for(Mesh[] row:meshes)for(Mesh m:row)if(m!=null)m.release();}
}
