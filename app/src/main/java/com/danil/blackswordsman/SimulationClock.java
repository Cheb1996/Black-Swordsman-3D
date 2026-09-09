package com.danil.blackswordsman;

/** Rendering cadence never changes gameplay time. At most 250 ms is recovered after a stall. */
public final class SimulationClock {
    public interface Step { void simulate(float dt); }
    public static final float STEP=1f/120f;
    private double remaining;
    public int advance(float elapsed,Step simulation){
        if(Float.isNaN(elapsed)||elapsed<=0)return 0;
        remaining+=Math.min(.25,elapsed);int n=0;
        while(remaining+1e-9>=STEP&&n<31){simulation.simulate(STEP);remaining-=STEP;n++;}
        return n;
    }
    public void reset(){remaining=0;}
    public static float damping(float perSecond,float dt){return (float)Math.exp(-perSecond*dt);}
}
