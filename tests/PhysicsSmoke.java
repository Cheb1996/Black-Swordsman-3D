package com.danil.blackswordsman;

import java.util.ArrayList;

/** Exercises gravity, grabbing, throwing, puzzle state, ragdolls and rolling heads. */
public final class PhysicsSmoke {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        PhysicsWorld physics=new PhysicsWorld();physics.resetForChapter(2,2);
        check(physics.bodies.size()>40,"populated physical world");check(physics.hasPuzzle()&&!physics.isPuzzleSolved(),"physical puzzle starts locked");
        PhysicsWorld.Body key=null,plate=null,falling=null;
        for(int i=0;i<physics.bodies.size();i++){PhysicsWorld.Body b=physics.bodies.get(i);if(b.kind==PhysicsWorld.CRATE&&b.mass>=9)key=b;if(b.kind==PhysicsWorld.PRESSURE_PLATE)plate=b;if(falling==null&&b.kind==PhysicsWorld.ROCK)falling=b;}
        check(key!=null&&plate!=null,"puzzle bodies");GameWorld.Actor player=actor(true);player.x=key.x;player.z=key.z-1;player.y=physics.terrainHeight(player.x,player.z);
        check(physics.interact(player)==1&&physics.getHeld()==key,"grab");check(physics.interact(player)==2&&physics.getHeld()==null,"throw");
        key.x=plate.x;key.z=plate.z;key.y=plate.y+key.hy;key.vx=key.vy=key.vz=0;physics.update(.016f,player,new ArrayList<GameWorld.Actor>());check(physics.isPuzzleSolved(),"pressure plate solved");
        falling.sleeping=false;falling.y=physics.terrainHeight(falling.x,falling.z)+5;falling.vy=0;float high=falling.y;for(int i=0;i<60;i++)physics.update(.016f,player,new ArrayList<GameWorld.Actor>());check(falling.y<high,"global gravity");
        GameWorld.Actor enemy=actor(false);enemy.x=0;enemy.z=0;enemy.type=GameWorld.BANDIT;int before=physics.bodies.size();physics.spawnWeapon(enemy);physics.spawnRagdoll(enemy);check(enemy.ragdollSpawned,"ragdoll state");check(physics.bodies.size()>=before+17,"weapon plus full ragdoll");check(physics.joints.size()>=10,"articulated constraints");
        GameWorld.Actor other=actor(false);other.x=2;other.z=2;check(physics.detachLimb(other,CharacterSkeleton.GROUP_HEAD,5,2),"decapitation body");PhysicsWorld.Body head=null;for(int i=0;i<physics.bodies.size();i++){PhysicsWorld.Body b=physics.bodies.get(i);if(b.kind==PhysicsWorld.BODY_PART&&b.bone==CharacterSkeleton.HEAD)head=b;}check(head!=null&&head.shape==PhysicsWorld.SHAPE_SPHERE,"head is spherical body");
        head.y=head.radius;head.vx=4;float oldRotation=head.rz;for(int i=0;i<20;i++)physics.update(.016f,player,new ArrayList<GameWorld.Actor>());check(Math.abs(head.rz-oldRotation)>.01f,"head rolls from ground contact");
        System.out.println("PhysicsSmoke OK: rigid bodies, gravity, puzzle, ragdoll, severing, weapon drop");
    }
    private static GameWorld.Actor actor(boolean player){GameWorld.Actor a=new GameWorld.Actor();a.player=player;a.height=2.05f;a.radius=.52f;a.speed=5;a.maxHp=a.hp=100;a.colorR=.25f;a.colorG=.20f;a.colorB=.18f;a.roughness=.7f;a.metalness=.2f;return a;}
}
