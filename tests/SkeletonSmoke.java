package com.danil.blackswordsman;

/** Structural test for both humanoid and beast skeleton layouts. */
public final class SkeletonSmoke {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        check(CharacterSkeleton.NAMES.length==CharacterSkeleton.BONE_COUNT,"bone names");check(CharacterSkeleton.START.length==CharacterSkeleton.BONE_COUNT,"bone links");
        for(int i=0;i<CharacterSkeleton.BONE_COUNT;i++){check(CharacterSkeleton.PARENT[i]<i,"acyclic hierarchy "+i);check(CharacterSkeleton.START[i]>=0&&CharacterSkeleton.START[i]<17,"start joint");check(CharacterSkeleton.END[i]>=0&&CharacterSkeleton.END[i]<17,"end joint");}
        GameWorld.Actor actor=new GameWorld.Actor();actor.type=-1;actor.height=2.1f;actor.radius=.52f;actor.speed=5;actor.x=3;actor.z=-2;actor.yaw=.6f;actor.vx=3;actor.animationSpeed=.6f;
        CharacterSkeleton.Pose idle=new CharacterSkeleton.Pose(),walk=new CharacterSkeleton.Pose();CharacterSkeleton.sample(actor,0,idle);actor.gaitPhase=1.2f;CharacterSkeleton.sample(actor,.2f,walk);
        check(idle.y[4]>idle.y[0],"head above pelvis");check(Math.abs(idle.z[7]-walk.z[7])>.001f,"arm animation");
        actor.type=GameWorld.FLESH_HOUND;CharacterSkeleton.sample(actor,.4f,walk);check(walk.z[4]!=walk.z[0],"beast spine");
        actor.severedMask=CharacterSkeleton.GROUP_LEFT_ARM;check(!CharacterSkeleton.visible(actor,CharacterSkeleton.L_FOREARM),"limb mask");check(CharacterSkeleton.visible(actor,CharacterSkeleton.R_FOREARM),"other limb remains");
        System.out.println("SkeletonSmoke OK: 16-bone humanoid/beast rigs and limb masks");
    }
}
