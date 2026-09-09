package com.danil.blackswordsman;
/** Independent temporary mushroom powers. Peace ends on a deliberate attack. */
public final class MushroomEffects implements java.io.Serializable {
 private static final long serialVersionUID=8L;
 public static final int PEACE=0,FLIGHT=1,MAGNET=2,REPULSE=3,CHRONO=4,HEAL=5,FEATHER=6,LANTERN=7;
 public static final String[] NAMES={"МИРНОЕ ПРИСУТСТВИЕ","ПОЛЁТ ↑ ПРЫЖОК / ↓ РЫВОК","ПРИТЯЖЕНИЕ","ОТТАЛКИВАНИЕ","ЗАМЕДЛЕНИЕ ВОКРУГ","РЕГЕНЕРАЦИЯ + ОЧИЩЕНИЕ","ПРЫЖОК И МЯГКОЕ ПАДЕНИЕ","ЗРЕНИЕ ЧАЩИ"};
 public static final float[] DURATION={30,25,28,20,15,22,25,35};
 public static final float[][] COLORS={{.25f,.80f,.64f},{.34f,.65f,1},{.78f,.25f,.88f},{1,.36f,.17f},{.78f,.62f,.19f},{.28f,.88f,.27f},{.8f,.79f,.93f},{.09f,.93f,.8f}};
 public final float[] timer=new float[8];private float pulse,particles;
 public boolean active(int id){return timer[id]>0;}
 public void clear(GameWorld.Actor p){java.util.Arrays.fill(timer,0);p.flying=false;p.featherTime=0;}
 public void activate(int id,GameWorld w){if(id<0||id>=8)return;timer[id]=DURATION[id];if(id==MAGNET)timer[REPULSE]=0;if(id==REPULSE)timer[MAGNET]=0;if(id==FLIGHT){w.player.flying=true;w.player.flightY=Math.max(w.player.y+3,w.physics.terrainHeight(w.player.x,w.player.z)+3);w.player.grounded=false;}if(id==PEACE)for(GameWorld.Actor a:w.enemies){a.strikePending=false;a.attackTime=0;}if(id==HEAL)w.player.poisonTime=0;}
 public void attack(){timer[PEACE]=0;}
 public void update(GameWorld w,float dt){GameWorld.Actor p=w.player;if(p.dead){clear(p);return;}boolean flew=active(FLIGHT);for(int i=0;i<8;i++)timer[i]=Math.max(0,timer[i]-dt);p.flying=active(FLIGHT);if(flew&&!p.flying)p.featherTime=Math.max(p.featherTime,7);if(active(FEATHER))p.featherTime=1;else p.featherTime=Math.max(0,p.featherTime-dt);if(active(HEAL)){p.hp=Math.min(p.maxHp,p.hp+4*dt);p.poisonTime=0;}pulse-=dt;
 if(active(MAGNET)||active(REPULSE)){float sign=active(MAGNET)?-1:1;PhysicsWorld.Body held=w.physics.getHeld();for(PhysicsWorld.Body b:w.physics.bodies){if(!b.dynamic||b==held||b.hidden)continue;float dx=b.x-p.x,dy=b.y-(p.y+.75f),dz=b.z-p.z,d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(d<.8f||d>11)continue;float force=sign*(1-d/12)*16/Math.max(1,(float)Math.sqrt(b.mass/5));b.vx+=dx/d*force*dt;b.vy+=dy/d*force*dt;b.vz+=dz/d*force*dt;b.sleeping=false;}}
 if(active(CHRONO)&&pulse<=0){pulse=.3f;for(GameWorld.Actor a:w.enemies)if(!a.dead&&GameWorld.distance(p,a)<9){a.slowTime=Math.max(a.slowTime,.5f);a.attackCooldown=Math.max(a.attackCooldown,.32f);}for(PhysicsWorld.Body b:w.physics.bodies)if(b.dynamic&&b!=w.physics.getHeld()&&Math.abs(b.x-p.x)<8&&Math.abs(b.z-p.z)<8)b.stasis=Math.max(b.stasis,.22f);}
 particles-=dt;if(particles<=0){particles=.18f;for(int i=0;i<8;i++)if(active(i)){float[] c=COLORS[i];w.astralBurst(p.x,p.y+.2f,p.z,c[0],c[1],c[2],1);}}
 }
 public String status(int line){String s="";int count=0;for(int i=0;i<8;i++)if(active(i)){if(count/2==line){if(s.length()>0)s+="  ·  ";s+=NAMES[i]+" "+(int)Math.ceil(timer[i])+"с";}count++;}return s;}
}
