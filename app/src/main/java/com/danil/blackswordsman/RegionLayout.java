package com.danil.blackswordsman;
import java.util.Random;
/** Inhabited outskirts: accessible buildings, caves, oases, harbours and persistent water salvage. */
public final class RegionLayout {
    public static final int VILLAGE=0,CAVE=1,OASIS=2,TOWER=3,DOCK=4,WORKSHOP=5;
    // x,z,type. Authored campaign stays in the original centre.
    public static final float[][] PLACES={{-205,-110,0},{195,105,0},{-280,285,0},{265,-260,0},{-325,-95,1},{310,175,1},{-175,-295,1},{90,310,1},{237,-165,2},{-285,105,2},{-210,160,3},{195,295,3},{-270,-290,3},{115,-270,3},{250,206,4},{-215,-220,4},{-185,267,4},{360,-62,4},{-330,35,4},{285,25,5},{-170,-185,5},{-50,-235,5},{-40,270,0},{335,-155,3}};
    public static final String[] NAMES={"ПРИБРЕЖНОЕ ПОСЕЛЕНИЕ","ПЕЩЕРА ЭХА","ПАЛЬМОВЫЙ ОАЗИС","БАШНЯ НАБЛЮДАТЕЛЕЙ","ПРИЧАЛ ЗАБЫТЫХ ВЕЩЕЙ","МАСТЕРСКАЯ МЕХАНИЗМОВ"};
    private RegionLayout(){}
    private static int mod(int a,int n){return (a%n+n)%n;}
    public static int nearest(float x,float z,float radius){int best=-1;float d=radius*radius;for(int i=0;i<PLACES.length;i++){float dx=x-PLACES[i][0],dz=z-PLACES[i][1],v=dx*dx+dz*dz;if(v<d){best=i;d=v;}}return best;}
    public static boolean reserved(float x,float z){return nearest(x,z,19)>=0;}
    public static float desert(float x,float z){float a=ellipse(x,z,270,-235,155,195),b=ellipse(x,z,-307,104,62,67);return Math.max(a,b);}
    public static float mountains(float x,float z){return Math.max(ellipse(x,z,-120,310,290,150),ellipse(x,z,330,160,110,160));}
    private static float ellipse(float x,float z,float cx,float cz,float rx,float rz){float dx=(x-cx)/rx,dz=(z-cz)/rz,edge=(float)Math.sqrt(dx*dx+dz*dz)+(float)Math.sin(x*.033f)*.045f+(float)Math.sin(z*.041f)*.05f;float v=Math.max(0,Math.min(1,(1.1f-edge)*3));return v*v*(3-2*v);}
    public static int biome(float x,float z){return desert(x,z)>.48f?1:mountains(x,z)>.5f?2:0;}
    public static String label(int env,float x,float z){int place=nearest(x,z,23);if(place>=0)return NAMES[(int)PLACES[place][2]];int water=WaterField.kind(env,x,z);if(WaterField.depth(env,x,z)>.2f)return water==WaterField.OCEAN?"ОКЕАН НЕИЗВЕСТНОГО":water==WaterField.RIVER?"РЕКА ЗАБЫТЫХ ВЕЩЕЙ":water==WaterField.OASIS?"ВОДА ОАЗИСА":"ОЗЁРНЫЙ КРАЙ";return biome(x,z)==1?"БАРХАНЫ И ОАЗИСЫ":biome(x,z)==2?"ГОРЫ И ПЕЩЕРЫ":"ЗЕЛЁНЫЕ ОКРАИНЫ";}
    public static void populateChunk(PhysicsWorld p,int env,int cx,int cz){
        for(int i=0;i<PLACES.length;i++){float[] q=PLACES[i];if((int)Math.floor(q[0]/24)==cx&&(int)Math.floor(q[1]/24)==cz)place(p,env,q[0],q[1],(int)q[2],i);}
        Random r=new Random(0x5741544552L+cx*73856093L+cz*19349663L+env*911L);
        // Samples cover every water chunk; all 240 catalog shapes participate, including both boats.
        for(int i=0;i<14;i++){float x=(cx+r.nextFloat())*24,z=(cz+r.nextFloat())*24;if(Math.abs(x)>GameWorld.OCEAN_HALF||Math.abs(z)>GameWorld.OCEAN_HALF||WaterField.depth(env,x,z)<.55f)continue;int id=waterCatalog(cx,cz,i);PhysicsWorld.Body b;
            if(i%5!=4)b=p.createToy(SurrealCatalog.ITEMS[id],x,z);
            else{int v=mod(cx*3+cz+i,6),kind=v==0?PhysicsWorld.PLANK:v==1?PhysicsWorld.CRATE:v==2?PhysicsWorld.BARREL:v==3?PhysicsWorld.WEAPON:v==4?PhysicsWorld.BONE:PhysicsWorld.BODY_PART;float hx=v==0?.8f:v==3?.09f:.23f,hy=v==3?.6f:.2f;b=p.createLoose(kind,x,0,z,hx,hy,.23f,1.8f);if(v==5){b.bone=CharacterSkeleton.HEAD;b.shape=PhysicsWorld.SHAPE_SPHERE;b.radius=.23f;b.r=.35f;b.g=.26f;b.b=.21f;}if(v==3)b.weaponType=GameWorld.BANDIT;}
            b.y=WaterField.level(env,x,z)-b.hy*.25f;b.vx=WaterField.flowX(env,x,z,0);b.vz=WaterField.flowZ(env,x,z,0);b.ry=r.nextFloat()*360;
        }
        if(mod(cx*7+cz*13,5)==0){float x=(cx+.5f)*24,z=(cz+.5f)*24;if(Math.abs(x)<GameWorld.OCEAN_HALF&&Math.abs(z)<GameWorld.OCEAN_HALF&&WaterField.depth(env,x,z)>.8f){PhysicsWorld.Body b=p.createToy(SurrealCatalog.ITEMS[((cx+cz)&1)==0?152:182],x,z);b.y=WaterField.level(env,x,z)-b.hy*.2f;b.vx=.2f;b.vz=.3f;}}
    }
    public static int waterCatalog(int cx,int cz,int n){return mod(cx*73+cz*137+n*19,240);}
    private static void wall(PhysicsWorld p,float x,float y,float z,float hx,float hy,float hz){PhysicsWorld.Body b=p.createStatic(PhysicsWorld.STONE_WALL,x,y,z,hx,hy,hz);b.r=.27f;b.g=.24f;b.b=.19f;}
    private static void house(PhysicsWorld p,float x,float z,float scale){float y=p.terrainHeight(x,z),w=3*scale,d=3.5f*scale,h=2.6f*scale;wall(p,x-w,y+h/2,z,.22f,h/2,d);wall(p,x+w,y+h/2,z,.22f,h/2,d);wall(p,x,y+h/2,z+d,w,.5f*h,.22f);wall(p,x-w*.7f,y+h/2,z-d,w*.3f,h/2,.22f);wall(p,x+w*.7f,y+h/2,z-d,w*.3f,h/2,.22f);wall(p,x,y+h-.25f,z-d,w,.25f,.22f);PhysicsWorld.Body roof=p.createStatic(PhysicsWorld.ROOF,x,y+h+.65f,z,w+.3f,.7f,d+.3f);roof.r=.21f;roof.g=.1f;roof.b=.065f;for(int side=-1;side<=1;side+=2){PhysicsWorld.Body post=p.createStatic(PhysicsWorld.COLUMN,x+side*w,y+1.35f,z-d,.14f,1.35f,.14f);post.r=.35f;post.g=.2f;post.b=.09f;}p.createLoose(PhysicsWorld.CRATE,x+1,y+.42f,z,.42f,.42f,.42f,5);p.createStatic(PhysicsWorld.LANTERN,x,y+2,z-d-.5f,.12f,.18f,.12f);}
    private static void place(PhysicsWorld p,int env,float x,float z,int kind,int index){float y=p.terrainHeight(x,z);
        if(kind==VILLAGE){house(p,x-7,z,1);house(p,x+7,z,1);house(p,x,z+10,.85f);for(int i=0;i<4;i++)p.createLoose(PhysicsWorld.BARREL,x-2+i*1.2f,y+.6f,z+4,.42f,.6f,.42f,4);}
        else if(kind==CAVE){ // A traversable covered chamber, with two open entrances and a side alcove.
            for(int i=0;i<5;i++){float zz=z-10+i*4,yy=p.terrainHeight(x,zz);wall(p,x-4,yy+2.3f,zz,1.1f,2.3f,2);wall(p,x+4,yy+2.3f,zz,1.1f,2.3f,2);wall(p,x,yy+4.7f,zz,5.2f,.6f,2);}
            for(int i=0;i<9;i++){float xx=x-2.5f+(i%3)*2.4f,zz=z-7+(i/3)*6;PhysicsWorld.Body c=p.createStatic(PhysicsWorld.MUSHROOM,xx,p.terrainHeight(xx,zz)+.4f,zz,.35f,.4f,.35f);c.variant=4;c.r=.08f;c.g=.65f;c.b=.8f;}p.createLoose(PhysicsWorld.CRATE,x,y+.55f,z,.55f,.55f,.55f,8);
        }else if(kind==OASIS){for(int i=0;i<10;i++){float a=i*.628318f,xx=x+(float)Math.sin(a)*12,zz=z+(float)Math.cos(a)*12;if(WaterField.depth(env,xx,zz)>.1f)continue;PhysicsWorld.Body palm=p.createStatic(PhysicsWorld.PALM,xx,p.terrainHeight(xx,zz)+3.2f,zz,.3f,3.2f,.3f);palm.variant=i;}house(p,x+9,z+10,.8f);}
        else if(kind==TOWER){for(int side=-1;side<=1;side+=2){wall(p,x+side*3,y+4,z,.45f,4,3);wall(p,x,y+4,z+side*3,1.1f,4,.35f);}wall(p,x,y+8,z,3.6f,.3f,3.6f);for(int i=0;i<12;i++){float xx=x-1.7f+(i/6)*1.9f,zz=z-2.2f+(i%6)*.82f;p.createStatic(PhysicsWorld.PLANK,xx,y+.12f+i*.26f,zz,.85f,.12f,.43f);}p.createStatic(PhysicsWorld.LANTERN,x,y+8.8f,z,.25f,.5f,.25f);}
        else if(kind==DOCK){float wy=WaterField.level(env,x,z)+.5f,best=80,ox=1,oz=0;for(int n=0;n<16;n++){float angle=n*.3926991f,dx=(float)Math.sin(angle),dz=(float)Math.cos(angle);for(float d=1;d<75;d+=1){if(WaterField.depth(env,x+dx*d,z+dz*d)<.05f){if(d<best){best=d;ox=dx;oz=dz;}break;}}}float sx=x+ox*(best+1),sz=z+oz*(best+1);for(int i=0;i<10;i++){float xx=sx-ox*i*1.4f,zz=sz-oz*i*1.4f;PhysicsWorld.Body deck=p.createStatic(PhysicsWorld.PLANK,xx,wy,zz,.72f,.12f,1.9f);deck.ry=-(float)Math.atan2(oz,ox)*57.2958f;CollisionMath.refresh(deck);for(int side=-1;side<=1;side+=2){float px=xx-oz*side*1.75f,pz=zz+ox*side*1.75f,floor=p.terrainHeight(px,pz);p.createStatic(PhysicsWorld.COLUMN,px,(wy+floor)*.5f,pz,.11f,Math.max(.2f,(wy-floor)*.5f)+.25f,.11f);}}for(int i=0;i<3;i++){float xx=sx-ox*(12+i),zz=sz-oz*(12+i);PhysicsWorld.Body toy=p.createToy(SurrealCatalog.ITEMS[152+(i%2)*30],xx,zz);toy.y=WaterField.level(env,xx,zz);}}

        else{house(p,x,z,1.5f);for(int i=0;i<6;i++){PhysicsWorld.Body machine=p.createStatic(PhysicsWorld.ALTAR,x-8+i*3,y+1,z+8,.8f,1,.8f);machine.metalness=.9f;machine.r=.22f;machine.g=.35f;machine.b=.4f;}p.addChain(x+3,y+5,z+3,8);}
    }
}
