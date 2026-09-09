package com.danil.blackswordsman;

import java.util.Arrays;

/** CPU-baked coloured mesh: detailed objects cost one draw, regardless of part count. */
public final class ToyGeometry {
    public static final class Data {
        public float[] vertices;
        public short[] indices;
        public int parts;
    }
    private static final float[] WHITE={.94f,.91f,.81f},DARK={.025f,.035f,.06f},METAL={.48f,.53f,.59f},GOLD={.90f,.59f,.13f},RED={.94f,.09f,.15f},BLUE={.06f,.54f,.94f},GREEN={.09f,.78f,.35f};
    private ToyGeometry(){}

    public static Data build(SurrealCatalog.Item d,boolean detailed){
        Builder b=new Builder(detailed);float[] c={d.r,d.g,d.blue};
        if(d.family==SurrealCatalog.BALL)ball(b,d,c);
        else if(d.family==SurrealCatalog.RING)ring(b,d,c);
        else if(d.family==SurrealCatalog.BLOCK)block(b,d,c);
        else if(d.family==SurrealCatalog.DOMINO)domino(b,d);
        else if(d.family==SurrealCatalog.BRICK)brick(b,d,c);
        else if(d.family==SurrealCatalog.TOY)toy(b,d.a,d.b,c);
        else relic(b,d.a,d.b,c);
        return b.finish();
    }
    public static Data pickup(int kind,boolean detailed){
        Builder b=new Builder(detailed);float[] c=SurrealCatalog.COLORS[kind%9];
        if(kind==0){b.box(0,0,0,.9f,.6f,.52f,WHITE);b.box(0,.615f,0,.23f,.03f,.15f,RED);b.box(0,0,.535f,.50f,.11f,.035f,RED);b.box(0,0,.54f,.12f,.38f,.04f,RED);b.torus(0,.72f,0,.28f,.055f,METAL);}
        else if(kind==1){b.cylinder(0,-.30f,0,.075f,.72f,.075f,1,WHITE);b.sphere(0,.36f,0,.55f,.55f,.20f,c);for(int i=0;i<3;i++)b.rot(90,0,0).torus(0,.36f,.21f,.12f+i*.14f,.035f,WHITE);}
        else if(kind==2||kind==8){b.cylinder(0,-.22f,0,.63f,.4f,.63f,.78f,c);b.sphere(0,.21f,0,.68f,.40f,.68f,WHITE);b.sphere(0,.63f,0,.17f,.17f,.17f,RED);for(int i=0;i<8;i++){float a=i*.7854f;b.sphere((float)Math.sin(a)*.5f,.32f,(float)Math.cos(a)*.5f,.055f,.055f,.055f,c);}}
        else if(kind==3){b.sphere(0,0,0,.76f,.57f,.76f,c);b.cylinder(0,-.56f,0,.9f,.06f,.9f,1,WHITE);b.sphere(.22f,.49f,.2f,.16f,.10f,.16f,WHITE);}
        else if(kind==4){b.sphere(0,-.18f,0,.60f,.63f,.60f,c);b.cylinder(0,.53f,0,.18f,.27f,.18f,1,WHITE);b.cylinder(0,.83f,0,.24f,.08f,.24f,1,GOLD);b.rot(90,0,0).torus(0,-.18f,.58f,.22f,.065f,WHITE);}
        else if(kind==5){b.box(0,0,0,.8f,.5f,.56f,GREEN);b.box(0,.54f,0,.85f,.06f,.62f,METAL);for(int i=-2;i<=2;i++)b.cylinder(i*.26f,.23f,.59f,.07f,.26f,.07f,.72f,GOLD);}
        else {int shape=kind==6?0:kind==7?19:11;toy(b,shape,1,c);b.torus(0,-.64f,0,.85f,.06f,WHITE);}
        return b.finish();
    }

