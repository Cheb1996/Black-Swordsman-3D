package com.danil.blackswordsman;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;

/** Accelerometer movement plus low-latency gyroscope camera control. */
public final class MotionController implements SensorEventListener, GameWorld.MotionInput {
    private final SensorManager manager;
    private final WindowManager windowManager;
    private final Sensor accelerometer;
    private final Sensor gyroscope;
    private final Sensor rotationVector;
    private final float[] rotation = new float[9];
    private final float[] orientation = new float[3];
    private final MotionFilter tiltFilter=new MotionFilter();
    private float gyroYaw;
    private float gyroPitch;
    private long gyroTimestamp;
    private volatile boolean running;
    private volatile float absoluteYaw;
    private volatile float absolutePitch;
    private float gyroBiasYaw;
    private float gyroBiasPitch;
    private float previousAbsoluteYaw;
    private float previousAbsolutePitch;
    private boolean absoluteInitialized;

    public MotionController(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        accelerometer = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationVector = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    public void resume() {
        if (manager == null || running) return;
        if (accelerometer != null) manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (gyroscope != null) manager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        if (rotationVector != null) manager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
        running = true;
    }

    public void pause() {
        if (manager != null) manager.unregisterListener(this);
        running = false; gyroTimestamp = 0L;
        synchronized (this) { gyroYaw = 0f; gyroPitch = 0f; }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER) {
            tiltFilter.sample(event.values[0],event.values[1],event.values[2],windowManager.getDefaultDisplay().getRotation());
        } else if (type == Sensor.TYPE_GYROSCOPE) {
            if (gyroTimestamp != 0L) {
                float dt = Math.min(.04f, (event.timestamp - gyroTimestamp) * 1.0e-9f);
                int displayRotation = windowManager.getDefaultDisplay().getRotation();
                float yawRate;
                float pitchRate;
                if (displayRotation == Surface.ROTATION_270) {
                    yawRate = event.values[0]; pitchRate = event.values[1];
                } else if(displayRotation==Surface.ROTATION_90) {
                    yawRate = -event.values[0]; pitchRate = -event.values[1];
                } else {
                    yawRate=-event.values[1];pitchRate=event.values[0];
                }
                if(Math.abs(yawRate)<.10f)gyroBiasYaw=gyroBiasYaw*.995f+yawRate*.005f;
                if(Math.abs(pitchRate)<.10f)gyroBiasPitch=gyroBiasPitch*.995f+pitchRate*.005f;
                yawRate=deadRate(yawRate-gyroBiasYaw);pitchRate=deadRate(pitchRate-gyroBiasPitch);
                synchronized (this) {
                    gyroYaw += yawRate * dt;
                    gyroPitch += pitchRate * dt;
                }
            }
            gyroTimestamp = event.timestamp;
        } else if (type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            SensorManager.getOrientation(rotation, orientation);
            absoluteYaw = orientation[0]; absolutePitch = orientation[1];
            if(!absoluteInitialized){previousAbsoluteYaw=absoluteYaw;previousAbsolutePitch=absolutePitch;absoluteInitialized=true;}
            else if(gyroscope==null){
                float dy=wrap(absoluteYaw-previousAbsoluteYaw),dp=absolutePitch-previousAbsolutePitch;
                synchronized(this){gyroYaw+=dy;gyroPitch+=dp;}
                previousAbsoluteYaw=absoluteYaw;previousAbsolutePitch=absolutePitch;
            }
        }
    }

    @Override public float getTiltX() { return tiltFilter.getTiltX(); }
    @Override public float getTiltY() { return tiltFilter.getTiltY(); }

    @Override
    public synchronized float consumeGyroYaw() {
        float value = gyroYaw; gyroYaw = 0f; return value;
    }

    @Override
    public synchronized float consumeGyroPitch() {
        float value = gyroPitch; gyroPitch = 0f; return value;
    }

    @Override public boolean hasGyroscope() { return gyroscope != null; }
    @Override public boolean hasAccelerometer() { return accelerometer != null; }

    @Override
    public void calibrate() {
        tiltFilter.calibrate();gyroTimestamp = 0L;
        previousAbsoluteYaw=absoluteYaw;previousAbsolutePitch=absolutePitch;absoluteInitialized=true;
        synchronized (this) { gyroYaw = 0f; gyroPitch = 0f; }
    }

    public float getAbsoluteYaw() { return absoluteYaw; }
    public float getAbsolutePitch() { return absolutePitch; }
    private static float deadRate(float value){return Math.abs(value)<.012f?0f:value;}
    private static float wrap(float value){while(value>(float)Math.PI)value-=(float)Math.PI*2f;while(value<-(float)Math.PI)value+=(float)Math.PI*2f;return value;}

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
