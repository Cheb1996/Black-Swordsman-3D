package com.danil.blackswordsman;
/** Small cached A* grid around an obstructed actor, followed by local steering. */
public final class NavigationSystem {
 private static final int SIDE=17,COUNT=SIDE*SIDE;
 public static float direction(GameWorld w,GameWorld.Actor a,float desired){
  if(a.navCooldown>0)return a.navDirection;a.navCooldown=.6f;
  float targetX=a.x+(float)Math.sin(desired)*14,targetZ=a.z+(float)Math.cos(desired)*14;
  if(w.physics.raycast(a.x,a.y+.65f,a.z,targetX,w.physics.terrainHeight(targetX,targetZ)+.65f,targetZ,a.radius*.7f,null,a.type==GameWorld.WRAITH)>1){a.navDirection=desired;return desired;}
  boolean[] blocked=new boolean[COUNT],closed=new boolean[COUNT];float[] costs=new float[COUNT];int[] parents=new int[COUNT];java.util.Arrays.fill(costs,Float.MAX_VALUE);java.util.Arrays.fill(parents,-1);
  float originX=a.x-16,originZ=a.z-16;int goalX=Math.max(0,Math.min(16,Math.round((targetX-originX)/2))),goalZ=Math.max(0,Math.min(16,Math.round((targetZ-originZ)/2))),goal=goalZ*SIDE+goalX,start=8*SIDE+8;
  for(int z=0;z<SIDE;z++)for(int x=0;x<SIDE;x++){float px=originX+x*2,pz=originZ+z*2,y=w.physics.terrainHeight(px,pz);for(PhysicsWorld.Body b:w.physics.bodies)if(!b.dynamic&&b.solid&&!(b.astral&&a.type==GameWorld.WRAITH)&&b.y+b.ey>y+.38f&&b.y-b.ey<y+a.height&&Math.abs(px-b.x)<b.ex+a.radius&&Math.abs(pz-b.z)<b.ez+a.radius){blocked[z*SIDE+x]=true;break;}}
  blocked[start]=false;costs[start]=0;int closest=start;float closestD=1e9f;
  for(int n=0;n<COUNT;n++){int node=-1;float score=Float.MAX_VALUE;for(int i=0;i<COUNT;i++)if(!closed[i]&&!blocked[i]&&costs[i]<Float.MAX_VALUE){float dx=i%SIDE-goalX,dz=i/SIDE-goalZ,h=(float)Math.sqrt(dx*dx+dz*dz),s=costs[i]+h;if(s<score){score=s;node=i;}}if(node<0)break;float gx=node%SIDE-goalX,gz=node/SIDE-goalZ,d=gx*gx+gz*gz;if(d<closestD){closestD=d;closest=node;}if(node==goal)break;closed[node]=true;
   for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){int x=node%SIDE+dx,z=node/SIDE+dz;if(x<0||z<0||x>=SIDE||z>=SIDE||dx==0&&dz==0)continue;int next=z*SIDE+x;if(blocked[next]||dx!=0&&dz!=0&&(blocked[node+dx]||blocked[node+dz*SIDE]))continue;float cost=costs[node]+(dx!=0&&dz!=0?1.414f:1);if(cost<costs[next]){costs[next]=cost;parents[next]=node;}}
  }
  if(closest==start){a.navDirection=desired+(a.id%2==0?1.6f:-1.6f);return a.navDirection;}while(parents[closest]>=0&&parents[closest]!=start)closest=parents[closest];a.navDirection=(float)Math.atan2(originX+(closest%SIDE)*2-a.x,originZ+(closest/SIDE)*2-a.z);return a.navDirection;
 }
}
