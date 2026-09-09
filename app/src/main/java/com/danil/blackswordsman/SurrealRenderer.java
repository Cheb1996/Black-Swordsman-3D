package com.danil.blackswordsman;

import android.opengl.GLES30;

/** Detailed, single-draw toy meshes plus animated anomalous landmarks and creatures. */
public final class SurrealRenderer {
    private final GameRenderer painter;private final android.content.res.AssetManager assets;
    private final Mesh cube,sphere,cylinder,cone,ring;
    private final Mesh[][] toys=new Mesh[240][2],pickups=new Mesh[10][2];
    public SurrealRenderer(android.content.res.AssetManager assets,GameRenderer painter,Mesh cube,Mesh sphere,Mesh cylinder,Mesh cone,Mesh ring){this.assets=assets;this.painter=painter;this.cube=cube;this.sphere=sphere;this.cylinder=cylinder;this.cone=cone;this.ring=ring;}
    private int warmed;
    public void prewarm(int budget){for(int n=0;n<budget&&warmed<500;n++,warmed++){int id=warmed/2,lod=warmed%2;Mesh m;try{java.io.DataInputStream in=new java.io.DataInputStream(new java.io.BufferedInputStream(assets.open("meshes/"+id+"-"+lod+".bin")));int nv=in.readInt(),ni=in.readInt();float[] v=new float[nv];short[] ix=new short[ni];for(int i=0;i<nv;i++)v[i]=in.readFloat();for(int i=0;i<ni;i++)ix[i]=in.readShort();in.close();m=new Mesh(v,ix,true);}catch(java.io.IOException error){throw new IllegalStateException("Missing baked geometry "+id,error);}if(id<240)toys[id][lod]=m;else pickups[id-240][lod]=m;}}
    private Mesh toy(int id,boolean detail){Mesh m=toys[id][detail?1:0];return m!=null?m:toys[id][0]!=null?toys[id][0]:cube;}
    private Mesh pickup(int id,boolean detail){Mesh m=pickups[id][detail?1:0];return m!=null?m:pickups[id][0]!=null?pickups[id][0]:sphere;}
    private void draw(Mesh m,float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b,float a,float metal,float rough,float glow){painter.draw(m,x,y,z,sx,sy,sz,rx,ry,rz,r,g,b,a,metal,rough,glow);}

    public boolean body(PhysicsWorld.Body b,GameWorld world,float d2){
        if(b.kind==PhysicsWorld.TOY_ITEM){boolean detail=b==world.physics.getHeld()||d2<(world.qualityLevel==1?49:196);draw(toy(b.catalogId,detail),b.x,b.y,b.z,b.hx,b.hy,b.hz,b.rx,b.ry,b.rz,1,1,1,1,b.metalness,b.roughness,.10f);return true;}
        if(b.kind==PhysicsWorld.UFO){
            float t=world.surreal.time;draw(sphere,b.x,b.y,b.z,3.2f,.42f,3.2f,0,b.ry,0,.31f,.37f,.43f,1,.86f,.18f,.08f);
            draw(ring,b.x,b.y-.08f,b.z,3.05f,.75f,3.05f,0,b.ry,0,.06f,.78f,.78f,1,.65f,.24f,.80f);
            draw(sphere,b.x,b.y+.48f,b.z,1.24f,.86f,1.24f,0,0,0,.09f,.24f,.36f,1,.54f,.15f,.18f);
            for(int i=0;i<10;i++){float a=i*.6283185f+b.ry*.0174533f;draw(sphere,b.x+(float)Math.sin(a)*2.6f,b.y-.29f,b.z+(float)Math.cos(a)*2.6f,.12f,.08f,.12f,0,0,0,i%2==0?.12f:.95f,.65f,.94f,1,.05f,.2f,2);}
            int index=world.surreal.ufos.indexOf(b);if(index>=0&&(world.surreal.beamActive(index)||world.encounters.beam(b))){
                float floor=world.physics.terrainHeight(b.x,b.z),h=b.y-floor;
                GLES30.glDepthMask(false);GLES30.glDisable(GLES30.GL_CULL_FACE);
                draw(cone,b.x,floor+h*.5f,b.z,4.1f,h*.5f,4.1f,0,0,0,.08f,.64f,.65f,.055f,.02f,.3f,.5f);
                for(int i=0;i<3;i++){float y=floor+((t*.6f+i*1.4f)%h);draw(ring,b.x,y,b.z,1.2f+(b.y-y)*.23f,.2f,1.2f+(b.y-y)*.23f,0,t*20,0,.1f,.9f,.72f,.20f,.02f,.3f,1.2f);}
                GLES30.glEnable(GLES30.GL_CULL_FACE);GLES30.glDepthMask(true);
            }return true;
        }return false;
    }

