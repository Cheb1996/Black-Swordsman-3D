package com.danil.blackswordsman;
/** User preferences are independent of save-game state. */
public final class GameSettings {
 public volatile float sensitivity=1,controlScale=1,shake=.7f,dream=1,sfx=.8f,ambience=.45f;
 public volatile int fps=60;public volatile boolean diagnostics;
 public static final String[] LABELS={"Чувствительность камеры","Размер кнопок","Тряска камеры","Искажения мира","Звуки и шаги","Атмосфера","Частота кадров","Диагностика"};
 public float value(int n){return n==0?sensitivity:n==1?controlScale:n==2?shake:n==3?dream:n==4?sfx:n==5?ambience:n==6?fps:diagnostics?1:0;}
 public String text(int n){return n==6?fps+" FPS":n==7?(diagnostics?"Вкл":"Выкл"):Math.round(value(n)*100)+"%";}
 public void adjust(int n,int d){float v=value(n)+d*.1f;if(n==0)sensitivity=limit(v,.3f,2);if(n==1)controlScale=limit(v,.8f,1.12f);if(n==2)shake=limit(v,0,1);if(n==3)dream=limit(v,0,1);if(n==4)sfx=limit(v,0,1);if(n==5)ambience=limit(v,0,1);if(n==6)fps=d>0?(fps==30?60:120):(fps==120?60:30);if(n==7)diagnostics=!diagnostics;}
 private float limit(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
