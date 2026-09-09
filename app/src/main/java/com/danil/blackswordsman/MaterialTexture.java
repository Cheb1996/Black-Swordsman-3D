package com.danil.blackswordsman;
import android.opengl.GLES30;
import java.nio.ByteBuffer;
/** Mipmapped material arrays, shared by all actors, animals, vegetation and world geometry. */
public final class MaterialTexture {
 private final int[] id=new int[2];
 public MaterialTexture(android.content.res.AssetManager assets){GLES30.glGenTextures(2,id,0);int side=SurfaceLibrary.SIDE;byte[] bytes=new byte[side*side*4];ByteBuffer buffer=ByteBuffer.allocateDirect(bytes.length);for(int c=0;c<2;c++){GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY,id[c]);GLES30.glTexStorage3D(GLES30.GL_TEXTURE_2D_ARRAY,10,GLES30.GL_RGBA8,side,side,SurfaceLibrary.COUNT);for(int k=0;k<SurfaceLibrary.COUNT;k++){try{java.io.DataInputStream in=new java.io.DataInputStream(assets.open("materials/"+SurfaceLibrary.NAMES[k]+(c==0?"-color.rgba":"-normal.rgba")));in.readFully(bytes);in.close();}catch(java.io.IOException e){throw new IllegalStateException("Missing material",e);}buffer.clear();buffer.put(bytes);buffer.position(0);GLES30.glTexSubImage3D(GLES30.GL_TEXTURE_2D_ARRAY,0,0,0,k,side,side,1,GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,buffer);}GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D_ARRAY);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR_MIPMAP_LINEAR);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_REPEAT);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_REPEAT);}}
 public void bind(){for(int c=0;c<2;c++){GLES30.glActiveTexture(GLES30.GL_TEXTURE1+c);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY,id[c]);}GLES30.glActiveTexture(GLES30.GL_TEXTURE0);}
 public void release(){GLES30.glDeleteTextures(2,id,0);}
}
