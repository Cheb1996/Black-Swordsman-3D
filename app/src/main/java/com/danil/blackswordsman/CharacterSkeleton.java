package com.danil.blackswordsman;

/**
 * Hierarchical 16-bone character rig. Skin pieces are attached to these bones;
 * the same definitions are reused by animation, hit dismemberment and ragdolls.
 */
public final class CharacterSkeleton {
    public static final int PELVIS=0,LOWER_TORSO=1,UPPER_TORSO=2,HEAD=3;
    public static final int L_UPPER_ARM=4,L_FOREARM=5,L_HAND=6;
    public static final int R_UPPER_ARM=7,R_FOREARM=8,R_HAND=9;
    public static final int L_THIGH=10,L_SHIN=11,L_FOOT=12;
    public static final int R_THIGH=13,R_SHIN=14,R_FOOT=15;
    public static final int BONE_COUNT=16,JOINT_COUNT=17;

    public static final int GROUP_HEAD=1,GROUP_LEFT_ARM=2,GROUP_RIGHT_ARM=4,GROUP_LEFT_LEG=8,GROUP_RIGHT_LEG=16;
    public static final int SHAPE_BOX=0,SHAPE_CAPSULE=1,SHAPE_SPHERE=2;

    public static final String[] NAMES={"pelvis","lower_torso","upper_torso","head","left_upper_arm","left_forearm","left_hand","right_upper_arm","right_forearm","right_hand","left_thigh","left_shin","left_foot","right_thigh","right_shin","right_foot"};
    public static final int[] PARENT={-1,PELVIS,LOWER_TORSO,UPPER_TORSO,UPPER_TORSO,L_UPPER_ARM,L_FOREARM,UPPER_TORSO,R_UPPER_ARM,R_FOREARM,PELVIS,L_THIGH,L_SHIN,PELVIS,R_THIGH,R_SHIN};
    public static final int[] START={0,0,1,3,5,6,7,8,9,10,11,12,13,14,15,16};
    public static final int[] END={1,2,3,4,6,7,7,9,10,10,12,13,13,15,16,16};
    public static final int[] GROUP={0,0,0,GROUP_HEAD,GROUP_LEFT_ARM,GROUP_LEFT_ARM,GROUP_LEFT_ARM,GROUP_RIGHT_ARM,GROUP_RIGHT_ARM,GROUP_RIGHT_ARM,GROUP_LEFT_LEG,GROUP_LEFT_LEG,GROUP_LEFT_LEG,GROUP_RIGHT_LEG,GROUP_RIGHT_LEG,GROUP_RIGHT_LEG};
    public static final int[] SHAPE={SHAPE_BOX,SHAPE_BOX,SHAPE_BOX,SHAPE_SPHERE,SHAPE_CAPSULE,SHAPE_CAPSULE,SHAPE_SPHERE,SHAPE_CAPSULE,SHAPE_CAPSULE,SHAPE_SPHERE,SHAPE_CAPSULE,SHAPE_CAPSULE,SHAPE_BOX,SHAPE_CAPSULE,SHAPE_CAPSULE,SHAPE_BOX};

    /** Joint positions in world space. One instance can be reused between actors. */
    public static final class Pose {
        public final float[] x=new float[JOINT_COUNT],y=new float[JOINT_COUNT],z=new float[JOINT_COUNT];
    }

    private CharacterSkeleton(){}

    public static boolean isBeast(int type){return type==GameWorld.TUSK_BOAR||type==GameWorld.MIRE_SPITTER||type==GameWorld.WALKER||type==GameWorld.FLESH_HOUND||type==GameWorld.SERPENT_SPAWN||type==GameWorld.VOID_BEAST;}

    public static void sample(GameWorld.Actor actor,float time,Pose out){
        if(isBeast(actor.type))sampleBeast(actor,time,out);else sampleHumanoid(actor,time,out);
    }

