package com.danil.blackswordsman;
/** Pointer identities survive index reordering but never survive mode/input resets. */
public final class TouchOwnership {
 public int joystick=-1,camera=-1;private int mode=-1,serial=-1,overlayPointer=-1,overlayMode=-1;private float downX,downY;
 public boolean sync(int nextMode,int nextSerial){if(mode==nextMode&&serial==nextSerial)return false;reset();mode=nextMode;serial=nextSerial;return true;}
 public void reset(){joystick=camera=overlayPointer=-1;}
 public boolean acquire(int id,boolean stick){if(stick){if(joystick>=0)return false;joystick=id;}else{if(camera>=0)return false;camera=id;}return true;}
 public boolean release(int id){boolean stick=id==joystick;if(stick)joystick=-1;if(id==camera)camera=-1;return stick;}
 public void overlayDown(int id,int currentMode,float x,float y){overlayPointer=id;overlayMode=currentMode;downX=x;downY=y;}
 public boolean overlayUp(int id,int currentMode,float x,float y){boolean valid=id==overlayPointer&&currentMode==overlayMode&&Math.abs(x-downX)<40&&Math.abs(y-downY)<40;overlayPointer=-1;return valid;}
}