    private static void ball(Builder b,SurrealCatalog.Item d,float[] c){
        b.sphere(0,0,0,.82f,.82f,.82f,c);int v=d.variant,detail=b.detailed?8:4;
        if(v==0||v==1||v==4){for(int i=0;i<(v==1?3:2+d.a);i++){if(v==1)b.torus(0,(i-1)*.43f,0,(float)Math.sqrt(.82f*.82f-(i-1)*(i-1)*.43f*.43f),.045f,WHITE);else b.rot(v==4?i*35f:90,i*60f,0).torus(0,0,0,.83f,.04f,WHITE);}}
        else if(v==5){b.sphere(0,0,.72f,.49f,.49f,.17f,WHITE);b.sphere(0,0,.89f,.23f,.26f,.09f,BLUE);b.sphere(0,0,.97f,.10f,.18f,.03f,DARK);}
        else for(int i=0;i<detail+d.a;i++){float a=i*2.39996f,y=-.64f+1.28f*i/(detail+d.a-1f),r=(float)Math.sqrt(.82f*.82f-y*y),x=(float)Math.sin(a)*r,z=(float)Math.cos(a)*r;
            if(v==3)b.sphere(x,y,z,.13f,.17f,.13f,WHITE);
            else if(v==6)b.sphere(x,y,z,.09f,.09f,.09f,DARK);
            else b.sphere(x,y,z,v==7?.17f:.10f,v==7?.17f:.10f,v==7?.17f:.10f,v==7?GOLD:WHITE);
        }
    }
    private static void ring(Builder b,SurrealCatalog.Item d,float[] c){
        int style=d.variant%6;b.torus(0,0,0,.68f,.13f+d.b*.018f,c);
        b.torus(0,.095f+d.b*.015f,0,.68f,.022f,WHITE);b.torus(0,-.095f-d.b*.015f,0,.68f,.022f,c);
        if(style==4)b.torus(0,.26f,0,.64f,.09f,WHITE);
        if(style==1||style==2||style==3||style==5){int count=d.a+4+d.b*2;for(int i=0;i<count;i++){float a=i*6.283185f/count,x=(float)Math.sin(a)*.70f,z=(float)Math.cos(a)*.70f;
            if(style==1)b.rot(0,a*57.2958f,0).box(x,0,z,.05f,.17f,.18f,WHITE);
            else if(style==3)b.rot(0,a*57.2958f,0).cylinder(x,0,z,.21f,.15f,.21f,0,GOLD);
            else b.sphere(x,style==5?(float)Math.sin(a*3f)*.20f:0,z,.135f,.16f,.135f,SurrealCatalog.COLORS[(i+d.id)%12]);
        }}
    }
    private static void block(Builder b,SurrealCatalog.Item d,float[] c){
        b.box(0,0,0,.85f,.85f,.85f,c);b.box(0,0,.867f,.71f,.71f,.018f,WHITE);
        digit(b,d.a,0,0,.90f,.82f,DARK);
        if(d.b==1){for(int i=0;i<d.a;i++)b.sphere((i%3-1)*.38f,.88f,(i/3-1)*.38f,.07f,.035f,.07f,WHITE);}
        else if(d.b==2){for(int i=0;i<d.a;i++){float a=i*6.283185f/d.a;b.sphere((float)Math.sin(a)*.54f,.89f,(float)Math.cos(a)*.54f,.11f,.06f,.11f,GOLD);}}
        else if(b.detailed){for(int i=-1;i<=1;i+=2)for(int j=-1;j<=1;j+=2)b.sphere(i*.80f,j*.80f,.87f,.06f,.06f,.04f,GOLD);}
    }
    private static void domino(Builder b,SurrealCatalog.Item d){
        b.box(0,0,0,.88f,.73f,.95f,WHITE);b.box(0,.765f,0,.86f,.015f,.025f,DARK);
        pips(b,d.a,-.48f);pips(b,d.b,.48f);
        if(b.detailed){b.box(0,-.735f,0,.8f,.012f,.86f,DARK);b.sphere(0,.79f,0,.055f,.025f,.055f,GOLD);}
    }
    private static void pips(Builder b,int n,float z){
        if((n&1)==1)b.sphere(0,.77f,z,.11f,.028f,.067f,DARK);
        if(n>=2){b.sphere(-.45f,.77f,z-.19f,.11f,.028f,.067f,DARK);b.sphere(.45f,.77f,z+.19f,.11f,.028f,.067f,DARK);}
        if(n>=4){b.sphere(.45f,.77f,z-.19f,.11f,.028f,.067f,DARK);b.sphere(-.45f,.77f,z+.19f,.11f,.028f,.067f,DARK);}
        if(n==6){b.sphere(-.45f,.77f,z,.11f,.028f,.067f,DARK);b.sphere(.45f,.77f,z,.11f,.028f,.067f,DARK);}
    }
    private static void brick(Builder b,SurrealCatalog.Item d,float[] c){
        int style=d.variant/8,cols=d.a,rows=d.b;
        if(style==3){b.box(-.66f,-.05f,0,.29f,.60f,.87f,c);b.box(.66f,-.05f,0,.29f,.60f,.87f,c);b.box(0,.51f,0,.94f,.19f,.87f,c);}
        else if(style==2){b.box(-.49f,0,0,.45f,.58f,.88f,c);b.box(.46f,0,-.47f,.48f,.58f,.41f,c);}
        else if(style==4){for(int i=0;i<4;i++){float h=.20f+i*.12f;b.box(0,-.57f+h,-.67f+i*.43f,.94f,h,.23f,c);}}
        else {b.box(0,.12f,0,.94f,.50f,.88f,c);if(b.detailed){b.box(-.86f,-.52f,0,.08f,.15f,.88f,c);b.box(.86f,-.52f,0,.08f,.15f,.88f,c);b.box(0,-.52f,-.80f,.78f,.15f,.08f,c);b.box(0,-.52f,.80f,.78f,.15f,.08f,c);}}
        for(int x=0;x<cols;x++)for(int z=0;z<rows;z++){
            if(style==2&&x>=cols/2f&&z>=rows/2f)continue;
            float px=-.78f+(x+.5f)*1.56f/cols,pz=-.72f+(z+.5f)*1.44f/rows;
            float y=style==4?-.32f+(pz+.72f)*.55f:.74f;
            b.cylinder(px,y,pz,.48f/cols,.12f,.43f/rows,1,c);
            if(b.detailed){b.box(px,y+.125f,pz,.14f/cols,.007f,.09f/rows,WHITE);if(style<2)b.torus(px,-.62f,pz,.40f/Math.max(cols,rows),.025f,c);}
        }
    }

