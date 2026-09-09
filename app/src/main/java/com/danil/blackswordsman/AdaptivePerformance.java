package com.danil.blackswordsman;

import android.content.Context;
import android.os.PowerManager;
import android.os.Process;

import java.lang.reflect.Method;

/** ADPF-aware frame pacing with reflection so the compact build remains API-24 compatible. */
public final class AdaptivePerformance {
    private final PowerManager power;private final Context context;private long targetNs=16666667L;
    private Object hintSession;
    private Method reportWork;
    private Method setTarget;
    private long averageNs = 16666667L;
    private long lastThermalCheck;
    private int thermalStatus;
    private int quality = 3;
    private int slowFrames;
    private int fastFrames;
    private Method preferEfficiency;
    private boolean efficiencyEnabled;

    public AdaptivePerformance(Context context) {
        this.context=context;power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
    }
    public void bindRenderThread(){close();
        try {
            Object manager=context.getSystemService("performance_hint");
            if(manager!=null){
                Method create=manager.getClass().getMethod("createHintSession",int[].class,long.class);
                hintSession=create.invoke(manager,new int[]{Process.myTid()},Long.valueOf(targetNs));
                if(hintSession!=null){
                    reportWork=hintSession.getClass().getMethod("reportActualWorkDuration",long.class);
                    setTarget=hintSession.getClass().getMethod("updateTargetWorkDuration",long.class);
                    try { preferEfficiency=hintSession.getClass().getMethod("setPreferPowerEfficiency",boolean.class);preferEfficiency.invoke(hintSession,Boolean.FALSE); } catch(Throwable ignored) { }
                }
            }
        } catch(Throwable ignored) { hintSession=null; }
    }

    public int reportFrame(long durationNs,long frameNs) {
        averageNs=(averageNs*29L+Math.max(durationNs,frameNs))/30L;
        if(hintSession!=null&&reportWork!=null)try{reportWork.invoke(hintSession,Long.valueOf(durationNs));}catch(Throwable ignored){}
        long now=System.nanoTime();
        if(now-lastThermalCheck>1000000000L){
            lastThermalCheck=now;
            try { Method m=PowerManager.class.getMethod("getCurrentThermalStatus");thermalStatus=((Integer)m.invoke(power)).intValue(); } catch(Throwable ignored){thermalStatus=0;}
        }
        if(thermalStatus>=4){quality=1;slowFrames=0;fastFrames=0;}
        else if(thermalStatus>=3){quality=Math.min(quality,2);slowFrames=0;}
        else {
            if(averageNs>targetNs*1.25){slowFrames++;fastFrames=0;}else if(durationNs<targetNs*.68&&averageNs<targetNs*1.1){fastFrames++;slowFrames=0;}else{slowFrames=0;fastFrames=0;}
            if(slowFrames>75&&quality>1){quality--;slowFrames=0;}
            if(fastFrames>360&&quality<3){quality++;fastFrames=0;}
        }
        boolean wantEfficiency=thermalStatus>=3||quality==1;
        if(wantEfficiency!=efficiencyEnabled&&hintSession!=null&&preferEfficiency!=null){try{preferEfficiency.invoke(hintSession,Boolean.valueOf(wantEfficiency));efficiencyEnabled=wantEfficiency;}catch(Throwable ignored){}}
        return quality;
    }

    public void setTargetFrameNs(long ns){targetNs=ns;if(hintSession!=null&&setTarget!=null)try{setTarget.invoke(hintSession,Long.valueOf(ns));}catch(Throwable ignored){}}
    public int getThermalStatus(){return thermalStatus;}
    public long getAverageNs(){return averageNs;}
    public void close(){if(hintSession!=null)try{hintSession.getClass().getMethod("close").invoke(hintSession);}catch(Throwable ignored){}hintSession=null;}
}
