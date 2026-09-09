package com.danil.blackswordsman;
import java.io.*;
/** Validated designer-editable enemy definitions loaded once before the world is created. */
public final class EnemyDefinitions {
 private static String[][] rows=new String[33][];
 public static void load(InputStream input)throws IOException{BufferedReader r=new BufferedReader(new InputStreamReader(input,"UTF-8"));String line;String[][] next=new String[33][];while((line=r.readLine())!=null){if(line.length()==0||line.startsWith("#"))continue;String[] v=line.split("\t");if(v.length!=15)throw new IOException("Enemy columns");int id=Integer.parseInt(v[0]);if(id<0||id>=33||next[id]!=null)throw new IOException("Enemy id");for(int i=2;i<=12;i++)if((Float.isNaN(Float.parseFloat(v[i]))||Float.isInfinite(Float.parseFloat(v[i]))))throw new IOException("Enemy value");if(Integer.parseInt(v[14])<0||Integer.parseInt(v[14])>2)throw new IOException("Enemy nature");next[id]=v;}r.close();for(String[] v:next)if(v==null)throw new IOException("Missing enemy");rows=next;}
 public static void apply(GameWorld.Actor e,int type,float scale){if(type<0||type>=rows.length)return;String[] v=rows[type];if(v==null)throw new IllegalStateException("Load enemies.tsv before starting world");e.name=v[1];e.maxHp=f(v,2)*scale;e.damage=f(v,3)*scale;e.speed=f(v,4);e.radius=f(v,5);e.height=f(v,6);e.range=f(v,7);e.colorR=f(v,8);e.colorG=f(v,9);e.colorB=f(v,10);e.metalness=f(v,11);e.roughness=f(v,12);e.boss=Boolean.parseBoolean(v[13]);e.category=Integer.parseInt(v[14]);}
 private static float f(String[] v,int n){return Float.parseFloat(v[n]);}
}
