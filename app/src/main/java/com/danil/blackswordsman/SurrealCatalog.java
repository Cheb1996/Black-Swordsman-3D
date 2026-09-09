package com.danil.blackswordsman;

import java.util.ArrayList;

/** Stable, named geometry identities. Colour alone never creates a catalog entry. */
public final class SurrealCatalog {
    public static final int BALL=0,RING=1,BLOCK=2,DOMINO=3,BRICK=4,TOY=5,RELIC=6;
    public static final int COUNT=240;
    public static final class Item {
        public final int id,family,variant,a,b;
        public final String name;
        public final float hx,hy,hz,mass,r,g,blue;
        Item(int id,int family,int variant,int a,int b,String name,float hx,float hy,float hz,float mass){
            this.id=id;this.family=family;this.variant=variant;this.a=a;this.b=b;this.name=name;
            this.hx=hx;this.hy=hy;this.hz=hz;this.mass=mass;
            float[] color=COLORS[(id*7+family*3)%COLORS.length];r=color[0];g=color[1];blue=color[2];
        }
    }
    public static final float[][] COLORS={{.96f,.08f,.16f},{1f,.48f,.035f},{.98f,.83f,.08f},{.12f,.8f,.26f},{.02f,.66f,.94f},{.20f,.18f,.92f},{.68f,.10f,.9f},{1f,.22f,.58f},{.06f,.91f,.76f},{.9f,.88f,.77f},{.16f,.18f,.24f},{.52f,.28f,.12f}};
    public static final String[] TOY_NAMES={"Медведь","Заводной робот","Ракета","Гоночная машинка","Резиновая утка","Самолёт","Волчок","Пирамидка","Барабан","Паровоз","Ксилофон","Динозавр","Кораблик","Телефон на колёсах","Кукла","Погремушка","Ёлочка","Игрушечный замок","Телескоп","Рыба","Карусель","Лопатка","Ведёрко","Лошадка-качалка","Вертолёт","Луноход","Плюшевый заяц","Осьминог","Робот-паук","Йо-йо"};
    public static final String[] RELIC_NAMES={"Кинескопный телевизор","Дисковый телефон","Часы с колоколами","Радиоприёмник","Дорожный конус","Чемодан","Чайник","Граммофон","Огнетушитель","Автомобильная шина","Шестерня","Уличный фонарь","Почтовый ящик","Тостер","Фотокамера","Ботинок","Шлем космонавта","Спутник","Светофор","Песочные часы"};
    public static final Item[] ITEMS=build();
    public static void load(java.io.InputStream input)throws java.io.IOException{java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(input,"UTF-8"));Item[] next=new Item[COUNT];String line;while((line=r.readLine())!=null){if(line.length()==0||line.startsWith("#"))continue;String[] v=line.split("\t");if(v.length!=10)throw new java.io.IOException("Item columns");int id=Integer.parseInt(v[0]),family=Integer.parseInt(v[1]),variant=Integer.parseInt(v[2]),a=Integer.parseInt(v[3]),b=Integer.parseInt(v[4]);if(id<0||id>=COUNT||family<0||family>6||next[id]!=null)throw new java.io.IOException("Item id");float[] values=new float[4];for(int n=0;n<4;n++){values[n]=Float.parseFloat(v[n+6]);if(Float.isNaN(values[n])||Float.isInfinite(values[n])||values[n]<=0)throw new java.io.IOException("Item dimensions");}next[id]=new Item(id,family,variant,a,b,v[5],values[0],values[1],values[2],values[3]);}r.close();for(int id=0;id<COUNT;id++){if(next[id]==null)throw new java.io.IOException("Missing item "+id);}System.arraycopy(next,0,ITEMS,0,COUNT);}
    private SurrealCatalog(){}
    private static Item[] build(){
        ArrayList<Item> out=new ArrayList<Item>();
        String[] balls={"Мяч с меридианами","Полосатый мяч","Мяч в горошек","Шипастый мяч","Шар с кольцами","Глаз-шар","Мяч с лунками","Шар-погремушка"};
        for(int size=0;size<3;size++)for(int v=0;v<8;v++){float s=.18f+size*.075f;add(out,BALL,v,size,0,balls[v]+" · "+(size+1),s,s,s,.16f+size*.14f);}
        for(int v=0;v<24;v++){float s=.24f+(v/6)*.045f;add(out,RING,v,v%6+3,v/6,"Кольцо "+new String[]{"гладкое","ребристое","бусинное","звёздчатое","двойное","спиральное"}[v%6]+" · "+(v/6+1),s,.075f+(v/6)*.018f,s,.22f);}
        for(int v=0;v<24;v++){float s=.19f+(v/8)*.025f;add(out,BLOCK,v,v%8+1,v/8,new String[]{"Куб с цифрой ","Кубик-счёт ","Рельефный куб "}[v/8]+(v%8+1),s,s,s,.30f);}
        for(int a=0;a<=6;a++)for(int b=a;b<=6;b++)add(out,DOMINO,a*7+b,a,b,"Домино "+a+"|"+b,.15f,.065f,.30f,.20f);
        for(int v=0;v<40;v++){int x=1+v%4,z=2+(v/4)%2,style=v/8;float h=style==1?.045f:style==3?.16f:.105f;add(out,BRICK,v,x,z,new String[]{"Кирпич","Пластина","Угловая деталь","Арка","Скос"}[style]+" "+x+"×"+z,.085f*x,h,.085f*z,.12f+x*z*.026f);}
        for(int v=0;v<60;v++){int shape=v%30,edition=v/30;float s=edition==0?.38f:.47f;add(out,TOY,v,shape,edition,TOY_NAMES[shape]+(edition==0?" · классическая":" · механическая"),s,s,s,.7f+edition*.25f);}
        for(int v=0;v<40;v++){int shape=v%20,edition=v/20;float s=.40f+edition*.16f;add(out,RELIC,v,shape,edition,RELIC_NAMES[shape]+(edition==0?" · старый мир":" · другая эпоха"),s,s,s,2.1f+edition*1.3f);}
        if(out.size()!=COUNT)throw new IllegalStateException("catalog size");
        return out.toArray(new Item[out.size()]);
    }
    private static void add(ArrayList<Item> out,int f,int v,int a,int b,String name,float x,float y,float z,float mass){out.add(new Item(out.size(),f,v,a,b,name,x,y,z,mass));}
}
