package com.danil.blackswordsman;
import android.opengl.GLES30;
import java.util.LinkedHashMap;
/** Transparent, animated water patches; cached GPU geometry and a coarse ocean horizon. */
public final class WaterRenderer {
 private final GameRenderer renderer;private int environment=-1;private Mesh horizon;
 private final LinkedHashMap<Long,Mesh> cache=new LinkedHashMap<Long,Mesh>(64,.75f,true);
 public WaterRenderer(GameRenderer r){renderer=r;}
 public void draw(GameWorld w){int env=w.currentChapter().environment;if(environment!=env){release();environment=env;WaterGeometry g=new WaterGeometry(env,-1500,-1500,3000,120); // Ocean only in the coarse horizon; inland water is drawn in fine patches.
   for(int i=0;i<g.vertices.length;i+=9)if(WaterField.kind(env,g.vertices[i],g.vertices[i+2])!=WaterField.OCEAN)g.vertices[i+6]=-20;
   horizon=new Mesh(g.vertices,g.indices,true);}
  renderer.waterMode(true);GLES30.glDisable(GLES30.GL_CULL_FACE);GLES30.glDepthMask(false);
  int cx=(int)Math.floor(w.player.x/24),cz=(int)Math.floor(w.player.z/24),radius=w.qualityLevel==1?2:3;
  // Draw horizon first, omitting nearby cells in fragment shader to avoid double blending.
  renderer.waterHorizon(true);renderer.draw(horizon,0,0,0,1,1,1,0,0,0,1,1,1,1,0,.1f,0);renderer.waterHorizon(false);
  for(int z=cz-radius;z<=cz+radius;z++)for(int x=cx-radius;x<=cx+radius;x++){long key=((long)x<<32)^(z&0xffffffffL);if(!cache.containsKey(key)){WaterGeometry g=new WaterGeometry(env,x*24,z*24,24,24);cache.put(key,g.wet()?new Mesh(g.vertices,g.indices,true):null);}Mesh m=cache.get(key);if(m!=null)renderer.draw(m,0,0,0,1,1,1,0,0,0,1,1,1,1,0,.1f,0);}
  while(cache.size()>72){Long k=cache.keySet().iterator().next();Mesh m=cache.remove(k);if(m!=null)m.release();}
  GLES30.glDepthMask(true);GLES30.glEnable(GLES30.GL_CULL_FACE);renderer.waterMode(false);
 }
 public void release(){for(Mesh m:cache.values())if(m!=null)m.release();cache.clear();if(horizon!=null){horizon.release();horizon=null;}}
}
