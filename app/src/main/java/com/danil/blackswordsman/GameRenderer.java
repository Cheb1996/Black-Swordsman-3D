package com.danil.blackswordsman;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** OpenGL ES 3.2 third-person renderer with an HDR-like scene pass and cinematic post pass. */
public final class GameRenderer implements GLSurfaceView.Renderer {
    private final GameWorld world;private final android.content.res.AssetManager assets;
    private final AdaptivePerformance performance;
    private final FeedbackEngine feedback;
    private HudView hud;

    private ShaderProgram scene;
    private ShaderProgram post;
    private Mesh cube, plane, sphere, lowSphere, cylinder, cone, quad;
    private SurrealRenderer surrealRenderer;
    private WaterRenderer water;private int uWater,uHorizon;
    private TerrainRenderer terrain;private MaterialTexture materials;private FloraRenderer floraRenderer;private CloudRenderer clouds;private int uNormals,uSurface,surfaceOverride=-1;
    private final java.util.HashMap<String,SkinGeometry> skins=new java.util.HashMap<String,SkinGeometry>();private final java.util.HashMap<String,Mesh> skinMeshes=new java.util.HashMap<String,Mesh>();
    private final float[] palette=new float[256];private int uSkin,uBones,uHidden,uTexture;
    private final float[] projection=new float[16];
    private final float[] view=new float[16];
    private final float[] viewProjection=new float[16];
    private final float[] model=new float[16];
    private int uModel,uViewProjection,uCamera,uColor,uMaterial,uTime,uLightDir,uTorch,uFogColor;
    private int pTexture,pTime,pDamage,pRage,pResolution,pQuality,pDream;
    private int width,height,fboWidth,fboHeight,fboQuality=-1;
    private final int[] fbo=new int[1],sceneTexture=new int[1],depthBuffer=new int[1];
    private boolean fboReady;
    private long previousFrame;
    private long fpsClock;
    private int fpsFrames;
    private float cameraX,cameraY=3.5f,cameraZ;
    private float targetX,targetY=1f,targetZ;
    private float renderTime;
    private float frameDurationNs=16666667f;
    private Mesh neutralMesh;private final CharacterSkeleton.Pose animalPose=new CharacterSkeleton.Pose();
    private final CharacterSkeleton.Pose skeletonPose=new CharacterSkeleton.Pose();

    public GameRenderer(android.content.Context context,GameWorld world,AdaptivePerformance performance,FeedbackEngine feedback){
        assets=context.getAssets();
        this.world=world;this.performance=performance;this.feedback=feedback;
    }
    public void setHud(HudView value){hud=value;}

