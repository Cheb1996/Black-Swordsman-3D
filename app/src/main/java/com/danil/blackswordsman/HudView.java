package com.danil.blackswordsman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

import java.io.InputStream;
import java.util.ArrayList;

/** Multitouch virtual controls and comic-style Russian story presentation. */
public final class HudView extends View {
    private static final float DESIGN_W=1280f,DESIGN_H=720f;
    private final GameWorld world;
    private final GameInput input;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface condensed=Typeface.create("sans-serif-condensed",Typeface.BOLD);
    private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL);
    private Bitmap title;
    private final TouchOwnership touch=new TouchOwnership();
    private float joystickCenterX=155f,joystickCenterY=565f,knobX=155f,knobY=565f;
    private float lastCameraX,lastCameraY;
    private float sx=1f,sy=1f,safeLeft,safeRight,safeTop;

    public HudView(Context context,GameWorld world,GameInput input){
        super(context);this.world=world;this.input=input;setFocusable(true);setClickable(true);setBackgroundColor(Color.TRANSPARENT);
        stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(2f);stroke.setColor(0x88ffffff);
        try{InputStream in=context.getAssets().open("title-3d.png");title=BitmapFactory.decodeStream(in);in.close();}
        catch(Exception first){try{InputStream in=context.getAssets().open("title-legacy.png");title=BitmapFactory.decodeStream(in);in.close();}catch(Exception ignored){}}
        setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener(){@Override public WindowInsets onApplyWindowInsets(View v,WindowInsets insets){safeLeft=insets.getStableInsetLeft();safeRight=insets.getStableInsetRight();safeTop=insets.getStableInsetTop();try{Object cutout=insets.getClass().getMethod("getDisplayCutout").invoke(insets);if(cutout!=null){safeLeft=Math.max(safeLeft,((Integer)cutout.getClass().getMethod("getSafeInsetLeft").invoke(cutout)).intValue());safeRight=Math.max(safeRight,((Integer)cutout.getClass().getMethod("getSafeInsetRight").invoke(cutout)).intValue());safeTop=Math.max(safeTop,((Integer)cutout.getClass().getMethod("getSafeInsetTop").invoke(cutout)).intValue());}}catch(Exception ignored){}return insets;}});
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);sx=getWidth()/DESIGN_W;sy=getHeight()/DESIGN_H;
        canvas.save();canvas.scale(sx,sy);
        if(world.settingsOpen)drawSettings(canvas);
        else if(world.mode==GameWorld.MENU)drawMenu(canvas);
        else if(world.mode==GameWorld.STORY)drawStory(canvas);
        else if(world.mode==GameWorld.PLAYING)drawBattle(canvas);
        else if(world.mode==GameWorld.UPGRADE)drawUpgrade(canvas);
        else if(world.mode==GameWorld.PAUSED)drawPause(canvas);
        else if(world.mode==GameWorld.GAME_OVER)drawGameOver(canvas);
        else if(world.mode==GameWorld.ENDING)drawEnding(canvas);
        if(world.bannerTime>0f&&world.mode!=GameWorld.MENU)drawBanner(canvas);
        canvas.restore();
    }

    private void drawMenu(Canvas c){
        if(title!=null){paint.setAlpha(225);c.drawBitmap(title,null,new RectF(0,0,1280,720),paint);paint.setAlpha(255);}
        paint.setColor(0x88000000);c.drawRect(0,0,1280,720,paint);
        paint.setColor(0xbb08080b);c.drawRoundRect(new RectF(72,55,620,676),8,8,paint);stroke.setColor(0x667e1a25);stroke.setStrokeWidth(3);c.drawRoundRect(new RectF(72,55,620,676),8,8,stroke);
        label(c,"BLACK SWORDSMAN",105,137,48,0xfff1ede1,Paint.Align.LEFT,condensed);
        label(c,"v8.0 · MYCELIUM / RED STAR",108,175,17,0xffb6363f,Paint.Align.LEFT,condensed);
        paint.setColor(0xff7a1620);c.drawRect(108,195,520,199,paint);
        drawParagraph(c,"Физическая 3D-песочница по мотивам арки «Чёрный мечник»: девять глав, скелетные персонажи, разрушаемый бой и дорога через Междумирье.",108,235,445,24,0xffd3cec3,regular,4);
        String sensor=(world.gyroAvailable?"ГИРОСКОП":"БЕЗ ГИРОСКОПА")+"  •  "+(world.accelerometerAvailable?"АКСЕЛЕРОМЕТР":"СЕНСОРНЫЙ СТИК");
        label(c,sensor,108,370,14,0xff8f8b84,Paint.Align.LEFT,condensed);
        button(c,new RectF(108,430,540,500),world.hasSave?"ПРОДОЛЖИТЬ":"НАЧАТЬ ИГРУ",true);
        button(c,new RectF(108,520,540,580),"НОВАЯ ИГРА",false);
        button(c,new RectF(690,560,1110,622),"НАСТРОЙКИ",false);
        label(c,"ДЕВЯТЬ ДОРОГ ЧЕРЕЗ МЕЖДУМИРЬЕ",108,635,13,0xff77736c,Paint.Align.LEFT,regular);
    }

    private void drawStory(Canvas c){
        paint.setColor(0x8d000000);c.drawRect(0,0,1280,720,paint);
        StoryData.Chapter chapter=world.currentChapter();String[] line=world.currentStoryLine();
        label(c,"ГЛАВА "+chapter.roman,85,68,17,0xffa9232f,Paint.Align.LEFT,condensed);
        label(c,chapter.title,85,111,38,0xffeee9dc,Paint.Align.LEFT,condensed);
        label(c,world.storyOutro?"ПОСЛЕ БОЯ • "+chapter.place.toUpperCase():chapter.place.toUpperCase(),85,141,14,0xff88847d,Paint.Align.LEFT,condensed);
        // Comic panel frame.
        paint.setColor(0xc90a0a0d);c.drawRoundRect(new RectF(70,390,1210,655),5,5,paint);
        stroke.setStrokeWidth(3);stroke.setColor(0xffddd5c4);c.drawRoundRect(new RectF(70,390,1210,655),5,5,stroke);
        paint.setColor(0xffa6202b);c.drawRect(70,390,80,655,paint);
        label(c,line[0],110,438,19,0xffbd303a,Paint.Align.LEFT,condensed);
        drawParagraph(c,line[1],110,478,1030,28,0xfff0ebdf,regular,4);
        label(c,"КОСНИТЕСЬ, ЧТОБЫ ПРОДОЛЖИТЬ  ›",1170,625,14,0xff8f8a82,Paint.Align.RIGHT,condensed);
    }

    private void drawBattle(Canvas c){
        float leftInset=safeLeft/Math.max(.001f,sx),rightInset=safeRight/Math.max(.001f,sx),topInset=safeTop/Math.max(.001f,sy);
        paint.setColor(0x9b060609);c.drawRoundRect(new RectF(28+leftInset,22+topInset,405+leftInset,118+topInset),8,8,paint);
        label(c,world.hudPlayerName,47+leftInset,53+topInset,15,0xffeee9dd,Paint.Align.LEFT,condensed);
        drawBar(c,47+leftInset,66+topInset,330,15,world.hudHealth/Math.max(1f,world.hudMaxHealth),0xffa61f2b,0xff311015);
        drawBar(c,47+leftInset,89+topInset,330,8,world.hudRage/100f,0xffdc6b28,0xff342018);
        label(c,(int)world.hudHealth+" / "+(int)world.hudMaxHealth,377+leftInset,80+topInset,12,0xffded8cd,Paint.Align.RIGHT,condensed);
        label(c,"ЯРОСТЬ",377+leftInset,104+topInset,11,0xffc9a38a,Paint.Align.RIGHT,condensed);
        if(world.hudCreature.length()>0)label(c,world.hudCreature,640,181+topInset,11,0xff82d6d0,Paint.Align.CENTER,condensed);
        label(c,world.biomeLabel,47+leftInset,139+topInset,10,0xff85817a,Paint.Align.LEFT,condensed);

        drawSurrealHud(c,leftInset,topInset);
        label(c,"ГЛАВА "+world.currentChapter().roman+" • "+world.currentChapter().title,640,34+topInset,14,0xffd0cbc1,Paint.Align.CENTER,condensed);
        label(c,world.objective,640,57+topInset,15,0xff908c85,Paint.Align.CENTER,regular);
        label(c,"ВОЛНА "+Math.max(1,world.wave)+"/"+world.waveCount+"   •   ВРАГОВ "+world.enemiesAlive,640,82+topInset,12,0xff77736e,Paint.Align.CENTER,condensed);

        if(world.bossMaxHealth>0){paint.setColor(0xb4070709);c.drawRoundRect(new RectF(385,105,895,159),5,5,paint);label(c,world.bossName,640,127,14,0xffe5ddd1,Paint.Align.CENTER,condensed);drawBar(c,417,138,446,9,world.bossHealth/world.bossMaxHealth,0xff7f1423,0xff291016);}
        if(world.hitChain>1){int rank=world.hitChain>=20?0xfff07a2f:world.hitChain>=10?0xffd83b45:0xffd5cec2;label(c,"x"+world.hitChain,865-rightInset,256,39,rank,Paint.Align.RIGHT,condensed);label(c,world.hitChain>=20?"БЕРСЕРК":world.hitChain>=10?"НЕУДЕРЖИМЫЙ":"СЕРИЯ",865-rightInset,280,12,rank,Paint.Align.RIGHT,condensed);}

        if(world.settings.diagnostics)label(c,world.performanceLabel+"  •  "+world.fps+" FPS  •  PHYS "+world.physics.activeBodies,1237-rightInset,28+topInset,11,0xff8d8981,Paint.Align.RIGHT,condensed);
        smallButton(c,1048-rightInset,56+topInset,58,"TILT",world.tiltEnabled&&world.accelerometerAvailable);
        smallButton(c,1120-rightInset,56+topInset,58,"GYRO",world.gyroEnabled&&world.gyroAvailable);
        smallButton(c,1192-rightInset,56+topInset,58,"CAL",false);
        smallButton(c,1238-rightInset,126+topInset,38,"Ⅱ",false);

        // Joystick.
        paint.setStyle(Paint.Style.FILL);paint.setColor(0x351b1b20);c.drawCircle(joystickCenterX+leftInset,joystickCenterY,103,paint);stroke.setColor(0x547c7973);stroke.setStrokeWidth(3);c.drawCircle(joystickCenterX+leftInset,joystickCenterY,103,stroke);
        paint.setColor(0x78474649);c.drawCircle(knobX+leftInset,knobY,42,paint);stroke.setColor(0x80d2ccc1);c.drawCircle(knobX+leftInset,knobY,42,stroke);
        label(c,"ДВИЖЕНИЕ • НАКЛОН: TILT",155+leftInset,695,11,0xff85817a,Paint.Align.CENTER,condensed);

        float ox=rightInset;
        smallButton(c,690-ox,650,60,"ПРЫЖОК",false);smallButton(c,745-ox,535,60,"ЦЕЛЬ",world.lockedTarget!=null);smallButton(c,685-ox,590,60,"ПОЛОЖ.",false);smallButton(c,745-ox,680,48,"ВРАЩ.",false);
        actionButton(c,1144-ox,584,67,"УДАР","A",0xff74151f,world.player.attackCooldown);
        actionButton(c,1004-ox,628,55,"ТЯЖ.","B",0xff423238,world.player.heavyCooldown);
        actionButton(c,1210-ox,448,53,"РЫВОК","↯",0xff253d49,world.player.dodgeCooldown);
        actionButton(c,1086-ox,423,49,world.player.type==-1?"ПУШКА":"АСТРАЛ",world.player.type==-1?world.hudAmmo+"/"+world.hudMaxAmmo:"✦",0xff5c321e,world.player.cannonCooldown);
        boolean rageReady=world.hudRage>=100f;actionButton(c,935-ox,492,48,"ЯРОСТЬ",rageReady?"MAX":"R",rageReady?0xffa43a1f:0xff37262a,0);
        actionButton(c,835-ox,620,48,"ДЕЙСТВИЕ",world.physics.getHeld()!=null?"БРОС":world.hudPickup.length()>0?"ВЗЯТЬ":"ХВАТ",0xff273b35,0);
        if(world.physics.hasPuzzle()&&!world.physics.isPuzzleSolved()){paint.setColor(0xa20a0a0d);c.drawRoundRect(new RectF(420,640,760,691),5,5,paint);label(c,"ЯЩИК → БАГРОВАЯ ПЛИТА",590,671,14,0xffd7bbb0,Paint.Align.CENTER,condensed);}
    }

    private android.graphics.Bitmap worldMap;private int mapEnvironment=-1;
    private void drawSurrealHud(Canvas c,float left,float top){
        label(c,world.hudEffectAttack,47+left,165+top,12,0xffffc55c,Paint.Align.LEFT,condensed);
        label(c,world.hudEffectDefense,47+left,185+top,12,0xff68e8cc,Paint.Align.LEFT,condensed);
        label(c,world.hudForm,47+left,207+top,11,0xffc998ff,Paint.Align.LEFT,condensed);
        label(c,world.hudMushroom0,47+left,425+top,10,0xff9be8bb,Paint.Align.LEFT,condensed);
        label(c,world.hudMushroom1,47+left,443+top,10,0xff9be8bb,Paint.Align.LEFT,condensed);
        label(c,world.hudMushroom2,47+left,461+top,10,0xff9be8bb,Paint.Align.LEFT,condensed);
        label(c,world.hudMushroom3,47+left,479+top,10,0xff9be8bb,Paint.Align.LEFT,condensed);
        label(c,"НАЙДЕНО "+world.hudDiscovered+" / 240 ВИДОВ",47+left,231+top,12,0xffb8b1c6,Paint.Align.LEFT,condensed);
        float mapX=47+left,mapY=253+top,size=146;
        paint.setColor(0xc20a1018);c.drawRoundRect(new RectF(mapX,mapY,mapX+size,mapY+size),7,7,paint);
        if(worldMap==null||mapEnvironment!=world.currentChapter().environment){mapEnvironment=world.currentChapter().environment;int[] pixels=new int[96*96];for(int yy=0;yy<96;yy++)for(int xx=0;xx<96;xx++){float wx=(xx/95f*2-1)*GameWorld.OCEAN_HALF,wz=(1-yy/95f*2)*GameWorld.OCEAN_HALF;boolean wet=WaterField.depth(mapEnvironment,wx,wz)>.1f;pixels[yy*96+xx]=wet?0xff174555:RegionLayout.biome(wx,wz)==1?0xff6d5a34:0xff344836;}if(worldMap!=null)worldMap.recycle();worldMap=android.graphics.Bitmap.createBitmap(pixels,96,96,android.graphics.Bitmap.Config.ARGB_8888);}
        c.drawBitmap(worldMap,null,new RectF(mapX,mapY,mapX+size,mapY+size),paint);
        for(float[] place:RegionLayout.PLACES){float x=mapX+(place[0]+GameWorld.OCEAN_HALF)/(GameWorld.OCEAN_HALF*2)*size,y=mapY+(GameWorld.OCEAN_HALF-place[1])/(GameWorld.OCEAN_HALF*2)*size;paint.setColor(place[2]==RegionLayout.CAVE?0xffc19be8:0xffd5c697);c.drawRect(x-1.5f,y-1.5f,x+1.5f,y+1.5f,paint);}
        stroke.setColor(0x8069aaa5);stroke.setStrokeWidth(1);c.drawRoundRect(new RectF(mapX,mapY,mapX+size,mapY+size),7,7,stroke);
        for(int i=0;i<SurrealWorld.ZONES.length;i++){float x=mapX+(SurrealWorld.ZONES[i][0]+GameWorld.OCEAN_HALF)/(GameWorld.OCEAN_HALF*2)*size,y=mapY+(GameWorld.OCEAN_HALF-SurrealWorld.ZONES[i][1])/(GameWorld.OCEAN_HALF*2)*size;paint.setColor(i==3||i==8?0xffbe7bfd:i%3==1?0xff60ded1:0xffe2aa65);c.drawCircle(x,y,3.3f,paint);}
        float qx=mapX+(world.hudQuestX+GameWorld.OCEAN_HALF)/(GameWorld.OCEAN_HALF*2)*size,qy=mapY+(GameWorld.OCEAN_HALF-world.hudQuestZ)/(GameWorld.OCEAN_HALF*2)*size;paint.setColor(0xffffd56b);c.drawCircle(qx,qy,5,paint);
        float px=mapX+(world.hudPlayerX+GameWorld.OCEAN_HALF)/(GameWorld.OCEAN_HALF*2)*size,py=mapY+(GameWorld.OCEAN_HALF-world.hudPlayerZ)/(GameWorld.OCEAN_HALF*2)*size;paint.setColor(0xfff1f6ff);c.drawCircle(px,py,4,paint);
        label(c,"ОСТРОВ · РЕКИ · ПОСЕЛЕНИЯ",mapX+size*.5f,mapY+size+16,9,0xffbab3c6,Paint.Align.CENTER,condensed);
        label(c,world.hudZone,640,547,13,0xffa4b9c9,Paint.Align.CENTER,condensed);
        if(world.hudPickup.length()>0){paint.setColor(0xd2111724);c.drawRoundRect(new RectF(354,559,801,604),6,6,paint);label(c,world.hudPickup,577,586,14,0xffefdb97,Paint.Align.CENTER,condensed);}
    }

    private void drawUpgrade(Canvas c){
        paint.setColor(0xdd050507);c.drawRect(0,0,1280,720,paint);label(c,"ГЛАВА ЗАВЕРШЕНА",640,92,18,0xffb12835,Paint.Align.CENTER,condensed);label(c,"ВЫБЕРИТЕ УСИЛЕНИЕ",640,145,42,0xffeee8dc,Paint.Align.CENTER,condensed);
        upgradeCard(c,new RectF(95,220,405,590),"ДРАКОНОБОЙ","+7 к урону","Каждый удар становится тяжелее.",0);
        upgradeCard(c,new RectF(485,220,795,590),"ЖЕЛЕЗНАЯ ВОЛЯ","+45 здоровья • +3% брони","Пережить ещё один удар — значит победить.",1);
        upgradeCard(c,new RectF(875,220,1185,590),"АРСЕНАЛ","+1 снаряд • +скорость","Пушка быстрее решает опасный спор.",2);
    }

    private void drawPause(Canvas c){overlay(c);label(c,"ПАУЗА",640,210,55,0xffeee9dd,Paint.Align.CENTER,condensed);label(c,"Камера: свайп / GYRO  •  Движение: стик; TILT включается отдельно",640,260,16,0xff918d85,Paint.Align.CENTER,regular);button(c,new RectF(440,330,840,402),"ПРОДОЛЖИТЬ",true);button(c,new RectF(440,430,840,492),"В ГЛАВНОЕ МЕНЮ",false);button(c,new RectF(440,520,840,582),"НАСТРОЙКИ",false);}
    private void drawGameOver(Canvas c){overlay(c);label(c,"ВЫ ПОГИБЛИ",640,214,54,0xffb32230,Paint.Align.CENTER,condensed);label(c,"Клеймо ещё горит. Поднимайтесь.",640,265,17,0xffb9b3a9,Paint.Align.CENTER,regular);button(c,new RectF(440,340,840,412),"ПОВТОРИТЬ БОЙ",true);button(c,new RectF(440,442,840,504),"В ГЛАВНОЕ МЕНЮ",false);}
    private void drawEnding(Canvas c){overlay(c);label(c,"КОНЕЦ ПЕРВОЙ ОХОТЫ",640,104,18,0xffa5222f,Paint.Align.CENTER,condensed);label(c,"ЧЁРНЫЙ МЕЧНИК ИДЁТ ДАЛЬШЕ",640,164,40,0xffede7da,Paint.Align.CENTER,condensed);drawParagraph(c,"Граф пал, но дорога к Рукавице Господа только началась. Клеймо зовёт духов каждую ночь, а имя Гриффита остаётся впереди — за горизонтом новой главы.",330,235,620,27,0xffc7c0b5,regular,6);label(c,"УНИЧТОЖЕНО ВРАГОВ: "+world.getProgressCopy().totalKills,640,436,18,0xffad3942,Paint.Align.CENTER,condensed);button(c,new RectF(440,520,840,592),"В ГЛАВНОЕ МЕНЮ",true);}

    private void drawSettings(Canvas c){overlay(c);label(c,"НАСТРОЙКИ",640,65,34,0xffeee9dd,Paint.Align.CENTER,condensed);for(int i=0;i<8;i++){float y=115+i*57;label(c,GameSettings.LABELS[i],250,y+8,20,0xffd8d3cb,Paint.Align.LEFT,regular);button(c,new RectF(755,y-24,815,y+24),"−",false);label(c,world.settings.text(i),885,y+8,18,0xffc9b9ab,Paint.Align.CENTER,regular);button(c,new RectF(955,y-24,1015,y+24),"+",false);}button(c,new RectF(470,615,810,681),"ГОТОВО",true);}
    private void overlay(Canvas c){paint.setColor(0xe4070709);c.drawRect(0,0,1280,720,paint);}
    private void drawBanner(Canvas c){float alpha=Math.min(1f,world.bannerTime*2f);paint.setColor(((int)(alpha*190)<<24)|0x08080b);c.drawRoundRect(new RectF(375,175,940,232),5,5,paint);label(c,world.banner,658,210,17,((int)(alpha*255)<<24)|0x00ece5d8,Paint.Align.CENTER,condensed);}

    private void upgradeCard(Canvas c,RectF rect,String titleText,String stat,String description,int index){paint.setColor(0xff111116);c.drawRoundRect(rect,8,8,paint);stroke.setColor(0xff5f252c);stroke.setStrokeWidth(2);c.drawRoundRect(rect,8,8,stroke);label(c,"0"+(index+1),rect.centerX(),rect.top+68,25,0xff8a1e29,Paint.Align.CENTER,condensed);label(c,titleText,rect.centerX(),rect.top+126,24,0xffe6dfd3,Paint.Align.CENTER,condensed);label(c,stat,rect.centerX(),rect.top+180,17,0xffb83a43,Paint.Align.CENTER,condensed);drawParagraph(c,description,rect.left+36,rect.top+235,rect.width()-72,20,0xffaaa49a,regular,5);label(c,"ВЫБРАТЬ",rect.centerX(),rect.bottom-45,14,0xffddd6ca,Paint.Align.CENTER,condensed);}
    private void button(Canvas c,RectF rect,String text,boolean primary){paint.setColor(primary?0xff861824:0xcc17171b);c.drawRoundRect(rect,5,5,paint);stroke.setColor(primary?0xffd04c54:0xff56524e);stroke.setStrokeWidth(2);c.drawRoundRect(rect,5,5,stroke);label(c,text,rect.centerX(),rect.centerY()+8,20,0xfff0eadf,Paint.Align.CENTER,condensed);}
    private void smallButton(Canvas c,float x,float y,float radius,String text,boolean active){paint.setColor(active?0xb9651a25:0x8a17171c);c.drawCircle(x,y,radius*.5f,paint);stroke.setColor(active?0xffd34851:0x807a7670);stroke.setStrokeWidth(2);c.drawCircle(x,y,radius*.5f,stroke);label(c,text,x,y+4,10,0xffded7cc,Paint.Align.CENTER,condensed);}
    private void actionButton(Canvas c,float x,float y,float radius,String top,String center,int color,float cooldown){radius*=world.settings.controlScale;paint.setColor((color&0x00ffffff)|0xb6000000);c.drawCircle(x,y,radius,paint);stroke.setColor(0xaaccc5b9);stroke.setStrokeWidth(2.5f);c.drawCircle(x,y,radius,stroke);label(c,center,x,y+6,center.length()>3?14:24,0xfff2ece1,Paint.Align.CENTER,condensed);label(c,top,x,y+radius+17,11,0xffaaa59c,Paint.Align.CENTER,condensed);if(cooldown>0){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(6);paint.setColor(0xaa08080a);c.drawArc(new RectF(x-radius+4,y-radius+4,x+radius-4,y+radius-4),-90,Math.min(355,cooldown*210),false,paint);paint.setStyle(Paint.Style.FILL);}}
    private void drawBar(Canvas c,float x,float y,float w,float h,float ratio,int fill,int back){ratio=Math.max(0,Math.min(1,ratio));paint.setColor(back);c.drawRoundRect(new RectF(x,y,x+w,y+h),h*.5f,h*.5f,paint);paint.setColor(fill);c.drawRoundRect(new RectF(x,y,x+w*ratio,y+h),h*.5f,h*.5f,paint);}
    private void label(Canvas c,String text,float x,float y,float size,int color,Paint.Align align,Typeface face){paint.setStyle(Paint.Style.FILL);paint.setTypeface(face);paint.setTextSize(size);paint.setTextAlign(align);paint.setColor(color);c.drawText(text==null?"":text,x,y,paint);}
    private void drawParagraph(Canvas c,String text,float x,float y,float maxWidth,float lineHeight,int color,Typeface face,int maxLines){paint.setTypeface(face);paint.setTextSize(lineHeight*.72f);paint.setColor(color);paint.setTextAlign(Paint.Align.LEFT);ArrayList<String> lines=wrap(text,maxWidth);for(int i=0;i<lines.size()&&i<maxLines;i++)c.drawText(lines.get(i),x,y+i*lineHeight,paint);}
    private ArrayList<String> wrap(String text,float width){ArrayList<String> result=new ArrayList<String>();String[] words=text.split(" ");String line="";for(int i=0;i<words.length;i++){String attempt=line.length()==0?words[i]:line+" "+words[i];if(paint.measureText(attempt)>width&&line.length()>0){result.add(line);line=words[i];}else line=attempt;}if(line.length()>0)result.add(line);return result;}

    @Override public boolean onTouchEvent(MotionEvent e){
        float invX=DESIGN_W/Math.max(1,getWidth()),invY=DESIGN_H/Math.max(1,getHeight());int action=e.getActionMasked(),index=e.getActionIndex();
        if(world.settingsOpen){resetPointers();if(action==MotionEvent.ACTION_UP){float x=e.getX(index)*invX,y=e.getY(index)*invY;if(y>615){world.settingsOpen=false;AndroidProgressStore.saveSettings(getContext(),world.settings);}else for(int i=0;i<8;i++)if(Math.abs(y-(115+i*57))<25){if(x>755&&x<815)world.settings.adjust(i,-1);if(x>955&&x<1015)world.settings.adjust(i,1);}}return true;}
        if(touch.sync(world.mode,input.resetSerial))resetPointers();
        if(action==MotionEvent.ACTION_CANCEL){resetPointers();return true;}
        if(world.mode!=GameWorld.PLAYING){float x=e.getX(index)*invX,y=e.getY(index)*invY;if(action==MotionEvent.ACTION_DOWN)touch.overlayDown(e.getPointerId(index),world.mode,x,y);if(action==MotionEvent.ACTION_UP&&touch.overlayUp(e.getPointerId(index),world.mode,x,y)){handleOverlayTap(x,y);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}return true;}
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){int id=e.getPointerId(index);float x=e.getX(index)*invX,y=e.getY(index)*invY;if(handleBattleButton(x,y))return true;float leftInset=safeLeft/Math.max(.001f,sx);if(ControlMap.joystick(x,y,leftInset)&&touch.joystick<0){touch.acquire(id,true);updateJoystick(x-leftInset,y);}else if(touch.camera<0){touch.acquire(id,false);lastCameraX=x;lastCameraY=y;}return true;}
        if(action==MotionEvent.ACTION_MOVE){float leftInset=safeLeft/Math.max(.001f,sx);for(int n=0;n<e.getPointerCount();n++){int id=e.getPointerId(n);float x=e.getX(n)*invX,y=e.getY(n)*invY;if(id==touch.joystick)updateJoystick(x-leftInset,y);else if(id==touch.camera){input.addCamera(x-lastCameraX,y-lastCameraY);lastCameraX=x;lastCameraY=y;}}return true;}
        if(action==MotionEvent.ACTION_CANCEL){resetPointers();return true;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){int id=e.getPointerId(index);if(id==touch.joystick){touch.joystick=-1;input.clearMovement();knobX=joystickCenterX;knobY=joystickCenterY;}if(id==touch.camera)touch.camera=-1;return true;}return true;
    }

    private boolean handleBattleButton(float x,float y){
        int action=ControlMap.battle(x,y,safeRight/Math.max(.001f,sx),safeTop/Math.max(.001f,sy),world.settings.controlScale);
        if(action==ControlMap.LIGHT)input.pressLight();else if(action==ControlMap.HEAVY)input.pressHeavy();else if(action==ControlMap.DODGE)input.pressDodge();else if(action==ControlMap.CANNON)input.pressCannon();else if(action==ControlMap.RAGE)input.pressRage();else if(action==ControlMap.INTERACT)input.pressInteract();else if(action==ControlMap.TILT)input.toggleTilt();else if(action==ControlMap.GYRO)input.toggleGyro();else if(action==ControlMap.CALIBRATE)input.calibrate();else if(action==ControlMap.PAUSE)input.pressPause();else if(action==ControlMap.JUMP)input.pressJump();else if(action==ControlMap.LOCK)input.pressLock();else if(action==ControlMap.PLACE)input.pressPlace();else if(action==ControlMap.ROTATE)input.pressRotate();else return false;tap();return true;
    }
    private void updateJoystick(float x,float y){float dx=x-joystickCenterX,dy=y-joystickCenterY,len=(float)Math.sqrt(dx*dx+dy*dy),max=78f;if(len>max){dx=dx/len*max;dy=dy/len*max;}knobX=joystickCenterX+dx;knobY=joystickCenterY+dy;input.moveX=dx/max;input.moveY=dy/max;invalidate();}
    private void handleOverlayTap(float x,float y){
        if((world.mode==GameWorld.PAUSED&&x>440&&x<840&&y>520&&y<582)||(world.mode==GameWorld.MENU&&x>690&&x<1110&&y>560&&y<622)){input.resetAll();world.settingsOpen=true;return;}
        int action=ControlMap.overlay(world.mode,x,y);
        if(action==ControlMap.PRIMARY)input.pressAction();else if(action==ControlMap.SECONDARY)input.pressMenu();else if(action==ControlMap.UPGRADE_1)input.chooseUpgrade(0);else if(action==ControlMap.UPGRADE_2)input.chooseUpgrade(1);else if(action==ControlMap.UPGRADE_3)input.chooseUpgrade(2);
    }
    private void tap(){performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
    private void resetPointers(){touch.reset();input.clearMovement();knobX=joystickCenterX;knobY=joystickCenterY;invalidate();}
    @Override public void onWindowFocusChanged(boolean hasWindowFocus){super.onWindowFocusChanged(hasWindowFocus);if(!hasWindowFocus)resetPointers();}
    @Override protected void onDetachedFromWindow(){resetPointers();super.onDetachedFromWindow();}
}