    private static void sampleHumanoid(GameWorld.Actor a,float time,Pose p){
        float h=a.height,r=a.radius,speed=Math.min(1.25f,a.animationSpeed),phase=a.gaitPhase,step=(float)Math.sin(phase)*speed,opposite=-step;
        float breathe=(float)Math.sin(time*1.8f+a.type*.13f)*.003f*h,land=(float)Math.sin(Math.min(1,a.landingTime/.36f)*Math.PI)*.09f*h,crouch=(a.dodgeTime>0?.14f*h:0)+land;
        set(p,0,0,.43f*h-crouch,0);set(p,1,0,.52f*h-crouch,0);set(p,2,0,.64f*h-crouch+breathe,0);set(p,3,0,.78f*h-crouch+breathe,0);set(p,4,0,.90f*h-crouch+breathe,0);
        set(p,5,-r*.82f,.72f*h-crouch,0);set(p,6,-r*.92f,.56f*h-crouch,0);set(p,7,-r*.78f,.41f*h-crouch,opposite*.15f*h);
        set(p,8,r*.82f,.72f*h-crouch,0);set(p,9,r*.92f,.56f*h-crouch,0);set(p,10,r*.78f,.41f*h-crouch,step*.15f*h);
        for(int side=0;side<2;side++){int hip=side==0?11:14,knee=hip+1,foot=hip+2;float cycle=phase+side*(float)Math.PI,walk=(float)Math.sin(cycle)*speed,lift=Math.max(0,(float)Math.cos(cycle))*speed*.08f*h;set(p,hip,(side==0?-1:1)*r*.34f,.43f*h-crouch,0);set(p,knee,p.x[hip],.24f*h,.08f*h);set(p,foot,p.x[hip],.045f*h+lift,walk*.15f*h+.07f);}
        if(a.attackTime>0||a.attackBlend>.01f){float duration=a.attackDuration>0?a.attackDuration:a.attackKind==5?.82f:a.attackKind==6?.48f:a.player?.31f:.46f,t=1-Math.min(1,a.attackTime/duration),contact=a.strikeAt>0?1-a.strikeAt/duration:.38f;
            contact=Math.max(.22f,Math.min(.68f,contact));float swing=t<contact?t/contact:1-(t-contact)/(1-contact),arc=(float)Math.sin(swing*Math.PI*.5f),weight=a.attackBlend;
            float tx=r*(.85f-1.9f*arc),ty=h*(.63f+.12f*(1-arc)),tz=h*(-.18f+.6f*arc);
            if(a.attackKind==5){tx=r*.2f;ty=h*(1.13f-.62f*arc);tz=h*(-.16f+.62f*arc);}if(a.attackKind==6){tx=r*.48f;ty=h*.60f;tz=h*.37f;}
            p.x[10]+=(tx-p.x[10])*weight;p.y[10]+=(ty-crouch-p.y[10])*weight;p.z[10]+=(tz-p.z[10])*weight;
            if(a.attackKind==5||a.type==GameWorld.CROSSBOW){p.x[7]=p.x[10]-r*.22f;p.y[7]=p.y[10]-.05f;p.z[7]=p.z[10]-.08f;}else if(a.attackKind==6&&a.player){p.x[7]=-r*.3f;p.y[7]=h*.63f;p.z[7]=h*(.37f-.07f*(float)Math.sin(t*Math.PI));}
            float twist=(.25f-arc*.52f)*weight;for(int i=2;i<=10;i++){float x=p.x[i],z=p.z[i];p.x[i]=(float)Math.cos(twist)*x+(float)Math.sin(twist)*z;p.z[i]=-(float)Math.sin(twist)*x+(float)Math.cos(twist)*z;}
        }
        if(a.collisionTime>0&&a.attackTime<=0){p.y[7]=p.y[10]=h*.66f-crouch;p.z[7]=p.z[10]=h*.23f;}
        if(!a.grounded&&a.motionReady&&!a.swimming&&!a.flying){float tuck=a.vy>0?.11f:0;p.y[13]+=h*tuck;p.y[16]+=h*tuck*.65f;p.z[13]-=h*.13f;p.z[16]-=h*.08f;p.z[7]+=h*.08f;p.z[10]+=h*.08f;}
        if(a.swimming||a.flying){float wave=(float)Math.sin(time*(a.swimming?4:1.8f));p.y[7]=p.y[10]=h*.69f;p.x[7]=-r*(a.flying?1.8f:1.2f);p.x[10]=-p.x[7];p.z[7]=h*(.25f+wave*.12f);p.z[10]=h*(.25f-wave*.12f);p.y[13]=p.y[16]=h*.11f;p.z[13]=-h*.15f+wave*.07f;p.z[16]=-h*.15f-wave*.07f;}
        AnimationSystem.joint(p,5,6,7,h*.17f,h*.17f,-1,0,-.4f);AnimationSystem.joint(p,8,9,10,h*.17f,h*.17f,1,0,-.4f);
        AnimationSystem.joint(p,11,12,13,h*.205f,h*.205f,0,0,1);AnimationSystem.joint(p,14,15,16,h*.205f,h*.205f,0,0,1);
        float forward=a.leanForward+a.hitReaction*-.11f+(a.dodgeTime>0?.24f:0),side=a.leanSide+a.turnLean;for(int i=1;i<=10;i++){float weight=Math.max(0,(p.y[i]/h-.43f));p.z[i]+=forward*h*weight;p.x[i]+=side*h*weight;}
        AnimationSystem.joint(p,5,6,7,h*.17f,h*.17f,-1,0,-.4f);AnimationSystem.joint(p,8,9,10,h*.17f,h*.17f,1,0,-.4f);
        transformAll(a,p);
    }

