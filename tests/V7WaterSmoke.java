package com.danil.blackswordsman;
import java.util.ArrayList;
public final class V7WaterSmoke {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static GameWorld.Actor player(float x,float z){GameWorld.Actor p=new GameWorld.Actor();p.player=true;p.height=2;p.radius=.5f;p.x=x;p.z=z;p.y=6;p.hp=p.maxHp=100;return p;}
 public static void main(String[] args)throws Exception{
  float width=GameWorld.WORLD_HALF*2;check(Math.abs(width*width/(284*284f)-10)<.0001f,"tenfold map area");
  for(float[] lake:WaterField.LAKES){check(WaterField.depth(0,lake[0],lake[1])>3,"lake basin");check(WaterField.kind(0,lake[0],lake[1])>=WaterField.LAKE,"lake/oasis volume");}
  for(int i=0;i<16;i++){float angle=i*.3926991f,x=(float)Math.sin(angle)*800,z=(float)Math.cos(angle)*800;check(WaterField.kind(0,x,z)==WaterField.OCEAN&&WaterField.depth(0,x,z)>8,"surrounding ocean");}
  check(WaterField.kind(0,0,40)==WaterField.RIVER&&WaterField.depth(0,0,40)>1.5f,"river under bridge");check(WaterField.depth(0,0,5)==0,"starting road remains dry");
  PhysicsWorld p=new PhysicsWorld();p.resetForChapter(0,0);GameWorld.Actor a=player(550,550);p.streamAround(a.x,a.z);p.bodies.clear();p.joints.clear();
  for(int i=0;i<240;i++){PhysicsWorld.Body b=p.createToy(SurrealCatalog.ITEMS[i],480+i%20*2.2f,480+i/20*2.2f);b.y=1+(i%3)*.7f;b.vy=-3;b.ry=i*13;}
  for(int kind=1;kind<=7;kind++){PhysicsWorld.Body b=p.createLoose(kind,480+kind*3,2,516,.22f,.3f,.22f,kind*10);if(kind==7){b.shape=PhysicsWorld.SHAPE_SPHERE;b.radius=.22f;b.bone=CharacterSkeleton.HEAD;}b.life=10;}
  ArrayList<GameWorld.Actor> enemies=new ArrayList<GameWorld.Actor>();
  for(int i=0;i<1200;i++)p.update(1f/120,a,enemies);
  int afloat=0;for(PhysicsWorld.Body b:p.bodies){check(!Float.isNaN(b.y),"finite buoyancy");if(b.catalogId>=0){float surface=WaterField.surface(0,b.x,b.z,10);check(b.y>surface-b.ey-1f&&b.y<surface+b.ey+1f,"floating catalog id "+b.catalogId+" y="+b.y);check(b.submerged>.05f,"catalog immersion "+b.catalogId);afloat++;}else check(b.submerged>.05f,"body kind floats "+b.kind);}
  check(afloat==240,"every catalog type stays in water");check(a.swimming&&a.y>-4&&a.y< -2,"player swims at surface");
  GameWorld.Particle particle=new GameWorld.Particle();particle.x=490;particle.z=490;particle.y=-3;particle.size=.1f;particle.vy=-4;WaterField.particle(0,particle,3,.016f);check(particle.floating&&particle.vy==0&&particle.y> -2.3f,"particles ride surface");
  PhysicsWorld stream=new PhysicsWorld();stream.resetForChapter(0,0);stream.streamAround(500,500);PhysicsWorld.Body saved=null;int floating=0;for(PhysicsWorld.Body b:stream.bodies)if(b.streamed&&b.catalogId>=0){saved=b;floating++;}check(floating>30,"ocean salvage is populated");float original=saved.x;saved.ry=77;stream.streamAround(-400,-400);check(!stream.bodies.contains(saved),"distant water props unload");stream.streamAround(500,500);check(stream.bodies.contains(saved)&&saved.x==original&&saved.ry==77,"drift state restores without duplication");check(stream.bodies.size()<600,"bounded active water bodies");
  boolean[] seen=new boolean[240];int boats=0;for(int cz=-25;cz<=25;cz++)for(int cx=-25;cx<=25;cx++)for(int n=0;n<14;n++){int id=RegionLayout.waterCatalog(cx,cz,n);seen[id]=true;if(id==152||id==182)boats++;}for(boolean v:seen)check(v,"all catalog IDs distributed");check(boats>10,"occasional toy boats");
  for(int kind=0;kind<5;kind++){float[] q=WaterField.LAKES[kind];WaterGeometry mesh=new WaterGeometry(0,q[0]-12,q[1]-12,24,24);check(mesh.wet(),"lake render mesh");for(short index:mesh.indices)check((index&65535)<mesh.vertices.length/9,"valid water index");}
  System.out.println("V7WaterSmoke OK: 10x area, five lakes/oases, river, surrounding ocean, 240 floating toys + seven body kinds, swimming, particles, streamed drift, boat distribution");
 }
}
