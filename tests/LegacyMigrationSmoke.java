package com.danil.blackswordsman;
import java.io.*;
public final class LegacyMigrationSmoke {
 public static void main(String[] args)throws Exception{File file=new File("verification/v6-checkpoint.bin");FileInputStream in=new FileInputStream(file);byte[] data=new byte[(int)file.length()];new DataInputStream(in).readFully(data);in.close();GameWorld w=new GameWorld(new GameInput(),new V6RegressionSmoke.Motion(),new V6RegressionSmoke.Store());if(!SnapshotCodec.restore(w,data)||w.worldVersion!=8||w.player.hp!=112||w.quest.stage!=1||w.surreal.props.get(100).body.x!=41.25f||w.encounters.landings.size()!=10)throw new AssertionError("Actual v6 checkpoint migration");w.physics.resetForChapter(1,1);w.physics.streamAround(500,500);System.out.println("LegacyMigrationSmoke OK: genuine archived v6 checkpoint -> v8, progress/props preserved, new content initialized, streaming and chapter reset");}
}
