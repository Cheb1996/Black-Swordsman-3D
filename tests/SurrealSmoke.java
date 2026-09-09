package com.danil.blackswordsman;

import java.util.HashSet;

/** Regression tests of catalog identity, physics streaming and actual pickup interactions. */
public final class SurrealSmoke {
    private static final class Motion implements GameWorld.MotionInput {public float getTiltX(){return 0;}public float getTiltY(){return -1;}public float consumeGyroYaw(){return 0;}public float consumeGyroPitch(){return 0;}public boolean hasGyroscope(){return true;}public boolean hasAccelerometer(){return true;}public void calibrate(){}}
    private static final class Store implements GameWorld.ProgressStore {GameWorld.Progress saved;public GameWorld.Progress load(){return saved;}public void save(GameWorld.Progress p){saved=p.copy();}public void clear(){saved=null;}}
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    private static void close(float a,float b,String why){check(Math.abs(a-b)<.001f,why+" "+a+" != "+b);}
    private static GameWorld start(GameInput input){GameWorld w=new GameWorld(input,new Motion(),new Store());w.mode=GameWorld.STORY;w.storyLine=w.currentChapter().intro.length-1;input.pressAction();w.update(.016f);return w;}

    public static void main(String[] args){
        HashSet<String> names=new HashSet<String>();HashSet<Integer> geometries=new HashSet<Integer>();long vertices=0;int minParts=999;
        check(SurrealCatalog.ITEMS.length==240,"240 definitions");
        for(int id=0;id<240;id++){
            SurrealCatalog.Item item=SurrealCatalog.ITEMS[id];check(item.id==id,"stable id");check(names.add(item.name),"unique name "+item.name);
            ToyGeometry.Data full=ToyGeometry.build(item,true),low=ToyGeometry.build(item,false);
            check(full.indices.length>24&&full.parts>=3,"detailed multi-part mesh "+id);check(full.vertices.length>low.vertices.length,"real geometry LOD "+id);
            int hash=1;for(int n=0;n<full.vertices.length;n+=9){for(int a=0;a<6;a++){float f=full.vertices[n+a];check(!Float.isNaN(f)&&!Float.isInfinite(f),"finite mesh "+id);if(a<3){check(Math.abs(f)<=1.001f,"body envelope "+id);hash=31*hash+Float.floatToIntBits(f);}}}
            hash=31*hash+Float.floatToIntBits(item.hx);hash=31*hash+Float.floatToIntBits(item.hy);hash=31*hash+Float.floatToIntBits(item.hz);check(geometries.add(hash),"not just a recolor: "+id+" "+item.name);
            for(int n=0;n<full.indices.length;n++)check((full.indices[n]&65535)<full.vertices.length/9,"index range");
            int wrong=0,faces=0;for(int n=0;n<full.indices.length;n+=3){int a=(full.indices[n]&65535)*9,b=(full.indices[n+1]&65535)*9,c=(full.indices[n+2]&65535)*9;float[] v=full.vertices;float ax=v[b]-v[a],ay=v[b+1]-v[a+1],az=v[b+2]-v[a+2],bx=v[c]-v[a],by=v[c+1]-v[a+1],bz=v[c+2]-v[a+2];float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,area=nx*nx+ny*ny+nz*nz;if(area<1e-12)continue;faces++;float dot=nx*(v[a+3]+v[b+3]+v[c+3])+ny*(v[a+4]+v[b+4]+v[c+4])+nz*(v[a+5]+v[b+5]+v[c+5]);if(dot<-.0000001)wrong++;}
            check(wrong==0,"outward surfaces "+id+": "+wrong+"/"+faces);vertices+=full.vertices.length/9;minParts=Math.min(minParts,full.parts);
        }
        GameInput input=new GameInput();GameWorld w=start(input);check(w.surreal.props.size()==720,"720 placed props");check(w.surreal.pickups.size()==192,"192 useful pickups");check(w.surreal.ufos.size()==10,"10 physical UFOs");
        int[] occurrences=new int[240];for(SurrealWorld.Prop p:w.surreal.props)occurrences[p.catalogId]++;for(int n:occurrences)check(n==3,"each geometry is placed three times");
        w.enemies.clear();float x=w.player.x,z=w.player.z;for(int n=0;n<40;n++)w.update(.016f);close(w.player.x,x,"idle x");close(w.player.z,z,"idle z");
        w.player.invulnerable=10000;
        SurrealWorld.Prop saved=w.surreal.props.get(0);float originalX=saved.body.x,originalZ=saved.body.z;saved.body.x=2f;saved.body.z=7f;saved.body.vx=saved.body.vy=saved.body.vz=0;saved.body.sleeping=true;
        w.teleportTo(100,100);w.surreal.update(w,.3f);check(!saved.loaded,"far props unloaded");check(w.surreal.loadedProps<260,"bounded active props");
        w.teleportTo(0,5);w.surreal.update(w,.3f);check(saved.loaded,"props reloaded");close(saved.body.x,2,"moved prop survives streaming");close(saved.body.z,7,"moved prop position");saved.body.x=originalX;saved.body.z=originalZ;
        check(!w.collectPickup(PlayerEffects.MEDKIT),"full health keeps medkit");w.player.hp=100;check(w.collectPickup(0),"heal");close(w.player.hp,180,"heals 80");
        SurrealWorld.Pickup speed=w.surreal.pickups.get(1);w.player.x=speed.x;w.player.z=speed.z;w.surreal.update(w,.016f);close(w.effects.speedTime,0,"walking past boosts does not consume them");
        input.pressInteract();w.update(.016f);check(w.effects.speedTime>24&&speed.cooldown>74,"explicit interact consumes speed candy");float enhanced=w.player.speed;w.collectPickup(PlayerEffects.SPEED);close(w.player.speed,enhanced,"same buff refreshes instead of stacking");
        input.pressPause();w.update(.016f);float remaining=w.effects.speedTime,clock=w.surreal.time;for(int n=0;n<90;n++)w.update(.034f);close(w.effects.speedTime,remaining,"pause freezes buff");close(w.surreal.time,clock,"pause freezes anomalies");input.pressAction();w.update(.016f);
        GameWorld.Progress base=w.getProgressCopy();for(int form=6;form<=9;form++){
            check(w.collectPickup(form),"form pickup");check(w.player.type!=-1&&w.effects.form==form,"changes actor mesh/type");check(!PhysicsWorld.carriesWeapon(w.player),"form has no Guts sword");
            w.player.cannonCooldown=0;w.player.dodgeTime=0;int ammo=w.hudAmmo;input.pressCannon();w.update(.016f);check(w.player.cannonCooldown>0&&w.hudAmmo==ammo,"form spell works without using ammo");
            w.effects.tick(50,w.player);w.effects.apply(w.player,base);check(w.player.type==-1,"form expires back to Guts");close(w.player.speed,base.speed,"base speed restored");close(w.player.damage,base.attack,"base damage restored");
        }
        close(w.getProgressCopy().speed,base.speed,"buff cannot corrupt save speed");close(w.getProgressCopy().attack,base.attack,"buff cannot corrupt save attack");
        w.player.hp=80;w.collectPickup(PlayerEffects.REGEN);w.effects.tick(20,w.player);close(w.player.hp,200,"regen integrates bounded duration");w.collectPickup(PlayerEffects.ARMOR);close(w.effects.damageScale(),.55f,"temporary protection");
        w.player.dead=true;check(!w.collectPickup(6),"no transformation after death");w.player.dead=false;
        w.teleportTo(SurrealWorld.ZONES[3][0],SurrealWorld.ZONES[3][1]);w.surreal.update(w,.3f);w.surreal.nearestPickup=null;check(w.surreal.interact(w),"portal interaction");check(w.player.x==SurrealWorld.ZONES[8][0],"paired portal destination");close(input.moveX,0,"portal clears input");
        PhysicsWorld field=new PhysicsWorld();field.resetForChapter(0,0);field.bodies.clear();w.player.x=10;w.player.z=5;w.player.y=field.terrainHeight(10,5);PhysicsWorld.Body lifted=field.createToy(SurrealCatalog.ITEMS[5],0,5);lifted.sleeping=true;float startY=lifted.y;for(int n=0;n<40;n++){field.levitate(lifted.x,lifted.z,4,lifted.y+2,.016f);field.update(.016f,w.player,new java.util.ArrayList<GameWorld.Actor>());}check(lifted.y>startY+.3f,"UFO/magnet field really lifts bodies");
        System.out.println("SurrealSmoke OK: 240 geometric identities, "+vertices+" vertices, minimum "+minParts+" parts; 720 placements, streaming, pickups, timers, spells, portals, levitation");
    }
}
