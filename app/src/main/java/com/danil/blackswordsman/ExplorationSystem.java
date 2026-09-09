package com.danil.blackswordsman;
public final class ExplorationSystem implements java.io.Serializable {
 private static final long serialVersionUID=6L;
 public PhysicsWorld.Body heavy,barrier,lift,cache,dominoLast;public boolean tracked,phased,passed,lifted,dominoSolved;public String hint="";
 public void reset(GameWorld w){
  PhysicsWorld p=w.physics;tracked=phased=passed=lifted=dominoSolved=false;
  heavy=p.createToy(SurrealCatalog.ITEMS[56],24,16);heavy.hx=1.2f;heavy.hy=1.1f;heavy.hz=1.2f;heavy.mass=85;heavy.invMass=1f/85;heavy.y=p.terrainHeight(24,16)+1.1f;
  cache=p.createStatic(PhysicsWorld.ALTAR,30,p.terrainHeight(30,16)+.4f,16,.5f,.4f,.5f);
  // A low lintel leaves 0.95 m clearance: serpent fits, Guts cannot.
  for(int s=-1;s<=1;s+=2)p.createStatic(PhysicsWorld.RUIN,24+s*1.6f,p.terrainHeight(24,27)+1.2f,27,.65f,1.2f,3);
  p.createStatic(PhysicsWorld.PLANK,24,p.terrainHeight(24,27)+1.3f,27,1,.35f,3);
  barrier=p.createStatic(PhysicsWorld.GATE,24,p.terrainHeight(24,37)+1.6f,37,2.8f,1.6f,.35f);barrier.astral=true;barrier.r=.1f;barrier.g=.5f;barrier.b=.9f;
  lift=p.createToy(SurrealCatalog.ITEMS[100],-25,22);lift.hx=1.8f;lift.hy=.24f;lift.hz=1.8f;lift.mass=12;lift.invMass=1f/12;lift.y=p.terrainHeight(-25,22)+.25f;
  for(int i=0;i<12;i++){PhysicsWorld.Body b=p.createToy(SurrealCatalog.ITEMS[72+i],-12,19+i*.45f);b.hx=.35f;b.hy=.55f;b.hz=.09f;b.rx=0;b.y=p.terrainHeight(b.x,b.z)+b.hy;b.friction=.6f;b.restitution=0;CollisionMath.refresh(b);dominoLast=b;}
 }
 private float distance(GameWorld w,float x,float z){float dx=w.player.x-x,dz=w.player.z-z;return (float)Math.sqrt(dx*dx+dz*dz);}
 public void update(GameWorld w,float dt){
  hint="";if(w.effects.form==PlayerEffects.HOUND&&!tracked&&distance(w,30,16)<35){hint="СЛЕД ТАЙНИКА · "+(int)distance(w,30,16)+" м";if(distance(w,30,16)<3){tracked=true;w.collectPickup(PlayerEffects.POWER);w.banner="ТАЙНИК ГОНЧЕЙ: ПИРОЖНОЕ СИЛЫ";w.bannerTime=3;}}
  if(!passed&&w.effects.form==PlayerEffects.SERPENT&&distance(w,24,31)<2){passed=true;w.collectPickup(PlayerEffects.ARMOR);w.banner="ЗМЕИНЫЙ ЛАЗ: ЖЕЛЕ ЗАЩИТЫ";w.bannerTime=3;}
  if(!phased&&distance(w,24,40)<3)hint=w.effects.form==PlayerEffects.WRAITH?"АСТРАЛЬНЫЙ ПЕРЕКЛЮЧАТЕЛЬ · ДЕЙСТВИЕ":"ПЕЧАТЬ ОТВЕЧАЕТ ТОЛЬКО ДУХУ";
  if(!lifted&&lift!=null&&lift.y>w.physics.terrainHeight(lift.x,lift.z)+3&&distance(w,lift.x,lift.z)<5){lifted=true;w.collectPickup(PlayerEffects.SPEED);w.banner="ГРУЗ ПОДНЯТ ЛУЧОМ НЛО";w.bannerTime=3;}
  if(!dominoSolved&&dominoLast!=null&&Math.abs(dominoLast.rx)>45){dominoSolved=true;w.collectPickup(PlayerEffects.REGEN);w.banner="ЦЕПНАЯ РЕАКЦИЯ ДОМИНО";w.bannerTime=3;}
  if(distance(w,24,16)<4&&heavy!=null&&heavy.x>22&&heavy.x<26)hint="85 КГ · ЗВЕРЬ МОЖЕТ ПОДНЯТЬ ГРУЗ";
 }
 public boolean interact(GameWorld w){if(!phased&&w.effects.form==PlayerEffects.WRAITH&&distance(w,24,40)<3){phased=true;barrier.solid=false;w.collectPickup(PlayerEffects.AMMO);w.banner="АСТРАЛЬНЫЕ ВОРОТА ОТКРЫТЫ";w.bannerTime=3;w.saveCheckpoint();return true;}return false;}
}
