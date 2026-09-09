package com.danil.blackswordsman;

/** Deterministic water volumes. Rendering, buoyancy, swimming and world generation share them. */
public final class WaterField {
    public static final int DRY=0,OCEAN=1,RIVER=2,LAKE=3,OASIS=4;
    public static final float SEA=-2f;
    // x,z,ellipse radii,level. The last two pools are desert oases.
    public static final float[][] LAKES={{220,195,44,33,2},{-240,-205,38,29,3},{-210,235,34,46,1.5f},{218,-165,19,15,3},{-305,105,16,21,2}};
    public static final float[][] RIVER_POINTS={{-490,30},{-375,52},{-285,24},{-220,33},{-145,40},{0,40},{115,40},{168,28},{205,4},{262,-18},{325,-65},{390,-50},{490,-65}};
    public static final float[][] TRIBUTARY_POINTS={{-210,204},{-208,160},{-238,122},{-222,82},{-220,33}};
    private WaterField(){}
    public static float coast(float x,float z){float a=(float)Math.atan2(z,x);float r=(float)Math.pow((double)x*x*x*x+(double)z*z*z*z,.25);return r-(GameWorld.WORLD_HALF-24+(float)Math.sin(a*5)*10+(float)Math.cos(a*3)*7);}
    public static float lakeDistance(int i,float x,float z){float[] l=LAKES[i];float dx=(x-l[0])/l[2],dz=(z-l[1])/l[3];return (float)Math.sqrt(dx*dx+dz*dz);}
    private static float pathDistance(float[][] path,float x,float z){float best=1000;for(int i=1;i<path.length;i++){float[] a=path[i-1],b=path[i];float dx=b[0]-a[0],dz=b[1]-a[1],t=clamp(((x-a[0])*dx+(z-a[1])*dz)/(dx*dx+dz*dz),0,1),px=x-a[0]-t*dx,pz=z-a[1]-t*dz;best=Math.min(best,(float)Math.sqrt(px*px+pz*pz));}return best;}
    public static float riverDistance(int env,float x,float z){if(Math.abs(x)<147){return env==0||env==3?Math.abs(z-40):1000;}return Math.min(pathDistance(RIVER_POINTS,x,z),pathDistance(TRIBUTARY_POINTS,x,z));}
    private static float riverLevel(float x,float z){if(x< -170&&pathDistance(TRIBUTARY_POINTS,x,z)<pathDistance(RIVER_POINTS,x,z))return -1.55f+clamp((z-33)/171,0,1)*3.05f;return -1.55f-clamp((Math.abs(x)-300)/150,0,1)*.45f;}
    public static int kind(int env,float x,float z){if(Math.max(Math.abs(x),Math.abs(z))<145)return (env==0||env==3)&&Math.abs(z-40)<10?RIVER:DRY;if(coast(x,z)>-48)return OCEAN;for(int i=0;i<LAKES.length;i++)if(lakeDistance(i,x,z)<1.35f)return i>=3?OASIS:LAKE;return riverDistance(env,x,z)<10?RIVER:DRY;}
    public static float level(int env,float x,float z){if(Math.max(Math.abs(x),Math.abs(z))<145)return -1.55f;if(coast(x,z)>-48)return SEA;for(int i=0;i<LAKES.length;i++)if(lakeDistance(i,x,z)<1.35f)return LAKES[i][4];return riverDistance(env,x,z)<10?riverLevel(x,z):SEA;}
    public static float bed(int env,float x,float z,float original){
        float c=coast(x,z),h=original;
        if(c> -48){float t=smooth((c+48)/60);h=h*(1-t)+(-10f-Math.max(0,c)*.04f)*t;}
        if(Math.max(Math.abs(x),Math.abs(z))>145){for(int i=0;i<LAKES.length;i++){float d=lakeDistance(i,x,z);if(d<1.35f){float t=smooth((1.35f-d)/.48f);h=h*(1-t)+(LAKES[i][4]-3.8f)*t;}}}
        float rd=riverDistance(env,x,z);if(rd<10){float t=smooth((10-rd)/5.5f);h=Math.min(h,h*(1-t)+(riverLevel(x,z)-2.2f)*t);}
        return h;
    }
    public static float depth(int env,float x,float z){if(kind(env,x,z)==DRY)return 0;return Math.max(0,level(env,x,z)-WorldLayout.height(env,x,z));}
    public static float surface(int env,float x,float z,float time){float k=kind(env,x,z)==OCEAN?.12f:.025f;return level(env,x,z)+k*((float)Math.sin(x*.23f+z*.17f-time*1.9f)+.45f*(float)Math.sin(z*.49f-x*.12f+time*2.4f));}
    private static float riverFlow(float x,float z,boolean axisX){boolean tributary=x< -170&&pathDistance(TRIBUTARY_POINTS,x,z)<pathDistance(RIVER_POINTS,x,z);float[][] path=tributary?TRIBUTARY_POINTS:RIVER_POINTS;float best=Float.MAX_VALUE,result=0;for(int i=1;i<path.length;i++){float dx=path[i][0]-path[i-1][0],dz=path[i][1]-path[i-1][1],len=(float)Math.sqrt(dx*dx+dz*dz),t=clamp(((x-path[i-1][0])*dx+(z-path[i-1][1])*dz)/(len*len),0,1),px=x-path[i-1][0]-dx*t,pz=z-path[i-1][1]-dz*t,d=px*px+pz*pz;if(d<best){best=d;result=(axisX?dx:dz)/len*.65f*(tributary||x>=0?1:-1);}}return result;}
    public static float flowX(int env,float x,float z,float time){int k=kind(env,x,z);return k==RIVER?riverFlow(x,z,true):k==OCEAN?.16f+(float)Math.sin(z*.019f+time*.06f)*.16f:(float)Math.sin(z*.05f+time*.08f)*.08f;}
    public static float flowZ(int env,float x,float z,float time){int k=kind(env,x,z);return k==RIVER?riverFlow(x,z,false):k==OCEAN?.12f+(float)Math.cos(x*.02f+time*.04f)*.14f:(float)Math.cos(x*.05f+time*.08f)*.08f;}
    public static void body(int env,PhysicsWorld.Body b,float time,float dt){
        float depth=depth(env,b.x,b.z);b.submerged=0;if(depth<=.02f)return;
        float water=surface(env,b.x,b.z,time),extent=Math.max(.04f,b.ey),fraction=clamp((water-b.y+extent)/(extent*2),0,1);b.submerged=fraction;if(fraction<=0)return;
        b.sleeping=false;b.stillTime=0;
        // Arcade buoyancy: every loose object can float, including metal and severed limbs.
        // Acceleration is mass independent, with critical-ish drag and a 62% immersion target.
        b.vy+=(9.81f*fraction/.62f-b.vy*3.7f*fraction)*dt;
        float drag=1-SimulationClock.damping(2.4f*fraction,dt);
        b.vx+=(flowX(env,b.x,b.z,time)-b.vx)*drag;b.vz+=(flowZ(env,b.x,b.z,time)-b.vz)*drag;
        float spin=SimulationClock.damping(1.5f*fraction,dt);b.avx*=spin;b.avy*=spin;b.avz*=spin;
        if(b.catalogId==152||b.catalogId==182){b.avx+=(-angle(b.rx)*.07f-b.avx*2f)*dt;b.avz+=(-angle(b.rz)*.07f-b.avz*2f)*dt;}
    }
    public static void particle(int env,GameWorld.Particle p,float time,float dt){if(depth(env,p.x,p.z)<.03f)return;float water=surface(env,p.x,p.z,time);if(p.y-p.size>water)return;p.floating=true;p.y=water+p.size*.25f;p.vy=0;float f=1-SimulationClock.damping(4,dt);p.vx+=(flowX(env,p.x,p.z,time)-p.vx)*f;p.vz+=(flowZ(env,p.x,p.z,time)-p.vz)*f;}
    private static float angle(float a){a%=360;if(a>180)a-=360;if(a< -180)a+=360;return a;}
    private static float smooth(float v){v=clamp(v,0,1);return v*v*(3-2*v);}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
}
