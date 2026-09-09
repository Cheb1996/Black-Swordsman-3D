package com.danil.blackswordsman;

/** One authored landscape specification feeds terrain, collision and chapter routes. */
public final class WorldLayout {
    private WorldLayout(){}
    public static float road(float z){return (float)Math.sin(z*.035f)*3f;}
    public static float height(int env,float x,float z){
        float h=legacyHeight(env,x,z),extent=Math.max(Math.abs(x),Math.abs(z));
        if(extent>145){float t=Math.min(1,(extent-145)/40f);t=t*t*(3-2*t);float hills=6f+(float)Math.sin(x*.022f)*4f+(float)Math.cos(z*.018f)*3f+(float)Math.sin((x+z)*.045f)*1.2f;hills+=RegionLayout.mountains(x,z)*(7f+14f*Math.abs((float)Math.sin(x*.019f)*(float)Math.cos(z*.016f)))+RegionLayout.desert(x,z)*(float)Math.sin(x*.08f+z*.025f)*1.8f;h=h*(1-t)+hills*t;}
        return WaterField.bed(env,x,z,h);
    }
    private static float legacyHeight(int env,float x,float z){
        float lane=Math.min(1,Math.abs(x-road(z))/8f);lane=lane*lane*(3-2*lane);
        float hills=(float)Math.sin(x*.031f+env*.71f)*2.8f+(float)Math.cos(z*.037f-env*.43f)*1.9f+(float)Math.sin((x+z)*.097f)*.42f;
        float altitude=(env==2||env==6||env==7)?Math.max(0,z)*.055f:0;
        if(env==1||env==4)return .08f+altitude+hills*.13f*lane;
        if(env==5)return -.35f+altitude+hills*.12f*lane;
        if(env==8)return .08f+altitude+hills*.65f*lane+(float)Math.sin(z*.026f)*.5f;
        float ravine=env==0||env==3?3.8f*(float)Math.exp(-Math.pow((z-40f)/4f,2)):0;
        return .08f+altitude+hills*lane-ravine;
    }
    public static void populate(PhysicsWorld p,int env){
        // A real bridge over the river: both characters and crates can stand on its deck.
        if(env==0||env==3){
            p.createStatic(PhysicsWorld.PLANK,road(40),.30f,40,3.6f,.22f,8.2f);
            p.createStatic(PhysicsWorld.PLANK,road(40),.12f,31.0f,3.6f,.13f,1.1f);p.createStatic(PhysicsWorld.PLANK,road(40),.12f,49.0f,3.6f,.13f,1.1f);
            for(int side=-1;side<=1;side+=2){p.createStatic(PhysicsWorld.PLANK,road(40)+side*3.45f,1.1f,40,.12f,.12f,8.2f);for(int z=34;z<=46;z+=4)p.createStatic(PhysicsWorld.COLUMN,road(40)+side*3.45f,.70f,z,.11f,.7f,.11f);}
        }
        // Buildings leave a clear central road and side alleys. All visible buildings collide.
        if(env==1||env==4){for(int side=-1;side<=1;side+=2)for(int n=0;n<6;n++){float x=side*(13+(n%2)*5),z=18+n*17,y=height(env,x,z);PhysicsWorld.Body house=p.createStatic(PhysicsWorld.RUIN,x,y+3.4f,z,4.5f,3.4f,5.5f);house.variant=n%3;}}
        if(env==2||env==6||env==7){for(int z=22;z<=112;z+=30)for(int side=-1;side<=1;side+=2){float x=side*10,y=height(env,x,z);p.createStatic(PhysicsWorld.COLUMN,x,y+4.2f,z,1.15f,4.2f,1.15f);p.createStatic(PhysicsWorld.RUIN,x+side*6,y+2.1f,z,5.2f,2.1f,.8f);}}
        for(int n=0;n<4;n++){float z=18+n*29,x=road(z)+5.7f;PhysicsWorld.Body sign=p.createStatic(PhysicsWorld.LANTERN,x,height(env,x,z)+1.1f,z,.12f,1.1f,.12f);sign.variant=env;}
        if(env==3)for(int n=0;n<12;n++){float x=(n%2==0?-1:1)*(8+(n%3)*3),z=55+n*4;p.createStatic(PhysicsWorld.GRAVE,x,height(env,x,z)+.65f,z,.5f,.65f,.15f);}
        // Distinct gateway at the physical destination of every chapter.
        if(env>=5&&env<=7)for(int side=-1;side<=1;side+=2)p.addChain(side*7,height(env,side*7,58)+4.5f,58,9);
        float end=118,y=height(env,0,end);for(int side=-1;side<=1;side+=2)p.createStatic(PhysicsWorld.COLUMN,side*5,y+3.7f,end,.65f,3.7f,.65f);p.createStatic(PhysicsWorld.PLANK,0,y+7.1f,end,5.6f,.38f,.65f);
    }
}
