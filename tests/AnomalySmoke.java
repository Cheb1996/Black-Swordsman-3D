package com.danil.blackswordsman;

/** Runs all six abilities through the actual world update loop. */
public final class AnomalySmoke {
    private static final class Motion implements GameWorld.MotionInput {public float getTiltX(){return 0;}public float getTiltY(){return 0;}public float consumeGyroYaw(){return 0;}public float consumeGyroPitch(){return 0;}public boolean hasGyroscope(){return false;}public boolean hasAccelerometer(){return false;}public void calibrate(){}}
    private static final class Store implements GameWorld.ProgressStore {public GameWorld.Progress load(){return null;}public void save(GameWorld.Progress p){}public void clear(){}}
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    private static GameWorld world(int kind){
        GameInput input=new GameInput();GameWorld w=new GameWorld(input,new Motion(),new Store());w.mode=GameWorld.STORY;w.storyLine=w.currentChapter().intro.length-1;input.pressAction();w.update(.016f);
        w.enemies.clear();w.wildlife.clear();w.physics.bodies.clear();w.surreal.props.clear();w.surreal.ufos.clear();w.surreal.landmarks.clear();w.surreal.pickups.clear();for(int i=0;i<12;i++)w.surreal.creatureSpawned[i]=true;
        w.player.x=0;w.player.z=0;w.spawnOddity(kind,3,0);GameWorld.Actor e=w.enemies.get(0);e.spawnTime=0;e.specialCooldown=0;return w;
    }
    public static void main(String[] args){
        GameWorld w=world(0);GameWorld.Actor victim=new GameWorld.Actor();victim.x=4;victim.z=0;victim.hp=victim.maxHp=100;victim.speed=3;victim.height=2;victim.radius=.4f;w.enemies.add(victim);w.update(.016f);check(victim.stun>1.5f,"clock moth suspends nearby hostile actors");
        w=world(1);GameWorld.Actor hare=w.enemies.get(0);w.update(.016f);check(Math.abs(hare.x-3)>1f||Math.abs(hare.z)>1f,"mirror hare teleports");check(w.projectiles.size()>0,"mirror hare fires after teleport");
        w=world(2);PhysicsWorld.Body toy=w.physics.createToy(SurrealCatalog.ITEMS[0],3.6f,.5f);float y=toy.y;for(int i=0;i<40;i++)w.update(.016f);check(toy.y>y+.3f,"magnetic jelly lifts toys");
        w=world(3);toy=w.physics.createToy(SurrealCatalog.ITEMS[50],2.6f,.5f);float hp=w.player.hp;w.update(.016f);check(toy.vy>0,"dice crab imparts upward impulse");check(w.player.hp<hp,"dice crab shockwave deals damage");
        w=world(4);w.update(.016f);check(w.effects.speedTime>5,"balloon keeper gives speed aura");w.update(.016f);check(w.player.speed>w.getProgressCopy().speed*1.6f,"aura changes gameplay speed");
        w=world(5);int before=w.physics.bodies.size();w.update(.016f);check(w.physics.bodies.size()>before,"golem spawns a physical cube");toy=w.physics.bodies.get(w.physics.bodies.size()-1);check(toy.catalogId>=48&&toy.catalogId<72&&toy.vx<0&&toy.life>10,"golem throws a timed cube toward player");
        System.out.println("AnomalySmoke OK: time stop, teleport, magnetism, shockwave, speed aura, physical projectiles");
    }
}
