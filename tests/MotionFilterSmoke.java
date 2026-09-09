package com.danil.blackswordsman;

/** Regression test for the former constant-forward accelerometer drift. */
public final class MotionFilterSmoke {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        MotionFilter filter=new MotionFilter();
        for(int i=0;i<35;i++){filter.sample(0,9.81f,0,1);check(filter.getTiltX()==0&&filter.getTiltY()==0,"no input during warm-up");}
        filter.sample(0,9.81f,0,1);for(int i=0;i<50;i++)filter.sample((i%2==0?.08f:-.08f),9.81f+(i%3-1)*.08f,0,1);
        check(Math.abs(filter.getTiltX())<.01f&&Math.abs(filter.getTiltY())<.01f,"sensor noise remains in dead zone");
        for(int i=0;i<25;i++)filter.sample(2.2f,9.81f,0,1);check(filter.getTiltY()<-.08f,"deliberate tilt works");
        filter.sample(2.2f,9.81f,0,3);check(filter.isCalibrating()&&filter.getTiltX()==0&&filter.getTiltY()==0,"rotation change suppresses stale calibration");for(int i=0;i<35;i++)filter.sample(2.2f,9.81f,0,3);check(!filter.isCalibrating()&&filter.getTiltX()==0&&filter.getTiltY()==0,"rotation recalibrates in neutral pose");
        filter.calibrate();check(filter.getTiltX()==0&&filter.getTiltY()==0,"manual neutral reset");
        System.out.println("MotionFilterSmoke OK: startup/noise cannot create forward drift");
    }
}
