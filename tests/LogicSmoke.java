package com.danil.blackswordsman;

/** Host-JVM smoke test for campaign flow, combat input and all authored chapters. */
public final class LogicSmoke {
    private static final class Motion implements GameWorld.MotionInput {
        int calibrations;
        float tiltX,tiltY;
        public float getTiltX(){return tiltX;}public float getTiltY(){return tiltY;}
        public float consumeGyroYaw(){return 0;}public float consumeGyroPitch(){return 0;}
        public boolean hasGyroscope(){return true;}public boolean hasAccelerometer(){return true;}public void calibrate(){calibrations++;}
    }
    private static final class Store implements GameWorld.ProgressStore {
        GameWorld.Progress saved;
        public GameWorld.Progress load(){return saved==null?null:saved.copy();}
        public void save(GameWorld.Progress p){saved=p.copy();}
        public void clear(){saved=null;}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(StoryData.CHAPTERS.length==9,"campaign must have nine chapters");
        for(int i=0;i<StoryData.CHAPTERS.length;i++){
            StoryData.Chapter c=StoryData.CHAPTERS[i];
            check(c.intro.length>=5,"chapter "+i+" intro");check(c.outro.length>=4,"chapter "+i+" outro");check(c.waves.length>=2,"chapter "+i+" waves");
            for(int w=0;w<c.waves.length;w++){check(c.waves[w].length%2==0,"wave pairs");for(int n=0;n<c.waves[w].length;n+=2){check(c.waves[w][n]>=0&&c.waves[w][n]<=GameWorld.VOID_BEAST,"enemy id");check(c.waves[w][n+1]>0,"enemy count");}}
        }
        GameInput input=new GameInput();Store store=new Store();Motion motion=new Motion();GameWorld world=new GameWorld(input,motion,store);
        check(world.mode==GameWorld.MENU,"initial menu");input.pressMenu();world.update(.016f);check(world.mode==GameWorld.STORY,"new campaign story");
        int pages=world.currentChapter().intro.length;for(int i=0;i<pages;i++){input.pressAction();world.update(.016f);}
        check(world.mode==GameWorld.PLAYING,"story enters combat");
        float stillX=world.player.x,stillZ=world.player.z;motion.tiltY=-1f;for(int i=0;i<45;i++)world.update(.016f);check(Math.abs(world.player.x-stillX)<.001f&&Math.abs(world.player.z-stillZ)<.001f,"nonzero sensor cannot move player while tilt is disabled");motion.tiltY=0f;
        input.toggleTilt();world.update(.016f);check(world.tiltEnabled&&motion.calibrations==1,"tilt requires explicit toggle and calibration");motion.tiltY=-1f;float tiltStartX=world.player.x,tiltStartZ=world.player.z;for(int i=0;i<18;i++)world.update(.016f);check(Math.abs(world.player.x-tiltStartX)+Math.abs(world.player.z-tiltStartZ)>.08f,"explicit tilt moves player");motion.tiltY=0f;input.toggleTilt();world.update(.016f);check(!world.tiltEnabled,"tilt disables independently");
        for(int i=0;i<150;i++)world.update(.016f);check(world.enemies.size()>0,"first wave spawns");
        GameWorld.Actor enemy=world.enemies.get(0);enemy.spawnTime=0;enemy.x=world.player.x;enemy.z=world.player.z+1.1f;enemy.y=world.player.y;float hp=enemy.hp;
        input.pressLight();world.update(.016f);check(enemy.hp==hp,"light windup");for(int t=0;t<8;t++)world.update(.016f);check(enemy.hp<hp,"active light hit");
        for(int i=0;i<14;i++)world.update(.016f);input.pressLight();world.update(.016f);check(world.player.combo==2,"timed attack continues combo");
        boolean gyro=world.gyroEnabled;input.toggleGyro();world.update(.016f);check(world.gyroEnabled!=gyro,"gyro toggle");
        input.calibrate();world.update(.016f);check(motion.calibrations==2,"calibration button");
        for(int i=0;i<35;i++)world.update(.016f);input.pressDodge();world.update(.016f);check(world.player.invulnerable>0,"dodge grants invulnerability");
        world.player.attackTime=0;world.player.attackCooldown=0;world.player.heavyCooldown=0;world.player.dodgeTime=0;enemy.dead=false;enemy.ragdollSpawned=false;enemy.hp=enemy.maxHp;enemy.x=world.player.x;enemy.z=world.player.z+1.1f;enemy.y=world.player.y;hp=enemy.hp;input.pressHeavy();world.update(.016f);check(enemy.hp==hp,"heavy windup");for(int t=0;t<23;t++)world.update(.016f);check(enemy.hp<hp,"heavy attack button");
        world.player.attackTime=0;world.player.cannonCooldown=0;int ammo=world.hudAmmo;input.pressCannon();world.update(.016f);check(world.hudAmmo==ammo-1,"cannon button");
        world.player.rage=100;input.pressRage();world.update(.016f);check(world.player.rageTime>0,"rage button");
        input.pressPause();world.update(.016f);check(world.mode==GameWorld.PAUSED,"pause button");input.pressAction();world.update(.016f);check(world.mode==GameWorld.PLAYING,"resume button");
        check(store.saved!=null,"progress persisted");System.out.println("LogicSmoke OK: 9 chapters, combat, sensors, save");
    }
}
