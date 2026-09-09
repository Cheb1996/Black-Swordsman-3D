package com.danil.blackswordsman;

/** Verifies that every non-combat button causes the intended game-state transition. */
public final class StateButtonSmoke {
    private static final class Motion implements GameWorld.MotionInput {public float getTiltX(){return 0;}public float getTiltY(){return 0;}public float consumeGyroYaw(){return 0;}public float consumeGyroPitch(){return 0;}public boolean hasGyroscope(){return true;}public boolean hasAccelerometer(){return true;}public void calibrate(){}}
    private static final class Store implements GameWorld.ProgressStore {GameWorld.Progress p;int clears;public GameWorld.Progress load(){return p;}public void save(GameWorld.Progress v){p=v.copy();}public void clear(){p=null;clears++;}}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        Store store=new Store();GameInput input=new GameInput();GameWorld world=new GameWorld(input,new Motion(),store);
        input.pressMenu();world.update(.016f);check(world.mode==GameWorld.STORY&&store.clears==1,"new game");int line=world.storyLine;input.pressAction();world.update(.016f);check(world.storyLine==line+1,"story next");input.pressMenu();world.update(.016f);check(world.mode==GameWorld.MENU,"story back");
        input.pressAction();world.update(.016f);check(world.mode==GameWorld.STORY,"continue");
        for(int choice=0;choice<3;choice++){GameInput i=new GameInput();GameWorld w=new GameWorld(i,new Motion(),new Store());w.mode=GameWorld.UPGRADE;w.chapter=0;GameWorld.Progress before=w.getProgressCopy();i.chooseUpgrade(choice);w.update(.016f);GameWorld.Progress after=w.getProgressCopy();check(w.mode==GameWorld.STORY&&w.chapter==1,"upgrade transition "+choice);check(choice==0?after.attack>before.attack:choice==1?after.maxHp>before.maxHp:after.maxAmmo>before.maxAmmo,"upgrade effect "+choice);}
        input=new GameInput();world=new GameWorld(input,new Motion(),new Store());world.mode=GameWorld.PLAYING;input.pressPause();world.update(.016f);check(world.mode==GameWorld.PAUSED,"pause");input.pressAction();world.update(.016f);check(world.mode==GameWorld.PLAYING,"resume");input.pressPause();world.update(.016f);input.pressMenu();world.update(.016f);check(world.mode==GameWorld.MENU,"pause menu");
        world.mode=GameWorld.GAME_OVER;input.pressAction();world.update(.016f);check(world.mode==GameWorld.PLAYING,"retry");world.mode=GameWorld.GAME_OVER;input.pressMenu();world.update(.016f);check(world.mode==GameWorld.MENU,"game over menu");world.mode=GameWorld.ENDING;input.pressAction();world.update(.016f);check(world.mode==GameWorld.MENU,"ending menu");
        System.out.println("StateButtonSmoke OK: menu, story, upgrades, pause, retry and ending");
    }
}
