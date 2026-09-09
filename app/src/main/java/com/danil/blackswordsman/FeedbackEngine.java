package com.danil.blackswordsman;
import android.content.Context;
import android.media.SoundPool;
import android.media.AudioAttributes;
import android.os.Vibrator;
/** Polyphonic original sounds, material footsteps and a controllable ambient bed. */
public final class FeedbackEngine {
 private final SoundPool sounds;private final Vibrator vibrator;private final int[] ids=new int[11];private final boolean[] ready=new boolean[64];private int ambienceStream;private float lastX,lastZ,walk;private int environment=-1;
 public FeedbackEngine(Context context){sounds=new SoundPool.Builder().setMaxStreams(12).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);sounds.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener(){public void onLoadComplete(SoundPool p,int id,int status){if(id<ready.length)ready[id]=status==0;}});String[] names={"slash","hit","cannon","dodge","rage","page","boss","grass","stone","wood","ambience"};for(int i=0;i<names.length;i++)try{android.content.res.AssetFileDescriptor fd=context.getAssets().openFd("audio/"+names[i]+".wav");ids[i]=sounds.load(fd,1);fd.close();}catch(Exception ignored){}}
 private void play(int n,float volume,float pitch){int id=ids[n];if(id>0&&id<ready.length&&ready[id])sounds.play(id,volume,volume,1,0,pitch);}
 public void poll(GameWorld w){while(!w.feedbackQueue.isEmpty()){int e=w.feedbackQueue.removeFirst();if(e>0&&e<=7)play(e-1,w.settings.sfx,1);if(w.settings.sfx>0&&e==GameWorld.EVENT_HIT&&vibrator!=null)vibrator.vibrate(16);}
  boolean playing=w.mode==GameWorld.PLAYING&&!w.settingsOpen;if(!playing){pause();lastX=w.player.x;lastZ=w.player.z;return;}
  if(ambienceStream==0&&ids[10]>0&&ready[ids[10]])ambienceStream=sounds.play(ids[10],w.settings.ambience,w.settings.ambience,0,-1,1);
  if(ambienceStream!=0){sounds.setVolume(ambienceStream,w.settings.ambience,w.settings.ambience);if(environment!=w.chapter){environment=w.chapter;sounds.setRate(ambienceStream,.85f+(w.chapter%5)*.05f);}}
  float dx=w.player.x-lastX,dz=w.player.z-lastZ,d=(float)Math.sqrt(dx*dx+dz*dz);lastX=w.player.x;lastZ=w.player.z;if(d<2&&w.player.grounded)walk+=d;
  if(walk>.9f){walk=0;int env=w.currentChapter().environment,surface=env==1||env==2||env>=4&&env<=7?8:7;if((env==0||env==3)&&Math.abs(w.player.z-40)<8&&Math.abs(w.player.x-WorldLayout.road(40))<3.5f)surface=9;play(surface,w.settings.sfx*.6f,.94f+(w.player.gaitPhase%1)*.12f);}
 }
 public void pause(){if(ambienceStream!=0){sounds.stop(ambienceStream);ambienceStream=0;}}
 public void release(){sounds.release();}
}
