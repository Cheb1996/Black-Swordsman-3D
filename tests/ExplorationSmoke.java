package com.danil.blackswordsman;
public final class ExplorationSmoke {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){GameInput in=new GameInput();GameWorld w=V6RegressionSmoke.start(in,new V6RegressionSmoke.Store());w.player.invulnerable=100000;
  w.collectPickup(PlayerEffects.HOUND);w.player.x=30;w.player.z=16;w.exploration.update(w,.02f);check(w.exploration.tracked&&w.effects.powerTime>0,"hound reveals stash and reward");
  w.collectPickup(PlayerEffects.SERPENT);w.player.x=24;w.player.z=31;w.exploration.update(w,.02f);check(w.exploration.passed&&w.effects.armorTime>0,"serpent passage reward");
  w.collectPickup(PlayerEffects.WRAITH);w.player.x=24;w.player.z=40;check(w.exploration.interact(w)&&!w.exploration.barrier.solid,"spirit activates seal");
  w.collectPickup(PlayerEffects.BEAST);w.player.x=24;w.player.z=14.5f;w.player.y=w.physics.terrainHeight(24,14.5f);check(w.physics.interact(w.player)==1&&w.physics.getHeld()==w.exploration.heavy,"beast grabs 85 kg construction weight");w.physics.placeHeld(w.player,false);
  PhysicsWorld p=new PhysicsWorld();p.resetForChapter(0,0);p.bodies.clear();GameWorld.Actor a=w.player;a.x=8;a.z=8;a.y=p.terrainHeight(8,8);a.type=-1;
  PhysicsWorld.Body base=p.createToy(SurrealCatalog.ITEMS[108],0,6),brick=p.createToy(SurrealCatalog.ITEMS[109],0,3);a.x=0;a.z=4.2f;a.y=p.terrainHeight(0,4.2f);a.yaw=0;check(p.interact(a)==1&&p.getHeld()==brick,"grab construction brick");p.placeHeld(a,true);check(p.joints.size()>0&&brick.parent==base,"LEGO connection locks bodies");
  p.freezeNear(base.x,base.z,3,1);float y=base.y;for(int i=0;i<30;i++)p.update(1f/120,a,new java.util.ArrayList<GameWorld.Actor>());check(Math.abs(base.y-y)<.001f,"moth stasis freezes construction");
  p.bodies.clear();p.joints.clear();PhysicsWorld.Body last=null;for(int i=0;i<7;i++){PhysicsWorld.Body b=p.createToy(SurrealCatalog.ITEMS[72+i],0,i*.45f);b.hx=.35f;b.hy=.55f;b.hz=.09f;b.y=p.terrainHeight(b.x,b.z)+.55f;b.rx=0;b.friction=.6f;b.restitution=0;CollisionMath.refresh(b);if(i==0){b.avx=4;b.vz=.6f;}last=b;}a.x=10;a.z=10;a.y=p.terrainHeight(10,10);for(int i=0;i<720;i++)p.update(1f/120,a,new java.util.ArrayList<GameWorld.Actor>());check(Math.abs(last.rx)>40,"domino chain propagates to final piece");
  System.out.println("ExplorationSmoke OK: tracking, serpent, astral switch, heavy lifting, connected bricks, frozen constructions, domino chain");
 }
}
