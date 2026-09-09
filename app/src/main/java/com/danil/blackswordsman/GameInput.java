package com.danil.blackswordsman;

/** Thread-safe bridge between Android UI/sensors and the GL game thread. */
public final class GameInput {
    public volatile float moveX;
    public volatile float moveY;
    public volatile boolean gyroEnabled = true;
    public volatile boolean tiltEnabled;

    private float cameraDx;
    private float cameraDy;
    private int lightPresses;
    private int heavyPresses;
    private int dodgePresses;
    private int cannonPresses;
    private int ragePresses;
    private int actionPresses;
    private int pausePresses;
    private int menuPresses;
    private int gyroToggles;
    private int tiltToggles;
    private int calibrations;
    private int interactPresses;
    private int upgradeChoice = -1;
    private int jump,lock,place,rotate; public volatile int resetSerial;

    public synchronized void addCamera(float dx, float dy) { cameraDx += dx; cameraDy += dy; }
    public synchronized float consumeCameraX() { float value = cameraDx; cameraDx = 0f; return value; }
    public synchronized float consumeCameraY() { float value = cameraDy; cameraDy = 0f; return value; }

    public synchronized void pressLight() { lightPresses=1; }
    public synchronized void pressHeavy() { heavyPresses=1; }
    public synchronized void pressDodge() { dodgePresses=1; }
    public synchronized void pressCannon() { cannonPresses=1; }
    public synchronized void pressRage() { ragePresses=1; }
    public synchronized void pressAction() { actionPresses=1; }
    public synchronized void pressPause() { pausePresses=1; }
    public synchronized void pressMenu() { menuPresses=1; }
    public synchronized void toggleGyro() { gyroToggles=1; }
    public synchronized void toggleTilt() { tiltToggles=1; }
    public synchronized void calibrate() { calibrations=1; }
    public synchronized void pressInteract() { interactPresses=1; }
    public synchronized void chooseUpgrade(int choice) { upgradeChoice = choice; }

    private int take(int value) { return value > 0 ? value - 1 : 0; }
    public synchronized boolean consumeLight() { if (lightPresses == 0) return false; lightPresses = take(lightPresses); return true; }
    public synchronized boolean consumeHeavy() { if (heavyPresses == 0) return false; heavyPresses = take(heavyPresses); return true; }
    public synchronized boolean consumeDodge() { if (dodgePresses == 0) return false; dodgePresses = take(dodgePresses); return true; }
    public synchronized boolean consumeCannon() { if (cannonPresses == 0) return false; cannonPresses = take(cannonPresses); return true; }
    public synchronized boolean consumeRage() { if (ragePresses == 0) return false; ragePresses = take(ragePresses); return true; }
    public synchronized boolean consumeAction() { if (actionPresses == 0) return false; actionPresses = take(actionPresses); return true; }
    public synchronized boolean consumePause() { if (pausePresses == 0) return false; pausePresses = take(pausePresses); return true; }
    public synchronized boolean consumeMenu() { if (menuPresses == 0) return false; menuPresses = take(menuPresses); return true; }
    public synchronized boolean consumeGyroToggle() { if (gyroToggles == 0) return false; gyroToggles = take(gyroToggles); return true; }
    public synchronized boolean consumeTiltToggle() { if (tiltToggles == 0) return false; tiltToggles = take(tiltToggles); return true; }
    public synchronized boolean consumeCalibration() { if (calibrations == 0) return false; calibrations = take(calibrations); return true; }
    public synchronized boolean consumeInteract() { if (interactPresses == 0) return false; interactPresses = take(interactPresses); return true; }
    public synchronized int consumeUpgradeChoice() { int value = upgradeChoice; upgradeChoice = -1; return value; }

    public synchronized void pressJump(){jump=1;} public synchronized boolean consumeJump(){boolean v=jump>0;jump=0;return v;}
    public synchronized void pressLock(){lock=1;} public synchronized boolean consumeLock(){boolean v=lock>0;lock=0;return v;}
    public synchronized void pressPlace(){place=1;} public synchronized boolean consumePlace(){boolean v=place>0;place=0;return v;}
    public synchronized void pressRotate(){rotate=1;} public synchronized boolean consumeRotate(){boolean v=rotate>0;rotate=0;return v;}
    public synchronized void resetAll(){clearMovement();cameraDx=cameraDy=0;lightPresses=heavyPresses=dodgePresses=cannonPresses=ragePresses=actionPresses=pausePresses=menuPresses=gyroToggles=tiltToggles=calibrations=interactPresses=jump=lock=place=rotate=0;upgradeChoice=-1;resetSerial++;}
    public void clearMovement() { moveX = 0f; moveY = 0f; }
}
