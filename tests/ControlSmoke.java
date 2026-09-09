package com.danil.blackswordsman;

/** Verifies every HUD button center and safe-inset-adjusted hit target. */
public final class ControlSmoke {
    private static void eq(int actual,int expected,String label){if(actual!=expected)throw new AssertionError(label+": "+actual+" != "+expected);}
    public static void main(String[] args){
        float inset=37f;
        eq(ControlMap.battle(1144-inset,584,inset),ControlMap.LIGHT,"light");
        eq(ControlMap.battle(1004-inset,628,inset),ControlMap.HEAVY,"heavy");
        eq(ControlMap.battle(1210-inset,448,inset),ControlMap.DODGE,"dodge");
        eq(ControlMap.battle(1086-inset,423,inset),ControlMap.CANNON,"cannon");
        eq(ControlMap.battle(935-inset,492,inset),ControlMap.RAGE,"rage");
        eq(ControlMap.battle(835-inset,620,inset),ControlMap.INTERACT,"physics interaction");
        eq(ControlMap.battle(1048-inset,56,inset),ControlMap.TILT,"tilt movement");
        eq(ControlMap.battle(1120-inset,56,inset),ControlMap.GYRO,"gyro");
        eq(ControlMap.battle(1192-inset,56,inset),ControlMap.CALIBRATE,"calibrate");
        eq(ControlMap.battle(1238-inset,126,inset),ControlMap.PAUSE,"pause");
        eq(ControlMap.battle(690-inset,650,inset),ControlMap.JUMP,"jump");eq(ControlMap.battle(745-inset,535,inset),ControlMap.LOCK,"lock");eq(ControlMap.battle(685-inset,590,inset),ControlMap.PLACE,"place");eq(ControlMap.battle(745-inset,680,inset),ControlMap.ROTATE,"rotate");
        if(!ControlMap.joystick(155+29,565,29))throw new AssertionError("safe joystick");
        eq(ControlMap.overlay(GameWorld.MENU,320,465),ControlMap.PRIMARY,"continue");
        eq(ControlMap.overlay(GameWorld.MENU,320,550),ControlMap.SECONDARY,"new game");
        eq(ControlMap.overlay(GameWorld.STORY,1,1),ControlMap.PRIMARY,"story next");
        eq(ControlMap.overlay(GameWorld.UPGRADE,250,400),ControlMap.UPGRADE_1,"upgrade 1");
        eq(ControlMap.overlay(GameWorld.UPGRADE,640,400),ControlMap.UPGRADE_2,"upgrade 2");
        eq(ControlMap.overlay(GameWorld.UPGRADE,1030,400),ControlMap.UPGRADE_3,"upgrade 3");
        eq(ControlMap.overlay(GameWorld.PAUSED,640,366),ControlMap.PRIMARY,"pause resume");
        eq(ControlMap.overlay(GameWorld.PAUSED,640,460),ControlMap.SECONDARY,"pause menu");
        eq(ControlMap.overlay(GameWorld.GAME_OVER,640,376),ControlMap.PRIMARY,"retry");
        eq(ControlMap.overlay(GameWorld.GAME_OVER,640,473),ControlMap.SECONDARY,"game-over menu");
        eq(ControlMap.overlay(GameWorld.ENDING,20,20),ControlMap.PRIMARY,"ending");
        eq(ControlMap.battle(640,360,inset),ControlMap.NONE,"camera area");
        System.out.println("ControlSmoke OK: all 25 HUD actions mapped");
    }
}
