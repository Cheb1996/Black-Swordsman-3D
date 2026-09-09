package com.danil.blackswordsman;
/** Animation state comes from resolved movement, not from a requested velocity against a wall. */
public final class AnimationSystem {
 private AnimationSystem(){}
 public static void update(GameWorld.Actor a,float dt){
  if(!a.motionReady){a.motionReady=true;a.motionX=a.x;a.motionY=a.y;a.motionZ=a.z;a.motionYaw=a.yaw;a.previousGrounded=a.grounded;}
  float dx=a.x-a.motionX,dz=a.z-a.motionZ,travel=(float)Math.sqrt(dx*dx+dz*dz),speed=travel/Math.max(.001f,dt);if(travel>2){speed=0;travel=0;}
  float blend=1-SimulationClock.damping(10,dt);a.animationSpeed+=(Math.min(1.5f,speed/Math.max(.2f,a.speed))-a.animationSpeed)*blend;
  if(a.grounded&&!a.dead)a.gaitPhase+=travel*5f/Math.max(.8f,a.height);
  float s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw),forward=(dx*s+dz*c)/Math.max(.001f,dt),side=(dx*c-dz*s)/Math.max(.001f,dt);
  a.leanForward+=(clamp((forward-a.previousForward)/Math.max(.001f,dt)*.009f,-.18f,.18f)-a.leanForward)*blend;
  a.leanSide+=(clamp((side-a.previousSide)/Math.max(.001f,dt)*.009f,-.16f,.16f)-a.leanSide)*blend;
  float turn=angle(a.yaw-a.motionYaw)/Math.max(.001f,dt);a.turnLean+=(clamp(turn*.035f,-.18f,.18f)-a.turnLean)*blend;
  if(a.grounded&&!a.previousGrounded)a.landingTime=.36f;
  a.landingTime=Math.max(0,a.landingTime-dt);a.collisionTime=Math.max(0,a.collisionTime-dt);a.hitReaction=Math.max(0,a.hitReaction-dt*2.4f);
  if(a.attackTime>a.previousAttackTime+.001f)a.attackDuration=Math.max(.1f,a.attackTime+dt);
  a.previousAttackTime=a.attackTime;a.previousGrounded=a.grounded;a.previousForward=forward;a.previousSide=side;
  a.motionX=a.x;a.motionY=a.y;a.motionZ=a.z;a.motionYaw=a.yaw;
 }
 public static float angle(float a){while(a>Math.PI)a-=6.2831853f;while(a< -Math.PI)a+=6.2831853f;return a;}
 private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
 /** Analytic two-link joint; target is clamped to prevent a knee/elbow from stretching. */
 public static void joint(CharacterSkeleton.Pose p,int root,int middle,int tip,float l1,float l2,float poleX,float poleY,float poleZ){float dx=p.x[tip]-p.x[root],dy=p.y[tip]-p.y[root],dz=p.z[tip]-p.z[root],d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(d<.001f)return;float limited=Math.max(Math.abs(l1-l2)+.002f,Math.min(l1+l2-.002f,d));dx/=d;dy/=d;dz/=d;p.x[tip]=p.x[root]+dx*limited;p.y[tip]=p.y[root]+dy*limited;p.z[tip]=p.z[root]+dz*limited;
  float dot=dx*poleX+dy*poleY+dz*poleZ,px=poleX-dx*dot,py=poleY-dy*dot,pz=poleZ-dz*dot,len=(float)Math.sqrt(px*px+py*py+pz*pz);if(len<.001f){float refX=Math.abs(dx)<.9f?1:0,refY=Math.abs(dx)<.9f?0:1,projection=refX*dx+refY*dy;px=refX-dx*projection;py=refY-dy*projection;pz=-dz*projection;len=(float)Math.sqrt(px*px+py*py+pz*pz);}
  float along=(l1*l1-l2*l2+limited*limited)/(2*limited),rise=(float)Math.sqrt(Math.max(0,l1*l1-along*along));p.x[middle]=p.x[root]+dx*along+px/len*rise;p.y[middle]=p.y[root]+dy*along+py/len*rise;p.z[middle]=p.z[root]+dz*along+pz/len*rise;
 }
}
