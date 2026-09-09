package com.danil.blackswordsman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Deterministic lightweight rigid-body world for mobile hardware. It handles
 * gravity, impulses, body/body and actor/body contacts, articulated ragdolls,
 * detachable limbs, dropped equipment, grabbing/throwing and pressure plates.
 */
public final class PhysicsWorld implements java.io.Serializable {
    private static final long serialVersionUID=6L;
    public static final float GRAVITY=-9.81f;
    public static final int CRATE=1,BARREL=2,ROCK=3,PLANK=4,WEAPON=5,RAGDOLL=6,BODY_PART=7;
    public static final int PRESSURE_PLATE=8,GATE=9,COLUMN=10,CART=11,BONE=12,CHAIN=13,ALTAR=14;
    public static final int TREE=15,GALLOWS=16,CAGE=17,STAKE=18,STATUE=19,FLESH=20,GRAVE=21;
    public static final int SHRUB=22,MUSHROOM=23,RUIN=24,LANTERN=25,SPRING=26;
    public static final int TOY_ITEM=27,UFO=28,STONE_WALL=29,ROOF=30,PALM=31;
    public static final int SHAPE_BOX=0,SHAPE_SPHERE=1,SHAPE_CAPSULE=2;
    public static final float CHUNK_SIZE=24f;

