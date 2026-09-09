package com.danil.blackswordsman;

/** One authoritative hit map shared by the HUD and host-side button tests. */
public final class ControlMap {
    public static final int NONE=0;
    public static final int LIGHT=1,HEAVY=2,DODGE=3,CANNON=4,RAGE=5;
    public static final int GYRO=6,CALIBRATE=7,PAUSE=8,INTERACT=9,TILT=10,JUMP=11,LOCK=12,PLACE=13,ROTATE=14;
    public static final int PRIMARY=20,SECONDARY=21,UPGRADE_1=22,UPGRADE_2=23,UPGRADE_3=24;

    private ControlMap(){}

    public static int battle(float x,float y,float rightInset){return battle(x,y,rightInset,0);}
    public static int battle(float x,float y,float rightInset,float topInset){return battle(x,y,rightInset,topInset,1);}
    public static int battle(float x,float y,float rightInset,float topInset,float scale){
        if(hit(x,y,1144-rightInset,584,67*scale))return LIGHT;
        if(hit(x,y,1004-rightInset,628,55*scale))return HEAVY;
        if(hit(x,y,1210-rightInset,448,53*scale))return DODGE;
        if(hit(x,y,1086-rightInset,423,49*scale))return CANNON;
        if(hit(x,y,935-rightInset,492,48*scale))return RAGE;
        if(hit(x,y,835-rightInset,620,48*scale))return INTERACT;
        if(hit(x,y,690-rightInset,650,30))return JUMP;
        if(hit(x,y,745-rightInset,535,30))return LOCK;
        if(hit(x,y,685-rightInset,590,30))return PLACE;
        if(hit(x,y,745-rightInset,680,24))return ROTATE;
        y-=topInset;
        if(hit(x,y,1048-rightInset,56,32))return TILT;
        if(hit(x,y,1120-rightInset,56,32))return GYRO;
        if(hit(x,y,1192-rightInset,56,32))return CALIBRATE;
        if(hit(x,y,1238-rightInset,126,38))return PAUSE;
        return NONE;
    }

    public static boolean joystick(float x,float y,float leftInset){return x>25+leftInset&&x<285+leftInset&&y>410&&y<710;}

    public static int overlay(int mode,float x,float y){
        if(mode==GameWorld.STORY||mode==GameWorld.ENDING)return PRIMARY;
        if(mode==GameWorld.MENU){
            if(x>=108&&x<=540&&y>=430&&y<=500)return PRIMARY;
            if(x>=108&&x<=540&&y>=520&&y<=580)return SECONDARY;
        }else if(mode==GameWorld.UPGRADE&&y>=220&&y<=590){
            if(x>=95&&x<=405)return UPGRADE_1;
            if(x>=485&&x<=795)return UPGRADE_2;
            if(x>=875&&x<=1185)return UPGRADE_3;
        }else if(mode==GameWorld.PAUSED){
            if(x>=440&&x<=840&&y>=330&&y<=402)return PRIMARY;
            if(x>=440&&x<=840&&y>=430&&y<=492)return SECONDARY;
        }else if(mode==GameWorld.GAME_OVER){
            if(x>=440&&x<=840&&y>=340&&y<=412)return PRIMARY;
            if(x>=440&&x<=840&&y>=442&&y<=504)return SECONDARY;
        }
        return NONE;
    }

    private static boolean hit(float x,float y,float cx,float cy,float radius){float dx=x-cx,dy=y-cy;return dx*dx+dy*dy<radius*radius;}
}
