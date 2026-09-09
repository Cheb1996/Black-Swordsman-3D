package com.danil.blackswordsman;

import android.content.Context;
import android.content.SharedPreferences;

public final class AndroidProgressStore implements SnapshotCodec.Store {
    private static final String NAME = "black_swordsman_3d_progress";
    private final SharedPreferences preferences;
    private final android.util.AtomicFile checkpoint,legacyCheckpoint,olderCheckpoint;

    public AndroidProgressStore(Context context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);checkpoint=new android.util.AtomicFile(new java.io.File(context.getFilesDir(),"world-v8.bin"));legacyCheckpoint=new android.util.AtomicFile(new java.io.File(context.getFilesDir(),"world-v7.bin"));olderCheckpoint=new android.util.AtomicFile(new java.io.File(context.getFilesDir(),"world-v6.bin"));
    }

    @Override
    public GameWorld.Progress load() {
        if (!preferences.getBoolean("exists", false)) return null;
        GameWorld.Progress p = new GameWorld.Progress();
        p.chapter = preferences.getInt("chapter", 0);
        p.unlocked = preferences.getInt("unlocked", 0);
        p.maxHp = preferences.getFloat("max_hp", 240f);
        p.attack = preferences.getFloat("attack", 38f);
        p.armor = preferences.getFloat("armor", 0f);
        p.speed = preferences.getFloat("speed", 5.15f);
        p.maxAmmo = preferences.getInt("max_ammo", 5);
        p.completed = preferences.getBoolean("completed", false);p.pendingUpgrade=preferences.getBoolean("pending_upgrade",false);
        p.totalKills = preferences.getInt("kills", 0);
        return p;
    }

    @Override
    public void save(GameWorld.Progress p) {
        preferences.edit().putBoolean("exists", true)
                .putInt("chapter", p.chapter).putInt("unlocked", p.unlocked)
                .putFloat("max_hp", p.maxHp).putFloat("attack", p.attack)
                .putFloat("armor", p.armor).putFloat("speed", p.speed)
                .putInt("max_ammo", p.maxAmmo).putBoolean("completed", p.completed)
                .putInt("kills", p.totalKills).putBoolean("pending_upgrade",p.pendingUpgrade).apply();
    }

    public static GameSettings loadSettings(Context c){GameSettings s=new GameSettings();SharedPreferences p=c.getSharedPreferences("settings_v6",0);s.sensitivity=p.getFloat("sensitivity",1);s.controlScale=p.getFloat("scale",1);s.shake=p.getFloat("shake",.7f);s.dream=p.getFloat("dream",1);s.sfx=p.getFloat("sfx",.8f);s.ambience=p.getFloat("ambience",.45f);s.fps=p.getInt("fps",60);s.diagnostics=p.getBoolean("diagnostics",false);return s;}
    public static void saveSettings(Context c,GameSettings s){c.getSharedPreferences("settings_v6",0).edit().putFloat("sensitivity",s.sensitivity).putFloat("scale",s.controlScale).putFloat("shake",s.shake).putFloat("dream",s.dream).putFloat("sfx",s.sfx).putFloat("ambience",s.ambience).putInt("fps",s.fps).putBoolean("diagnostics",s.diagnostics).apply();}
    @Override public byte[] loadCheckpoint(){try{return checkpoint.readFully();}catch(java.io.IOException missing){try{return legacyCheckpoint.readFully();}catch(java.io.IOException absent){try{return olderCheckpoint.readFully();}catch(java.io.IOException gone){return null;}}}}
    @Override public void saveCheckpoint(byte[] data){java.io.FileOutputStream out=null;try{out=checkpoint.startWrite();out.write(data);checkpoint.finishWrite(out);}catch(java.io.IOException error){checkpoint.failWrite(out);throw new IllegalStateException("Checkpoint write failed",error);}}
    @Override public void clearCheckpoint(){checkpoint.delete();legacyCheckpoint.delete();olderCheckpoint.delete();}
    @Override public void clear() { preferences.edit().clear().apply(); }
}
