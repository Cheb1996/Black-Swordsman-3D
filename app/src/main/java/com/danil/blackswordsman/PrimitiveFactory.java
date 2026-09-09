package com.danil.blackswordsman;

import java.util.ArrayList;

/** Procedural geometry keeps the APK compact while providing a complete 3D scene. */
public final class PrimitiveFactory {
    private PrimitiveFactory() { }

    public static Mesh cube() {
        float[] v = {
            -1,-1, 1, 0,0,1,   1,-1, 1, 0,0,1,   1, 1, 1, 0,0,1,  -1, 1, 1, 0,0,1,
             1,-1,-1, 0,0,-1, -1,-1,-1, 0,0,-1, -1, 1,-1, 0,0,-1,  1, 1,-1, 0,0,-1,
            -1,-1,-1,-1,0,0,  -1,-1, 1,-1,0,0,  -1, 1, 1,-1,0,0,  -1, 1,-1,-1,0,0,
             1,-1, 1, 1,0,0,   1,-1,-1, 1,0,0,   1, 1,-1, 1,0,0,   1, 1, 1, 1,0,0,
            -1, 1, 1, 0,1,0,   1, 1, 1, 0,1,0,   1, 1,-1, 0,1,0,  -1, 1,-1, 0,1,0,
            -1,-1,-1, 0,-1,0,  1,-1,-1, 0,-1,0,  1,-1, 1, 0,-1,0, -1,-1, 1, 0,-1,0
        };
        short[] i = new short[36];
        for (int f=0; f<6; f++) {
            int o=f*6, b=f*4;
            i[o]=(short)b;i[o+1]=(short)(b+1);i[o+2]=(short)(b+2);
            i[o+3]=(short)b;i[o+4]=(short)(b+2);i[o+5]=(short)(b+3);
        }
        return new Mesh(v,i);
    }

    public static Mesh plane() {
        return new Mesh(new float[] {
            -1,0,-1, 0,1,0,  -1,0,1, 0,1,0,  1,0,1, 0,1,0,  1,0,-1, 0,1,0
        }, new short[] {0,1,2,0,2,3});
    }

    public static Mesh quad() {
        return new Mesh(new float[] {
            -1,-1,0, 0,0,1,  1,-1,0, 0,0,1,  1,1,0, 0,0,1,  -1,1,0, 0,0,1
        }, new short[] {0,1,2,0,2,3});
    }

    public static Mesh sphere(int slices, int stacks) {
        ArrayList<Float> verts = new ArrayList<Float>();
        ArrayList<Short> inds = new ArrayList<Short>();
        for (int y=0;y<=stacks;y++) {
            float v=(float)y/stacks, phi=(float)Math.PI*v;
            float sy=(float)Math.cos(phi), sr=(float)Math.sin(phi);
            for (int x=0;x<=slices;x++) {
                float u=(float)x/slices, theta=u*(float)Math.PI*2f;
                float sx=sr*(float)Math.sin(theta), sz=sr*(float)Math.cos(theta);
                add(verts,sx,sy,sz,sx,sy,sz);
            }
        }
        for(int y=0;y<stacks;y++)for(int x=0;x<slices;x++){
            short a=(short)(y*(slices+1)+x),b=(short)(a+slices+1);
            inds.add(a);inds.add(b);inds.add((short)(a+1));
            inds.add((short)(a+1));inds.add(b);inds.add((short)(b+1));
        }
        return new Mesh(toFloat(verts),toShort(inds));
    }

    public static Mesh cylinder(int segments, float topRadius) {
        ArrayList<Float> verts=new ArrayList<Float>();ArrayList<Short> inds=new ArrayList<Short>();
        for(int n=0;n<=segments;n++){
            float a=(float)n/segments*(float)Math.PI*2f,s=(float)Math.sin(a),c=(float)Math.cos(a);
            float slope=1f-topRadius,ny=slope*.55f,nl=(float)Math.sqrt(1f+ny*ny);
            add(verts,s,-1,c,s/nl,ny/nl,c/nl);add(verts,s*topRadius,1,c*topRadius,s/nl,ny/nl,c/nl);
        }
        for(int n=0;n<segments;n++){short b=(short)(n*2);inds.add(b);inds.add((short)(b+2));inds.add((short)(b+1));inds.add((short)(b+2));inds.add((short)(b+3));inds.add((short)(b+1));}
        short bottom=(short)(verts.size()/6);add(verts,0,-1,0,0,-1,0);
        short top=(short)(verts.size()/6);add(verts,0,1,0,0,1,0);
        for(int n=0;n<segments;n++){short b=(short)(n*2),nb=(short)((n+1)*2);inds.add(bottom);inds.add(nb);inds.add(b);inds.add(top);inds.add((short)(b+1));inds.add((short)(nb+1));}
        return new Mesh(toFloat(verts),toShort(inds));
    }

    private static void add(ArrayList<Float> a,float x,float y,float z,float nx,float ny,float nz){a.add(x);a.add(y);a.add(z);a.add(nx);a.add(ny);a.add(nz);}
    public static Mesh torus(){
        ArrayList<Float> v=new ArrayList<Float>();ArrayList<Short> ix=new ArrayList<Short>();int seg=32,side=8;
        for(int i=0;i<=seg;i++){float a=i*6.283185f/seg;for(int j=0;j<=side;j++){float t=j*6.283185f/side,co=(float)Math.cos(t),si=(float)Math.sin(t),sn=(float)Math.sin(a),cs=(float)Math.cos(a);add(v,sn*(.85f+.15f*co),.15f*si,cs*(.85f+.15f*co),sn*co,si,cs*co);}}
        for(int i=0;i<seg;i++)for(int j=0;j<side;j++){short a=(short)(i*(side+1)+j),b=(short)(a+side+1);ix.add(a);ix.add(b);ix.add((short)(a+1));ix.add((short)(a+1));ix.add(b);ix.add((short)(b+1));}
        return new Mesh(toFloat(v),toShort(ix));
    }
    private static float[] toFloat(ArrayList<Float> a){float[] r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i).floatValue();return r;}
    private static short[] toShort(ArrayList<Short> a){short[] r=new short[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i).shortValue();return r;}
}
