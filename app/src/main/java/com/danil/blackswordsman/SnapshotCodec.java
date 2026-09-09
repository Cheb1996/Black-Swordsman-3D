package com.danil.blackswordsman;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.zip.*;

/** Versioned, checksummed local checkpoints. Runtime Android objects are never persisted. */
public final class SnapshotCodec {
    public interface Store extends GameWorld.ProgressStore {byte[] loadCheckpoint();void saveCheckpoint(byte[] bytes);void clearCheckpoint();}
    private static final int MAGIC=0x42533630,VERSION=8;
    private SnapshotCodec(){}
    public static byte[] encode(GameWorld world)throws IOException{
        ByteArrayOutputStream data=new ByteArrayOutputStream();ObjectOutputStream out=new ObjectOutputStream(new GZIPOutputStream(data));out.writeObject(world);out.close();byte[] payload=data.toByteArray();CRC32 crc=new CRC32();crc.update(payload);
        ByteArrayOutputStream framed=new ByteArrayOutputStream();DataOutputStream header=new DataOutputStream(framed);header.writeInt(MAGIC);header.writeInt(VERSION);header.writeLong(crc.getValue());header.writeInt(payload.length);header.write(payload);header.close();return framed.toByteArray();
    }
    public static boolean restore(GameWorld target,byte[] bytes){
        if(bytes==null)return false;
        try{DataInputStream header=new DataInputStream(new ByteArrayInputStream(bytes));if(header.readInt()!=MAGIC)return false;int version=header.readInt();if(version!=6&&version!=7&&version!=VERSION)return false;long checksum=header.readLong();int count=header.readInt();if(count<1||count>8*1024*1024||count!=bytes.length-20)return false;byte[] payload=new byte[count];header.readFully(payload);CRC32 crc=new CRC32();crc.update(payload);if(crc.getValue()!=checksum)return false;
            ObjectInputStream in=new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(payload)));Object object=in.readObject();in.close();if(!(object instanceof GameWorld))return false;GameWorld saved=(GameWorld)object;
            if(saved.chapter<0||saved.chapter>=StoryData.CHAPTERS.length||saved.player==null||saved.physics==null)return false;
            for(Field field:GameWorld.class.getDeclaredFields()){int m=field.getModifiers();if(Modifier.isStatic(m)||Modifier.isTransient(m))continue;field.setAccessible(true);field.set(target,field.get(saved));}
            target.migrateWorld();return true;
        }catch(Exception invalid){return false;}
    }
}
