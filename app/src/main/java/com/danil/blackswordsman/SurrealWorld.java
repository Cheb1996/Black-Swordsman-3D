package com.danil.blackswordsman;

import java.util.ArrayList;
import java.util.Random;

/** A persistent-in-session map of 720 physical props, 120 pickups and 12 anomalies. */
public final class SurrealWorld implements java.io.Serializable {
    private static final long serialVersionUID=6L;
    public static final int PROP_COUNT=720;
    public static final float[][] ZONES={{10,12},{-25,22},{31,-23},{-48,-36},{64,38},{-71,51},{30,83},{-24,-92},{91,-67},{-101,-73},{104,97},{-96,100}};
    public static final String[] ZONE_NAMES={"САД ПОТЕРЯННЫХ ИГРУШЕК","КРУГ ПОХИЩЕНИЯ","СВАЛКА ВРЕМЕНИ","КОЛЬЦА НЕВОЗМОЖНОГО","КУБИЧЕСКИЙ ЛЕС","ГНЕЗДО ГЛАЗ","ПАРАД ПЕРЕВЁРНУТЫХ ВЕЩЕЙ","ПАДЕНИЕ СПУТНИКА","ПОЛЯНА ЧУЖИХ СНОВ","ДОЛИНА МАРМЕЛАДА","ЯРМАРКА ЗАБЫТЫХ ЭПОХ","ПОСЛЕДНЯЯ ДЕТСКАЯ"};
    public static final class Prop implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public int id,catalogId;
        public PhysicsWorld.Body body;
        public boolean loaded;
    }
    public static final class Pickup implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public int id,kind,zone;
        public float x,y,z,cooldown;
    }
    public final ArrayList<Prop> props=new ArrayList<Prop>();
    public final ArrayList<Pickup> pickups=new ArrayList<Pickup>();
    public final boolean[] creatureSpawned=new boolean[12];
    public final ArrayList<PhysicsWorld.Body> ufos=new ArrayList<PhysicsWorld.Body>();
    public final ArrayList<PhysicsWorld.Body> landmarks=new ArrayList<PhysicsWorld.Body>();
    public int loadedProps,discovered;
    private final boolean[] seen=new boolean[SurrealCatalog.COUNT];
    public float time,intensity,portalCooldown;
    private float streamClock;
    public int nearestZone;
    public Pickup nearestPickup;
    public String hint="";

    public void reset(PhysicsWorld physics,int chapter){
        props.clear();pickups.clear();ufos.clear();landmarks.clear();time=0;streamClock=0;portalCooldown=0;discovered=0;loadedProps=0;nearestPickup=null;hint="";intensity=0;
        for(int i=0;i<seen.length;i++)seen[i]=false;
        for(int i=0;i<creatureSpawned.length;i++)creatureSpawned[i]=false;
        Random rng=new Random(0x545249504cL+chapter*3719L);
        for(int i=0;i<PROP_COUNT;i++){
            int zone=i/60,local=i%60,catalog=(zone*20+local%20+(local/20)*80)%SurrealCatalog.COUNT;
            float a=local*2.399963f+.13f*chapter,rad=3f+(float)Math.sqrt(local+1)*.88f;
            float x=ZONES[zone][0]+(float)Math.sin(a)*rad,z=ZONES[zone][1]+(float)Math.cos(a)*rad;
            // Kept outside the exact start position and mechanism positions.
            Prop p=new Prop();p.id=i;p.catalogId=catalog;p.body=physics.createToy(SurrealCatalog.ITEMS[catalog],x,z);
            p.body.ry=rng.nextFloat()*360;p.body.sleeping=true;p.body.surrealId=i;p.loaded=false;
            physics.bodies.remove(p.body);props.add(p);
        }
        for(int zone=0;zone<ZONES.length;zone++){
            for(int k=0;k<10;k++){
                Pickup p=new Pickup();p.id=zone*10+k;p.kind=k;p.zone=zone;
                float a=k*.6283185f;p.x=ZONES[zone][0]+(float)Math.sin(a)*2.4f;p.z=ZONES[zone][1]+(float)Math.cos(a)*2.4f;
                p.y=physics.terrainHeight(p.x,p.z)+.42f;pickups.add(p);
            }
            if(zone%3==1){PhysicsWorld.Body b=physics.createUfo(ZONES[zone][0],ZONES[zone][1]);b.variant=zone;ufos.add(b);}
            for(int n=0;n<3;n++){float a=n*2.0944f;int id=zone%3==0?5+(n%3)*8:zone%3==1?200+(zone+n)%40:140+(zone+n)%60;PhysicsWorld.Body b=physics.createToy(SurrealCatalog.ITEMS[id],ZONES[zone][0]+(float)Math.sin(a)*5.5f,ZONES[zone][1]+(float)Math.cos(a)*5.5f);b.dynamic=false;b.invMass=0;b.grabbable=false;b.hx*=3;b.hy*=3;b.hz*=3;b.radius=Math.max(b.hx,b.hz);b.y=physics.terrainHeight(b.x,b.z)+5+n*.6f;landmarks.add(b);}
        }
        // A welcoming, explicit set of useful pickups close to the starting path.
        pickups.get(0).x=-1.6f;pickups.get(0).z=6.5f;pickups.get(1).x=2.1f;pickups.get(1).z=6.8f;
        for(int i=0;i<2;i++)pickups.get(i).y=physics.terrainHeight(pickups.get(i).x,pickups.get(i).z)+.42f;
        stream(physics,0,5);
    }

    private void stream(PhysicsWorld physics,float x,float z){
        loadedProps=0;
        for(int i=0;i<props.size();i++){
            Prop p=props.get(i);PhysicsWorld.Body b=p.body;float dx=x-b.x,dz=z-b.z,d2=dx*dx+dz*dz;
            boolean wanted=d2<(p.loaded?43f*43f:37f*37f)||b==physics.getHeld();
            if(wanted&&!p.loaded){physics.bodies.add(b);p.loaded=true;}
            else if(!wanted&&p.loaded){physics.bodies.remove(b);p.loaded=false;}
            if(p.loaded)loadedProps++;
            if(d2<5.5f*5.5f&&!seen[p.catalogId]){seen[p.catalogId]=true;discovered++;}
        }
    }

    public void update(GameWorld world,float dt){
        time+=dt;portalCooldown=Math.max(0,portalCooldown-dt);streamClock-=dt;
        if(streamClock<=0){streamClock=.20f;stream(world.physics,world.player.x,world.player.z);}
        float best=Float.MAX_VALUE;nearestZone=0;
        for(int i=0;i<ZONES.length;i++){
            float dx=world.player.x-ZONES[i][0],dz=world.player.z-ZONES[i][1],d2=dx*dx+dz*dz;
            if(d2<best){best=d2;nearestZone=i;}
            if(d2<34f*34f&&!creatureSpawned[i]){world.spawnOddity(i%6,ZONES[i][0]+3.7f,ZONES[i][1]-3.2f);creatureSpawned[i]=true;}
        }
        intensity+=(Math.max(0,1f-(float)Math.sqrt(best)/23f)*.8f-intensity)*Math.min(1,dt*2f);
        for(int i=0;i<landmarks.size();i++){PhysicsWorld.Body b=landmarks.get(i);b.y=world.physics.terrainHeight(b.x,b.z)+5+(i%3)*.6f+(float)Math.sin(time*.65f+i)*.25f;b.ry=time*9+i*47;b.rx=(float)Math.sin(time*.35f+i)*16;}
        for(int i=0;i<ufos.size();i++){
            PhysicsWorld.Body u=ufos.get(i);u.y=world.physics.terrainHeight(u.x,u.z)+8f+(float)Math.sin(time*.72f+i)*.7f;u.ry=time*13f+i*60;
            if(beamActive(i))world.physics.levitate(u.x,u.z,4.5f,world.physics.terrainHeight(u.x,u.z)+4.6f,dt);
        }
        nearestPickup=null;float nearest=2.15f;
        for(int i=0;i<pickups.size();i++){
            Pickup p=pickups.get(i);p.cooldown=Math.max(0,p.cooldown-dt);if(p.cooldown>0)continue;
            float dx=world.player.x-p.x,dz=world.player.z-p.z,d=(float)Math.sqrt(dx*dx+dz*dz);
            if(p.kind==PlayerEffects.MEDKIT&&d<.92f&&world.collectPickup(p.kind)){p.cooldown=75;continue;}
            if(p.kind==PlayerEffects.MEDKIT&&world.player.hp>=world.player.maxHp||p.kind==PlayerEffects.AMMO&&world.hudAmmo>=world.hudMaxAmmo)continue;
            if(d<nearest){nearest=d;nearestPickup=p;}
        }
        hint=nearestPickup==null?"":PlayerEffects.NAMES[nearestPickup.kind]+" · ХВАТ";
        if(nearestPickup==null&&portalCooldown<=0&&portalInReach(world.player))hint="ПЕРЕЙТИ ЧЕРЕЗ КОЛЬЦО · ХВАТ";
    }

    public boolean interact(GameWorld world){
        if(world.physics.getHeld()!=null)return false;
        if(nearestPickup!=null){if(world.collectPickup(nearestPickup.kind)){nearestPickup.cooldown=nearestPickup.kind<6?75:95;nearestPickup=null;hint="";return true;}return false;}
        if(portalCooldown<=0&&portalInReach(world.player)){
            int destination=nearestZone==3?8:3;
            world.teleportTo(ZONES[destination][0],ZONES[destination][1]+3.8f);portalCooldown=4f;streamClock=0;return true;
        }
        return false;
    }
    private boolean portalInReach(GameWorld.Actor p){if(nearestZone!=3&&nearestZone!=8)return false;float dx=p.x-ZONES[nearestZone][0],dz=p.z-ZONES[nearestZone][1];return dx*dx+dz*dz<3.3f*3.3f;}
    public boolean beamActive(int index){return Math.sin(time*.65f+index*1.8f)>.05;}
    public int zoneAt(float x,float z){for(int i=0;i<ZONES.length;i++){float dx=x-ZONES[i][0],dz=z-ZONES[i][1];if(dx*dx+dz*dz<16f*16f)return i;}return -1;}
}