    @Override public void onSurfaceCreated(GL10 unused,EGLConfig config){
        skins.clear();skinMeshes.clear();fbo[0]=sceneTexture[0]=depthBuffer[0]=0;fboReady=false;fboQuality=-1;
        String version=GLES30.glGetString(GLES30.GL_VERSION);
        boolean es32=version!=null&&version.contains("3.2");
        world.glLabel=es32?"OPENGL ES 3.2":"OPENGL ES 3.x FALLBACK";
        String shaderVersion=es32?"#version 320 es\n":"#version 300 es\n";
        scene=new ShaderProgram(shaderVersion+SCENE_VERTEX,shaderVersion+SCENE_FRAGMENT);
        post=new ShaderProgram(shaderVersion+POST_VERTEX,shaderVersion+POST_FRAGMENT);
        cube=PrimitiveFactory.cube();plane=PrimitiveFactory.plane();sphere=PrimitiveFactory.sphere(18,12);
        lowSphere=PrimitiveFactory.sphere(8,6);cylinder=PrimitiveFactory.cylinder(16,1f);
        cone=PrimitiveFactory.cylinder(16,0f);quad=PrimitiveFactory.quad();
        surrealRenderer=new SurrealRenderer(assets,this,cube,sphere,cylinder,cone,PrimitiveFactory.torus());
        cacheUniforms();water=new WaterRenderer(this);terrain=new TerrainRenderer(this);materials=new MaterialTexture(assets);floraRenderer=new FloraRenderer(assets,this);clouds=new CloudRenderer(shaderVersion);performance.bindRenderThread();
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        GLES30.glEnable(GLES30.GL_CULL_FACE);GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glEnable(GLES30.GL_BLEND);GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,GLES30.GL_ONE_MINUS_SRC_ALPHA);
        previousFrame=System.nanoTime();fpsClock=previousFrame;
    }

    private void cacheUniforms(){
        uWater=scene.uniform("uWater");uHorizon=scene.uniform("uHorizon");uSkin=scene.uniform("uSkin");uBones=scene.uniform("uBones");uHidden=scene.uniform("uHidden");uTexture=scene.uniform("uDetail");uNormals=scene.uniform("uNormals");uSurface=scene.uniform("uSurface");
        uModel=scene.uniform("uModel");uViewProjection=scene.uniform("uViewProjection");uCamera=scene.uniform("uCamera");
        uColor=scene.uniform("uColor");uMaterial=scene.uniform("uMaterial");uTime=scene.uniform("uTime");
        uLightDir=scene.uniform("uLightDir");uTorch=scene.uniform("uTorch");uFogColor=scene.uniform("uFogColor");
        pTexture=post.uniform("uScene");pTime=post.uniform("uTime");pDamage=post.uniform("uDamage");
        pDream=post.uniform("uDream");pRage=post.uniform("uRage");pResolution=post.uniform("uResolution");pQuality=post.uniform("uQuality");
    }

    @Override public void onSurfaceChanged(GL10 unused,int w,int h){
        width=Math.max(1,w);height=Math.max(1,h);fboQuality=-1;
        Matrix.perspectiveM(projection,0,58f,(float)width/height,.12f,2400f);
    }

    @Override public void onDrawFrame(GL10 unused){
        long start=System.nanoTime();
        float dt=Math.min(.25f,Math.max(.001f,(start-previousFrame)*1.0e-9f));previousFrame=start;
        renderTime+=dt;world.update(dt);surrealRenderer.prewarm(5);updateCamera(dt);
        ensureFrameBuffer();
        if(fboReady){GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,fbo[0]);GLES30.glViewport(0,0,fboWidth,fboHeight);}else{GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);GLES30.glViewport(0,0,width,height);}
        renderScene();
        if(fboReady)renderPost();
        feedback.poll(world);
        if(hud!=null)hud.postInvalidateOnAnimation();
        fpsFrames++;if(start-fpsClock>=1000000000L){world.fps=fpsFrames;fpsFrames=0;fpsClock=start;}
        frameDurationNs=System.nanoTime()-start;
        performance.setTargetFrameNs(1000000000L/world.settings.fps);
        long spare=1000000000L/world.settings.fps-(System.nanoTime()-start);if(spare>1000000L)java.util.concurrent.locks.LockSupport.parkNanos(Math.min(spare,32000000L));
        int level=performance.reportFrame((long)frameDurationNs,(long)(dt*1e9));if(level!=world.qualityLevel)world.setQualityLevel(level);
    }

    private void updateCamera(float dt){
        float yaw=world.cameraYaw,pitch=world.cameraPitch;
        float distance=world.cameraDistance;world.cameraDistance+=(8.4f-world.cameraDistance)*Math.min(1f,dt*3.2f);
        float focusX=world.player.x,focusZ=world.player.z,focusY=world.player.y+Math.max(.65f,world.player.height*.55f);
        if(world.mode==GameWorld.MENU){yaw=renderTime*.10f+.55f;pitch=.30f;distance=10.7f;focusX=0f;focusZ=2f;}
        else if(world.mode==GameWorld.STORY){yaw=world.cameraYaw+renderTime*.035f;distance=9.4f;}
        float horizontal=distance*(float)Math.cos(pitch),desiredY=focusY+distance*(float)Math.sin(pitch)+1.15f;
        float desiredX=focusX-(float)Math.sin(yaw)*horizontal,desiredZ=focusZ-(float)Math.cos(yaw)*horizontal;
        float hit=world.physics.raycast(focusX,focusY,focusZ,desiredX,desiredY,desiredZ,.28f,null,false);
        if(hit<=1){float t=Math.max(.04f,hit-.025f);desiredX=focusX+(desiredX-focusX)*t;desiredY=focusY+(desiredY-focusY)*t;desiredZ=focusZ+(desiredZ-focusZ)*t;}
        float shake=world.settings.shake*world.damageFlash*.12f*(float)Math.sin(renderTime*79f);
        float response=hit<=1?1f:1f-(float)Math.exp(-dt*7.5f);
        cameraX+=(desiredX+shake-cameraX)*response;cameraY+=(desiredY-cameraY)*response;cameraZ+=(desiredZ-shake-cameraZ)*response;
        targetX+=(focusX-targetX)*response;targetY+=(focusY-targetY)*response;targetZ+=(focusZ-targetZ)*response;
        Matrix.setLookAtM(view,0,cameraX,cameraY,cameraZ,targetX,targetY,targetZ,0,1,0);
        Matrix.multiplyMM(viewProjection,0,projection,0,view,0);
    }

    private void renderScene(){
        int env=world.currentChapter().environment;
        float dusk=(env==8)?0.015f:(env==3?.026f:.040f);
        GLES30.glClearColor(dusk,dusk*.72f,dusk*.85f,1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT|GLES30.GL_DEPTH_BUFFER_BIT|GLES30.GL_STENCIL_BUFFER_BIT);
        clouds.draw(viewProjection,cameraX,cameraY,cameraZ,renderTime,world.qualityLevel,fboReady?fboWidth:width,fboReady?fboHeight:height);scene.use();GLES30.glUniform1i(uWater,0);GLES30.glUniform1i(uHorizon,0);GLES30.glUniform1i(uSkin,0);materials.bind();GLES30.glUniform1i(uTexture,1);GLES30.glUniform1i(uNormals,2);GLES30.glUniform1i(uSurface,SurfaceLibrary.STONE);
        GLES30.glUniformMatrix4fv(uViewProjection,1,false,viewProjection,0);
        GLES30.glUniform3f(uCamera,cameraX,cameraY,cameraZ);
        GLES30.glUniform1f(uTime,renderTime);
        GLES30.glUniform3f(uLightDir,-.40f,.17f,.88f);
        GLES30.glUniform3f(uTorch,world.player.x,world.player.y+1.35f,world.player.z);
        if(env==8)GLES30.glUniform3f(uFogColor,.036f,.008f,.047f);else if(env==5||env==6)GLES30.glUniform3f(uFogColor,.012f,.018f,.020f);else if(env==1||env==4)GLES30.glUniform3f(uFogColor,.027f,.018f,.019f);else GLES30.glUniform3f(uFogColor,.018f,.015f,.023f);
        renderEnvironment(env);
        if(world.mode!=GameWorld.MENU||world.cinematicTime>0f)drawActor(world.player,true);
        for(int i=0;i<world.enemies.size();i++)drawActor(world.enemies.get(i),false);
        for(int i=0;i<world.wildlife.size();i++)drawNeutral(world.wildlife.get(i));
        if(world.mode==GameWorld.PLAYING||world.mode==GameWorld.PAUSED||world.mode==GameWorld.GAME_OVER){renderPhysics();surface(-1);}
        if(world.mode==GameWorld.PLAYING||world.mode==GameWorld.PAUSED||world.mode==GameWorld.GAME_OVER)surrealRenderer.environment(world);
        floraRenderer.draw(world,renderTime);water.draw(world);renderProjectiles();renderParticles();
    }

    void surface(int value){surfaceOverride=value;}
    private void setSurface(float metal,float rough){GLES30.glUniform1i(uSurface,surfaceOverride>=0?surfaceOverride:metal>.5f?SurfaceLibrary.METAL:rough>.75f?SurfaceLibrary.BARK:SurfaceLibrary.STONE);}
    void waterMode(boolean on){GLES30.glUniform1i(uWater,on?1:0);GLES30.glUniform1f(uTime,on?world.physics.waterTime():renderTime);}
    void waterHorizon(boolean on){GLES30.glUniform1i(uHorizon,on?1:0);}
    private void renderEnvironment(int env){
        float r=.10f,g=.095f,b=.09f;
        if(env==1||env==4){r=.12f;g=.105f;b=.095f;}else if(env==2||env==7){r=.085f;g=.075f;b=.07f;}else if(env==5||env==6){r=.065f;g=.068f;b=.07f;}else if(env==8){r=.055f;g=.018f;b=.062f;}
        terrain.draw(world,env,r,g,b);if(env==8)renderVoid();
    }

    private void renderVoid(){
        for(int n=0;n<22;n++){
            float a=n*2.39996f+renderTime*.035f,rad=3.2f+n*.55f,y=.25f+(n%6)*.48f;
            float x=(float)Math.sin(a)*rad,z=(float)Math.cos(a)*rad;
            draw(lowSphere,x,y,z,.18f+(n%3)*.1f,.18f,.18f,0,0,0,.20f,.015f,.25f,.85f,.05f,.7f,1.5f);
        }
        for(int n=0;n<5;n++){
            float a=n*1.256f+renderTime*.08f;
            draw(cone,(float)Math.sin(a)*11f,2.5f,(float)Math.cos(a)*11f,1.2f,2.6f,1.2f,0,-a*57.3f,180,.075f,.018f,.09f,1,.08f,.68f,.35f);
        }
    }

    private void drawActor(GameWorld.Actor a,boolean player){
        if(a.ragdollSpawned)return;
        float actorDx=a.x-cameraX,actorDz=a.z-cameraZ,actorLimit=world.qualityLevel==3?58f:world.qualityLevel==2?47f:36f;if(!player&&actorDx*actorDx+actorDz*actorDz>actorLimit*actorLimit)return;
        CharacterSkeleton.sample(a,world.cinematicTime,skeletonPose);CharacterSkeleton.groundFeet(a,skeletonPose,world.physics);
        if(a.surreal){surface(SurfaceLibrary.actor(a.type,0));surrealRenderer.oddity(a,skeletonPose,world.surreal.time,actorDx*actorDx+actorDz*actorDz<400);surface(-1);return;}
        if(a.player&&a.type==GameWorld.WRAITH){surrealRenderer.spirit(a,world.surreal.time);return;}
        player=player&&a.type==-1;
        float rr=a.hitFlash>0f?1f:a.colorR,gg=a.hitFlash>0f?.22f:a.colorG,bb=a.hitFlash>0f?.18f:a.colorB;
        float cdx=a.x-cameraX,cdz=a.z-cameraZ;boolean detailed=player||world.qualityLevel>=2&&cdx*cdx+cdz*cdz<310f;
        GLES30.glDepthMask(false);draw(lowSphere,a.x,a.y+.022f,a.z,a.radius*1.18f,.022f,a.radius*.90f,0,0,0,0,0,0,.34f,0,1,0);GLES30.glDepthMask(true);
        drawWeighted(a,skeletonPose,detailed,rr,gg,bb,a.severedMask);
        if(a.type>=GameWorld.TUSK_BOAR)drawIslandCreature(a,detailed);else if(detailed)drawArmorAndDetails(a,rr,gg,bb,player);
        if(CharacterSkeleton.visible(a,CharacterSkeleton.R_HAND)&&PhysicsWorld.carriesWeapon(a))drawHeldWeapon(a,player);
        if(a.strikePending&&!a.player||a==world.lockedTarget){GLES30.glDepthMask(false);draw(lowSphere,a.x,a.y+.05f,a.z,a.radius*1.5f,.04f,a.radius*1.5f,0,0,0,1,a==world.lockedTarget?.7f:.05f,.05f,.75f,0,1,1);GLES30.glDepthMask(true);}
    }

    private void drawIslandCreature(GameWorld.Actor a,boolean detailed){
        float h=a.height,q=a.radius,s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw),hx=skeletonPose.x[4],hy=skeletonPose.y[4],hz=skeletonPose.z[4];
        if(a.type==GameWorld.ALIEN){if(CharacterSkeleton.visible(a,CharacterSkeleton.HEAD)){draw(sphere,hx,hy+.08f,hz,q*.70f,q*.8f,q*.59f,0,a.yaw*57.3f,0,.50f,.62f,.53f,1,.06f,.35f,0);for(int side=-1;side<=1;side+=2)draw(sphere,hx+c*side*q*.31f+s*q*.45f,hy+.10f,hz-s*side*q*.31f+c*q*.45f,q*.20f,q*.30f,q*.12f,0,a.yaw*57.3f,side*-20,.018f,.024f,.035f,1,.5f,.12f,.08f);}draw(lowSphere,a.x,a.y+h*.65f,a.z,q*.38f,q*.25f,q*.55f,0,a.yaw*57.3f,0,.11f,.35f,.34f,1,.9f,.23f,.1f);}
        else if(a.category==CreatureSystem.MECHANICAL){
            for(int bone=0;bone<CharacterSkeleton.BONE_COUNT;bone++){if(!CharacterSkeleton.visible(a,bone))continue;int j=CharacterSkeleton.END[bone];if(bone>3&&!detailed&&bone%3!=1)continue;draw(lowSphere,skeletonPose.x[j],skeletonPose.y[j],skeletonPose.z[j],q*.18f,q*.18f,q*.18f,0,0,0,.16f,.20f,.23f,1,.95f,.18f,0);}
            draw(cube,a.x,a.y+h*.55f,a.z,q*.82f,h*.20f,q*.65f,0,a.yaw*57.3f,0,.20f,.27f,.32f,1,.9f,.25f,0);
            draw(sphere,hx+s*q*.4f,hy,hz+c*q*.4f,q*.23f,q*.20f,q*.16f,0,0,0,.07f,.85f,.94f,1,.1f,.13f,2);
            if(a.type==GameWorld.SENTRY)for(int i=-1;i<=1;i++)drawBetween(cylinder,hx+c*i*.16f,hy,hz-s*i*.16f,hx+c*i*.16f+s*1.05f,hy,hz-s*i*.16f+c*1.05f,.065f,.065f,.13f,.16f,.19f,1,.92f,.2f,0);
            if(a.type==GameWorld.SCAN_DRONE){for(int i=0;i<3;i++){float angle=i*2.094f+renderTime*3;draw(cube,a.x+(float)Math.sin(angle)*q,a.y+h*.7f,a.z+(float)Math.cos(angle)*q,.12f,.03f,.6f,0,angle*57.3f,0,.21f,.3f,.35f,1,.8f,.2f,0);}}
        }else if(a.type==GameWorld.TUSK_BOAR){for(int side=-1;side<=1;side+=2)draw(cone,hx+c*side*q*.35f+s*q*.5f,hy+.08f,hz-s*side*q*.35f+c*q*.5f,.1f,.37f,.1f,-30,a.yaw*57.3f,side*15,.76f,.68f,.45f,1,.05f,.65f,0);}
        else if(a.type==GameWorld.MIRE_SPITTER){for(int i=0;i<5;i++)draw(sphere,a.x+(float)Math.sin(i*1.25f)*q*.65f,a.y+h*.7f,a.z+(float)Math.cos(i*1.25f)*q*.5f,.2f,.25f,.2f,0,0,0,.25f,.65f,.06f,1,0,.27f,.3f);}
        else if(a.type==GameWorld.STORM_SPIRIT||a.type==GameWorld.GROVE_WARDEN){for(int i=0;i<5;i++){float angle=i*1.2566f+renderTime;draw(lowSphere,a.x+(float)Math.sin(angle)*q*1.3f,a.y+h*.6f+(float)Math.sin(angle*2)*.5f,a.z+(float)Math.cos(angle)*q*1.3f,.065f,.14f,.065f,0,0,0,a.colorR,a.colorG,a.colorB,1,0,.2f,2.8f);}if(a.type==GameWorld.GROVE_WARDEN)for(int side=-1;side<=1;side+=2)drawBetween(cone,hx,hy,hz,hx+c*side*.75f,hy+.85f,hz-s*side*.75f,.18f,.13f,.24f,.37f,.13f,1,0,.9f,0);}
        if(a.shieldTime>0){GLES30.glDepthMask(false);draw(sphere,a.x,a.y+h*.5f,a.z,q*1.4f,h*.6f,q*1.4f,0,0,0,.1f,.8f,.55f,.19f,.1f,.2f,1.5f);GLES30.glDepthMask(true);}
        if(a.abilityState==1){float x=a.type==GameWorld.STORM_SPIRIT?a.markX:a.x,z=a.type==GameWorld.STORM_SPIRIT?a.markZ:a.z,radius=a.type==GameWorld.STORM_SPIRIT?2.5f:q*1.8f;GLES30.glDepthMask(false);draw(lowSphere,x,world.physics.terrainHeight(x,z)+.045f,z,radius,.03f,radius,0,0,0,.95f,.16f,.08f,.4f,0,1,1);GLES30.glDepthMask(true);}
    }

    private void drawWeighted(GameWorld.Actor a,CharacterSkeleton.Pose pose,boolean detailed,float r,float g,float b,int hidden){
        String key=a.type+":"+a.height+":"+a.radius+":"+detailed;SkinGeometry data=skins.get(key);Mesh mesh=skinMeshes.get(key);
        if(data==null){data=new SkinGeometry(a,detailed);mesh=new Mesh(data.vertices,data.indices,16);skins.put(key,data);skinMeshes.put(key,mesh);}
        data.palette(pose,a.yaw,palette);if(a.ragdollSpawned)data.paletteBodies(world.physics.bodies,a,palette);Matrix.setIdentityM(model,0);GLES30.glUniformMatrix4fv(uModel,1,false,model,0);GLES30.glUniformMatrix4fv(uBones,16,false,palette,0);GLES30.glUniform1i(uSkin,1);GLES30.glUniform1i(uHidden,hidden);GLES30.glUniform4f(uColor,r,g,b,1);GLES30.glUniform3f(uMaterial,a.metalness,a.roughness,a.rageTime>0?.2f:0);mesh.draw();GLES30.glUniform1i(uSkin,0);
    }

    private void drawArmorAndDetails(GameWorld.Actor a,float r,float g,float b,boolean player){
        boolean beast=CharacterSkeleton.isBeast(a.type);
        if(!beast){
            // Separate lower/upper cuirass plates are bound to their respective torso bones.
            drawBoneOverlay(a,CharacterSkeleton.LOWER_TORSO,cube,a.radius*.66f,a.radius*.58f,r*.65f+.05f,g*.65f+.05f,b*.68f+.06f,Math.max(.52f,a.metalness),.32f);
            drawBoneOverlay(a,CharacterSkeleton.UPPER_TORSO,cube,a.radius*.82f,a.radius*.70f,r*.58f+.07f,g*.58f+.07f,b*.62f+.08f,Math.max(.58f,a.metalness),.27f);
            // Shoulder plates sit on the skeleton joints instead of floating from the root actor.
            for(int joint=5;joint<=8;joint+=3)draw(lowSphere,skeletonPose.x[joint],skeletonPose.y[joint],skeletonPose.z[joint],a.radius*.30f,a.radius*.16f,a.radius*.34f,0,0,0,r*.52f+.08f,g*.52f+.08f,b*.56f+.09f,1,.72f,.25f,0);
            // Belt, buckle and layered boots.
            drawBoneOverlay(a,CharacterSkeleton.PELVIS,cube,a.radius*.68f,a.radius*.49f,.065f,.05f,.04f,.32f,.68f);
            float[] feet={skeletonPose.x[13],skeletonPose.y[13],skeletonPose.z[13],skeletonPose.x[16],skeletonPose.y[16],skeletonPose.z[16]};
            for(int i=0;i<2;i++)draw(lowSphere,feet[i*3],feet[i*3+1],feet[i*3+2],a.radius*.28f,a.radius*.15f,a.radius*.42f,0,a.yaw*57.2958f,0,.055f,.045f,.04f,1,.18f,.86f,0);
            if(player){
                // Three articulated cape plates, hair and prosthetic-arm metal shell.
                float s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw);
                surface(SurfaceLibrary.CLOTH);for(int n=-1;n<=1;n++){float ax=skeletonPose.x[3]+c*n*.21f-s*.25f,ay=skeletonPose.y[3],az=skeletonPose.z[3]-s*n*.21f-c*.25f;for(int k=0;k<3;k++){float lag=(.07f+a.animationSpeed*.14f)*k+(float)Math.sin(renderTime*3-k*.8f+n)*.025f,bx=ax-s*lag,by=ay-.34f,bz=az-c*lag;drawBetween(cube,ax,ay,az,bx,by,bz,.21f,.02f,.025f,.022f,.03f,1,.02f,.96f,0);ax=bx;ay=by;az=bz;}}surface(-1);
                drawBoneOverlay(a,CharacterSkeleton.L_FOREARM,cylinder,a.radius*.23f,a.radius*.23f,.16f,.17f,.18f,.88f,.20f);
                for(int n=-2;n<=2;n++){float angle=a.yaw+n*.46f;draw(cone,skeletonPose.x[4]+(float)Math.sin(angle)*a.radius*.18f,skeletonPose.y[4]+a.radius*.28f,skeletonPose.z[4]+(float)Math.cos(angle)*a.radius*.18f,.07f,.24f,.07f,0,angle*57.2958f,0,.035f,.028f,.025f,1,.05f,.92f,0);}
            }
            for(int hand:new int[]{7,10}){int bone=hand==7?CharacterSkeleton.L_HAND:CharacterSkeleton.R_HAND;if(!CharacterSkeleton.visible(a,bone))continue;float sn=(float)Math.sin(a.yaw),cs=(float)Math.cos(a.yaw);for(int f=0;f<4;f++){float fx=skeletonPose.x[hand]+cs*(f-1.5f)*a.radius*.07f,fz=skeletonPose.z[hand]-sn*(f-1.5f)*a.radius*.07f,fy=skeletonPose.y[hand];float curl=a.attackTime>0?.045f:.02f;drawBetween(cylinder,fx,fy,fz,fx+sn*curl,fy-a.radius*.14f,fz+cs*curl,a.radius*.024f,a.radius*.024f,.30f,.22f,.18f,1,.02f,.7f,0);drawBetween(cylinder,fx+sn*curl,fy-a.radius*.14f,fz+cs*curl,fx+sn*curl*2,fy-a.radius*.22f,fz+cs*curl*2,a.radius*.021f,a.radius*.021f,.28f,.20f,.16f,1,.02f,.7f,0);}}
            drawFaceAndEquipment(a,player);
        }else{
            // Layered back muscles/plates follow the beast spine.
            for(int joint=0;joint<=3;joint++)draw(lowSphere,skeletonPose.x[joint],skeletonPose.y[joint]+a.radius*.12f,skeletonPose.z[joint],a.radius*(.72f-joint*.07f),a.radius*.25f,a.radius*.48f,0,0,0,r*.76f,g*.68f,b*.70f,1,.08f,.74f,.06f);
        }
        if(a.boss||a.type==GameWorld.VOID_BEAST){
            float hx=skeletonPose.x[4],hy=skeletonPose.y[4],hz=skeletonPose.z[4];float s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw);
            for(int side=-1;side<=1;side+=2)draw(cone,hx+c*side*a.radius*.34f,hy+a.radius*.52f,hz-s*side*a.radius*.34f,.16f,.58f,.16f,0,a.yaw*57.2958f,side*20,.14f,.085f,.075f,1,.22f,.57f,.12f);
        }
    }

    /** Extra facial, armour and creature-specific layers attached to sampled skeleton joints. */
    private void drawFaceAndEquipment(GameWorld.Actor a,boolean player){
        if(!CharacterSkeleton.visible(a,CharacterSkeleton.HEAD))return;float hx=skeletonPose.x[4],hy=skeletonPose.y[4],hz=skeletonPose.z[4],s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw),side=a.radius*.15f;
        for(int eye=-1;eye<=1;eye+=2){float ex=hx+c*side*eye+s*a.radius*.30f,ez=hz-s*side*eye+c*a.radius*.30f;float er=player&&eye<0?.015f:.032f;draw(lowSphere,ex,hy+a.radius*.06f,ez,er,er*.65f,er,0,0,0,player&&eye<0?.025f:.52f,player&&eye<0?.018f:.055f,player&&eye<0?.02f:.035f,1,.04f,.45f,player&&eye>0?.18f:0);}
        draw(cone,hx+s*a.radius*.36f,hy-a.radius*.035f,hz+c*a.radius*.36f,a.radius*.075f,a.radius*.16f,a.radius*.075f,90,a.yaw*57.2958f,0,.30f,.22f,.18f,1,.02f,.78f,0);
        if(player){draw(cube,hx-c*side+s*a.radius*.315f,hy+a.radius*.07f,hz+s*side+c*a.radius*.315f,a.radius*.19f,a.radius*.055f,a.radius*.025f,0,a.yaw*57.2958f,-8,.035f,.028f,.026f,1,.22f,.72f,0);}
        float tx=skeletonPose.x[3],ty=skeletonPose.y[3],tz=skeletonPose.z[3];for(int n=-1;n<=1;n++){float px=tx+c*n*a.radius*.36f+s*a.radius*.47f,pz=tz-s*n*a.radius*.36f+c*a.radius*.47f;draw(lowSphere,px,ty+n*.025f,pz,a.radius*.055f,a.radius*.055f,a.radius*.055f,0,0,0,.32f,.31f,.30f,1,.78f,.23f,0);}
        if(a.type==GameWorld.CROSSBOW){float bx=a.x-c*a.radius*.72f-s*.18f,bz=a.z+s*a.radius*.72f-c*.18f;for(int n=0;n<4;n++)drawBetween(cylinder,bx,ty-n*.03f,bz,bx-s*.55f+c*(n-1.5f)*.07f,ty+.65f,bz-c*.55f-s*(n-1.5f)*.07f,.025f,.025f,.24f,.21f,.17f,1,.68f,.34f,0);}
        if(a.type==GameWorld.ZEALOT||a.type==GameWorld.JAILER)draw(cone,hx,hy+a.radius*.16f,hz,a.radius*.53f,a.radius*.62f,a.radius*.53f,0,a.yaw*57.2958f,180,a.colorR*.72f,a.colorG*.70f,a.colorB*.68f,1,.03f,.94f,0);
        if(a.type==GameWorld.SKELETON||a.type==GameWorld.BONE_KNIGHT)for(int rib=-2;rib<=2;rib++)drawBetween(cylinder,tx-c*a.radius*.46f,ty+rib*a.radius*.12f,tz+s*a.radius*.46f,tx+c*a.radius*.46f,ty+rib*a.radius*.12f,tz-s*a.radius*.46f,.025f,.025f,.58f,.55f,.48f,1,.03f,.89f,0);
    }

    private void drawNeutral(GameWorld.Neutral n){surface(n.magical?SurfaceLibrary.ASTRAL:n.type==GameWorld.FISH||n.type==GameWorld.TURTLE||n.type==GameWorld.LIZARD?SurfaceLibrary.SCALES:SurfaceLibrary.FUR);neutralMesh=(n.x-cameraX)*(n.x-cameraX)+(n.z-cameraZ)*(n.z-cameraZ)<324?sphere:lowSphere;drawNeutralModel(n);surface(-1);}
    private void drawNeutralModel(GameWorld.Neutral n){
        float dx=n.x-cameraX,dz=n.z-cameraZ,neutralLimit=world.qualityLevel==3?55f:world.qualityLevel==2?43f:32f;if(dx*dx+dz*dz>neutralLimit*neutralLimit&&!n.companion)return;
        float q=n.scale,ground=n.y,s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw);
        if(n.type==GameWorld.ELF){drawElf(n);return;}if(n.type==GameWorld.WISP){drawWisp(n);return;}if(n.type==GameWorld.FOREST_SPIRIT){drawForestSpirit(n);return;}if(n.type==GameWorld.CROW||n.type==GameWorld.GULL){drawCrow(n);return;}
        if(n.type>=GameWorld.FISH){drawIslandAnimal(n);return;}
        GLES30.glDepthMask(false);draw(neutralMesh,n.x,ground+.018f,n.z,.62f*q,.018f,.42f*q,0,0,0,0,0,0,.22f,0,1,0);GLES30.glDepthMask(true);
        if(n.type==GameWorld.HARE){
            draw(neutralMesh,n.x,ground+.27f*q,n.z,.27f*q,.24f*q,.38f*q,0,n.yaw*57.2958f,0,.30f,.25f,.20f,1,.01f,.94f,0);float fx=n.x+s*.32f*q,fz=n.z+c*.32f*q;draw(neutralMesh,fx,ground+.47f*q,fz,.20f*q,.21f*q,.20f*q,0,0,0,.34f,.29f,.24f,1,.01f,.92f,0);for(int side=-1;side<=1;side+=2)draw(cone,fx+c*side*.10f*q,ground+.77f*q,fz-s*side*.10f*q,.07f*q,.28f*q,.07f*q,0,n.yaw*57.2958f,0,.32f,.27f,.23f,1,.01f,.94f,0);smallLegs(n,.22f*q,.29f*q,.11f*q,.25f,.20f,.16f);return;
        }
        if(n.type==GameWorld.RAT){
            draw(neutralMesh,n.x,ground+.13f*q,n.z,.19f*q,.13f*q,.34f*q,0,n.yaw*57.2958f,0,.20f,.18f,.17f,1,.01f,.95f,0);float hx=n.x+s*.29f*q,hz=n.z+c*.29f*q;draw(neutralMesh,hx,ground+.17f*q,hz,.13f*q,.12f*q,.15f*q,0,0,0,.22f,.19f,.18f,1,.01f,.92f,0);drawBetween(cylinder,n.x-s*.30f*q,ground+.14f*q,n.z-c*.30f*q,n.x-s*.78f*q+c*.12f*q,ground+.10f*q,n.z-c*.78f*q-s*.12f*q,.025f*q,.025f*q,.26f,.16f,.16f,1,.01f,.93f,0);smallLegs(n,.11f*q,.22f*q,.06f*q,.21f,.18f,.16f);return;
        }
        drawQuadruped(n,s,c,q,ground);
    }

    private void drawQuadruped(GameWorld.Neutral n,float s,float c,float q,float ground){
        boolean large=n.type==GameWorld.HORSE||n.type==GameWorld.DEER;float bodyH=(large?.78f:n.type==GameWorld.GOAT?.58f:.42f)*q,bodyL=(n.type==GameWorld.HORSE?1.05f:n.type==GameWorld.DEER?.82f:n.type==GameWorld.GOAT?.64f:.58f)*q,width=(large?.34f:.28f)*q;
        float r=n.type==GameWorld.FOX?.50f:n.type==GameWorld.DEER?.35f:n.type==GameWorld.HORSE?.24f:n.type==GameWorld.GOAT?.37f:.27f,g=n.type==GameWorld.FOX?.19f:n.type==GameWorld.DEER?.24f:n.type==GameWorld.HORSE?.19f:n.type==GameWorld.GOAT?.32f:.24f,b=n.type==GameWorld.FOX?.07f:n.type==GameWorld.DEER?.14f:n.type==GameWorld.HORSE?.13f:n.type==GameWorld.GOAT?.25f:.18f;
        draw(neutralMesh,n.x,ground+bodyH,n.z,width*1.05f,bodyH*.47f,bodyL,0,n.yaw*57.2958f,0,r,g,b,1,.01f,.91f,0);draw(neutralMesh,n.x+s*bodyL*.48f,ground+bodyH*1.08f,n.z+c*bodyL*.48f,width*.94f,bodyH*.44f,bodyL*.54f,0,n.yaw*57.2958f,0,r*.96f,g*.96f,b*.96f,1,.01f,.90f,0);
        float neckX=n.x+s*bodyL*.83f,neckZ=n.z+c*bodyL*.83f,headX=n.x+s*bodyL*1.03f,headZ=n.z+c*bodyL*1.03f,headY=ground+bodyH*(large?1.62f:1.35f);drawBetween(cylinder,n.x+s*bodyL*.55f,ground+bodyH*1.15f,n.z+c*bodyL*.55f,neckX,headY-.08f,neckZ,width*.39f,width*.40f,r,g,b,1,.01f,.91f,0);draw(neutralMesh,headX,headY,headZ,width*.64f,width*.60f,bodyL*.31f,0,n.yaw*57.2958f,0,r,g,b,1,.01f,.90f,0);
        for(int side=-1;side<=1;side+=2)for(int fore=-1;fore<=1;fore+=2){float rootX=n.x+c*side*width*.62f+s*fore*bodyL*.53f,rootZ=n.z-s*side*width*.62f+c*fore*bodyL*.53f,gait=(float)Math.sin(n.gait+side*fore)*.17f*q*n.moveBlend;animalLeg(n,rootX,ground+bodyH*.88f,rootZ,rootX+s*gait,ground+.05f*q+Math.max(0,(float)Math.cos(n.gait+side*fore))*.12f*q*n.moveBlend,rootZ+c*gait,width*.17f,r*.72f,g*.72f,b*.72f,fore);}
        for(int side=-1;side<=1;side+=2)draw(cone,headX+c*side*width*.42f,headY+width*.53f,headZ-s*side*width*.42f,width*.18f,width*.45f,width*.14f,0,n.yaw*57.2958f,side*12,r*.9f,g*.9f,b*.9f,1,.01f,.94f,0);
        if(n.type==GameWorld.DEER||n.type==GameWorld.GOAT)for(int side=-1;side<=1;side+=2){float ax=headX+c*side*width*.36f,az=headZ-s*side*width*.36f;drawBetween(cylinder,ax,headY+width*.30f,az,ax+c*side*width*.26f-s*.08f,headY+width*1.35f,az-s*side*width*.26f-c*.08f,width*.07f,width*.06f,.32f,.27f,.20f,1,.02f,.91f,0);if(n.type==GameWorld.DEER)drawBetween(cylinder,ax+c*side*width*.18f,headY+width*.92f,az-s*side*width*.18f,ax+c*side*width*.62f+s*.08f,headY+width*1.15f,az-s*side*width*.62f+c*.08f,width*.045f,width*.04f,.32f,.27f,.20f,1,.02f,.91f,0);}
        for(int eye=-1;eye<=1;eye+=2){draw(neutralMesh,headX+c*eye*width*.48f+s*bodyL*.15f,headY+width*.12f,headZ-s*eye*width*.48f+c*bodyL*.15f,width*.075f,width*.065f,width*.05f,0,n.yaw*57.3f,0,.015f,.02f,.019f,1,.1f,.2f,0);}draw(neutralMesh,headX+s*bodyL*.22f,headY-width*.15f,headZ+c*bodyL*.22f,width*.40f,width*.26f,bodyL*.19f,0,n.yaw*57.3f,0,r*.65f,g*.65f,b*.65f,1,.02f,.7f,0);
        float tailX=n.x-s*bodyL*.84f,tailZ=n.z-c*bodyL*.84f;drawBetween(cylinder,n.x-s*bodyL*.65f,ground+bodyH*1.05f,n.z-c*bodyL*.65f,tailX-s*bodyL*.35f,ground+bodyH*.82f,tailZ-c*bodyL*.35f,width*.15f,width*.14f,r*.75f,g*.75f,b*.75f,1,.01f,.94f,0);
    }

    private void drawCrow(GameWorld.Neutral n){float q=n.scale,s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw),flap=(float)Math.sin(n.phase);float r=n.type==GameWorld.GULL?.61f:.035f,g=n.type==GameWorld.GULL?.64f:.042f,b=n.type==GameWorld.GULL?.67f:.052f;draw(neutralMesh,n.x,n.y,n.z,.18f*q,.15f*q,.34f*q,0,n.yaw*57.2958f,0,r,g,b,1,.04f,.84f,.03f);draw(neutralMesh,n.x+s*.27f*q,n.y+.1f*q,n.z+c*.27f*q,.13f*q,.13f*q,.16f*q,0,n.yaw*57.3f,0,r,g,b,1,.03f,.82f,0);draw(cone,n.x+s*.40f*q,n.y+.09f*q,n.z+c*.40f*q,.045f*q,.15f*q,.045f*q,90,n.yaw*57.2958f,0,.32f,.25f,.12f,1,.04f,.78f,0);for(int side=-1;side<=1;side+=2){float ex=n.x+c*side*.38f*q,ez=n.z-s*side*.38f*q,ey=n.y+flap*.2f*q;painterBirdWing(n,n.x,n.y,n.z,ex,ey,ez,.12f*q,r,g,b);painterBirdWing(n,ex,ey,ez,n.x+c*side*.72f*q,n.y+flap*.35f*q,n.z-s*side*.72f*q,.10f*q,r,g,b);for(int f=0;f<(neutralMesh==sphere?6:3);f++){float spread=.32f+f*.065f;draw(neutralMesh,n.x+c*side*spread*q-s*.10f*q,n.y+flap*spread*.45f*q,n.z-s*side*spread*q-c*.10f*q,.052f*q,.020f*q,(.20f-f*.014f)*q,flap*side*15,n.yaw*57.3f,side*10,r*.75f,g*.75f,b*.75f,1,.02f,.92f,0);}draw(neutralMesh,n.x+c*side*.085f*q+s*.33f*q,n.y+.14f*q,n.z-s*side*.085f*q+c*.33f*q,.022f*q,.022f*q,.022f*q,0,0,0,.01f,.012f,.015f,1,0,.1f,0);}}
    private void painterBirdWing(GameWorld.Neutral n,float ax,float ay,float az,float bx,float by,float bz,float width,float r,float g,float b){drawBetween(cylinder,ax,ay,az,bx,by,bz,width,width*.4f,r,g,b,1,.02f,.89f,0);}

    private void drawIslandAnimal(GameWorld.Neutral n){float s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw),y=n.y;boolean camel=n.type==GameWorld.CAMEL,fish=n.type==GameWorld.FISH,turtle=n.type==GameWorld.TURTLE;float length=camel?1.1f:fish?.32f:n.type==GameWorld.OTTER?.5f:.35f,bodyY=camel?1.35f:fish?0:.16f;
        float r=camel?.52f:turtle?.18f:fish?.2f:.30f,g=camel?.35f:turtle?.34f:fish?.52f:.22f,b=camel?.18f:turtle?.10f:fish?.65f:.15f;
        draw(sphere,n.x,y+bodyY,n.z,camel?.48f:fish?.12f:.24f,camel?.44f:fish?.15f:.16f,length,0,n.yaw*57.3f,0,r,g,b,1,.02f,fish?.3f:.8f,0);
        float headX=n.x+s*length,headZ=n.z+c*length,headY=y+bodyY+(camel?.6f:.03f);
        draw(neutralMesh,headX,headY,headZ,camel?.19f:.12f,camel?.22f:.09f,camel?.3f:.14f,0,n.yaw*57.3f,0,r,g,b,1,.01f,.75f,0);
        if(camel){drawBetween(cylinder,n.x+s*.75f,y+1.3f,n.z+c*.75f,headX,headY,headZ,.16f,.16f,r,g,b,1,0,.85f,0);for(int i=-1;i<=1;i+=2)draw(neutralMesh,n.x+s*i*.36f,y+1.8f,n.z+c*i*.36f,.34f,.4f,.36f,0,0,0,r,g,b,1,0,.9f,0);}
        if(turtle){draw(sphere,n.x,y+.26f,n.z,.33f,.21f,.39f,0,n.yaw*57.3f,0,.14f,.24f,.07f,1,0,.72f,0);for(int i=0;i<7;i++){float a=i*.8976f;draw(neutralMesh,n.x+(float)Math.sin(a)*.22f,y+.41f,n.z+(float)Math.cos(a)*.25f,.08f,.025f,.08f,0,0,0,.28f,.36f,.12f,1,0,.8f,0);}}
        if(fish){draw(cone,n.x-s*length,y,n.z-c*length,.22f,.20f,.035f,90,n.yaw*57.3f+(float)Math.sin(n.phase)*22,0,r,g,b,1,.1f,.3f,0);}else for(int side=-1;side<=1;side+=2)for(int end=-1;end<=1;end+=2){float xx=n.x+c*side*(camel?.3f:.16f)+s*end*length*.6f,zz=n.z-s*side*(camel?.3f:.16f)+c*end*length*.6f,step=(float)Math.sin(n.gait+side*end*1.57f)*(camel?.2f:.06f);animalLeg(n,xx,y+bodyY-.05f,zz,xx+s*step,y+.04f+Math.max(0,(float)Math.cos(n.gait+side*end))*n.moveBlend*(camel?.16f:.025f),zz+c*step,camel?.075f:.035f,r*.8f,g*.8f,b*.8f,end);}
        for(int side=-1;side<=1;side+=2)draw(neutralMesh,headX+c*side*.08f+s*.08f,headY+.045f,headZ-s*side*.08f+c*.08f,.022f,.022f,.022f,0,0,0,.025f,.022f,.018f,1,.04f,.3f,0);
    }

    private void drawElf(GameWorld.Neutral n){float q=n.scale*.42f,s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw);GLES30.glDepthMask(false);for(int side=-1;side<=1;side+=2){float wing=(float)Math.sin(n.phase*2f+side)*.08f;draw(neutralMesh,n.x+c*side*.24f*q-s*.06f,n.y+wing,n.z-s*side*.24f*q-c*.06f,.26f*q,.42f*q,.055f*q,0,n.yaw*57.2958f,side*24,.34f,.76f,.92f,.52f,.01f,.18f,2.1f);}GLES30.glDepthMask(true);draw(cylinder,n.x,n.y-.12f*q,n.z,.12f*q,.31f*q,.12f*q,0,n.yaw*57.2958f,0,.72f,.58f,.34f,1,.02f,.72f,.35f);draw(neutralMesh,n.x,n.y+.28f*q,n.z,.22f*q,.25f*q,.22f*q,0,0,0,.72f,.58f,.43f,1,.01f,.70f,.28f);for(int side=-1;side<=1;side+=2)drawBetween(cylinder,n.x+c*side*.09f*q,n.y-.15f*q,n.z-s*side*.09f*q,n.x+c*side*.28f*q+s*.08f,n.y-.38f*q,n.z-s*side*.28f*q+c*.08f,.035f*q,.03f*q,.68f,.52f,.35f,1,.01f,.76f,.18f);}

    private void drawWisp(GameWorld.Neutral n){float q=n.scale,pulse=.82f+(float)Math.sin(n.phase*2f)*.16f;GLES30.glDepthMask(false);draw(neutralMesh,n.x,n.y,n.z,.22f*q*pulse,.22f*q*pulse,.22f*q*pulse,0,0,0,.18f,.66f,1f,.72f,.01f,.08f,3.4f);for(int i=0;i<3;i++){float a=n.phase+i*2.094f;draw(neutralMesh,n.x+(float)Math.sin(a)*.34f*q,n.y+(float)Math.sin(a*1.7f)*.13f*q,n.z+(float)Math.cos(a)*.34f*q,.055f*q,.055f*q,.055f*q,0,0,0,.38f,.82f,1f,.65f,.01f,.12f,2.6f);}GLES30.glDepthMask(true);}

    private void drawForestSpirit(GameWorld.Neutral n){float q=n.scale,s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw);GLES30.glDepthMask(false);draw(cylinder,n.x,n.y,n.z,.24f*q,.82f*q,.24f*q,0,n.yaw*57.2958f,0,.18f,.48f,.31f,.82f,.01f,.74f,1.1f);draw(neutralMesh,n.x,n.y+.86f*q,n.z,.29f*q,.32f*q,.29f*q,0,0,0,.25f,.68f,.45f,.78f,.01f,.58f,1.5f);for(int side=-1;side<=1;side+=2){float ax=n.x+c*side*.18f*q+s*.05f,az=n.z-s*side*.18f*q+c*.05f;drawBetween(cylinder,ax,n.y+1.02f*q,az,ax+c*side*.55f*q-s*.12f*q,n.y+1.55f*q,az-s*side*.55f*q-c*.12f*q,.045f*q,.04f*q,.20f,.48f,.27f,.82f,.02f,.82f,.65f);drawBetween(cylinder,ax+c*side*.35f*q,n.y+1.35f*q,az-s*side*.35f*q,ax+c*side*.72f*q+s*.08f*q,n.y+1.47f*q,az-s*side*.72f*q+c*.08f*q,.035f*q,.03f*q,.20f,.48f,.27f,.78f,.02f,.84f,.55f);}GLES30.glDepthMask(true);}

    private void animalLeg(GameWorld.Neutral n,float ax,float ay,float az,float bx,float by,float bz,float radius,float r,float g,float b,int fore){CharacterSkeleton.Pose p=animalPose;p.x[0]=ax;p.y[0]=ay;p.z[0]=az;p.x[2]=bx;p.y[2]=by;p.z[2]=bz;float length=Math.max(.04f,ay-n.y),s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw);AnimationSystem.joint(p,0,1,2,length*.55f,length*.55f,s*fore,0,c*fore);drawBetween(cylinder,p.x[0],p.y[0],p.z[0],p.x[1],p.y[1],p.z[1],radius,radius*.9f,r,g,b,1,.02f,.86f,0);drawBetween(cylinder,p.x[1],p.y[1],p.z[1],p.x[2],p.y[2],p.z[2],radius*.73f,radius*.66f,r,g,b,1,.02f,.86f,0);draw(neutralMesh,p.x[2],p.y[2],p.z[2],radius*1.25f,radius*.7f,radius*1.8f,0,n.yaw*57.3f,0,r*.6f,g*.6f,b*.6f,1,.02f,.75f,0);}
    private void smallLegs(GameWorld.Neutral n,float height,float length,float radius,float r,float g,float b){float s=(float)Math.sin(n.yaw),c=(float)Math.cos(n.yaw);for(int side=-1;side<=1;side+=2)for(int fore=-1;fore<=1;fore+=2){float x=n.x+c*side*radius*1.6f+s*fore*length*.6f,z=n.z-s*side*radius*1.6f+c*fore*length*.6f,step=(float)Math.sin(n.gait+fore*side)*length*.6f*n.moveBlend;animalLeg(n,x,n.y+height,z,x+s*step,n.y+.025f+Math.max(0,(float)Math.cos(n.gait+side*fore))*height*.3f*n.moveBlend,z+c*step,radius*.55f,r,g,b,fore);}}
    private void drawBoneOverlay(GameWorld.Actor a,int bone,Mesh mesh,float radius,float depth,float r,float g,float b,float metal,float rough){
        if(!CharacterSkeleton.visible(a,bone))return;int start=CharacterSkeleton.START[bone],end=CharacterSkeleton.END[bone];
        drawBetween(mesh,skeletonPose.x[start],skeletonPose.y[start],skeletonPose.z[start],skeletonPose.x[end],skeletonPose.y[end],skeletonPose.z[end],radius,depth,r,g,b,1,metal,rough,0);
    }

    private void drawHeldWeapon(GameWorld.Actor a,boolean player){
        float hx=skeletonPose.x[10],hy=skeletonPose.y[10],hz=skeletonPose.z[10],s=(float)Math.sin(a.yaw),c=(float)Math.cos(a.yaw);
        float length=player?2.9f:(a.type==GameWorld.CROSSBOW?.62f:a.boss?1.55f:1.12f);
        float tx=hx+s*length*.32f,ty=hy+length*.82f,tz=hz+c*length*.32f;
        if(a.type==GameWorld.CROSSBOW){tx=hx+s*.62f;ty=hy+.08f;tz=hz+c*.62f;}
        drawBetween(cube,hx,hy,hz,tx,ty,tz,player?.13f:.065f,player?.23f:.10f,player?.24f:.27f,player?.25f:.25f,player?.27f:.22f,1,.91f,.19f,a.rageTime>0?.25f:0);
        // Crossguard and pommel are separate skin pieces on the hand bone.
        float rx=c*(player?.42f:.22f),rz=-s*(player?.42f:.22f);
        drawBetween(cube,hx-rx,hy+.05f,hz-rz,hx+rx,hy+.05f,hz+rz,.045f,.055f,.20f,.17f,.14f,1,.76f,.31f,0);
        draw(lowSphere,hx-s*.14f,hy-.12f,hz-c*.14f,player?.13f:.08f,player?.13f:.08f,player?.13f:.08f,0,0,0,.16f,.12f,.10f,1,.64f,.35f,0);
    }

    void drawBetween(Mesh mesh,float ax,float ay,float az,float bx,float by,float bz,float radius,float depth,float r,float g,float b,float alpha,float metal,float rough,float emissive){
        float dx=bx-ax,dy=by-ay,dz=bz-az,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<.001f)return;float ux=dx/len,uy=dy/len,uz=dz/len;
        float refx=Math.abs(uy)>.92f?1f:0f,refy=Math.abs(uy)>.92f?0f:1f,refz=0f;
        float rx=refy*uz-refz*uy,ry=refz*ux-refx*uz,rz=refx*uy-refy*ux,rl=(float)Math.sqrt(rx*rx+ry*ry+rz*rz);rx/=rl;ry/=rl;rz/=rl;
        float fx=ry*uz-rz*uy,fy=rz*ux-rx*uz,fz=rx*uy-ry*ux;
        model[0]=rx*radius;model[1]=ry*radius;model[2]=rz*radius;model[3]=0;
        model[4]=ux*len*.5f;model[5]=uy*len*.5f;model[6]=uz*len*.5f;model[7]=0;
        model[8]=fx*depth;model[9]=fy*depth;model[10]=fz*depth;model[11]=0;
        model[12]=(ax+bx)*.5f;model[13]=(ay+by)*.5f;model[14]=(az+bz)*.5f;model[15]=1;
        GLES30.glUniformMatrix4fv(uModel,1,false,model,0);GLES30.glUniform4f(uColor,r,g,b,alpha);GLES30.glUniform3f(uMaterial,metal,rough,emissive);setSurface(metal,rough);mesh.draw();
    }

    private void renderProjectiles(){for(int i=0;i<world.projectiles.size();i++){GameWorld.Projectile p=world.projectiles.get(i);draw(lowSphere,p.x,p.y,p.z,p.radius*1.35f,p.radius*1.35f,p.radius*1.35f,0,0,0,p.r,p.g,p.b,1,.1f,.25f,2.4f);}}

    private int bodySurface(PhysicsWorld.Body b){if(b.owner!=null&&b.bone>=0)return SurfaceLibrary.actor(b.owner.type,b.bone);if(b.metalness>.5f)return SurfaceLibrary.METAL;if(b.kind==PhysicsWorld.FLESH||b.kind==PhysicsWorld.BODY_PART)return SurfaceLibrary.SKIN;if(b.kind==PhysicsWorld.TREE||b.kind==PhysicsWorld.PALM||b.kind==PhysicsWorld.PLANK||b.kind==PhysicsWorld.CRATE||b.kind==PhysicsWorld.BARREL)return SurfaceLibrary.BARK;if(b.kind==PhysicsWorld.MUSHROOM)return SurfaceLibrary.FUNGUS;return SurfaceLibrary.STONE;}
    private void renderPhysics(){
        java.util.HashSet<GameWorld.Actor> rendered=new java.util.HashSet<GameWorld.Actor>();
        for(PhysicsWorld.Body body:world.physics.bodies)if(body.kind==PhysicsWorld.RAGDOLL&&body.owner!=null&&!body.owner.surreal&&rendered.add(body.owner)){
            GameWorld.Actor a=body.owner;if((body.x-world.player.x)*(body.x-world.player.x)+(body.z-world.player.z)*(body.z-world.player.z)>1600)continue;CharacterSkeleton.sample(a,0,skeletonPose);drawWeighted(a,skeletonPose,false,a.colorR,a.colorG,a.colorB,a.severedMask);
        }

        for(int i=0;i<world.physics.bodies.size();i++){
            PhysicsWorld.Body b=world.physics.bodies.get(i);surface(bodySurface(b));
            if(b.hidden||b.kind==PhysicsWorld.RAGDOLL&&b.owner!=null&&!b.owner.surreal)continue;
            float dx=b.x-world.player.x,dz=b.z-world.player.z,drawDistance=world.qualityLevel==3?58f:world.qualityLevel==2?47f:35f;
            if(dx*dx+dz*dz>drawDistance*drawDistance&&b!=world.physics.getHeld())continue;
            if(world.qualityLevel==1&&(b.kind==PhysicsWorld.BONE||b.kind==PhysicsWorld.CHAIN)&&(i&1)==0)continue;
            if(world.qualityLevel<3&&(b.kind==PhysicsWorld.RAGDOLL||b.kind==PhysicsWorld.BODY_PART)&&(b.bone==CharacterSkeleton.L_HAND||b.bone==CharacterSkeleton.R_HAND||b.bone==CharacterSkeleton.L_FOOT||b.bone==CharacterSkeleton.R_FOOT))continue;
            if(surrealRenderer.body(b,world,dx*dx+dz*dz))continue;
            if(renderSetPiece(b))continue;
            if(b.kind==PhysicsWorld.RAGDOLL||b.kind==PhysicsWorld.BODY_PART){
                if(b.bone==CharacterSkeleton.HEAD||b.shape==PhysicsWorld.SHAPE_SPHERE){draw(sphere,b.x,b.y,b.z,b.radius,b.radius,b.radius,b.rx,b.ry,b.rz,b.r,b.g,b.b,1,b.metalness,b.roughness,0);}
                else if(b.parent!=null&&world.physics.bodies.contains(b.parent)){Mesh m=b.shape==PhysicsWorld.SHAPE_BOX?cube:cylinder;drawBetween(m,b.parent.x,b.parent.y,b.parent.z,b.x,b.y,b.z,b.radius,b.radius*.82f,b.r,b.g,b.b,1,b.metalness,b.roughness,0);}
                else draw(b.shape==PhysicsWorld.SHAPE_BOX?cube:cylinder,b.x,b.y,b.z,b.hx,b.hy,b.hz,b.rx,b.ry,b.rz,b.r,b.g,b.b,1,b.metalness,b.roughness,0);
                continue;
            }
            Mesh mesh=b.shape==PhysicsWorld.SHAPE_SPHERE?sphere:b.shape==PhysicsWorld.SHAPE_CAPSULE?cylinder:cube;
            float emissive=b.kind==PhysicsWorld.PRESSURE_PLATE&&!world.physics.isPuzzleSolved()?.55f:0f;
            draw(mesh,b.x,b.y,b.z,b.hx,b.hy,b.hz,b.rx,b.ry,b.rz,b.r,b.g,b.b,1,b.metalness,b.roughness,emissive);
            if(b.kind==PhysicsWorld.CRATE){draw(cube,b.x,b.y,b.z,b.hx*1.04f,b.hy*.075f,b.hz*1.04f,b.rx,b.ry,b.rz,.10f,.065f,.035f,1,.36f,.57f,0);}
            else if(b.kind==PhysicsWorld.BARREL){draw(cylinder,b.x,b.y+b.hy*.72f,b.z,b.hx*1.05f,b.hy*.055f,b.hz*1.05f,b.rx,b.ry,b.rz,.24f,.22f,.20f,1,.82f,.28f,0);draw(cylinder,b.x,b.y-b.hy*.72f,b.z,b.hx*1.05f,b.hy*.055f,b.hz*1.05f,b.rx,b.ry,b.rz,.24f,.22f,.20f,1,.82f,.28f,0);}
            else if(b.kind==PhysicsWorld.WEAPON){
                if(b.weaponType==GameWorld.CROSSBOW){draw(cube,b.x,b.y,b.z,b.hx*1.18f,b.hy*.42f,b.hz*.22f,b.rx,b.ry,b.rz+90,.16f,.10f,.065f,1,.36f,.62f,0);draw(cube,b.x,b.y,b.z,b.hx*.15f,b.hy*.12f,b.hz*1.22f,b.rx,b.ry,b.rz,.25f,.25f,.26f,1,.82f,.24f,0);}
                else if(b.weaponType==GameWorld.EXECUTIONER){draw(cube,b.x,b.y+b.hy*.72f,b.z,b.hx*1.65f,b.hy*.23f,b.hz*1.25f,b.rx,b.ry,b.rz,.24f,.24f,.25f,1,.86f,.28f,0);}
                else if(b.weaponType==GameWorld.GUARD){draw(cone,b.x,b.y+b.hy*.92f,b.z,b.hx*2.7f,b.hy*.20f,b.hz*2.7f,b.rx,b.ry,b.rz,.28f,.27f,.25f,1,.84f,.24f,0);}
                else draw(cube,b.x,b.y,b.z,b.hx*2.4f,b.hy*.055f,b.hz*2.2f,b.rx,b.ry,b.rz,.13f,.09f,.07f,1,.7f,.35f,0);
            }
            else if(b.kind==PhysicsWorld.CART){draw(cylinder,b.x-b.hx*.78f,b.y-b.hy*.52f,b.z,b.hy*.42f,b.hz*.18f,b.hy*.42f,90,b.ry,0,.08f,.055f,.035f,1,.18f,.86f,0);draw(cylinder,b.x+b.hx*.78f,b.y-b.hy*.52f,b.z,b.hy*.42f,b.hz*.18f,b.hy*.42f,90,b.ry,0,.08f,.055f,.035f,1,.18f,.86f,0);}
            else if(b.kind==PhysicsWorld.ALTAR){draw(cone,b.x,b.y+b.hy*1.3f,b.z,b.hx*.48f,b.hy*.85f,b.hz*.48f,0,b.ry,0,b.r*.72f,b.g*.72f,b.b*.72f,1,.28f,.66f,.08f);}
        }
    }

    private boolean renderSetPiece(PhysicsWorld.Body b){
        if(b.kind==PhysicsWorld.STONE_WALL){draw(cube,b.x,b.y,b.z,b.hx,b.hy,b.hz,0,0,0,b.r,b.g,b.b,1,.02f,.92f,0);int count=Math.min(8,(int)(b.hy*2));for(int i=0;i<count;i++)draw(cube,b.x,b.y-b.hy+i*.8f,b.z+b.hz+.012f,b.hx,.014f,.025f,0,0,0,.07f,.065f,.05f,1,0,1,0);return true;}
        if(b.kind==PhysicsWorld.ROOF){draw(cube,b.x,b.y,b.z,b.hx,b.hy,b.hz,0,0,0,b.r,b.g,b.b,1,.05f,.8f,0);for(int side=-1;side<=1;side+=2)draw(cube,b.x+side*b.hx*.5f,b.y+b.hy*.8f,b.z,b.hx*.57f,.10f,b.hz,0,0,side*-20,b.r*1.3f,b.g*1.2f,b.b,1,.05f,.8f,0);return true;}
        if(b.kind==PhysicsWorld.PALM){draw(cylinder,b.x,b.y,b.z,b.hx,b.hy,b.hz,0,0,0,.30f,.20f,.10f,1,0,.9f,0);for(int i=0;i<7;i++){float a=i*.8976f+b.variant;drawBetween(cone,b.x,b.y+b.hy,b.z,b.x+(float)Math.sin(a)*3,b.y+b.hy-.6f,b.z+(float)Math.cos(a)*3,.65f,.05f,.12f,.35f,.07f,1,0,.8f,0);}for(int i=0;i<3;i++)draw(lowSphere,b.x+(i-1)*.22f,b.y+b.hy-.3f,b.z,.19f,.24f,.19f,0,0,0,.25f,.13f,.05f,1,0,.9f,0);return true;}
        if(b.kind==PhysicsWorld.TREE){
            float tdx=b.x-cameraX,tdz=b.z-cameraZ,td2=tdx*tdx+tdz*tdz;int lod=world.qualityLevel==1?0:td2<360f?2:td2<1250f?1:0;
            float tr=b.variant==2?.24f:.075f,tg=b.variant==2?.22f:.055f,tb=b.variant==2?.18f:.038f;
            draw(cylinder,b.x,b.y,b.z,b.hx,b.hy,b.hz,0,b.ry,0,tr,tg,tb,1,.03f,.96f,0);
            if(lod>0){drawBetween(cylinder,b.x,b.y+b.hy*.25f,b.z,b.x-b.hx*2.7f,b.y+b.hy*.88f,b.z+b.hz*1.9f,b.hx*.48f,b.hx*.42f,.065f,.05f,.035f,1,.02f,.98f,0);drawBetween(cylinder,b.x,b.y+b.hy*.48f,b.z,b.x+b.hx*2.5f,b.y+b.hy*1.02f,b.z-b.hz*1.6f,b.hx*.42f,b.hx*.38f,.065f,.05f,.035f,1,.02f,.98f,0);}
            if(b.variant!=3){
                if(b.variant==1){int layers=lod==2?4:lod==1?2:1;for(int n=0;n<layers;n++){float scale=1f-n*.14f;draw(cone,b.x,b.y+b.hy*(.15f+n*.34f),b.z,b.hx*(4.9f*scale),b.hy*(lod==0?.72f:.38f),b.hz*(4.9f*scale),0,b.ry+n*17f,0,.025f,.095f+n*.006f,.040f,1,.01f,.98f,0);}}
                else {float fr=b.variant==2?.075f:.035f,fg=b.variant==2?.19f:.14f,fb=b.variant==2?.055f:.045f;int clusters=lod==2?5:lod==1?3:1;for(int n=0;n<clusters;n++){float a=n*2.39996f;draw(lowSphere,b.x+(float)Math.sin(a)*b.hx*(lod==0?0:2.15f),b.y+b.hy*(lod==0?.66f:.50f+(n%2)*.28f),b.z+(float)Math.cos(a)*b.hz*(lod==0?0:2.15f),b.hx*(lod==0?4.1f:3.25f),b.hy*(lod==0?.52f:.33f),b.hz*(lod==0?4.1f:3.25f),0,0,0,fr,fg-(n%3)*.012f,fb,1,.01f,.99f,0);}}
            }return true;
        }
        if(b.kind==PhysicsWorld.SHRUB){float glow=b.variant==4?.75f:0f;for(int n=0;n<4;n++){float a=n*1.5708f+b.ry;draw(lowSphere,b.x+(float)Math.sin(a)*b.hx*.42f,b.y+(n&1)*b.hy*.22f,b.z+(float)Math.cos(a)*b.hz*.42f,b.hx*.72f,b.hy*.72f,b.hz*.72f,0,0,0,b.r,b.g,b.b,1,.01f,.98f,glow);}return true;}
        if(b.kind==PhysicsWorld.MUSHROOM){float glow=b.variant==4?1.8f:.05f;for(int n=-1;n<=1;n++){float x=b.x+n*b.hx*.62f,z=b.z+(n&1)*b.hz*.35f;draw(cylinder,x,b.y-b.hy*.36f,z,b.hx*.18f,b.hy*.58f,b.hz*.18f,0,0,0,.38f,.32f,.25f,1,.02f,.90f,glow*.12f);draw(lowSphere,x,b.y+b.hy*.28f,z,b.hx*(.52f+Math.abs(n)*.10f),b.hy*.28f,b.hz*(.52f+Math.abs(n)*.10f),0,0,0,b.r,b.g,b.b,1,.02f,.76f,glow);}return true;}
        if(b.kind==PhysicsWorld.RUIN){
            draw(cube,b.x,b.y,b.z,b.hx,b.hy,b.hz,0,b.ry,0,b.r,b.g,b.b,1,b.metalness,.94f,0);
            int rows=world.qualityLevel==1?2:3;for(int row=0;row<rows;row++)for(int side=-1;side<=1;side+=2)draw(cube,b.x+side*b.hx*.72f,b.y-b.hy*.76f+row*b.hy*.63f,b.z+b.hz*1.03f,b.hx*.25f,b.hy*.13f,b.hz*.16f,0,b.ry+(row&1)*7f,0,b.r*.82f,b.g*.82f,b.b*.82f,1,.03f,.98f,0);return true;
        }
        if(b.kind==PhysicsWorld.SPRING){GLES30.glDepthMask(false);draw(lowSphere,b.x,b.y,b.z,b.hx,.035f,b.hz,0,0,0,b.r,b.g,b.b,.68f,.04f,.18f,.72f);float pulse=.82f+(float)Math.sin(renderTime*2.7f+b.x)*.12f;draw(lowSphere,b.x,b.y+.045f,b.z,b.hx*pulse,.018f,b.hz*pulse,0,0,0,.10f,.42f,.50f,.45f,.02f,.12f,1.4f);GLES30.glDepthMask(true);return true;}
        if(b.kind==PhysicsWorld.GALLOWS){
            for(int side=-1;side<=1;side+=2)draw(cube,b.x+side*b.hx*.78f,b.y,b.z,b.hx*.17f,b.hy,b.hz,.0f,b.ry,0,.095f,.062f,.035f,1,.04f,.94f,0);
            draw(cube,b.x,b.y+b.hy*.92f,b.z,b.hx,b.hy*.13f,b.hz*1.05f,0,b.ry,0,.09f,.057f,.032f,1,.04f,.94f,0);
            for(int n=0;n<5;n++)draw(cylinder,b.x,b.y+b.hy*.73f-n*.19f,b.z,b.hx*.035f,.11f,b.hx*.035f,0,0,0,.19f,.18f,.16f,1,.82f,.30f,0);return true;
        }
        if(b.kind==PhysicsWorld.CAGE){
            for(int side=-1;side<=1;side+=2)for(int edge=-1;edge<=1;edge+=2)draw(cylinder,b.x+side*b.hx*.82f,b.y,b.z+edge*b.hz*.82f,.045f,b.hy,.045f,0,0,0,.18f,.17f,.16f,1,.86f,.27f,0);
            for(int n=-2;n<=2;n++){draw(cylinder,b.x+n*b.hx*.32f,b.y,b.z-b.hz*.84f,.032f,b.hy,.032f,0,0,0,.17f,.16f,.15f,1,.84f,.30f,0);draw(cylinder,b.x+n*b.hx*.32f,b.y,b.z+b.hz*.84f,.032f,b.hy,.032f,0,0,0,.17f,.16f,.15f,1,.84f,.30f,0);}
            draw(cube,b.x,b.y-b.hy*.92f,b.z,b.hx,.08f,b.hz,0,0,0,.12f,.105f,.09f,1,.68f,.42f,0);draw(cube,b.x,b.y+b.hy*.92f,b.z,b.hx,.08f,b.hz,0,0,0,.12f,.105f,.09f,1,.68f,.42f,0);return true;
        }
        if(b.kind==PhysicsWorld.STAKE){draw(cylinder,b.x,b.y*.88f,b.z,b.hx,b.hy*.88f,b.hz,0,b.ry,0,.11f,.07f,.038f,1,.04f,.94f,0);draw(cone,b.x,b.y+b.hy*.92f,b.z,b.hx*1.2f,b.hy*.18f,b.hz*1.2f,0,b.ry,0,.12f,.075f,.04f,1,.04f,.91f,0);return true;}
        if(b.kind==PhysicsWorld.STATUE){
            draw(cube,b.x,b.y-b.hy*.78f,b.z,b.hx,b.hy*.20f,b.hz,0,b.ry,0,b.r,b.g,b.b,1,.05f,.91f,0);draw(cylinder,b.x,b.y*.98f,b.z,b.hx*.48f,b.hy*.62f,b.hz*.48f,0,b.ry,0,b.r*.86f,b.g*.86f,b.b*.86f,1,.08f,.84f,0);draw(sphere,b.x,b.y+b.hy*.70f,b.z,b.hx*.34f,b.hx*.42f,b.hx*.34f,0,b.ry,0,b.r*.78f,b.g*.78f,b.b*.78f,1,.06f,.86f,.04f);
            for(int side=-1;side<=1;side+=2)draw(cone,b.x+side*b.hx*.52f,b.y+b.hy*.72f,b.z,b.hx*.14f,b.hy*.27f,b.hz*.14f,0,b.ry,side*19,b.r*.65f,b.g*.65f,b.b*.65f,1,.10f,.77f,.03f);return true;
        }
        if(b.kind==PhysicsWorld.FLESH){for(int n=0;n<7;n++){float a=n*2.39996f,scale=.44f+(n%3)*.16f;draw(lowSphere,b.x+(float)Math.sin(a)*b.hx*.48f,b.y+(n%2)*b.hy*.42f,b.z+(float)Math.cos(a)*b.hz*.48f,b.hx*scale,b.hy*scale,b.hz*scale,0,0,0,.27f+(n%2)*.05f,.035f,.065f,1,.02f,.58f,.18f);}return true;}
        if(b.kind==PhysicsWorld.GRAVE){draw(cube,b.x,b.y,b.z,b.hx,b.hy,b.hz,-8,b.ry,0,.15f,.145f,.14f,1,.04f,.92f,0);draw(cube,b.x,b.y+b.hy*.72f,b.z,b.hx*.75f,b.hy*.10f,b.hz*1.12f,0,b.ry,0,.13f,.125f,.12f,1,.04f,.94f,0);return true;}
        if(b.kind==PhysicsWorld.CHAIN){draw(cylinder,b.x,b.y,b.z,b.hx,b.hy,b.hz,b.rx,b.ry,b.rz,b.r,b.g,b.b,1,.88f,.24f,0);return true;}
        return false;
    }

    private void renderParticles(){
        GLES30.glDepthMask(false);GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,GLES30.GL_ONE);
        int limit=world.qualityLevel==3?110:world.qualityLevel==2?70:42;
        int start=Math.max(0,world.particles.size()-limit);
        for(int i=start;i<world.particles.size();i++){
            GameWorld.Particle p=world.particles.get(i);float alpha=Math.max(0f,p.life/p.maxLife),size=p.size*(.6f+alpha*.7f);
            draw(lowSphere,p.x,p.y,p.z,size,size,size,0,0,0,p.r,p.g,p.b,alpha,.02f,.5f,p.kind==1?.2f:1.3f);
        }
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,GLES30.GL_ONE_MINUS_SRC_ALPHA);GLES30.glDepthMask(true);
    }

    void draw(Mesh mesh,float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b,float a,float metal,float rough,float emissive){
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.rotateM(model,0,ry,0,1,0);Matrix.rotateM(model,0,rx,1,0,0);Matrix.rotateM(model,0,rz,0,0,1);Matrix.scaleM(model,0,sx,sy,sz);
        GLES30.glUniformMatrix4fv(uModel,1,false,model,0);GLES30.glUniform4f(uColor,r,g,b,a);GLES30.glUniform3f(uMaterial,metal,rough,emissive);setSurface(metal,rough);mesh.draw();
    }

    private void ensureFrameBuffer(){
        int quality=world.qualityLevel;float scale=quality==3?1f:quality==2?.84f:.68f;
        int wantedW=Math.max(320,(int)(width*scale)),wantedH=Math.max(180,(int)(height*scale));
        if(fboQuality==quality&&wantedW==fboWidth&&wantedH==fboHeight)return;
        releaseFrameBuffer();fboQuality=quality;fboWidth=wantedW;fboHeight=wantedH;
        GLES30.glGenFramebuffers(1,fbo,0);GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,fbo[0]);
        GLES30.glGenTextures(1,sceneTexture,0);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,sceneTexture[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGBA16F,fboWidth,fboHeight,0,GLES30.GL_RGBA,GLES30.GL_HALF_FLOAT,null);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,GLES30.GL_COLOR_ATTACHMENT0,GLES30.GL_TEXTURE_2D,sceneTexture[0],0);
        GLES30.glGenRenderbuffers(1,depthBuffer,0);GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER,depthBuffer[0]);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,GLES30.GL_DEPTH24_STENCIL8,fboWidth,fboHeight);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER,GLES30.GL_DEPTH_STENCIL_ATTACHMENT,GLES30.GL_RENDERBUFFER,depthBuffer[0]);
        if(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)!=GLES30.GL_FRAMEBUFFER_COMPLETE)GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGBA8,fboWidth,fboHeight,0,GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,null);
        fboReady=GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)==GLES30.GL_FRAMEBUFFER_COMPLETE;
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
    }

    private void renderPost(){
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);GLES30.glViewport(0,0,width,height);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);GLES30.glDisable(GLES30.GL_CULL_FACE);post.use();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,sceneTexture[0]);GLES30.glUniform1i(pTexture,0);
        GLES30.glUniform1f(pDream,world.surreal.intensity*world.settings.dream);GLES30.glUniform1f(pTime,renderTime);GLES30.glUniform1f(pDamage,world.damageFlash);GLES30.glUniform1f(pRage,world.player.rageTime>0?1f:world.hudRage/100f*.18f);
        GLES30.glUniform2f(pResolution,fboWidth,fboHeight);GLES30.glUniform1i(pQuality,world.qualityLevel);quad.draw();
        GLES30.glEnable(GLES30.GL_CULL_FACE);GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    private void releaseFrameBuffer(){if(sceneTexture[0]!=0)GLES30.glDeleteTextures(1,sceneTexture,0);if(depthBuffer[0]!=0)GLES30.glDeleteRenderbuffers(1,depthBuffer,0);if(fbo[0]!=0)GLES30.glDeleteFramebuffers(1,fbo,0);sceneTexture[0]=depthBuffer[0]=fbo[0]=0;fboReady=false;}

    public void release(){releaseFrameBuffer();if(clouds!=null)clouds.release();if(floraRenderer!=null)floraRenderer.release();if(water!=null)water.release();if(terrain!=null)terrain.release();if(materials!=null)materials.release();for(Mesh m:skinMeshes.values())m.release();skinMeshes.clear();skins.clear();if(surrealRenderer!=null)surrealRenderer.release();if(scene!=null)scene.release();if(post!=null)post.release();if(cube!=null){cube.release();plane.release();sphere.release();lowSphere.release();cylinder.release();cone.release();quad.release();}}

    private static final String SCENE_VERTEX=
        "layout(location=0) in vec3 aPosition;layout(location=1) in vec3 aNormal;layout(location=2) in vec3 aTint;"+
        "layout(location=3) in vec2 aUv;layout(location=4) in vec2 aBones;layout(location=5) in vec2 aWeights;layout(location=6) in float aGroup;uniform highp int uWater;uniform float uTime;uniform int uSkin;uniform int uHidden;uniform mat4 uBones[16];uniform mat4 uModel;uniform mat4 uViewProjection;out vec2 vUv;flat out highp int vSurface;uniform highp int uSurface;flat out highp int vDiscard;"+
        "out vec3 vPosition;out vec3 vNormal;out vec3 vTint;"+
        "void main(){mat4 skin=uSkin==1?uBones[int(aBones.x)]*aWeights.x+uBones[int(aBones.y)]*aWeights.y:mat4(1.0);vSurface=uSkin==1?int(floor(aUv.y/2.0)):uSurface;vUv=uSkin==1?vec2(aUv.x,aUv.y-float(vSurface)*2.0):(abs(aNormal.y)>.6?aPosition.xz:abs(aNormal.z)>.6?aPosition.xy:aPosition.zy)*.5;vDiscard=uSkin==1&&(int(aGroup)&uHidden)!=0?1:0;vec4 w=uModel*skin*vec4(aPosition,1.0);vTint=aTint;vNormal=normalize(mat3(transpose(inverse(uModel*skin)))*aNormal);if(uWater==1){float a=w.x*.23+w.z*.17-uTime*1.9,b=w.z*.49-w.x*.12+uTime*2.4;w.y+=aTint.y*(sin(a)+.45*sin(b));vNormal=normalize(vec3(-aTint.y*(.23*cos(a)-.054*cos(b)),1.0,-aTint.y*(.17*cos(a)+.2205*cos(b))));}vPosition=w.xyz;gl_Position=uViewProjection*w;}";

    private static final String SCENE_FRAGMENT=
        "precision highp float;in vec3 vPosition;in vec3 vNormal;in vec3 vTint;in vec2 vUv;flat in highp int vDiscard;flat in highp int vSurface;uniform highp sampler2DArray uDetail;uniform highp sampler2DArray uNormals;out vec4 fragColor;"+
        "uniform highp int uWater;uniform int uHorizon;uniform vec3 uCamera;uniform vec4 uColor;uniform vec3 uMaterial;uniform float uTime;uniform vec3 uLightDir;uniform vec3 uTorch;uniform vec3 uFogColor;"+
        "const float PI=3.14159265;"+
        "float Dggx(float nDotH,float rough){float a=rough*rough;float a2=a*a;float d=nDotH*nDotH*(a2-1.0)+1.0;return a2/max(PI*d*d,0.001);}"+
        "float Gschlick(float nDotV,float rough){float r=rough+1.0;float k=r*r/8.0;return nDotV/(nDotV*(1.0-k)+k);}"+
        "vec3 fresnel(float hDotV,vec3 f0){return f0+(1.0-f0)*pow(1.0-hDotV,5.0);}"+
        "void main(){if(vDiscard!=0)discard;vec3 detail=texture(uDetail,vec3(vUv*4.0,float(vSurface))).rgb;vec4 normalMap=texture(uNormals,vec3(vUv*4.0,float(vSurface)));vec3 n=normalize(vNormal),v=normalize(uCamera-vPosition),l=normalize(uLightDir),h=normalize(v+l);if(uWater==1){if(vTint.x<=0.015)discard;float dist=length(vPosition.xz-uTorch.xz);float seam=46.0;if(uHorizon==1&&(dist<seam||vTint.z<10.0))discard;if(uHorizon==0&&vTint.y>.1&&dist>=seam&&vTint.z>=10.0)discard;float fres=pow(1.0-abs(dot(n,v)),4.0);vec3 water=mix(vec3(.035,.28,.27),vec3(.014,.065,.11),clamp(vTint.x/7.0,0.0,1.0));water=mix(water,vec3(.24,.36,.46),fres*.7);float glint=pow(max(dot(n,h),0.0),180.0)*.7;float foam=(1.0-smoothstep(.06,.85,vTint.x))*(.6+.4*sin(vPosition.x*3.0+vPosition.z*3.7+uTime*2.0));water+=vec3(1.0,.26,.18)*glint+vec3(.32,.42,.4)*foam;float fog=smoothstep(70.0,1400.0,dist);fragColor=vec4(mix(water,vec3(.035,.060,.085),fog),mix(.52,.90,clamp(vTint.x/8.0,0.0,1.0)));return;}vec3 dp1=dFdx(vPosition),dp2=dFdy(vPosition);vec2 du1=dFdx(vUv),du2=dFdy(vUv);vec3 t=dp1*du2.y-dp2*du1.y,bn=-dp1*du2.x+dp2*du1.x;float frameScale=inversesqrt(max(max(dot(t,t),dot(bn,bn)),.000001));vec3 mapped=normalMap.xyz*2.0-1.0;n=normalize(n*mapped.z+(t*mapped.x+bn*mapped.y)*frameScale*.45);float rough=clamp(uMaterial.y*(.7+normalMap.a*.55),0.08,1.0);float metal=vSurface==3?max(.65,uMaterial.x):vSurface==7?.08:min(.06,uMaterial.x);float grain=fract(sin(dot(floor(vPosition*19.0),vec3(12.9898,78.233,39.425)))*43758.5453);vec3 base=uColor.rgb*vTint*(0.78+grain*0.05+detail*.35);"+
        "float ndl=max(dot(n,l),0.0),ndv=max(dot(n,v),0.001),ndh=max(dot(n,h),0.0),hdv=max(dot(h,v),0.0);vec3 f0=mix(vec3(0.035),base,metal);vec3 F=fresnel(hdv,f0);float spec=Dggx(ndh,rough)*Gschlick(ndv,rough)*Gschlick(ndl,rough)/max(4.0*ndv*ndl,0.001);"+
        "vec3 kd=(1.0-F)*(1.0-metal);vec3 color=(kd*base/PI+F*spec)*ndl*vec3(1.0,0.25,0.18)*.82;"+
        "float rim=pow(1.0-max(dot(n,v),0.0),2.6);color+=base*vec3(.15,.17,.22)*(1.0+max(n.y,0.0)*.35)+rim*vec3(0.16,0.19,0.25);"+
        "vec3 td=uTorch-vPosition;float tl=max(dot(n,normalize(td)),0.0)/(1.0+dot(td,td)*0.13);color+=base*vec3(1.0,0.26,0.06)*tl*4.0;"+
        "color+=base*uMaterial.z;float distanceFog=length(uCamera-vPosition);float fog=smoothstep(35.0,100.0,distanceFog);color=mix(color,uFogColor,fog);fragColor=vec4(color,uColor.a);}";

    private static final String POST_VERTEX=
        "layout(location=0) in vec3 aPosition;out vec2 vUv;void main(){vUv=aPosition.xy*0.5+0.5;gl_Position=vec4(aPosition.xy,0.0,1.0);}";

    private static final String POST_FRAGMENT=
        "precision highp float;in vec2 vUv;out vec4 fragColor;uniform sampler2D uScene;uniform vec2 uResolution;uniform float uTime;uniform float uDamage;uniform float uRage;uniform float uDream;uniform int uQuality;"+
        "float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}"+
        "vec3 aces(vec3 x){return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0);}"+
        "void main(){vec2 px=1.0/uResolution;vec2 uv=clamp(vUv+vec2(sin(vUv.y*18.0+uTime*0.7),cos(vUv.x*14.0-uTime*0.55))*0.0025*uDream,vec2(0.002),vec2(0.998));vec3 c=texture(uScene,uv).rgb;if(uQuality>1){vec3 n=texture(uScene,uv+vec2(0.0,px.y)).rgb;vec3 s=texture(uScene,uv-vec2(0.0,px.y)).rgb;vec3 e=texture(uScene,uv+vec2(px.x,0.0)).rgb;vec3 w=texture(uScene,uv-vec2(px.x,0.0)).rgb;float edge=clamp(length((n+s)-(e+w))*0.28,0.0,0.42);c=mix(c,(n+s+e+w+c*2.0)/6.0,edge);}"+
        "if(uQuality>2){vec3 bloom=vec3(0.0);bloom+=texture(uScene,uv+px*vec2(2.0,2.0)).rgb;bloom+=texture(uScene,uv+px*vec2(-2.0,2.0)).rgb;bloom+=texture(uScene,uv+px*vec2(2.0,-2.0)).rgb;bloom+=texture(uScene,uv+px*vec2(-2.0,-2.0)).rgb;bloom*=0.25;float bright=max(max(bloom.r,bloom.g),bloom.b);c+=bloom*max(0.0,bright-0.52)*0.34;}"+
        "float aberr=(uDamage*2.3+0.35+uDream*1.3)*px.x;c.r+=texture(uScene,uv+vec2(aberr,0.0)).r-texture(uScene,uv).r;c.b+=texture(uScene,uv-vec2(aberr,0.0)).b-texture(uScene,uv).b;c=aces(c*1.28);"+
        "float vignette=pow(clamp(16.0*uv.x*uv.y*(1.0-uv.x)*(1.0-uv.y),0.0,1.0),0.18);c*=mix(0.48,1.0,vignette);c+=vec3(0.28,0.0,0.015)*(uDamage*0.48+uRage*0.11)*(1.0-vignette*.45);"+
        "c+=(hash(gl_FragCoord.xy+uTime*73.0)-0.5)/255.0*7.0;c=mix(vec3(dot(c,vec3(0.22,0.70,0.08))),c,0.84+uDream*0.16);fragColor=vec4(c,1.0);}";
}