    public void environment(GameWorld world){
        float t=world.surreal.time;
        if(world.effects.form==PlayerEffects.HOUND&&!world.exploration.tracked){float dx=30-world.player.x,dz=16-world.player.z,d=(float)Math.sqrt(dx*dx+dz*dz);if(d<35&&d>.1f)for(int n=1;n<=12;n++){float step=Math.min(d,n*.85f),x=world.player.x+dx/d*step,z=world.player.z+dz/d*step;draw(sphere,x,world.physics.terrainHeight(x,z)+.045f,z,.055f,.025f,.12f,0,0,0,.3f,.9f,.7f,1,0,.8f,1.8f);}}
        for(int i=0;i<world.surreal.pickups.size();i++){
            SurrealWorld.Pickup p=world.surreal.pickups.get(i);if(p.cooldown>0)continue;float dx=p.x-world.player.x,dz=p.z-world.player.z,d2=dx*dx+dz*dz;if(d2>34f*34f)continue;
            float y=p.y+(float)Math.sin(t*1.6f+p.kind)*.065f,q=p.kind==0?.46f:.33f;
            draw(pickup(p.kind,d2<144),p.x,y,p.z,q,q,q,0,t*24+p.id*37,0,1,1,1,1,.05f,.32f,.30f);
            float[] color=SurrealCatalog.COLORS[p.kind%9];GLES30.glDepthMask(false);
            draw(ring,p.x,world.physics.terrainHeight(p.x,p.z)+.03f,p.z,.63f,.12f,.63f,0,t*12,0,color[0],color[1],color[2],.8f,.02f,.25f,.85f);
            draw(sphere,p.x,y+.48f,p.z,.045f,.045f,.045f,0,0,0,color[0],color[1],color[2],.8f,.02f,.25f,1.7f);GLES30.glDepthMask(true);
        }
        for(int i=0;i<SurrealWorld.ZONES.length;i++){
            float x=SurrealWorld.ZONES[i][0],z=SurrealWorld.ZONES[i][1],dx=x-world.player.x,dz=z-world.player.z;if(dx*dx+dz*dz>58f*58f)continue;
            float y=world.physics.terrainHeight(x,z);float[] c=SurrealCatalog.COLORS[i%9];
            GLES30.glDepthMask(false);draw(ring,x,y+.04f,z,11.5f,.10f,11.5f,0,0,0,c[0],c[1],c[2],.16f,.02f,.9f,.25f);GLES30.glDepthMask(true);
            if(i==3||i==8){
                draw(ring,x,y+2.3f,z,2.3f,2.3f,2.3f,90,t*8,0,c[0],c[1],c[2],1,.46f,.28f,.95f);
                draw(ring,x,y+2.3f,z,1.8f,1.8f,1.8f,90,-t*12,0,.10f,.85f,1f,1,.38f,.2f,.7f);
                for(int n=0;n<8;n++){float a=t*.4f+n*.7854f;draw(sphere,x+(float)Math.sin(a)*2.05f,y+2.3f+(float)Math.cos(a)*2.05f,z,.10f,.10f,.10f,0,0,0,1,.54f,.92f,1,.02f,.2f,1.3f);}
            }

        }
    }

