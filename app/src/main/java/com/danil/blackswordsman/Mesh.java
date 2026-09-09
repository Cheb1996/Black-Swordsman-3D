package com.danil.blackswordsman;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** Compact VAO-backed mesh: position.xyz + normal.xyz. */
public final class Mesh {
    private final int[] vao = new int[1];
    private final int[] vbo = new int[1];
    private final int[] ibo = new int[1];
    private final int indexCount;
    private final boolean tinted;

    public Mesh(float[] vertices, short[] indices) {
        this(vertices,indices,false);
    }
    public Mesh(float[] vertices,short[] indices,boolean colors){
        this(vertices,indices,colors?9:6);
    }
    public Mesh(float[] vertices,short[] indices,int stride){
        boolean colors=stride>=9;tinted=colors;
        indexCount = indices.length;
        FloatBuffer vb = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(vertices).position(0);
        ShortBuffer ib = ByteBuffer.allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(indices).position(0);

        GLES30.glGenVertexArrays(1, vao, 0);
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glGenBuffers(1, ibo, 0);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, vb, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, ib, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride*4, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride*4, 12);
        if(colors){GLES30.glEnableVertexAttribArray(2);GLES30.glVertexAttribPointer(2,3,GLES30.GL_FLOAT,false,stride*4,24);}
        if(stride==16){for(int i=3;i<=6;i++)GLES30.glEnableVertexAttribArray(i);GLES30.glVertexAttribPointer(3,2,GLES30.GL_FLOAT,false,64,36);GLES30.glVertexAttribPointer(4,2,GLES30.GL_FLOAT,false,64,44);GLES30.glVertexAttribPointer(5,2,GLES30.GL_FLOAT,false,64,52);GLES30.glVertexAttribPointer(6,1,GLES30.GL_FLOAT,false,64,60);}
        GLES30.glBindVertexArray(0);
    }

    public void draw() {
        GLES30.glBindVertexArray(vao[0]);
        if(!tinted)GLES30.glVertexAttrib3f(2,1f,1f,1f);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindVertexArray(0);
    }

    public void release() {
        GLES30.glDeleteBuffers(1, vbo, 0);
        GLES30.glDeleteBuffers(1, ibo, 0);
        GLES30.glDeleteVertexArrays(1, vao, 0);
    }
}