    private static void toy(Builder b,int shape,int edition,float[] c){
        float[] accent=edition==0?WHITE:GOLD;
        if(shape==0||shape==26){
            b.sphere(0,-.18f,0,.48f,.59f,.32f,c);b.sphere(0,.51f,0,.43f,.43f,.36f,c);
            for(int s=-1;s<=1;s+=2){b.sphere(s*.32f,shape==26?.93f:.79f,0,.16f,shape==26?.43f:.16f,.12f,c);b.sphere(s*.51f,-.1f,0,.22f,.33f,.20f,c);b.sphere(s*.27f,-.65f,.07f,.25f,.25f,.30f,c);b.sphere(s*.14f,.57f,.34f,.055f,.055f,.035f,DARK);}
            b.sphere(0,.37f,.31f,.20f,.13f,.12f,accent);b.sphere(0,.42f,.42f,.065f,.045f,.03f,DARK);
            if(b.detailed)for(int i=0;i<7;i++)b.box(0,-.42f+i*.07f,.316f,.024f,.007f,.016f,DARK);
        }else if(shape==1||shape==14){
            b.box(0,-.05f,0,.40f,.44f,.25f,c);b.sphere(0,.62f,0,.36f,.30f,.29f,accent);
            for(int s=-1;s<=1;s+=2){b.cylinder(s*.56f,-.09f,0,.12f,.43f,.12f,1,c);b.box(s*.24f,-.67f,.02f,.14f,.26f,.22f,c);b.sphere(s*.14f,.67f,.27f,.085f,.085f,.065f,DARK);}
            b.cylinder(0,.99f,0,.035f,.13f,.035f,1,METAL);b.sphere(0,1.1f,0,.09f,.09f,.09f,RED);b.box(0,.11f,.265f,.23f,.14f,.025f,BLUE);
            if(shape==14){b.rot(180,0,0).cylinder(0,-.29f,0,.28f,.30f,.28f,1.9f,c);for(int i=-2;i<=2;i++)b.sphere(i*.13f,.83f,-.05f,.14f,.13f,.25f,GOLD);}
        }else if(shape==2||shape==5||shape==24){
            b.rot(shape==2?0:90,0,0).cylinder(0,0,0,.28f,.69f,.28f,.65f,c);
            b.rot(shape==2?0:90,0,0).cylinder(0,shape==2?.85f:0,shape==2?0:.85f,.22f,.22f,.22f,0,accent);
            b.box(0,-.30f,0,shape==2?.57f:.91f,.07f,.21f,accent);b.sphere(0,.24f,.25f,.12f,.18f,.065f,BLUE);
            if(shape==24){b.cylinder(0,.43f,0,.04f,.19f,.04f,1,METAL);b.box(0,.64f,0,.96f,.035f,.07f,DARK);}
        }else if(shape==3||shape==9||shape==13||shape==25){
            b.box(0,-.05f,0,.55f,.25f,.78f,c);b.box(0,.31f,-.10f,.41f,.25f,.38f,accent);b.box(0,.36f,.29f,.33f,.16f,.025f,BLUE);
            for(int s=-1;s<=1;s+=2)for(int f=-1;f<=1;f+=2){b.rot(0,0,90).cylinder(s*.60f,-.27f,f*.50f,.23f,.12f,.23f,1,DARK);b.rot(0,0,90).cylinder(s*.73f,-.27f,f*.50f,.10f,.018f,.10f,1,METAL);}
            if(shape==9){b.cylinder(0,.51f,.57f,.13f,.33f,.13f,.85f,DARK);b.rot(90,0,0).cylinder(0,.05f,.83f,.26f,.13f,.26f,1,GOLD);}
            if(shape==13)b.torus(0,.73f,0,.31f,.10f,c);
            if(shape==25){b.cylinder(0,.73f,-.30f,.035f,.40f,.035f,1,METAL);b.rot(32,0,0).cylinder(0,1.02f,-.30f,.30f,.06f,.30f,.2f,WHITE);}
        }else if(shape==4||shape==19||shape==11){
            b.sphere(0,-.14f,0,.49f,.41f,.70f,c);b.sphere(0,.37f,.48f,.33f,.36f,.30f,c);b.rot(90,0,0).cylinder(0,.28f,.87f,.17f,.22f,.17f,.1f,accent);
            for(int s=-1;s<=1;s+=2){b.sphere(s*.17f,.46f,.73f,.055f,.055f,.028f,DARK);b.sphere(s*.48f,-.12f,-.06f,.12f,.22f,.39f,accent);}
            if(shape==11)for(int i=0;i<5;i++)b.cylinder(0,.33f,-.60f+i*.24f,.13f,.22f,.13f,0,accent);
            if(shape==19){b.box(0,-.11f,-.84f,.51f,.30f,.055f,accent);b.cylinder(0,.41f,-.2f,.22f,.20f,.22f,0,accent);}
        }else if(shape==6||shape==29){b.cylinder(0,-.28f,0,.62f,.44f,.62f,0,c);b.cylinder(0,.08f,0,.62f,.08f,.62f,1,accent);b.cylinder(0,.39f,0,.08f,.30f,.08f,1,DARK);if(shape==29){b.rot(90,0,0).torus(0,0,0,.48f,.16f,c);}}
        else if(shape==7||shape==16){b.cylinder(0,-.76f,0,.66f,.09f,.66f,1,accent);b.cylinder(0,0,0,.06f,.85f,.06f,1,WHITE);for(int i=0;i<5;i++){float r=.57f-i*.09f;if(shape==7)b.torus(0,-.52f+i*.27f,0,r,.10f,SurrealCatalog.COLORS[i]);else b.cylinder(0,-.48f+i*.31f,0,r,.27f,r,0,SurrealCatalog.COLORS[i]);}}
        else if(shape==8){b.cylinder(0,0,0,.67f,.50f,.67f,1,c);b.cylinder(0,.51f,0,.70f,.055f,.70f,1,WHITE);b.cylinder(0,-.51f,0,.70f,.055f,.70f,1,accent);for(int i=0;i<8;i++){float a=i*.7854f;b.cylinder((float)Math.sin(a)*.66f,0,(float)Math.cos(a)*.66f,.035f,.46f,.035f,1,accent);}}
        else if(shape==10){b.box(0,-.22f,0,.70f,.14f,.86f,accent);for(int i=0;i<7;i++){b.box(0,0,-.72f+i*.24f,.64f-i*.046f,.095f,.087f,SurrealCatalog.COLORS[i]);if(b.detailed)for(int side=-1;side<=1;side+=2)b.sphere(side*(.52f-i*.04f),.105f,-.72f+i*.24f,.025f,.016f,.025f,METAL);}}
        else if(shape==12){b.sphere(0,-.43f,0,.55f,.25f,.8f,c);b.cylinder(0,.20f,0,.035f,.76f,.035f,1,accent);b.box(.31f,.32f,0,.28f,.47f,.025f,WHITE);b.box(-.24f,.22f,0,.20f,.38f,.025f,RED);}
        else if(shape==15){b.cylinder(0,-.31f,0,.13f,.62f,.13f,1,accent);b.torus(0,.42f,0,.36f,.13f,c);for(int i=0;i<5;i++){float a=i*1.25664f;b.sphere((float)Math.sin(a)*.35f,.43f,(float)Math.cos(a)*.35f,.13f,.14f,.13f,SurrealCatalog.COLORS[i]);}}
        else if(shape==17){b.box(0,-.32f,0,.80f,.41f,.59f,c);for(int s=-1;s<=1;s+=2){b.cylinder(s*.65f,.24f,0,.27f,.65f,.27f,1,accent);b.cylinder(s*.65f,.99f,0,.35f,.24f,.35f,0,RED);}b.box(0,-.38f,.61f,.21f,.32f,.03f,DARK);}
        else if(shape==18){b.rot(60,0,0).cylinder(0,.32f,0,.25f,.70f,.25f,1,c);for(int s=-1;s<=1;s+=2)b.rot(0,0,s*24).cylinder(s*.22f,-.38f,0,.055f,.52f,.055f,1,METAL);}
        else if(shape==20){b.cylinder(0,-.6f,0,.84f,.08f,.84f,1,c);b.cylinder(0,.10f,0,.09f,.73f,.09f,1,GOLD);b.cylinder(0,.84f,0,.93f,.28f,.93f,0,accent);for(int i=0;i<4;i++){float a=i*1.5708f;b.cylinder((float)Math.sin(a)*.6f,.05f,(float)Math.cos(a)*.6f,.035f,.63f,.035f,1,GOLD);b.sphere((float)Math.sin(a)*.6f,-.26f,(float)Math.cos(a)*.6f,.19f,.15f,.25f,c);}}
        else if(shape==21){b.cylinder(0,.23f,0,.07f,.76f,.07f,1,accent);b.box(0,-.65f,.03f,.35f,.31f,.08f,c);b.rot(90,0,0).torus(0,.90f,0,.22f,.055f,c);}
        else if(shape==22){b.cylinder(0,-.2f,0,.51f,.55f,.51f,1.35f,c);b.torus(0,.35f,0,.69f,.06f,accent);b.rot(90,0,0).torus(0,.51f,0,.63f,.045f,METAL);}
        else if(shape==23){b.sphere(0,0,0,.24f,.33f,.70f,c);b.sphere(0,.61f,.46f,.20f,.44f,.22f,c);for(int s=-1;s<=1;s+=2){b.box(s*.33f,-.68f,0,.075f,.07f,.96f,accent);for(int f=-1;f<=1;f+=2)b.cylinder(s*.25f,-.38f,f*.43f,.055f,.37f,.055f,1,c);}}
        else {b.sphere(0,.21f,0,.53f,.47f,.53f,c);for(int i=0;i<8;i++){float a=i*.7854f;b.rot(0,a*57.2958f,25).cylinder((float)Math.sin(a)*.62f,-.20f,(float)Math.cos(a)*.62f,shape==27?.12f:.06f,.40f,shape==27?.12f:.06f,1,accent);if(shape==28)b.sphere((float)Math.sin(a)*.77f,-.50f,(float)Math.cos(a)*.77f,.14f,.08f,.14f,METAL);}eyes(b,0,.32f,.48f);}
        // Edition changes geometry: wind-up key, rivets and mechanical sockets.
        if(edition==1){b.rot(0,0,90).cylinder(.65f,0,-.20f,.045f,.25f,.045f,1,METAL);b.rot(90,0,0).torus(.88f,.12f,-.20f,.13f,.035f,GOLD);b.rot(90,0,0).torus(.88f,-.12f,-.20f,.13f,.035f,GOLD);}
    }