    public void oddity(GameWorld.Actor a,CharacterSkeleton.Pose pose,float t,boolean detailed){
        float x=a.x,y=a.y,z=a.z,r=a.hitFlash>0?1:a.colorR,g=a.colorG,b=a.colorB;
        if(a.type==GameWorld.MAGNET_JELLY){
            draw(sphere,x,y+1.9f,z,.87f,.53f,.87f,0,t*17,0,r,g,b,1,.13f,.19f,.35f);
            for(int n=0;n<(detailed?8:4);n++){float angle=n*.7854f,ax=x+(float)Math.sin(angle)*.56f,ay=y+1.52f,az=z+(float)Math.cos(angle)*.56f;for(int seg=0;seg<(detailed?6:3);seg++){float wave=(float)Math.sin(t*2+n+seg*.6f)*.17f,bx=x+(float)Math.sin(angle)*(.56f+wave),by=ay-(detailed?.22f:.42f),bz=z+(float)Math.cos(angle)*(.56f+wave);painter.drawBetween(cylinder,ax,ay,az,bx,by,bz,.05f-seg*.004f,.05f-seg*.004f,r,g,b,1,.02f,.4f,.23f);ax=bx;ay=by;az=bz;}}
            eyes(x,y+1.93f,z+.76f,t);return;
        }
        if(a.type==GameWorld.DICE_CRAB){
            draw(toy(56,true),x,y+.56f,z,.67f,.67f,.67f,0,a.yaw*57.3f,0,1,1,1,1,.03f,.3f,.12f);
            for(int n=0;n<8;n++){float angle=n*.7854f,step=(float)Math.sin(a.gaitPhase+n)*.14f*a.animationSpeed,sn=(float)Math.sin(angle),cs=(float)Math.cos(angle);painter.drawBetween(cylinder,x+sn*.44f,y+.55f,z+cs*.44f,x+sn*.95f,y+.40f,z+cs*.95f,.07f,.07f,r,g,b,1,.4f,.4f,0);painter.drawBetween(cylinder,x+sn*.95f,y+.40f,z+cs*.95f,x+sn*1.15f,y+.07f+Math.max(0,step),z+cs*(1.15f+step),.04f,.04f,r,g,b,1,.4f,.4f,0);}return;
        }
        if(a.type==GameWorld.CLOCK_MOTH){
            draw(toy(202,true),x,y+1.1f,z,.50f,.50f,.50f,0,a.yaw*57.3f,0,1,1,1,1,.4f,.3f,.3f);
            for(int side=-1;side<=1;side+=2){float flap=(float)Math.sin(t*4)*25;draw(sphere,x+side*.74f,y+1.25f,z,.72f,.49f,.06f,flap*side,0,side*20,r,g,b,1,.18f,.4f,.22f);draw(sphere,x+side*.56f,y+.68f,z,.51f,.30f,.07f,-flap*side,0,-side*20,.40f,.21f,.8f,1,.12f,.4f,.35f);}return;
        }
        // Humanoid toys are visibly assembled on the same sampled skeleton used by ragdolls.
        for(int bone=0;bone<CharacterSkeleton.BONE_COUNT;bone++){
            if(!CharacterSkeleton.visible(a,bone))continue;int start=CharacterSkeleton.START[bone],end=CharacterSkeleton.END[bone];
            float rad=CharacterSkeleton.radiusFor(a,bone);if(start==end||bone==CharacterSkeleton.HEAD)draw(sphere,pose.x[end],pose.y[end],pose.z[end],rad,rad*1.2f,rad,0,0,0,r,g,b,1,.12f,.45f,.12f);
            else {painter.drawBetween(a.type==GameWorld.TOY_GOLEM?cube:cylinder,pose.x[start],pose.y[start],pose.z[start],pose.x[end],pose.y[end],pose.z[end],rad,rad,r,g,b,1,.16f,.4f,.12f);if(detailed)draw(sphere,pose.x[start],pose.y[start],pose.z[start],rad*1.05f,rad*1.05f,rad*1.05f,0,0,0,r*.7f,g*.7f,b*.7f,1,.5f,.22f,.06f);}
        }
        float hx=pose.x[4],hy=pose.y[4],hz=pose.z[4];eyes(hx,hy,hz+a.radius*.36f,t);
        if(a.type==GameWorld.MIRROR_HARE){for(int side=-1;side<=1;side+=2)draw(sphere,hx+side*.16f,hy+.53f,hz,.11f,.57f,.08f,0,0,side*12,.6f,.80f,.92f,1,.88f,.12f,.15f);}
        if(a.type==GameWorld.BALLOON_KEEPER){for(int n=0;n<6;n++){float bx=x+(n-2.5f)*.34f,bz=z+(float)Math.sin(n*1.9f)*.24f,by=y+3.15f+(float)Math.sin(t*1.3f+n)*.16f;float[] color=SurrealCatalog.COLORS[n];painter.drawBetween(cylinder,pose.x[7],pose.y[7],pose.z[7],bx,by-.3f,bz,.012f,.012f,.74f,.72f,.61f,1,.1f,.8f,0);draw(sphere,bx,by,bz,.28f,.37f,.28f,0,0,0,color[0],color[1],color[2],1,.03f,.22f,.18f);}}
        if(a.type==GameWorld.TOY_GOLEM)for(int n=-1;n<=1;n++)draw(toy(110+n,false),x+n*.33f,y+1.9f,z+.40f,.19f,.17f,.23f,0,0,0,1,1,1,1,.03f,.35f,.10f);
    }
    private void eyes(float x,float y,float z,float t){for(int s=-1;s<=1;s+=2){draw(sphere,x+s*.13f,y,z,.115f,.13f,.06f,0,0,0,.91f,.88f,.74f,1,.02f,.3f,.16f);draw(sphere,x+s*.13f,y,z+.05f,.042f,.065f,.025f,0,0,0,.02f,.035f,.09f,1,.02f,.2f,0);}}
    public void spirit(GameWorld.Actor a,float t){
        GLES30.glDepthMask(false);draw(sphere,a.x,a.y+1.25f,a.z,.42f,.94f,.42f,0,a.yaw*57.3f,0,.12f,.62f,.90f,.82f,.03f,.26f,.65f);draw(sphere,a.x,a.y+2.05f,a.z,.23f,.28f,.23f,0,0,0,.45f,.82f,1f,.91f,.01f,.3f,.45f);for(int n=0;n<5;n++){float q=t+n*1.256f;draw(sphere,a.x+(float)Math.sin(q)*.55f,a.y+.75f+(float)Math.sin(t*1.5f+n)*.45f,a.z+(float)Math.cos(q)*.55f,.07f,.13f,.07f,0,0,0,.13f,.68f,1f,.8f,.02f,.3f,.7f);}GLES30.glDepthMask(true);
    }
    public void release(){for(int i=0;i<toys.length;i++)for(int j=0;j<2;j++)if(toys[i][j]!=null)toys[i][j].release();for(int i=0;i<pickups.length;i++)for(int j=0;j<2;j++)if(pickups[i][j]!=null)pickups[i][j].release();ring.release();}
}