    public static final class Body implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public int id,kind,shape,bone=-1,group,weaponType,variant,chunkX,chunkZ;
        public int catalogId=-1,surrealId=-1;
        public boolean dynamic,solid=true,grabbable,sleeping,hidden,streamed;
        public float x,y,z,vx,vy,vz,rx,ry,rz,avx,avy,avz;
        public float ex,ey,ez,stasis,skinOffset; public final float[] axis=new float[9];
        public boolean astral; public GameWorld.Actor owner;
        public float hx=.4f,hy=.4f,hz=.4f,radius=.5f,mass=1f,invMass=1f;
        public float restitution=.18f,friction=.72f,life,stillTime;
        public float impactCooldown,submerged;
        public float r=.25f,g=.22f,b=.20f,metalness=.08f,roughness=.78f;
        public Body parent;
    }

    public static final class Joint implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public Body a,b;
        public float restLength,stiffness=.72f,breakDistance=2.2f;
        public boolean broken,anchored,locked;
        public float ax,ay,az,bx,by,bz,restX,restY,restZ,minX=-65,maxX=65,minY=-45,maxY=45,minZ=-55,maxZ=55;
    }

    public static final class Impact implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public GameWorld.Actor actor;
        public float damage,fromX,fromZ,impulseX,impulseZ;
    }

    public final ArrayList<Body> bodies=new ArrayList<Body>();
    public final ArrayList<Joint> joints=new ArrayList<Joint>();
    public final ArrayList<Impact> impacts=new ArrayList<Impact>();
    private final Random random=new Random(0x50485953L);
    private final HashSet<Long> loadedChunks=new HashSet<Long>();
    private HashSet<Long> generatedChunks=new HashSet<Long>();
    private int nextId=1;
    private Body held,plate,gate;
    private boolean puzzleRequired,puzzleSolved;
    private float gateOpen;
    private float time;
    private int environment;
    private Body[] contactOrder=new Body[512];
    private static final Comparator<Body> BY_LEFT=new Comparator<Body>(){public int compare(Body a,Body b){float d=(a.x-a.ex)-(b.x-b.ex);return d<0?-1:d>0?1:a.id-b.id;}};
    public int contactCandidates;
    private int streamChunkX=Integer.MIN_VALUE,streamChunkZ=Integer.MIN_VALUE;
    private String puzzleText="ПОМЕСТИТЕ ТЯЖЁЛЫЙ ЯЩИК НА БАГРОВУЮ ПЛИТУ";
    public int solverIterations=6;
    private final CollisionMath.Contact contact=new CollisionMath.Contact();
    private final Body actorProxy=new Body();
    private final java.util.HashMap<Long,ArrayList<Body>> chunkArchive=new java.util.HashMap<Long,ArrayList<Body>>();
    public volatile int activeBodies;
    public volatile int ragdollBodies;

    public void resetForChapter(int chapter,int env){
        if(generatedChunks==null)generatedChunks=new HashSet<Long>();
        bodies.clear();joints.clear();loadedChunks.clear();generatedChunks.clear();chunkArchive.clear();held=null;plate=null;gate=null;puzzleSolved=false;gateOpen=0;time=0;environment=env;nextId=1;streamChunkX=streamChunkZ=Integer.MIN_VALUE;
        random.setSeed(0x50485953L+chapter*7919L+env*313L);
        WorldLayout.populate(this,env);
        for(int n=0;n<5;n++){Body b=make(CRATE,true,-5-n*1.6f,terrainHeight(-5-n*1.6f,8)+.6f,8,.58f,.58f,.58f,6);b.grabbable=true;b.r=.25f;b.g=.15f;b.b=.08f;}
        for(int n=0;n<5;n++){Body b=make(ROCK,true,9+n,terrainHeight(9+n,14)+.35f,14,.35f,.35f,.35f,2);b.shape=SHAPE_SPHERE;b.grabbable=true;}
        puzzleRequired=chapter==2||chapter==5||chapter==7;
        if(puzzleRequired){
            puzzleText=env==2?"ПЕРЕМЕСТИТЕ ГРУЗ НА ЗМЕИНУЮ ПЕЧАТЬ":env==5?"ПРИЖМИТЕ СТОЧНУЮ ПЛИТУ ТЯЖЁЛЫМ ЯЩИКОМ":"ОТКРОЙТЕ ВОРОТА МЕХАНИЗМОМ ДАВЛЕНИЯ";
            plate=make(PRESSURE_PLATE,false,4.3f,terrainHeight(4.3f,-5.2f)+.055f,-5.2f,1.25f,.055f,1.25f,100);plate.solid=false;plate.r=.22f;plate.g=.08f;plate.b=.065f;plate.metalness=.7f;
            gate=make(GATE,false,0,terrainHeight(0,-9.3f)+1.55f,-9.3f,2.15f,1.55f,.32f,200);gate.r=.12f;gate.g=.11f;gate.b=.105f;gate.metalness=.72f;
            Body key=make(CRATE,true,-3.8f,terrainHeight(-3.8f,3.6f)+.78f,3.6f,.72f,.72f,.72f,10f);key.grabbable=true;key.r=.28f;key.g=.16f;key.b=.075f;key.metalness=.14f;
        }
        if(plate!=null){plate.x=4.3f;plate.z=69;plate.y=terrainHeight(plate.x,plate.z)+.055f;gate.x=0;gate.z=73;gate.y=terrainHeight(0,73)+1.55f;for(Body b:bodies)if(b.kind==CRATE&&b.mass>=9){b.x=-4;b.z=65;b.y=terrainHeight(-4,65)+b.hy;}}
        streamAround(0f,5f);
        publishCounts();
    }

    private Body make(int kind,boolean dynamic,float x,float y,float z,float hx,float hy,float hz,float mass){
        Body b=new Body();b.id=nextId++;b.kind=kind;b.dynamic=dynamic;b.x=x;b.y=y;b.z=z;b.hx=hx;b.hy=hy;b.hz=hz;b.radius=Math.max(hx,hz);b.mass=mass;b.invMass=dynamic?1f/Math.max(.05f,mass):0f;CollisionMath.refresh(b);bodies.add(b);return b;
    }

    public Body createStatic(int kind,float x,float y,float z,float hx,float hy,float hz){Body b=make(kind,false,x,y,z,hx,hy,hz,500);b.r=.18f;b.g=.14f;b.b=.10f;return b;}
    public Body createToy(SurrealCatalog.Item item,float x,float z){
        Body b=make(TOY_ITEM,true,x,terrainHeight(x,z)+item.hy+.015f,z,item.hx,item.hy,item.hz,item.mass);
        b.catalogId=item.id;b.variant=item.variant;b.grabbable=true;b.r=item.r;b.g=item.g;b.b=item.blue;
        b.shape=item.family==SurrealCatalog.BALL?SHAPE_SPHERE:SHAPE_BOX;CollisionMath.refresh(b);
        b.restitution=item.family==SurrealCatalog.BALL?.72f:item.family==SurrealCatalog.RING?.38f:.18f;
        b.friction=item.family==SurrealCatalog.BALL?.25f:.64f;b.metalness=item.family==SurrealCatalog.RELIC?.36f:.04f;b.roughness=.32f;return b;
    }
    public Body createLoose(int kind,float x,float y,float z,float hx,float hy,float hz,float mass){Body b=make(kind,true,x,y,z,hx,hy,hz,mass);b.grabbable=true;return b;}
    public int environment(){return environment;}
    public float waterTime(){return time;}
    public Body createUfo(float x,float z){Body b=make(UFO,false,x,terrainHeight(x,z)+8f,z,3.1f,1.15f,3.1f,800f);b.r=.30f;b.g=.43f;b.b=.52f;b.metalness=.85f;b.roughness=.18f;return b;}
    /** Bounded acceleration lifts loose objects; the player is never moved by this field. */
    public void levitate(float x,float z,float radius,float targetY,float dt){
        for(int i=0;i<bodies.size();i++){Body b=bodies.get(i);if(!b.dynamic||b==held||b.kind==RAGDOLL||b.kind==BODY_PART)continue;float dx=x-b.x,dz=z-b.z;if(dx*dx+dz*dz>radius*radius)continue;
            b.vy+=clamp((targetY-b.y)*4.5f-b.vy*2.5f-GRAVITY,-18f,23f)*dt;b.vx+=clamp(dx*.85f-dz*.75f-b.vx*.30f,-4f,4f)*dt;b.vz+=clamp(dz*.85f+dx*.75f-b.vz*.30f,-4f,4f)*dt;b.avy+=dt*.5f;b.sleeping=false;
        }
    }

    public boolean hasPuzzle(){return puzzleRequired;}
    public boolean isPuzzleSolved(){return !puzzleRequired||puzzleSolved;}
    public String puzzleObjective(){return puzzleText;}
    public Body getHeld(){return held;}

    /** Grab/throw the nearest item; if none is in reach, kick nearby loose bodies. */
    public Body grabCandidate(GameWorld.Actor actor){
        Body best=null;float bestDistance=3.1f;
        for(int i=0;i<bodies.size();i++){Body b=bodies.get(i);if(!b.dynamic||!b.grabbable||b.kind==RAGDOLL||b.mass>(actor.type==GameWorld.VOID_BEAST?180f:18f)||Math.abs(b.y-(actor.y+actor.height*.45f))>2.6f)continue;float dx=b.x-actor.x,dz=b.z-actor.z,d=(float)Math.sqrt(dx*dx+dz*dz);if(d<bestDistance){best=b;bestDistance=d;}}
        return best;
    }
    public int interact(GameWorld.Actor actor){
        if(held!=null){float s=(float)Math.sin(actor.yaw),c=(float)Math.cos(actor.yaw);held.vx=actor.vx+s*10.5f;held.vy=4.2f;held.vz=actor.vz+c*10.5f;held.avx=5f;held.avy=8f;held.avz=-4f;held.sleeping=false;held=null;return 2;}
        Body best=grabCandidate(actor);
        if(best!=null){held=best;best.sleeping=false;return 1;}
        float s=(float)Math.sin(actor.yaw),c=(float)Math.cos(actor.yaw);impulseSphere(actor.x+s*1.2f,.6f,actor.z+c*1.2f,2.1f,s*5.5f,2.2f,c*5.5f,false);return 3;
    }

    public void update(float dt,GameWorld.Actor player,ArrayList<GameWorld.Actor> enemies){
        time+=dt;impacts.clear();streamAround(player.x,player.z);
        if(held!=null){float s=(float)Math.sin(player.yaw),c=(float)Math.cos(player.yaw),tx=player.x+s*1.65f,ty=player.y+Math.max(.7f,player.height*.60f),tz=player.z+c*1.65f;held.vx+=(tx-held.x)*Math.min(22f,dt*90f);held.vy+=(ty-held.y)*Math.min(22f,dt*90f);held.vz+=(tz-held.z)*Math.min(22f,dt*90f);float drag=SimulationClock.damping(12f,dt);held.vx*=drag;held.vy*=drag;held.vz*=drag;held.avx*=drag;held.avy*=drag;held.avz*=drag;}
        for(int i=0;i<bodies.size();i++){Body b=bodies.get(i);if(b.life>0&&b.submerged<.05f)b.life-=dt;b.impactCooldown=Math.max(0,b.impactCooldown-dt);}
        for(int i=0;i<bodies.size();i++)integrate(bodies.get(i),dt);
        // Register a correctly placed puzzle weight before neighbouring bodies resolve overlap.
        updatePuzzle(0f);
        // Iterative contacts and articulated constraints are stable enough for 60-120 Hz mobile simulation.
        contactCandidates=0;
        for(int iteration=0;iteration<solverIterations;iteration++){
            for(int i=0;i<joints.size();i++)solveJoint(joints.get(i));
            solveBroadphase();
        }
        resolveActor(player,dt);for(int i=0;i<enemies.size();i++){GameWorld.Actor e=enemies.get(i);if(!e.dead&&Math.abs(e.x-player.x)<65&&Math.abs(e.z-player.z)<65){resolveActor(e,dt);}}
        separateActors(player,enemies);
        updatePuzzle(dt);cleanup();publishCounts();
    }

    private void integrate(Body b,float dt){
        CollisionMath.refresh(b);if(b.stasis>0){b.stasis=Math.max(0,b.stasis-dt);return;}if(!b.dynamic)return;if(b!=held)WaterField.body(environment,b,time,dt);if(b.sleeping)return;
        if(b!=held)b.vy+=GRAVITY*dt;
        float max=32f;b.vx=clamp(b.vx,-max,max);b.vy=clamp(b.vy,-max,max);b.vz=clamp(b.vz,-max,max);
        b.avx=clamp(b.avx,-18,18);b.avy=clamp(b.avy,-18,18);b.avz=clamp(b.avz,-18,18);
        b.x+=b.vx*dt;b.y+=b.vy*dt;b.z+=b.vz*dt;b.rx+=b.avx*dt*57.2958f;b.ry+=b.avy*dt*57.2958f;b.rz+=b.avz*dt*57.2958f;CollisionMath.refresh(b);
        float floor=groundHeight(b.x,b.z);
        if(b.y-b.ey<floor){
            float depth=floor-(b.y-b.ey);b.y+=depth;
            float rx=0,rz=0;if(b.shape!=SHAPE_SPHERE){for(int axis=0;axis<3;axis++){int k=axis*3;float sign=Math.abs(b.axis[k+1])<.0001f?0:-Math.signum(b.axis[k+1]),h=axis==0?b.hx:axis==1?b.hy:b.hz;rx+=b.axis[k]*sign*h;rz+=b.axis[k+2]*sign*h;}}
            float contactV=b.vy+b.avz*rx-b.avx*rz,k=3f*b.invMass/Math.max(.025f,b.hx*b.hx+b.hy*b.hy+b.hz*b.hz);
            if(contactV<0){float j=-(1+(contactV< -1.2f?b.restitution:0))*contactV/(b.invMass+k*(rx*rx+rz*rz));ContactSolver.impulse(b,0,j,0,rx,-b.ey,rz);}
            float drag=SimulationClock.damping(b.friction*2f,dt),speed=(float)Math.sqrt(b.vx*b.vx+b.vz*b.vz);if(speed>.0001f){float friction=Math.max(0,speed-b.friction*9.81f*dt*(b.shape==SHAPE_SPHERE?.025f:1f))/speed;b.vx*=friction;b.vz*=friction;}
            if(b.shape==SHAPE_SPHERE){float roll=Math.min(1,dt*8);b.avx+=(b.vz/Math.max(.08f,b.radius)-b.avx)*roll;b.avz+=(-b.vx/Math.max(.08f,b.radius)-b.avz)*roll;b.avy*=drag;}
            else{b.avx*=drag;b.avz*=drag;b.avy*=drag;}
        }
        if(Math.abs(b.x)>GameWorld.OCEAN_HALF-.4f){b.x=clamp(b.x,-GameWorld.OCEAN_HALF+.4f,GameWorld.OCEAN_HALF-.4f);b.vx*=-b.restitution;}
        if(Math.abs(b.z)>GameWorld.OCEAN_HALF-.4f){b.z=clamp(b.z,-GameWorld.OCEAN_HALF+.4f,GameWorld.OCEAN_HALF-.4f);b.vz*=-b.restitution;}
        float motion=Math.abs(b.vx)+Math.abs(b.vy)+Math.abs(b.vz)+Math.abs(b.avx)*.1f+Math.abs(b.avy)*.1f+Math.abs(b.avz)*.1f;
        if(motion<.065f&&b!=held&&b.submerged<.01f){b.stillTime+=dt;if(b.stillTime>1.8f){b.sleeping=true;b.vx=b.vy=b.vz=b.avx=b.avy=b.avz=0;}}else b.stillTime=0;
    }

    /** Sweep along X instead of testing every possible pair in a crowded toy field. */
    private void solveBroadphase(){
        int count=bodies.size();if(contactOrder.length<count)contactOrder=new Body[count+128];
        for(int i=0;i<count;i++)contactOrder[i]=bodies.get(i);
        Arrays.sort(contactOrder,0,count,BY_LEFT);
        for(int i=0;i<count;i++){Body a=contactOrder[i];float right=a.x+a.ex+.08f;
            for(int j=i+1;j<count;j++){Body b=contactOrder[j];if(b.x-b.ex>right)break;if((!a.dynamic&&!b.dynamic)||(a.sleeping&&b.sleeping))continue;contactCandidates++;solveContact(a,b);}
        }
    }

    private void solveJoint(Joint j){
        if(j.broken)return;Body a=j.a,b=j.b;if((a.streamed&&!loadedChunks.contains(chunkKey(a.chunkX,a.chunkZ)))||(b.streamed&&!loadedChunks.contains(chunkKey(b.chunkX,b.chunkZ))))return;if(a.stasis>0||b.stasis>0)return;float total=a.invMass+b.invMass;if(total<=0)return;
        float ax=a.x,ay=a.y,az=a.z,bx=b.x,by=b.y,bz=b.z;
        if(j.anchored){ax+=a.axis[0]*j.ax+a.axis[3]*j.ay+a.axis[6]*j.az;ay+=a.axis[1]*j.ax+a.axis[4]*j.ay+a.axis[7]*j.az;az+=a.axis[2]*j.ax+a.axis[5]*j.ay+a.axis[8]*j.az;bx+=b.axis[0]*j.bx+b.axis[3]*j.by+b.axis[6]*j.bz;by+=b.axis[1]*j.bx+b.axis[4]*j.by+b.axis[7]*j.bz;bz+=b.axis[2]*j.bx+b.axis[5]*j.by+b.axis[8]*j.bz;}
        float dx=bx-ax,dy=by-ay,dz=bz-az,d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(d>Math.max(2f,j.restLength*j.breakDistance)){j.broken=true;b.parent=null;return;}
        float correction=j.anchored?j.stiffness:d>.0001f?(d-j.restLength)/d*j.stiffness:0;
        if(a.dynamic){float w=a.invMass/total;a.x+=dx*correction*w;a.y+=dy*correction*w;a.z+=dz*correction*w;}
        if(b.dynamic){float w=b.invMass/total;b.x-=dx*correction*w;b.y-=dy*correction*w;b.z-=dz*correction*w;}
        if(j.anchored){
            float x=angle(b.rx-a.rx-j.restX),y=angle(b.ry-a.ry-j.restY),z=angle(b.rz-a.rz-j.restZ);
            float cx=x-clamp(x,j.minX,j.maxX),cy=y-clamp(y,j.minY,j.maxY),cz=z-clamp(z,j.minZ,j.maxZ);
            float wa=a.invMass/total,wb=b.invMass/total;
            a.rx+=cx*.45f*wa;a.ry+=cy*.45f*wa;a.rz+=cz*.45f*wa;b.rx-=cx*.45f*wb;b.ry-=cy*.45f*wb;b.rz-=cz*.45f*wb;
            if(Math.abs(cx)>1)b.avx*=.65f;if(Math.abs(cy)>1)b.avy*=.65f;if(Math.abs(cz)>1)b.avz*=.65f;
            CollisionMath.refresh(a);CollisionMath.refresh(b);
        }
    }
    private static float angle(float a){while(a>180)a-=360;while(a< -180)a+=360;return a;}
    private void solveContact(Body a,Body b){
        if(!a.solid||!b.solid||(!a.dynamic&&!b.dynamic)||a==held||b==held||a.stasis>0||b.stasis>0||a.parent==b||b.parent==a)return;
        if(CollisionMath.contact(a,b,contact))ContactSolver.solve(a,b,contact);
    }

    private void resolveActor(GameWorld.Actor actor,float dt){
        if(actor.dead)return;
        if(actor.hovering)return;
        float floor=terrainHeight(actor.x,actor.z),depth=WaterField.depth(environment,actor.x,actor.z),water=WaterField.surface(environment,actor.x,actor.z,time);actor.swimming=depth>actor.height*.6f&&actor.y<water-actor.height*.35f;
        if(actor.flying){actor.swimming=false;float target=Math.max(floor+.12f,Math.min(floor+14,actor.flightY));actor.vy+=(clamp((target-actor.y)*12,-10,14)-actor.vy*4-GRAVITY)*dt;}else if(actor.swimming){float target=water-actor.height*.65f;actor.vy+=(clamp((target-actor.y)*18f,-8,18)-actor.vy*5f-GRAVITY)*dt;actor.x+=WaterField.flowX(environment,actor.x,actor.z,time)*dt*.4f;actor.z+=WaterField.flowZ(environment,actor.x,actor.z,time)*dt*.4f;}
        actor.vy+=GRAVITY*dt;if(actor.featherTime>0)actor.vy=Math.max(actor.vy,-3.2f);actor.y+=actor.vy*dt;actor.grounded=false;
        if(actor.y<=floor){actor.y=floor;actor.vy=0;actor.grounded=true;}
        Body p=actorProxy;p.dynamic=true;p.invMass=1f/(actor.type==GameWorld.VOID_BEAST?400:70);p.mass=1/p.invMass;p.shape=SHAPE_BOX;p.hx=p.hz=actor.radius*.80f;p.hy=actor.height*.5f;p.radius=actor.radius;p.rx=p.ry=p.rz=0;p.vx=actor.vx;p.vy=actor.vy;p.vz=actor.vz;p.avx=p.avy=p.avz=0;p.restitution=0;p.friction=0f;p.x=actor.x;p.y=actor.y+p.hy;p.z=actor.z;CollisionMath.refresh(p);
        for(int iteration=0;iteration<2;iteration++)for(int i=0;i<bodies.size();i++){
            Body b=bodies.get(i);if(!b.solid||b==held||b.astral&&actor.type==GameWorld.WRAITH||Math.abs(p.x-b.x)>p.hx+b.ex+.35f||Math.abs(p.z-b.z)>p.hz+b.ez+.35f)continue;
            if(!CollisionMath.contact(p,b,contact))continue;
            float top=b.y+b.ey;if(actor.grounded&&top-actor.y>0&&top-actor.y<.32f&&actor.vy<=0){p.y=top+p.hy;continue;}
            float speed=(float)Math.sqrt(b.vx*b.vx+b.vy*b.vy+b.vz*b.vz);
            if(Math.abs(contact.ny)<.45f&&Math.abs(actor.vx)+Math.abs(actor.vz)>.25f){actor.collisionTime=.18f;actor.collisionX=contact.nx;actor.collisionZ=contact.nz;}
            if(contact.ny< -.55f){actor.grounded=true;p.vy=Math.max(0,p.vy);}
            ContactSolver.solve(p,b,contact);
            if(iteration==0&&b.dynamic&&speed>5.5f&&b.mass>.45f&&b.impactCooldown<=0){Impact h=new Impact();h.actor=actor;h.damage=Math.min(58,(speed-4.5f)*b.mass*.60f);h.fromX=b.x;h.fromZ=b.z;h.impulseX=-contact.nx*speed*.4f;h.impulseZ=-contact.nz*speed*.4f;impacts.add(h);b.impactCooldown=.6f;}
        }
        actor.x=p.x;actor.z=p.z;actor.y=Math.max(terrainHeight(p.x,p.z),p.y-p.hy);actor.vy=p.vy;
        if(actor.grounded&&actor.vy<0)actor.vy=0;
    }
    private void separateActors(GameWorld.Actor player,ArrayList<GameWorld.Actor> enemies){
        for(int i=-1;i<enemies.size();i++){GameWorld.Actor a=i<0?player:enemies.get(i);if(a.dead)continue;for(int j=i+1;j<enemies.size();j++){GameWorld.Actor b=enemies.get(j);if(b.dead||a.y+a.height<b.y||b.y+b.height<a.y)continue;float dx=b.x-a.x,dz=b.z-a.z,d=(float)Math.sqrt(dx*dx+dz*dz),r=a.radius+b.radius;if(d>=r||d<.001f)continue;float move=(r-d)*.45f;a.x-=dx/d*move;a.z-=dz/d*move;b.x+=dx/d*move;b.z+=dz/d*move;}}
    }
    public float supportHeight(float x,float z,float ceiling){float y=terrainHeight(x,z);for(Body b:bodies)if(b.solid&&b!=held&&Math.abs(x-b.x)<b.ex&&Math.abs(z-b.z)<b.ez&&b.y+b.ey<=ceiling)y=Math.max(y,b.y+b.ey);return y;}
    public boolean neutralBlocked(GameWorld.Neutral n,float ground){Body p=actorProxy;p.x=n.x;p.z=n.z;p.hx=p.hz=.23f*n.scale;p.hy=(n.type==GameWorld.CAMEL?.8f:n.type==GameWorld.HORSE?.65f:.23f)*n.scale;p.y=ground+p.hy+.09f;p.shape=SHAPE_BOX;p.rx=p.ry=p.rz=0;CollisionMath.refresh(p);for(Body b:bodies)if(b.solid&&b!=held&&Math.abs(b.x-p.x)<b.ex+p.hx&&Math.abs(b.z-p.z)<b.ez+p.hz&&CollisionMath.contact(p,b,contact)&&contact.depth>.06f)return true;return false;}
    public boolean actorClear(GameWorld.Actor actor,float x,float y,float z){Body p=actorProxy;p.x=x;p.y=y+actor.height*.5f;p.z=z;p.hx=p.hz=actor.radius*.8f;p.hy=actor.height*.5f;p.shape=SHAPE_BOX;p.rx=p.ry=p.rz=0;CollisionMath.refresh(p);for(Body b:bodies)if(b.solid&&b!=held&&CollisionMath.contact(p,b,contact)&&contact.depth>.08f)return false;return true;}
    public void recoverForm(GameWorld.Actor actor){if(actorClear(actor,actor.x,actor.y,actor.z))return;for(float radius=.6f;radius<7;radius+=.6f)for(int n=0;n<12;n++){float angle=n*.5235988f,x=actor.x+(float)Math.sin(angle)*radius,z=actor.z+(float)Math.cos(angle)*radius,y=supportHeight(x,z,actor.y+.4f);if(actorClear(actor,x,y,z)){actor.x=x;actor.y=y;actor.z=z;actor.vy=0;return;}}}
    public float raycast(float ax,float ay,float az,float bx,float by,float bz,float radius,Body ignored,boolean astral){
        float t=Float.POSITIVE_INFINITY;
        for(int i=0;i<bodies.size();i++){Body b=bodies.get(i);if(!b.solid||b==ignored||b==held||astral&&b.astral)continue;t=Math.min(t,CollisionMath.segmentBox(ax,ay,az,bx,by,bz,b,radius));}
        int steps=Math.max(2,(int)(Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay)+(bz-az)*(bz-az))*8));
        for(int i=1;i<=steps;i++){float f=(float)i/steps;if(f>=t)break;float x=ax+(bx-ax)*f,y=ay+(by-ay)*f,z=az+(bz-az)*f;if(y-radius<terrainHeight(x,z)){t=f;break;}}
        return t;
    }
    public void placeHeld(GameWorld.Actor actor,boolean snap){
        if(held==null)return;Body b=held;float x=actor.x+(float)Math.sin(actor.yaw)*1.8f,z=actor.z+(float)Math.cos(actor.yaw)*1.8f,top=Math.max(terrainHeight(x,z),WaterField.depth(environment,x,z)>.1f?WaterField.surface(environment,x,z,time):terrainHeight(x,z));Body base=null;
        for(Body other:bodies)if(other!=b&&other.solid&&Math.abs(other.x-x)<other.ex&&Math.abs(other.z-z)<other.ez&&other.y+other.ey>top&&other.y+other.ey<actor.y+actor.height+1.5f){top=other.y+other.ey;base=other;}
        b.x=x;b.z=z;b.y=top+b.ey+.008f;b.vx=b.vy=b.vz=b.avx=b.avy=b.avz=0;b.sleeping=false;held=null;
        if(snap&&base!=null&&b.catalogId>=100&&b.catalogId<140&&base.catalogId>=100&&base.catalogId<140){b.x=base.x;b.z=base.z;b.ry=base.ry;Joint j=new Joint();j.a=base;j.b=b;j.locked=true;j.anchored=true;j.stiffness=.9f;j.ay=base.hy;j.by=-b.hy;j.restLength=base.hy+b.hy;j.minX=j.maxX=j.minY=j.maxY=j.minZ=j.maxZ=0;j.breakDistance=8;b.parent=base;joints.add(j);}
        CollisionMath.refresh(b);
    }
    public void rotateHeld(){if(held!=null){if(held.catalogId>=72&&held.catalogId<100)held.rx=held.rx==0?90:0;else held.ry+=90;held.avx=held.avy=held.avz=0;CollisionMath.refresh(held);}}
    public void freezeNear(float x,float z,float radius,float seconds){for(Body b:bodies)if(b.dynamic&&(b.x-x)*(b.x-x)+(b.z-z)*(b.z-z)<radius*radius)b.stasis=Math.max(b.stasis,seconds);}

    private void updatePuzzle(float dt){
        if(!puzzleRequired)return;
        if(!puzzleSolved){for(int i=0;i<bodies.size();i++){Body b=bodies.get(i);if(!b.dynamic||b.mass<5f||b.kind==RAGDOLL||b.kind==BODY_PART)continue;float dx=b.x-plate.x,dz=b.z-plate.z;if(dx*dx+dz*dz<1.35f*1.35f&&b.y-verticalRadius(b)<plate.y+.72f){puzzleSolved=true;b.sleeping=true;break;}}}
        if(puzzleSolved&&gate!=null){gateOpen=Math.min(1f,gateOpen+dt*.68f);gate.y=terrainHeight(gate.x,gate.z)+1.55f+gateOpen*3.7f;if(gateOpen>.78f)gate.solid=false;}
    }

    public void impulseSphere(float x,float y,float z,float radius,float ix,float iy,float iz,boolean includeRagdolls){
        float rr=radius*radius;for(int i=0;i<bodies.size();i++){Body b=bodies.get(i);if(!b.dynamic||(!includeRagdolls&&(b.kind==RAGDOLL||b.kind==BODY_PART)))continue;float dx=b.x-x,dy=b.y-y,dz=b.z-z,d2=dx*dx+dy*dy+dz*dz;if(d2>rr)continue;float factor=1f-(float)Math.sqrt(d2)/radius;b.vx+=ix*factor*b.invMass*2f;b.vy+=iy*factor*b.invMass*2f;b.vz+=iz*factor*b.invMass*2f;b.avx+=(random.nextFloat()-.5f)*8f*factor;b.avy+=(random.nextFloat()-.5f)*8f*factor;b.avz+=(random.nextFloat()-.5f)*8f*factor;b.sleeping=false;}
    }

    public void spawnWeapon(GameWorld.Actor actor){
        if(!carriesWeapon(actor))return;float hx=actor.player?.13f:.075f,hy=actor.player?1.42f:.62f,hz=actor.player?.22f:.11f,mass=actor.player?15f:2.2f;
        if(actor.type==GameWorld.CROSSBOW){hx=.42f;hy=.16f;hz=.48f;mass=3.1f;}else if(actor.type==GameWorld.EXECUTIONER){hx=.22f;hy=.92f;hz=.28f;mass=6.5f;}else if(actor.type==GameWorld.GUARD){hx=.065f;hy=1.05f;hz=.065f;mass=3.5f;}
        Body b=make(WEAPON,true,actor.x,actor.y+Math.max(.55f,actor.height*.52f),actor.z,hx,hy,hz,mass);b.shape=SHAPE_BOX;b.weaponType=actor.type;b.grabbable=true;b.metalness=.9f;b.roughness=.22f;b.r=.22f;b.g=.22f;b.b=.23f;b.vx=actor.vx+(random.nextFloat()-.5f)*3f;b.vy=3f+random.nextFloat()*2f;b.vz=actor.vz+(random.nextFloat()-.5f)*3f;b.avx=5f;b.avy=7f;b.avz=4f;b.life=25f;
    }

    public static boolean carriesWeapon(GameWorld.Actor actor){if(actor.player)return actor.type==-1;int t=actor.type;return t==GameWorld.BANDIT||t==GameWorld.CAPTAIN||t==GameWorld.GUARD||t==GameWorld.CROSSBOW||t==GameWorld.SKELETON||t==GameWorld.BONE_KNIGHT||t==GameWorld.ZEALOT||t==GameWorld.EXECUTIONER||t==GameWorld.JAILER;}

    public void spawnRagdoll(GameWorld.Actor actor){
        if(actor.ragdollSpawned)return;actor.ragdollSpawned=true;CharacterSkeleton.Pose pose=new CharacterSkeleton.Pose();CharacterSkeleton.sample(actor,time,pose);Body[] parts=new Body[CharacterSkeleton.BONE_COUNT];
        for(int bone=0;bone<CharacterSkeleton.BONE_COUNT;bone++){if(!CharacterSkeleton.visible(actor,bone))continue;parts[bone]=makeBone(actor,pose,bone,RAGDOLL,22f);}
        connectHierarchy(parts);trimOldBodies();
    }

    public boolean detachLimb(GameWorld.Actor actor,int group,float impulseX,float impulseZ){
        if(group==0||(actor.severedMask&group)!=0)return false;actor.severedMask|=group;CharacterSkeleton.Pose pose=new CharacterSkeleton.Pose();CharacterSkeleton.sample(actor,time,pose);Body[] parts=new Body[CharacterSkeleton.BONE_COUNT];
        for(int bone=0;bone<CharacterSkeleton.BONE_COUNT;bone++)if(CharacterSkeleton.GROUP[bone]==group){Body b=makeBone(actor,pose,bone,BODY_PART,18f);b.vx+=impulseX;b.vy+=3.4f+random.nextFloat()*2.5f;b.vz+=impulseZ;b.restitution=bone==CharacterSkeleton.HEAD?.52f:.24f;b.grabbable=true;parts[bone]=b;}
        connectHierarchy(parts);return true;
    }

    private Body makeBone(GameWorld.Actor actor,CharacterSkeleton.Pose pose,int bone,int kind,float life){
        int a=CharacterSkeleton.START[bone],e=CharacterSkeleton.END[bone];float x=(pose.x[a]+pose.x[e])*.5f,y=(pose.y[a]+pose.y[e])*.5f,z=(pose.z[a]+pose.z[e])*.5f;float dx=pose.x[e]-pose.x[a],dy=pose.y[e]-pose.y[a],dz=pose.z[e]-pose.z[a],len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz),rad=CharacterSkeleton.radiusFor(actor,bone);
        if(CharacterSkeleton.SHAPE[bone]==CharacterSkeleton.SHAPE_SPHERE){x=pose.x[e];y=pose.y[e];z=pose.z[e];len=rad*2f;}
        Body b=make(kind,true,x,y,z,rad,Math.max(rad,len*.5f),rad,bodyMass(bone,actor));b.owner=actor;b.bone=bone;b.group=CharacterSkeleton.GROUP[bone];b.shape=CharacterSkeleton.SHAPE[bone]==CharacterSkeleton.SHAPE_SPHERE?SHAPE_SPHERE:CharacterSkeleton.SHAPE[bone]==CharacterSkeleton.SHAPE_CAPSULE?SHAPE_CAPSULE:SHAPE_BOX;b.life=life;b.r=actor.colorR;b.g=actor.colorG;b.b=actor.colorB;b.metalness=actor.metalness;b.roughness=actor.roughness;b.vx=actor.vx+(random.nextFloat()-.5f)*2.4f;b.vy=1.4f+random.nextFloat()*1.8f;b.vz=actor.vz+(random.nextFloat()-.5f)*2.4f;b.rx=(float)Math.atan2(dz,Math.max(.01f,dy))*57.2958f;b.ry=actor.yaw*57.2958f;b.rz=(float)Math.atan2(dx,Math.max(.01f,dy))*-57.2958f;b.avx=(random.nextFloat()-.5f)*7f;b.avy=(random.nextFloat()-.5f)*7f;b.avz=(random.nextFloat()-.5f)*7f;if(bone==CharacterSkeleton.HEAD){b.r=actor.player?.34f:Math.min(.65f,actor.colorR*1.5f);b.g=actor.player?.25f:Math.min(.58f,actor.colorG*1.45f);b.b=actor.player?.20f:Math.min(.50f,actor.colorB*1.35f);b.restitution=.48f;b.friction=.62f;}float[] frame=new float[16];SkinGeometry.frame(pose,bone,actor.yaw,frame);b.rx=(float)Math.asin(Math.max(-1,Math.min(1,-frame[9])))*57.2958f;b.ry=(float)Math.atan2(frame[8],frame[10])*57.2958f;b.rz=(float)Math.atan2(frame[1],frame[5])*57.2958f;b.skinOffset=(b.x-pose.x[a])*frame[4]+(b.y-pose.y[a])*frame[5]+(b.z-pose.z[a])*frame[6];CollisionMath.refresh(b);return b;
    }

    private void connectHierarchy(Body[] parts){for(int bone=0;bone<parts.length;bone++){Body child=parts[bone];if(child==null)continue;int parentIndex=CharacterSkeleton.PARENT[bone];if(parentIndex<0||parts[parentIndex]==null)continue;Body parent=parts[parentIndex];child.parent=parent;Joint j=new Joint();j.a=parent;j.b=child;float dx=child.x-parent.x,dy=child.y-parent.y,dz=child.z-parent.z;j.restLength=Math.max(.08f,(float)Math.sqrt(dx*dx+dy*dy+dz*dz));j.breakDistance=4f;j.anchored=true;j.restX=child.rx-parent.rx;j.restY=child.ry-parent.ry;j.restZ=child.rz-parent.rz;
        float px=(child.x+parent.x)*.5f,py=(child.y+parent.y)*.5f,pz=(child.z+parent.z)*.5f;CollisionMath.refresh(parent);CollisionMath.refresh(child);
        float dxp=px-parent.x,dyp=py-parent.y,dzp=pz-parent.z,dxc=px-child.x,dyc=py-child.y,dzc=pz-child.z;
        j.ax=dxp*parent.axis[0]+dyp*parent.axis[1]+dzp*parent.axis[2];j.ay=dxp*parent.axis[3]+dyp*parent.axis[4]+dzp*parent.axis[5];j.az=dxp*parent.axis[6]+dyp*parent.axis[7]+dzp*parent.axis[8];
        j.bx=dxc*child.axis[0]+dyc*child.axis[1]+dzc*child.axis[2];j.by=dxc*child.axis[3]+dyc*child.axis[4]+dzc*child.axis[5];j.bz=dxc*child.axis[6]+dyc*child.axis[7]+dzc*child.axis[8];
        if(bone==CharacterSkeleton.L_SHIN||bone==CharacterSkeleton.R_SHIN||bone==CharacterSkeleton.L_FOREARM||bone==CharacterSkeleton.R_FOREARM){j.minX=-5;j.maxX=120;j.minY=-8;j.maxY=8;j.minZ=-8;j.maxZ=8;}joints.add(j);}}
    private float bodyMass(int bone,GameWorld.Actor actor){float scale=actor.boss?2.5f:1f;if(bone==CharacterSkeleton.PELVIS||bone==CharacterSkeleton.LOWER_TORSO||bone==CharacterSkeleton.UPPER_TORSO)return 3f*scale;if(bone==CharacterSkeleton.HEAD)return 1.1f*scale;return .72f*scale;}

    private void cleanup(){
        for(int i=bodies.size()-1;i>=0;i--){Body b=bodies.get(i);if(b.life<0){if(b==held)held=null;for(int n=joints.size()-1;n>=0;n--){Joint j=joints.get(n);if(j.a==b||j.b==b)joints.remove(n);}bodies.remove(i);}}
        for(int i=joints.size()-1;i>=0;i--)if(joints.get(i).broken)joints.remove(i);
    }
    private void trimOldBodies(){int excess=-150;for(int i=0;i<bodies.size();i++){int k=bodies.get(i).kind;if(k==RAGDOLL||k==BODY_PART||k==WEAPON)excess++;}if(excess<=0)return;for(int i=0;i<bodies.size()&&excess>0;i++){Body b=bodies.get(i);if(b.kind==RAGDOLL||b.kind==BODY_PART||b.kind==WEAPON){b.life=.01f;excess--;}}}
    private void publishCounts(){activeBodies=bodies.size();ragdollBodies=0;for(int i=0;i<bodies.size();i++)if(bodies.get(i).kind==RAGDOLL||bodies.get(i).kind==BODY_PART)ragdollBodies++;}
    /** Continuous deterministic terrain shared by rendering, actors and rigid bodies. */
    public float terrainHeight(float x,float z){return WorldLayout.height(environment,x,z);}
    private float groundHeight(float x,float z){return terrainHeight(x,z);}
    private float verticalRadius(Body b){return b.ey;}
    private static float clamp(float v,float lo,float hi){return v<lo?lo:v>hi?hi:v;}

    /** Keeps a deterministic 3x3 ring of fully physical landscape chunks around the player. */
    public void streamAround(float playerX,float playerZ){
        if(generatedChunks==null){generatedChunks=new HashSet<Long>();generatedChunks.addAll(loadedChunks);generatedChunks.addAll(chunkArchive.keySet());}
        int cx=floorChunk(playerX),cz=floorChunk(playerZ);if(cx==streamChunkX&&cz==streamChunkZ)return;streamChunkX=cx;streamChunkZ=cz;
        loadedChunks.clear();
        for(int i=bodies.size()-1;i>=0;i--){Body b=bodies.get(i);if(!b.streamed)continue;if(b.dynamic){b.chunkX=floorChunk(b.x);b.chunkZ=floorChunk(b.z);}if((Math.abs(b.chunkX-cx)>1||Math.abs(b.chunkZ-cz)>1)&&b!=held){long key=chunkKey(b.chunkX,b.chunkZ);ArrayList<Body> archived=chunkArchive.get(key);if(archived==null){archived=new ArrayList<Body>();chunkArchive.put(key,archived);}archived.add(b);bodies.remove(i);}else loadedChunks.add(chunkKey(b.chunkX,b.chunkZ));}
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){int x=cx+dx,z=cz+dz;long key=chunkKey(x,z);ArrayList<Body> archived=chunkArchive.remove(key);if(archived!=null)bodies.addAll(archived);if(generatedChunks.add(key))generateChunk(x,z,playerX,playerZ);loadedChunks.add(key);}
        publishCounts();
    }

    private void generateChunk(int cx,int cz,float playerX,float playerZ){
        int first=bodies.size();RegionLayout.populateChunk(this,environment,cx,cz);for(int i=first;i<bodies.size();i++)markStreamed(bodies.get(i),cx,cz);
        long seed=0x574f524c44L+environment*0x9e3779b9L+cx*73856093L+cz*19349663L;Random rng=new Random(seed);
        boolean outskirts=Math.max(Math.abs(cx*24),Math.abs(cz*24))>145,forest=outskirts?RegionLayout.biome(cx*24,cz*24)==0:environment==0||environment==3;int count=forest?22:environment==8?8:7;
        for(int n=0;n<count;n++){
            float x=(cx+rng.nextFloat())*CHUNK_SIZE,z=(cz+rng.nextFloat())*CHUNK_SIZE;
            if(Math.abs(x)>GameWorld.WORLD_HALF-2.5f||Math.abs(z)>GameWorld.WORLD_HALF-2.5f)continue;
            if(WaterField.depth(environment,x,z)>.06f||RegionLayout.reserved(x,z))continue;
            if((Math.abs(z-24)<5||Math.abs(z-48)<5)&&Math.abs(x)<13||Math.abs(x-WorldLayout.road(z))<5.5f||Math.abs(x-7)<4&&Math.abs(z-48)<5||Math.abs(x-8)<4&&Math.abs(z-24)<5)continue;
            float roll=rng.nextFloat();Body b;
            if(outskirts&&RegionLayout.biome(x,z)==1){if(roll<.2f&&RegionLayout.nearest(x,z,48)>=0){b=make(PALM,false,x,terrainHeight(x,z)+2.8f,z,.25f,2.8f,.25f,100);}else{float rr=.3f+rng.nextFloat()*.55f;b=make(ROCK,false,x,terrainHeight(x,z)+rr*.5f,z,rr,rr*.5f,rr,25);b.shape=SHAPE_SPHERE;b.r=.36f;b.g=.26f;b.b=.14f;}}else if(forest&&roll<.68f){
                float trunk=.32f+rng.nextFloat()*.25f,h=2.35f+rng.nextFloat()*2.15f;
                b=make(TREE,false,x,terrainHeight(x,z)+h,z,trunk,h,trunk,140f);b.variant=environment==3&&rng.nextFloat()<.52f?3:rng.nextInt(3);b.r=.075f;b.g=.055f;b.b=.038f;b.roughness=.97f;
            }else if((forest&&roll<.84f)||environment==8&&roll<.42f){
                float radius=.45f+rng.nextFloat()*.38f;b=make(SHRUB,false,x,terrainHeight(x,z)+radius*.48f,z,radius,radius*.48f,radius,18f);b.solid=false;b.variant=environment==8?4:rng.nextInt(3);b.r=environment==8?.10f:.055f;b.g=environment==8?.025f:.12f;b.b=environment==8?.16f:.045f;
            }else if((forest&&roll<.93f)||environment==5&&roll<.36f||environment==8&&roll<.63f){
                float radius=.16f+rng.nextFloat()*.16f;b=make(MUSHROOM,false,x,terrainHeight(x,z)+radius,z,radius,radius,radius,1f);b.solid=false;b.variant=environment==8?4:rng.nextInt(3);b.r=environment==8?.34f:.42f;b.g=environment==8?.08f:.12f;b.b=environment==8?.48f:.09f;
            }else if(environment==1||environment==4){
                if(roll<.58f){float h=1.1f+rng.nextFloat()*1.8f;b=make(RUIN,false,x,terrainHeight(x,z)+h,z,1.1f+rng.nextFloat()*1.2f,h,.75f+rng.nextFloat()*.8f,180f);b.variant=rng.nextInt(3);b.r=.15f;b.g=.13f;b.b=.115f;}
                else {b=make(rng.nextBoolean()?CRATE:BARREL,true,x,terrainHeight(x,z)+.62f,z,.46f,.60f,.46f,5f);b.grabbable=true;b.shape=b.kind==BARREL?SHAPE_CAPSULE:SHAPE_BOX;b.r=.18f;b.g=.12f;b.b=.075f;}
            }else if(environment==2||environment==6||environment==7){
                if(roll<.52f){float h=1.2f+rng.nextFloat()*2.6f;b=make(RUIN,false,x,terrainHeight(x,z)+h,z,.65f+rng.nextFloat()*.85f,h,.65f+rng.nextFloat()*.85f,220f);b.variant=2;b.r=.12f;b.g=.115f;b.b=.11f;b.metalness=.12f;}
                else {float radius=.38f+rng.nextFloat()*.55f;b=make(ROCK,true,x,terrainHeight(x,z)+radius,z,radius,radius*.72f,radius,4f+radius*5f);b.shape=SHAPE_SPHERE;b.grabbable=radius<.65f;b.r=.18f;b.g=.17f;b.b=.16f;}
            }else if(environment==5){
                b=make(roll<.72f?RUIN:BARREL,roll>=.72f,x,terrainHeight(x,z)+.65f,z,roll<.72f?1.1f:.44f,.65f,roll<.72f?.35f:.44f,roll<.72f?140f:5f);b.shape=b.kind==BARREL?SHAPE_CAPSULE:SHAPE_BOX;b.grabbable=b.dynamic;b.r=.10f;b.g=.105f;b.b=.10f;b.metalness=.45f;
            }else{
                float radius=.45f+rng.nextFloat()*.62f;b=make(roll<.68f?FLESH:STATUE,false,x,terrainHeight(x,z)+radius,z,radius,radius*(roll<.68f?.68f:2.1f),radius,130f);b.variant=4;b.r=.15f;b.g=.025f;b.b=.17f;b.metalness=.15f;
            }
            markStreamed(b,cx,cz);
        }
        if(forest&&rng.nextFloat()<.72f){float x=(cx+.15f+rng.nextFloat()*.7f)*CHUNK_SIZE,z=(cz+.15f+rng.nextFloat()*.7f)*CHUNK_SIZE;if(WaterField.kind(environment,x,z)==WaterField.DRY&&!RegionLayout.reserved(x,z)&&Math.abs(x)<GameWorld.WORLD_HALF-2&&Math.abs(z)<GameWorld.WORLD_HALF-2){Body spring=make(SPRING,false,x,terrainHeight(x,z)+.035f,z,1.1f,.035f,1.1f,30f);spring.solid=false;spring.variant=environment==3?3:0;spring.r=.08f;spring.g=.23f;spring.b=.28f;markStreamed(spring,cx,cz);}}
    }

    private void markStreamed(Body b,int cx,int cz){b.streamed=true;b.chunkX=cx;b.chunkZ=cz;}
    private static int floorChunk(float value){return (int)Math.floor(value/CHUNK_SIZE);}
    private static long chunkKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}

    public void addChain(float x,float y,float z,int links){Body previous=make(CHAIN,false,x,y,z,.10f,.10f,.10f,100);previous.solid=false;previous.r=.21f;previous.g=.20f;previous.b=.18f;previous.metalness=.85f;for(int n=1;n<=links;n++){Body link=make(CHAIN,true,x,y-n*.29f,z,.075f,.16f,.075f,.32f);link.shape=SHAPE_CAPSULE;link.r=.20f;link.g=.19f;link.b=.18f;link.metalness=.86f;link.roughness=.25f;link.parent=previous;if(n==links)link.grabbable=true;Joint j=new Joint();j.a=previous;j.b=link;j.restLength=.29f;j.stiffness=.84f;j.breakDistance=2.7f;joints.add(j);previous=link;}}
}