    /** A second hierarchy layout for quadruped/serpentine enemies using the same bone IDs. */
    private static void sampleBeast(GameWorld.Actor a,float time,Pose p){
        float h=a.height,r=a.radius,cycle=(float)Math.sin(a.gaitPhase)*.19f*a.animationSpeed,bob=(float)Math.sin(a.gaitPhase*2)*.025f*a.animationSpeed;
        set(p,0,0,h*.42f,-r*.45f);set(p,1,0,h*.44f,-r*.10f);set(p,2,0,h*.48f,r*.25f);set(p,3,0,h*.53f,r*.62f);set(p,4,0,h*.55f,r*.92f);
        set(p,5,-r*.54f,h*.43f,r*.22f);set(p,6,-r*.61f,h*.18f,r*.31f+cycle);set(p,7,-r*.62f,h*.05f,r*.49f+cycle);
        set(p,8,r*.54f,h*.43f,r*.22f);set(p,9,r*.61f,h*.18f,r*.31f-cycle);set(p,10,r*.62f,h*.05f,r*.49f-cycle);
        set(p,11,-r*.49f,h*.40f,-r*.45f);set(p,12,-r*.55f,h*.17f,-r*.50f-cycle);set(p,13,-r*.56f,h*.05f,-r*.31f-cycle);
        set(p,14,r*.49f,h*.40f,-r*.45f);set(p,15,r*.55f,h*.17f,-r*.50f+cycle);set(p,16,r*.56f,h*.05f,-r*.31f+cycle);
        for(int i=0;i<JOINT_COUNT;i++){p.y[i]+=bob*h;if(i<5){p.z[i]+=a.leanForward*h*.3f;p.x[i]+=(a.leanSide+a.turnLean)*h*.3f;}}
        for(int hip:new int[]{5,8,11,14}){int mid=hip+1,foot=hip+2;float lift=Math.max(0,(float)Math.cos(a.gaitPhase+(hip==5||hip==14?0:Math.PI)))*.12f*h*a.animationSpeed;p.y[foot]+=lift;AnimationSystem.joint(p,hip,mid,foot,h*.22f,h*.22f,0,0,hip<10?-1:1);}
        if(a.attackTime>0){float bite=(float)Math.sin(Math.min(1,a.attackTime/.5f)*Math.PI)*.15f;for(int i=2;i<5;i++)p.z[i]+=bite*h;}
        transformAll(a,p);
    }

    public static void groundFeet(GameWorld.Actor a,Pose p,PhysicsWorld physics){if(!a.grounded||a.ragdollSpawned)return;int[] feet=isBeast(a.type)?new int[]{7,10,13,16}:new int[]{13,16};for(int f:feet){float ground=physics.supportHeight(p.x[f],p.z[f],a.y+.35f);float delta=Math.max(0,Math.min(.30f,ground+a.height*.045f-p.y[f]));p.y[f]+=delta;float l1=isBeast(a.type)?a.height*.22f:a.height*.205f,l2=l1;AnimationSystem.joint(p,f-2,f-1,f,l1,l2,(float)Math.sin(a.yaw),0,(float)Math.cos(a.yaw));}}
    private static void set(Pose p,int i,float x,float y,float z){p.x[i]=x;p.y[i]=y;p.z[i]=z;}
    private static void transformAll(GameWorld.Actor a,Pose p){float s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw);for(int i=0;i<JOINT_COUNT;i++){float x=p.x[i],z=p.z[i];p.x[i]=a.x+c*x+s*z;p.y[i]+=a.y;p.z[i]=a.z-s*x+c*z;}}

    public static float radiusFor(GameWorld.Actor a,int bone){
        float r=a.radius;
        if(bone==PELVIS)return r*.58f;if(bone==LOWER_TORSO)return r*.54f;if(bone==UPPER_TORSO)return r*.70f;if(bone==HEAD)return r*.39f;
        if(bone==L_HAND||bone==R_HAND)return r*.20f;if(bone==L_FOOT||bone==R_FOOT)return r*.25f;
        if(bone==L_THIGH||bone==R_THIGH)return r*.24f;if(bone==L_SHIN||bone==R_SHIN)return r*.19f;return r*.17f;
    }

    public static boolean visible(GameWorld.Actor a,int bone){return GROUP[bone]==0||(a.severedMask&GROUP[bone])==0;}
}
