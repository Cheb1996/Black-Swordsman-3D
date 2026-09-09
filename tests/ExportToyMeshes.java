package com.danil.blackswordsman;

import java.io.*;

/** Exports actual runtime geometry for offline visual inspection; no generated substitute art. */
public final class ExportToyMeshes {
    public static void main(String[] args)throws Exception{
        File dir=new File(args[0]);dir.mkdirs();Writer names=new OutputStreamWriter(new FileOutputStream(new File(dir,"names.tsv")),"UTF-8");
        for(int i=0;i<250;i++){
            ToyGeometry.Data d=i<240?ToyGeometry.build(SurrealCatalog.ITEMS[i],true):ToyGeometry.pickup(i-240,true);
            DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir,i+".bin"))));out.writeInt(d.vertices.length);out.writeInt(d.indices.length);for(float f:d.vertices)out.writeFloat(f);for(short s:d.indices)out.writeShort(s);out.close();
            float x=1,y=1,z=1;String name;
            if(i<240){SurrealCatalog.Item item=SurrealCatalog.ITEMS[i];x=item.hx;y=item.hy;z=item.hz;name=item.name;}else name=PlayerEffects.NAMES[i-240];
            names.write(i+"\t"+name+"\t"+x+"\t"+y+"\t"+z+"\n");
        }names.close();System.out.println("Exported 240 props + 10 pickups");
    }
}
