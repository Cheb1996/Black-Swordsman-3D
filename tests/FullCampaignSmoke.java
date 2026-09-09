package com.danil.blackswordsman;
/** Complete itinerary, physical mechanisms, every authored wave, exits and upgrade transaction.
 * Bot has invulnerability to isolate progression from balancing; it uses normal movement/attacks. */
public final class FullCampaignSmoke {
 static GameInput input;static GameWorld w;static int frames;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text+" chapter="+w.chapter+" stage="+w.quest.stage+" pos="+w.player.x+","+w.player.y+","+w.player.z+" wave="+w.wave+" enemies="+w.enemiesAlive+" survival="+w.quest.survival+" mode="+w.mode);}
 static void tick(){w.update(1f/30);frames++;}
 static float distance(float x,float z){float dx=w.player.x-x,dz=w.player.z-z;return (float)Math.sqrt(dx*dx+dz*dz);}
 static void move(float x,float z,float stop){int n=0;while(distance(x,z)>stop&&n++<2400&&w.mode==GameWorld.PLAYING){float dx=x-w.player.x,dz=z-w.player.z,l=(float)Math.sqrt(dx*dx+dz*dz),s=(float)Math.sin(w.cameraYaw),c=(float)Math.cos(w.cameraYaw);float direction=(float)Math.atan2(dx,dz);if(w.physics.raycast(w.player.x,w.player.y+.6f,w.player.z,x,w.physics.terrainHeight(x,z)+.6f,z,.2f,null,false)<=1){direction=CombatSystem.steer(w,w.player,NavigationSystem.direction(w,w.player,direction));dx=(float)Math.sin(direction);dz=(float)Math.cos(direction);l=1;}input.moveX=(dx*c-dz*s)/l;input.moveY=-(dx*s+dz*c)/l;if(n%45==0)input.pressJump();tick();}input.clearMovement();for(int j=0;j<12;j++)tick();check(w.mode==GameWorld.STORY&&w.storyOutro||distance(x,z)<stop+1.3f,"route cannot reach "+x+","+z);}
 static GameWorld.Actor nearest(){GameWorld.Actor best=null;float d=1e6f;for(GameWorld.Actor e:w.enemies){if(e.dead||e.peaceful||e.ambient)continue;float dx=e.x-w.player.x,dz=e.z-w.player.z,t=dx*dx+dz*dz;if(t<d){d=t;best=e;}}return best;}
 public static void main(String[] args)throws Exception{
  input=new GameInput();V6RegressionSmoke.Store store=new V6RegressionSmoke.Store();w=V6RegressionSmoke.start(input,store);
  for(int chapter=0;chapter<9;chapter++){
   check(w.chapter==chapter&&w.mode==GameWorld.PLAYING,"chapter starts sequentially");w.player.invulnerable=100000;
   move(w.quest.x(),w.quest.z(),2.7f);check(w.quest.stage==1,"discovery");
   if(chapter==0||chapter==3){move(WorldLayout.road(40),32,1);move(WorldLayout.road(40),47,1);}
   move(WorldLayout.road(w.quest.z()),w.quest.z(),2);move(w.quest.x(),w.quest.z(),2.5f);input.pressInteract();tick();check(w.quest.stage==2,"quest interaction");
   if(w.physics.hasPuzzle()){
    move(-4,63,1);input.pressInteract();tick();check(w.physics.getHeld()!=null,"grab mechanism weight");
    move(4.3f,67.2f,.35f);w.player.yaw=0;input.pressPlace();tick();for(int i=0;i<120;i++)tick();check(w.physics.isPuzzleSolved(),"weight placed on plate through controls");
   }
   move(0,78,3);
   int combatFrames=0;while(!w.quest.combatComplete&&combatFrames++<24000){GameWorld.Actor e=nearest();if(e!=null){float d=distance(e.x,e.z);if(d>2.0f){float angle=(float)Math.atan2(e.x-w.player.x,e.z-w.player.z),steer=CombatSystem.steer(w,w.player,angle);float dx=(float)Math.sin(steer),dz=(float)Math.cos(steer),s=(float)Math.sin(w.cameraYaw),c=(float)Math.cos(w.cameraYaw);input.moveX=dx*c-dz*s;input.moveY=-(dx*s+dz*c);}else input.clearMovement();if(d<3.6f)input.pressLight();if(d>4&&d<15)input.pressCannon();if(w.player.rage>=100)input.pressRage();if(combatFrames%80==0)input.pressJump();}else input.clearMovement();tick();}
   check(w.quest.combatComplete,"all waves finish");input.clearMovement();for(int i=0;i<15;i++)tick();check(w.quest.stage==3,"exit stage");check(!w.getProgressCopy().pendingUpgrade,"reward not granted before exit");move(0,118,3);
   for(int i=0;i<1500&&w.mode==GameWorld.PLAYING;i++){float dx=-w.player.x,dz=118-w.player.z,d=(float)Math.sqrt(dx*dx+dz*dz),s=(float)Math.sin(w.cameraYaw),c=(float)Math.cos(w.cameraYaw);if(d>1){input.moveX=(dx*c-dz*s)/d;input.moveY=-(dx*s+dz*c)/d;}else input.clearMovement();input.pressLight();tick();}input.clearMovement();check(w.mode==GameWorld.STORY&&w.storyOutro,"physical exit unlocks outro");
   byte[] snapshot=SnapshotCodec.encode(w);GameWorld restored=new GameWorld(new GameInput(),new V6RegressionSmoke.Motion(),new V6RegressionSmoke.Store());check(SnapshotCodec.restore(restored,snapshot)&&restored.storyOutro,"outro resume");
   for(int i=0;i<w.currentChapter().outro.length;i++){input.pressAction();tick();}
   if(chapter<8){check(w.mode==GameWorld.UPGRADE&&w.getProgressCopy().pendingUpgrade,"pending reward survives");float attack=w.getProgressCopy().attack;input.chooseUpgrade(0);tick();check(w.chapter==chapter+1&&w.getProgressCopy().attack==attack+7&&!w.getProgressCopy().pendingUpgrade,"exactly one upgrade");for(int i=0;i<w.currentChapter().intro.length;i++){input.pressAction();tick();}}
   else check(w.mode==GameWorld.ENDING&&w.getProgressCopy().completed,"campaign ending");
   System.out.println("FullCampaign chapter "+(chapter+1)+" passed; frames="+frames);
  }
  System.out.println("FullCampaignSmoke OK: walked 9 routes, operated 3 mechanisms, killed all waves, 9 exits, 8 pending upgrades, finale");
 }
}
