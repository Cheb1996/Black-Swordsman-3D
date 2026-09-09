package com.danil.blackswordsman;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Single-activity, edge-to-edge host for the native OpenGL game. */
public final class MainActivity extends Activity {
    private GameSurfaceView surface;
    private GameRenderer renderer;
    private MotionController motion;
    private AdaptivePerformance performance;
    private FeedbackEngine feedback;
    private GameInput input;
    private GameWorld world;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);configureWindow();
        input=new GameInput();motion=new MotionController(this);performance=new AdaptivePerformance(this);feedback=new FeedbackEngine(this);
        try{EnemyDefinitions.load(getAssets().open("enemies.tsv"));SurrealCatalog.load(getAssets().open("items.tsv"));}catch(java.io.IOException invalid){throw new IllegalStateException("Invalid game content",invalid);}
        world=new GameWorld(input,motion,new AndroidProgressStore(this));world.settings=AndroidProgressStore.loadSettings(this);
        HudView hud=new HudView(this,world,input);renderer=new GameRenderer(this,world,performance,feedback);renderer.setHud(hud);surface=new GameSurfaceView(this,renderer);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);root.addView(surface,new FrameLayout.LayoutParams(-1,-1));root.addView(hud,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
    }

    private void configureWindow(){
        Window window=getWindow();window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);window.setStatusBarColor(Color.TRANSPARENT);window.setNavigationBarColor(Color.TRANSPARENT);
        try{Window.class.getMethod("setSustainedPerformanceMode",boolean.class).invoke(window,Boolean.TRUE);}catch(Throwable ignored){}
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        try{Method m=Window.class.getMethod("setDecorFitsSystemWindows",boolean.class);m.invoke(window,Boolean.FALSE);}catch(Throwable ignored){}
        try{WindowManager.LayoutParams lp=window.getAttributes();Field field=WindowManager.LayoutParams.class.getField("layoutInDisplayCutoutMode");field.setInt(lp,3);window.setAttributes(lp);}catch(Throwable ignored){}
        try{Display display=getWindowManager().getDefaultDisplay();Display.Mode current=display.getMode(),best=current;Display.Mode[] modes=display.getSupportedModes();for(int i=0;i<modes.length;i++){Display.Mode m=modes[i];if(m.getPhysicalWidth()==current.getPhysicalWidth()&&m.getPhysicalHeight()==current.getPhysicalHeight()&&m.getRefreshRate()>best.getRefreshRate())best=m;}WindowManager.LayoutParams lp=window.getAttributes();lp.preferredDisplayModeId=best.getModeId();window.setAttributes(lp);long target=(long)(1000000000.0/Math.min(120f,best.getRefreshRate()));performanceTargetLater=target;}catch(Throwable ignored){}
    }

    private long performanceTargetLater=16666667L;
    @Override protected void onResume(){super.onResume();if(surface!=null)surface.onResume();if(motion!=null)motion.resume();if(performance!=null)performance.setTargetFrameNs(performanceTargetLater);hideSystemUi();}
    @Override protected void onPause(){
        if(input!=null)input.resetAll();if(surface!=null&&world!=null){final java.util.concurrent.CountDownLatch saved=new java.util.concurrent.CountDownLatch(1);surface.queueEvent(new Runnable(){public void run(){try{world.pauseAndSave();}finally{saved.countDown();}}});try{saved.await(700,java.util.concurrent.TimeUnit.MILLISECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}
        if(feedback!=null)feedback.pause();if(motion!=null)motion.pause();if(surface!=null)surface.onPause();super.onPause();
    }
    @Override protected void onDestroy(){if(surface!=null&&renderer!=null)surface.queueEvent(new Runnable(){@Override public void run(){renderer.release();}});if(performance!=null)performance.close();if(feedback!=null)feedback.release();super.onDestroy();}
    private void hideSystemUi(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)hideSystemUi();else if(input!=null)input.resetAll();}
    @Override public void onBackPressed(){if(world==null){super.onBackPressed();return;}if(world.settingsOpen){world.settingsOpen=false;AndroidProgressStore.saveSettings(this,world.settings);return;}if(world.mode==GameWorld.PLAYING){input.pressPause();return;}if(world.mode==GameWorld.PAUSED||world.mode==GameWorld.GAME_OVER){input.pressMenu();return;}if(world.mode==GameWorld.STORY||world.mode==GameWorld.UPGRADE){input.pressMenu();return;}super.onBackPressed();}
}
