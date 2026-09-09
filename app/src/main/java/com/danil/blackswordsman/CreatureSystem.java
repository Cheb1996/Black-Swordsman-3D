package com.danil.blackswordsman;
import java.util.Random;
/** The three nature classes and distinct, telegraphed behaviour of the island's new inhabitants. */
public final class CreatureSystem {
 public static final int BIOLOGICAL=0,MAGICAL=1,MECHANICAL=2;
 public static final String[] CATEGORIES={"БИОЛОГИЧЕСКОЕ","МАГИЧЕСКОЕ","МЕХАНИЗМ"};
 private CreatureSystem(){}
 public static int category(int type){if(type==GameWorld.TOY_GOLEM||type==GameWorld.SENTRY||type==GameWorld.WALKER||type==GameWorld.SCAN_DRONE)return MECHANICAL;return type==GameWorld.WRAITH||type==GameWorld.SHADE||type==GameWorld.SKELETON||type==GameWorld.BONE_KNIGHT||type>=GameWorld.CLOCK_MOTH&&type<GameWorld.TOY_GOLEM||type==GameWorld.STORM_SPIRIT||type==GameWorld.GROVE_WARDEN?MAGICAL:BIOLOGICAL;}
 public static String ability(int type){switch(type){case GameWorld.TUSK_BOAR:return "РАЗБЕГ → ТАРАН";case GameWorld.MIRE_SPITTER:return "ЯДОВИТЫЙ ПЛЕВОК";case GameWorld.STORM_SPIRIT:return "ОТМЕТКА МОЛНИИ";case GameWorld.GROVE_WARDEN:return "ЩИТ И ОПУТЫВАНИЕ";case GameWorld.SENTRY:return "ВЕЕР СНАРЯДОВ";case GameWorld.WALKER:return "МАГНИТНЫЙ ЗАХВАТ";case GameWorld.ALIEN:return "НЕИЗВЕСТНЫЕ НАМЕРЕНИЯ";case GameWorld.SCAN_DRONE:return "СКАНИРОВАНИЕ / ИМПУЛЬС";default:return "";}}
 public static boolean populate(GameWorld w,int cx,int cz,Random rng){float x=(cx+.5f)*24,z=(cz+.5f)*24;if(Math.max(Math.abs(x),Math.abs(z))<156)return false;if(Math.abs(x)>GameWorld.OCEAN_HALF||Math.abs(z)>GameWorld.OCEAN_HALF)return true;int env=w.currentChapter().environment;
  for(int i=0;i<3;i++){float xx=x+(rng.nextFloat()-.5f)*18,zz=z+(rng.nextFloat()-.5f)*18,depth=WaterField.depth(env,xx,zz);int biome=RegionLayout.biome(xx,zz);if(depth>.3f){w.spawnNeutral(i==0?GameWorld.FISH:i==1?GameWorld.TURTLE:GameWorld.GULL,xx,zz,cx,cz);continue;}
   int animal=biome==1?(i==0?GameWorld.CAMEL:i==1?GameWorld.LIZARD:GameWorld.GOAT):i==0?GameWorld.DEER:i==1?GameWorld.FOX:GameWorld.OTTER;w.spawnNeutral(animal,xx,zz,cx,cz);
  }
  int place=RegionLayout.nearest(x,z,30);for(int i=0;i<2;i++){float xx=x+(rng.nextFloat()-.5f)*16,zz=z+(rng.nextFloat()-.5f)*16;if(WaterField.depth(env,xx,zz)>.3f||Math.abs(xx)>GameWorld.WORLD_HALF||Math.abs(zz)>GameWorld.WORLD_HALF)continue;int type;
   if(place>=0&&RegionLayout.PLACES[place][2]==RegionLayout.WORKSHOP)type=i==0?GameWorld.SENTRY:GameWorld.WALKER;
   else if(place>=0&&RegionLayout.PLACES[place][2]==RegionLayout.CAVE)type=i==0?GameWorld.MIRE_SPITTER:GameWorld.STORM_SPIRIT;
   else{int roll=rng.nextInt(6);type=GameWorld.TUSK_BOAR+roll;}
   GameWorld.Actor a=w.spawnCreature(type,xx,zz);a.chunkX=cx;a.chunkZ=cz;
  }return true;
 }
 public static void update(GameWorld w,GameWorld.Actor a,float dt){
  if(a.hovering)return;float dx=w.player.x-a.x,dz=w.player.z-a.z,d=(float)Math.sqrt(dx*dx+dz*dz);if(d>60)return;
  a.specialCooldown-=dt;
  if(a.stun>0){a.x+=a.vx*dt;a.z+=a.vz*dt;a.vx*=SimulationClock.damping(10,dt);a.vz*=SimulationClock.damping(10,dt);a.abilityState=0;return;}
  if(a.peaceful){if(a.visitor&&a.type==GameWorld.ALIEN)return;a.yaw+=(dt*.2f);if(a.type==GameWorld.SCAN_DRONE){w.physics.levitate(a.x,a.z,2.2f,w.physics.terrainHeight(a.x,a.z)+1.5f,dt);}return;}
  if(d>22){roam(w,a,dt);return;}
  a.yaw=(float)Math.atan2(dx,dz);
  if(a.abilityState==2){a.abilityTime-=dt;a.x+=a.vx*dt;a.z+=a.vz*dt;if(d<a.radius+w.player.radius+.4f&&Math.abs(a.y-w.player.y)<2){w.damagePlayer(a.damage*1.6f,a.x,a.z);a.abilityTime=0;}if(a.abilityTime<=0){a.abilityState=0;a.vx=a.vz=0;}return;}
  if(a.abilityState==1){a.abilityTime-=dt;a.vx=a.vz=0;if(a.abilityTime<=0){a.abilityState=0;activate(w,a);a.abilityCount++;a.specialCooldown=a.type==GameWorld.STORM_SPIRIT?4.8f:3.6f;}return;}
  if(a.specialCooldown<=0&&d<17&&CombatSystem.visible(w,a,w.player)){a.abilityState=1;a.abilityTime=a.type==GameWorld.STORM_SPIRIT?1.1f:.7f;a.markX=w.player.x;a.markZ=w.player.z;return;}
  float preferred=a.type==GameWorld.SENTRY?100:a.type==GameWorld.MIRE_SPITTER||a.type==GameWorld.STORM_SPIRIT?8:2;
  if(d>preferred)move(w,a,(float)Math.atan2(dx,dz),a.speed,dt);else{a.vx*=SimulationClock.damping(9,dt);a.vz*=SimulationClock.damping(9,dt);}
  if(d<a.range+w.player.radius&&a.attackCooldown<=0&&CombatSystem.visible(w,a,w.player)){a.attackCooldown=1.4f;a.attackTime=.4f;w.damagePlayer(a.damage,a.x,a.z);}
 }
 private static void move(GameWorld w,GameWorld.Actor a,float direction,float speed,float dt){float yaw=CombatSystem.steer(w,a,NavigationSystem.direction(w,a,direction));a.vx=(float)Math.sin(yaw)*speed;a.vz=(float)Math.cos(yaw)*speed;a.x+=a.vx*dt;a.z+=a.vz*dt;}
 private static void roam(GameWorld w,GameWorld.Actor a,float dt){if(a.type==GameWorld.SENTRY)return;float angle=(float)Math.atan2(a.homeX-a.x,a.homeZ-a.z);if(Math.abs(a.homeX-a.x)+Math.abs(a.homeZ-a.z)<3)angle=w.cinematicTime*.12f+a.id;move(w,a,angle,a.speed*.18f,dt);}
 private static void activate(GameWorld w,GameWorld.Actor a){
  float dx=a.markX-a.x,dz=a.markZ-a.z,len=Math.max(.1f,(float)Math.sqrt(dx*dx+dz*dz));
  if(a.type==GameWorld.TUSK_BOAR){a.abilityState=2;a.abilityTime=1.1f;a.vx=dx/len*10;a.vz=dz/len*10;}
  else if(a.type==GameWorld.MIRE_SPITTER){shot(w,a,-.12f,8,.38f,6);shot(w,a,.12f,8,.38f,6);}
  else if(a.type==GameWorld.STORM_SPIRIT){float px=w.player.x-a.markX,pz=w.player.z-a.markZ;if(px*px+pz*pz<6.25f)w.damagePlayer(a.damage*1.5f,a.x,a.z);w.physics.impulseSphere(a.markX,w.physics.terrainHeight(a.markX,a.markZ)+.2f,a.markZ,3.5f,0,8,0,true);w.fireBurst(a.markX,w.physics.terrainHeight(a.markX,a.markZ)+1,a.markZ,18);}
  else if(a.type==GameWorld.GROVE_WARDEN){a.shieldTime=2.6f;for(GameWorld.Actor ally:w.enemies)if(!ally.dead&&ally.category==MAGICAL&&(ally.x-a.x)*(ally.x-a.x)+(ally.z-a.z)*(ally.z-a.z)<36)ally.shieldTime=2.6f;if(len<7)w.player.slowTime=Math.max(w.player.slowTime,2.5f);}
  else if(a.type==GameWorld.SENTRY){for(int i=-2;i<=2;i++)shot(w,a,i*.09f,15,.14f,0);}
  else if(a.type==GameWorld.WALKER){w.physics.impulseSphere(a.x,a.y+1,a.z,7,-dx/len*8,5,-dz/len*8,true);if(len<6){w.player.vx=-dx/len*8;w.player.vz=-dz/len*8;w.player.stun=Math.max(w.player.stun,.55f);}}
  else if(a.type==GameWorld.ALIEN){a.x-=dx/len*3;a.z-=dz/len*3;a.invulnerable=.35f;shot(w,a,0,11,.18f,0);}
  else if(a.type==GameWorld.SCAN_DRONE){w.physics.freezeNear(a.x,a.z,6,1.6f);if(len<7){w.damagePlayer(a.damage,a.x,a.z);w.player.slowTime=1.5f;}}
 }
 private static void shot(GameWorld w,GameWorld.Actor a,float offset,float speed,float radius,float poison){float angle=(float)Math.atan2(w.player.x-a.x,w.player.z-a.z)+offset;GameWorld.Projectile p=new GameWorld.Projectile();p.x=a.x+(float)Math.sin(angle)*a.radius;p.z=a.z+(float)Math.cos(angle)*a.radius;p.y=a.y+a.height*.7f;p.vx=(float)Math.sin(angle)*speed;p.vz=(float)Math.cos(angle)*speed;float d=Math.max(1,(float)Math.sqrt((w.player.x-a.x)*(w.player.x-a.x)+(w.player.z-a.z)*(w.player.z-a.z))),travel=d/speed;p.vy=(w.player.y+w.player.height*.55f-p.y)/Math.max(.2f,travel)+1.1772f*travel;p.radius=radius;p.damage=a.damage;p.life=3;p.pierce=1;p.poison=poison;p.r=poison>0?.18f:.18f;p.g=poison>0?.85f:.65f;p.b=poison>0?.08f:1;w.projectiles.add(p);}
}
