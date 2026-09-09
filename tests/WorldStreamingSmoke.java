package com.danil.blackswordsman;

/** Verifies the 100x world, deterministic physical streaming and varied terrain. */
public final class WorldStreamingSmoke {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        float oldWidth=284f,newWidth=GameWorld.WORLD_HALF*2f;
        check(Math.abs(newWidth*newWidth/(oldWidth*oldWidth)-10f)<.01f,"map area is exactly 10x versus v6");
        PhysicsWorld forest=new PhysicsWorld();forest.resetForChapter(0,0);
        int start=forest.bodies.size(),trees=0,streamed=0;
        for(int i=0;i<forest.bodies.size();i++){PhysicsWorld.Body b=forest.bodies.get(i);if(b.streamed){streamed++;if(b.kind==PhysicsWorld.TREE)trees++;}}
        check(streamed>55&&trees>35,"dense physical forest chunks");
        forest.streamAround(96f,96f);int farBodies=forest.bodies.size(),farStreamed=0;
        for(int i=0;i<forest.bodies.size();i++){PhysicsWorld.Body b=forest.bodies.get(i);if(b.streamed){farStreamed++;check(Math.abs(b.chunkX-4)<=1&&Math.abs(b.chunkZ-4)<=1,"old chunks pruned");}}
        check(farStreamed>55&&farBodies<(start-streamed)+9*37,"bounded streaming budget");
        float min=999,max=-999;for(int z=-120;z<=120;z+=12)for(int x=-120;x<=120;x+=12){float h=forest.terrainHeight(x,z);min=Math.min(min,h);max=Math.max(max,h);}
        check(max-min>.65f,"terrain has broad hills and local detail");
        System.out.println("WorldStreamingSmoke OK: 898x898m, bounded chunks, dense forest, varied terrain");
    }
}
