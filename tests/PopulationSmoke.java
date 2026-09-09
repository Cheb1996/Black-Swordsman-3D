package com.danil.blackswordsman;

/** Verifies streamed lore populations, peaceful fauna, magic and wave isolation. */
public final class PopulationSmoke {
    private static final class Motion implements GameWorld.MotionInput {public float getTiltX(){return 0;}public float getTiltY(){return 0;}public float consumeGyroYaw(){return 0;}public float consumeGyroPitch(){return 0;}public boolean hasGyroscope(){return true;}public boolean hasAccelerometer(){return true;}public void calibrate(){}}
    private static final class Store implements GameWorld.ProgressStore {public GameWorld.Progress load(){return null;}public void save(GameWorld.Progress p){}public void clear(){}}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int chapter=0;chapter<StoryData.CHAPTERS.length;chapter++){
            GameInput input=new GameInput();GameWorld world=new GameWorld(input,new Motion(),new Store());world.chapter=chapter;world.mode=GameWorld.STORY;world.storyLine=StoryData.CHAPTERS[chapter].intro.length-1;input.pressAction();world.update(.016f);
            check(world.wildlife.size()>9,"chapter "+chapter+" populated with neutral life");check(world.ambientEnemies>0,"chapter "+chapter+" ambient threats");check(world.enemiesAlive==0,"ambient threats do not block waves");
            boolean puck=false,magic=false;for(int i=0;i<world.wildlife.size();i++){GameWorld.Neutral n=world.wildlife.get(i);if(n.companion&&n.type==GameWorld.ELF)puck=true;if(n.magical)magic=true;}
            check(chapter==0?!puck:puck,"Puck must be rescued in first chapter "+chapter);check(magic,"magical nature represented in chapter "+chapter);
            for(int i=0;i<world.enemies.size();i++){GameWorld.Actor e=world.enemies.get(i);if(e.ambient)check(loreType(StoryData.CHAPTERS[chapter].environment,e.type),"lore population chapter "+chapter+" type "+e.type);}
            world.player.x=96;world.player.z=72;world.update(.016f);check(world.wildlife.size()<50&&world.enemies.size()<35,"bounded population streaming");
        }
        System.out.println("PopulationSmoke OK: lore threats, peaceful fauna, Puck and astral creatures");
    }
    private static boolean loreType(int env,int t){
        if(env==0)return t==GameWorld.BANDIT||t==GameWorld.WRAITH||t==GameWorld.CAPTAIN;
        if(env==1)return t==GameWorld.GUARD||t==GameWorld.CROSSBOW||t==GameWorld.BANDIT;
        if(env==2)return t==GameWorld.SERPENT_SPAWN||t==GameWorld.GUARD||t==GameWorld.CROSSBOW;
        if(env==3)return t==GameWorld.SKELETON||t==GameWorld.WRAITH||t==GameWorld.BONE_KNIGHT;
        if(env==4)return t==GameWorld.ZEALOT||t==GameWorld.GUARD||t==GameWorld.CROSSBOW||t==GameWorld.EXECUTIONER;
        if(env==5)return t==GameWorld.GHOUL||t==GameWorld.FLESH_HOUND||t==GameWorld.WRAITH;
        if(env==6)return t==GameWorld.JAILER||t==GameWorld.GUARD||t==GameWorld.ZEALOT||t==GameWorld.GHOUL;
        if(env==7)return t==GameWorld.APOSTLE_SPAWN||t==GameWorld.GHOUL||t==GameWorld.FLESH_HOUND;
        return t==GameWorld.SHADE||t==GameWorld.WRAITH||t==GameWorld.VOID_BEAST;
    }
}