    private static void relic(Builder b,int shape,int edition,float[] c){
        if(shape==0||shape==3||shape==13||shape==14){
            b.box(0,0,0,.75f,.58f,.50f,c);b.box(-.10f,.08f,.52f,.49f,.39f,.035f,shape==0?BLUE:DARK);
            if(shape==0){b.rot(0,0,-28).cylinder(-.28f,.96f,0,.025f,.41f,.025f,1,METAL);b.rot(0,0,28).cylinder(.28f,.96f,0,.025f,.41f,.025f,1,METAL);}
            if(shape==14)b.rot(90,0,0).cylinder(0,0,.64f,.30f,.25f,.30f,.8f,DARK);
            for(int i=0;i<4;i++)b.box(.56f,-.25f+i*.14f,.54f,.08f,.025f,.02f,METAL);
            if(shape==3&&b.detailed){for(int i=0;i<8;i++)b.box(-.10f,-.23f+i*.078f,.56f,.42f,.012f,.015f,METAL);b.sphere(.55f,.41f,.58f,.065f,.065f,.035f,WHITE);}
            if(shape==13){for(int i=-1;i<=1;i+=2)b.box(i*.28f,.6f,0,.10f,.02f,.31f,DARK);if(b.detailed){b.sphere(.81f,.16f,0,.08f,.06f,.08f,DARK);for(int n=0;n<6;n++)b.box(-.51f+n*.19f,-.42f,.52f,.012f,.085f,.025f,METAL);}}
        }else if(shape==1){b.box(0,-.32f,0,.67f,.26f,.63f,c);b.torus(0,-.04f,0,.29f,.085f,WHITE);for(int s=-1;s<=1;s+=2)b.sphere(s*.52f,.29f,0,.22f,.18f,.3f,c);b.box(0,.42f,0,.5f,.10f,.13f,c);}
        else if(shape==2){b.rot(90,0,0).cylinder(0,0,0,.65f,.22f,.65f,1,GOLD);b.sphere(0,0,.24f,.57f,.57f,.035f,WHITE);b.rot(0,0,25).box(0,.17f,.29f,.035f,.20f,.02f,DARK);b.rot(0,0,-55).box(.12f,0,.30f,.03f,.24f,.02f,DARK);for(int s=-1;s<=1;s+=2)b.sphere(s*.46f,.65f,0,.30f,.18f,.26f,c);}
        else if(shape==4){b.box(0,-.74f,0,.75f,.07f,.75f,DARK);b.cylinder(0,0,0,.50f,.70f,.50f,0,RED);b.torus(0,-.12f,0,.29f,.075f,WHITE);}
        else if(shape==5||shape==12){b.box(0,-.07f,0,.74f,.57f,.34f,c);b.rot(90,0,0).torus(0,.61f,0,.25f,.06f,METAL);for(int s=-1;s<=1;s+=2)b.box(s*.51f,0,.35f,.065f,.57f,.035f,GOLD);if(shape==12)b.box(0,.25f,.38f,.42f,.025f,.015f,DARK);}
        else if(shape==6){b.sphere(0,-.18f,0,.55f,.49f,.55f,c);b.cylinder(0,.40f,0,.39f,.07f,.39f,1,GOLD);b.sphere(0,.56f,0,.13f,.13f,.13f,GOLD);b.rot(90,0,0).torus(-.58f,-.01f,0,.28f,.07f,c);b.rot(0,0,-48).cylinder(.55f,.06f,0,.15f,.35f,.15f,.65f,c);}
        else if(shape==7){b.box(0,-.57f,0,.62f,.21f,.57f,c);b.cylinder(0,-.32f,0,.43f,.035f,.43f,1,DARK);b.rot(35,0,-30).cylinder(.21f,.34f,0,.54f,.52f,.54f,.12f,GOLD);}
        else if(shape==8){b.cylinder(0,-.1f,0,.33f,.69f,.33f,1,RED);b.box(0,.65f,0,.24f,.045f,.11f,DARK);b.cylinder(.35f,.1f,0,.045f,.52f,.045f,1,DARK);b.box(0,0,.34f,.19f,.28f,.02f,WHITE);}
        else if(shape==9||shape==10){b.torus(0,0,0,.61f,shape==9?.23f:.12f,shape==9?DARK:METAL);for(int i=0;i<(shape==9?16:12);i++){float a=i*6.283185f/(shape==9?16:12);b.rot(0,a*57.2958f,0).box((float)Math.sin(a)*.79f,0,(float)Math.cos(a)*.79f,.09f,.19f,.12f,shape==9?DARK:METAL);}}
        else if(shape==11||shape==18){b.cylinder(0,-.15f,0,.055f,.81f,.055f,1,METAL);b.box(0,.49f,0,.24f,.41f,.17f,DARK);for(int i=0;i<3;i++)b.sphere(0,.74f-i*.25f,.185f,.12f,.12f,.04f,shape==11?GOLD:SurrealCatalog.COLORS[i]);if(shape==11){b.cylinder(0,1.02f,0,.39f,.20f,.39f,0,METAL);b.sphere(0,.52f,0,.24f,.34f,.24f,GOLD);}else for(int i=0;i<3;i++)b.box(0,.87f-i*.25f,.24f,.18f,.025f,.11f,METAL);}
        else if(shape==15){b.sphere(0,-.44f,.25f,.43f,.25f,.67f,c);b.cylinder(0,0,-.1f,.35f,.43f,.35f,1,c);for(int i=0;i<5;i++)b.box(0,-.16f+i*.1f,.27f,.27f,.02f,.02f,WHITE);}
        else if(shape==16){b.sphere(0,.11f,0,.70f,.72f,.64f,WHITE);b.sphere(0,.12f,.44f,.51f,.42f,.25f,DARK);b.torus(0,-.49f,0,.5f,.09f,METAL);}
        else if(shape==17){b.sphere(0,0,0,.40f,.40f,.40f,METAL);for(int s=-1;s<=1;s+=2){b.box(s*.75f,0,0,.4f,.025f,.50f,BLUE);if(b.detailed)for(int i=-2;i<=2;i++)b.box(s*.75f,.031f,i*.18f,.39f,.008f,.008f,WHITE);}b.cylinder(0,.64f,0,.035f,.31f,.035f,1,METAL);}
        else {b.cylinder(0,.71f,0,.54f,.08f,.54f,1,c);b.cylinder(0,-.71f,0,.54f,.08f,.54f,1,c);b.cylinder(0,.34f,0,.44f,.34f,.44f,0,GOLD);b.rot(180,0,0).cylinder(0,-.34f,0,.44f,.34f,.44f,0,BLUE);for(int i=0;i<4;i++){float a=i*1.57f;b.cylinder((float)Math.sin(a)*.48f,0,(float)Math.cos(a)*.48f,.035f,.69f,.035f,1,METAL);}}
        if(edition==1){b.torus(0,-.80f,0,.66f,.06f,BLUE);for(int i=-1;i<=1;i++)b.sphere(i*.23f,.33f,.61f,.05f,.05f,.035f,GOLD);b.box(0,-.34f,-.61f,.23f,.10f,.10f,METAL);}
    }
    private static void eyes(Builder b,float x,float y,float z){for(int s=-1;s<=1;s+=2){b.sphere(x+s*.19f,y,z,.14f,.16f,.09f,WHITE);b.sphere(x+s*.19f,y,z+.09f,.06f,.08f,.025f,DARK);}}
    private static void digit(Builder b,int digit,float x,float y,float z,float q,float[] c){int[] masks={63,6,91,79,102,109,125,7,127,111};int mask=masks[digit%10];float[][] p={{0,.46f,.28f,.04f},{.30f,.24f,.04f,.20f},{.30f,-.24f,.04f,.20f},{0,-.46f,.28f,.04f},{-.30f,-.24f,.04f,.20f},{-.30f,.24f,.04f,.20f},{0,0,.28f,.04f}};for(int i=0;i<7;i++)if((mask&(1<<i))!=0)b.box(x+p[i][0]*q,y+p[i][1]*q,z,p[i][2]*q,p[i][3]*q,.013f,c);}

