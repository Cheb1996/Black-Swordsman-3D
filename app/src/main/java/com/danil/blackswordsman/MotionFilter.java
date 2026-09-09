package com.danil.blackswordsman;

/** Testable accelerometer neutral-position filter used by the Android sensor bridge. */
public final class MotionFilter {
    private float ax,ay,az,baseX,baseY,tiltX,tiltY,sumX,sumY;
    private int samples;
    private int lastRotation=-1;
    private boolean calibrating=true;

    public synchronized void sample(float x,float y,float z,int displayRotation){
        if(displayRotation!=lastRotation){lastRotation=displayRotation;sumX=sumY=0;samples=0;calibrating=true;tiltX=tiltY=0;}
        final float smoothing=.14f;ax+=(x-ax)*smoothing;ay+=(y-ay)*smoothing;az+=(z-az)*smoothing;
        if(calibrating){sumX+=x;sumY+=y;samples++;tiltX=tiltY=0;if(samples>=36){baseX=sumX/samples;baseY=sumY/samples;calibrating=false;}return;}
        float dx=ax-baseX,dy=ay-baseY,rawX,rawY;
        if(displayRotation==3){rawX=-dy/4.2f;rawY=dx/4.6f;}
        else if(displayRotation==1){rawX=dy/4.2f;rawY=-dx/4.6f;}
        else{rawX=dx/4.2f;rawY=dy/4.6f;}
        tiltX+=(deadZone(clamp(rawX,-1,1))-tiltX)*.18f;tiltY+=(deadZone(clamp(rawY,-1,1))-tiltY)*.18f;
    }
    public synchronized void calibrate(){baseX=ax;baseY=ay;tiltX=tiltY=0;samples=0;sumX=sumY=0;calibrating=false;}
    public synchronized float getTiltX(){return calibrating?0:tiltX;}public synchronized float getTiltY(){return calibrating?0:tiltY;}public synchronized boolean isCalibrating(){return calibrating;}
    private static float deadZone(float value){float abs=Math.abs(value);if(abs<.13f)return 0;return Math.signum(value)*(abs-.13f)/.87f;}
    private static float clamp(float value,float min,float max){return value<min?min:value>max?max:value;}
}
