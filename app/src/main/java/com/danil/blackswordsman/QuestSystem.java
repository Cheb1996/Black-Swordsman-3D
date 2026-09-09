package com.danil.blackswordsman;

/** Persistent chapter itinerary: discovery, interaction, encounter and actual exit. */
public final class QuestSystem implements java.io.Serializable {
    private static final long serialVersionUID=6L;
    public static final int REACH=0,INTERACT=1,COMBAT=2,EXIT=3;
    public int stage,chapter;public boolean combatComplete,complete;public float survival;
    public PhysicsWorld.Body marker;
    private static final String[][] TEXT={
        {"Найдите разбитую телегу у дороги","Освободите Пака из клетки · ДЕЙСТВИЕ","Разбейте засаду у северной дороги","Доберитесь до ворот Коки"},
        {"Поговорите с жителями площади","Откройте клетку с пленниками · ДЕЙСТВИЕ","Защитите городскую площадь","Войдите в крепость Барона"},
        {"Найдите змеиный механизм","Активируйте печать у ворот · ДЕЙСТВИЕ","Поставьте груз на плиту и сразите Барона","Покиньте крепость через северные ворота"},
        {"Перейдите мост в долину виселиц","Зажгите сигнальный огонь · ДЕЙСТВИЕ","Защитите огонь от мертвецов","Дождитесь рассвета у выхода из долины"},
        {"Найдите укрытие Варгаса","Заберите карту стоков · ДЕЙСТВИЕ","Прорвите оцепление площади","Найдите настоящий вход в старые стоки"},
        {"Спуститесь к затопленным камерам","Откройте заслонку стоков · ДЕЙСТВИЕ","Откройте механизм и уничтожьте тварей","Доберитесь до лестницы в башню"},
        {"Найдите караульное помещение","Освободите пленника · ДЕЙСТВИЕ","Прорвитесь через тюремщиков","Войдите в тронный зал"},
        {"Осмотрите следы у входа в зал","Пробудите кровавую печать · ДЕЙСТВИЕ","Откройте печать и уничтожьте Графа","Коснитесь раскрытого бехелита у врат"},
        {"Найдите осколок астрального пути","Активируйте якорь мира · ДЕЙСТВИЕ","Отразите тварей Бездны","Удержитесь у выходных врат до их открытия"}
    };
    public void reset(GameWorld w){stage=0;chapter=w.chapter;combatComplete=complete=false;survival=0;marker=w.physics.createStatic(PhysicsWorld.CART,x(),w.physics.terrainHeight(x(),24)+.6f,24,1.2f,.6f,.7f);}
    private static final float[][] ROUTE_X={{8,2,0,0},{-6,6,0,0},{6,7,0,0},{2,3,0,0},{-7,-6,0,0},{-6,6,0,0},{7,-6,0,0},{-6,7,0,0},{-9,8,0,0}};
    public float x(){return ROUTE_X[chapter][stage];}
    public float z(){return stage==0?24:stage==1?48:stage==2?78:118;}
    public int kind(){return stage==0?REACH:stage==1?INTERACT:stage==2?COMBAT:EXIT;}
    public float distance(GameWorld w){float dx=w.player.x-x(),dz=w.player.z-z();return (float)Math.sqrt(dx*dx+dz*dz);}
    public boolean battleReady(GameWorld w){return stage==2&&distance(w)<20&&w.physics.isPuzzleSolved();}
    public void update(GameWorld w,float dt){
        if(complete)return;float d=distance(w);
        if(stage==0&&d<4){stage=1;w.banner="СЛЕД ОБНАРУЖЕН — ОСМОТРИТЕ МЕСТО";w.bannerTime=2.2f;marker=w.physics.createStatic(chapter==0||chapter==1||chapter==6?PhysicsWorld.CAGE:PhysicsWorld.ALTAR,x(),w.physics.terrainHeight(x(),z())+.85f,z(),.9f,.85f,.9f);w.saveCheckpoint();}
        if(stage==2&&combatComplete){stage=3;d=distance(w);w.banner="ПУТЬ ОТКРЫТ — ДОБЕРИТЕСЬ ДО ВЫХОДА";w.bannerTime=2;w.saveCheckpoint();}
        if(stage==3&&d<5){if(chapter==3||chapter==8){survival+=dt;if(survival>=18)complete=true;}else complete=true;}
        w.objective=TEXT[chapter][stage]+((stage==3&&(chapter==3||chapter==8))?" · "+(int)Math.ceil(Math.max(0,18-survival))+" с":"")+" · "+(int)d+" м";
    }
    public boolean interact(GameWorld w){
        if(stage!=1||distance(w)>3.6f)return false;stage=2;if(chapter==0)w.spawnPuck();if(marker!=null){marker.solid=false;marker.variant=9;marker.r=.12f;marker.g=.5f;marker.b=.30f;}w.banner=chapter==0?"ПАК ОСВОБОЖДЁН: Я ПОЙДУ С ТОБОЙ":"ПУТЬ К ЦЕЛИ ОБНАРУЖЕН";w.bannerTime=2.8f;w.saveCheckpoint();return true;
    }
    public String hint(GameWorld w){return stage==1&&distance(w)<3.6f?TEXT[chapter][stage]:"";}
}