    /** Geometry builder with correct outward winding and per-part inverse-transpose normals. */
    static final class Builder {
        final boolean detailed;int detailScale=1;float[] v=new float[16384];short[] ix=new short[8192];int nv,ni,parts;float rx,ry,rz;
        Builder(boolean detailed){this.detailed=detailed;}
        Builder rot(float x,float y,float z){rx=x*.017453293f;ry=y*.017453293f;rz=z*.017453293f;return this;}
        void vertex(float x,float y,float z,float nx,float ny,float nz,float cx,float cy,float cz,float sx,float sy,float sz,float[] c){
            if(nv+9>v.length)v=Arrays.copyOf(v,v.length*2);
            float[] p=transform(x*sx,y*sy,z*sz),n=transform(nx/sx,ny/sy,nz/sz);float len=(float)Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
            v[nv++]=p[0]+cx;v[nv++]=p[1]+cy;v[nv++]=p[2]+cz;v[nv++]=n[0]/len;v[nv++]=n[1]/len;v[nv++]=n[2]/len;v[nv++]=c[0];v[nv++]=c[1];v[nv++]=c[2];
        }
        float[] transform(float x,float y,float z){float a=x*(float)Math.cos(rz)-y*(float)Math.sin(rz),b=x*(float)Math.sin(rz)+y*(float)Math.cos(rz);float yy=b*(float)Math.cos(rx)-z*(float)Math.sin(rx),zz=b*(float)Math.sin(rx)+z*(float)Math.cos(rx);return new float[]{a*(float)Math.cos(ry)+zz*(float)Math.sin(ry),yy,-a*(float)Math.sin(ry)+zz*(float)Math.cos(ry)};}
        void tri(int a,int b,int c){if(ni+3>ix.length)ix=Arrays.copyOf(ix,ix.length*2);if(a>=65535||b>=65535||c>=65535)throw new IllegalStateException("mesh exceeds uint16");ix[ni++]=(short)a;ix[ni++]=(short)b;ix[ni++]=(short)c;}
        void end(){rx=ry=rz=0;parts++;}
        void box(float x,float y,float z,float sx,float sy,float sz,float[] c){
            float[][] n={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for(int f=0;f<6;f++){float nx=n[f][0],ny=n[f][1],nz=n[f][2];float ux=ny==0?0:1,uy=ny==0?1:0,uz=0;float vx=ny*uz-nz*uy,vy=nz*ux-nx*uz,vz=nx*uy-ny*ux;int base=nv/9;
                for(int k=0;k<4;k++){float a=k==0||k==3?-1:1,b=k<2?-1:1;vertex(nx+ux*a+vx*b,ny+uy*a+vy*b,nz+uz*a+vz*b,nx,ny,nz,x,y,z,sx,sy,sz,c);}tri(base,base+1,base+2);tri(base,base+2,base+3);
            }end();
        }
        void sphere(float x,float y,float z,float sx,float sy,float sz,float[] c){
            int slices=detailed?12*detailScale:6,stacks=detailed?8*detailScale:4,base=nv/9;
            for(int iy=0;iy<=stacks;iy++){float phi=iy*(float)Math.PI/stacks;for(int i=0;i<=slices;i++){float a=i*6.283185f/slices,px=(float)Math.sin(phi)*(float)Math.sin(a),py=(float)Math.cos(phi),pz=(float)Math.sin(phi)*(float)Math.cos(a);vertex(px,py,pz,px,py,pz,x,y,z,sx,sy,sz,c);}}
            for(int iy=0;iy<stacks;iy++)for(int i=0;i<slices;i++){int a=base+iy*(slices+1)+i,b=a+slices+1;tri(a,b,a+1);tri(a+1,b,b+1);}end();
        }
        void cylinder(float x,float y,float z,float sx,float sy,float sz,float top,float[] c){
            int seg=detailed?12*detailScale:6,base=nv/9;float slope=(1-top)*.5f;
            for(int i=0;i<=seg;i++){float a=i*6.283185f/seg,s=(float)Math.sin(a),co=(float)Math.cos(a);vertex(s,-1,co,s,slope,co,x,y,z,sx,sy,sz,c);vertex(s*top,1,co*top,s,slope,co,x,y,z,sx,sy,sz,c);}
            for(int i=0;i<seg;i++){int a=base+i*2;tri(a,a+2,a+1);tri(a+2,a+3,a+1);}
            for(int cap=-1;cap<=1;cap+=2){int center=nv/9;vertex(0,cap,0,0,cap,0,x,y,z,sx,sy,sz,c);for(int i=0;i<=seg;i++){float a=i*6.283185f/seg,r=cap==1?top:1;vertex((float)Math.sin(a)*r,cap,(float)Math.cos(a)*r,0,cap,0,x,y,z,sx,sy,sz,c);}for(int i=0;i<seg;i++){if(cap==1)tri(center,center+1+i,center+2+i);else tri(center,center+2+i,center+1+i);}}
            end();
        }
        void torus(float x,float y,float z,float r,float tube,float[] c){
            int seg=detailed?20:10,side=detailed?8:4,base=nv/9;
            for(int i=0;i<=seg;i++){float a=i*6.283185f/seg;for(int j=0;j<=side;j++){float t=j*6.283185f/side,co=(float)Math.cos(t),si=(float)Math.sin(t),sn=(float)Math.sin(a),cs=(float)Math.cos(a);vertex(sn*(r+tube*co),tube*si,cs*(r+tube*co),sn*co,si,cs*co,x,y,z,1,1,1,c);}}
            for(int i=0;i<seg;i++)for(int j=0;j<side;j++){int a=base+i*(side+1)+j,b=a+side+1;tri(a,b,a+1);tri(a+1,b,b+1);}end();
        }
        Data finish(){
            // Fit every mesh into its body envelope while preserving proportions per category.
            float max=0;for(int i=0;i<nv;i+=9)max=Math.max(max,Math.max(Math.abs(v[i]),Math.max(Math.abs(v[i+1]),Math.abs(v[i+2]))));
            float scale=.98f/Math.max(.98f,max);for(int i=0;i<nv;i+=9){v[i]*=scale;v[i+1]*=scale;v[i+2]*=scale;}
            Data out=new Data();out.vertices=Arrays.copyOf(v,nv);out.indices=Arrays.copyOf(ix,ni);out.parts=parts;return out;
        }
    }
}
