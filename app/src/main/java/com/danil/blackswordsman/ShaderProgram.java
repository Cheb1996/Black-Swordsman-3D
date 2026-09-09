package com.danil.blackswordsman;

import android.opengl.GLES30;

/** Shader compile/link helper with readable failures in logcat. */
public final class ShaderProgram {
    public final int id;
    private final java.util.HashMap<String,Integer> uniforms=new java.util.HashMap<String,Integer>();

    public ShaderProgram(String vertex, String fragment) {
        int vs = compile(GLES30.GL_VERTEX_SHADER, vertex);
        int fs = compile(GLES30.GL_FRAGMENT_SHADER, fragment);
        id = GLES30.glCreateProgram();
        GLES30.glAttachShader(id, vs);
        GLES30.glAttachShader(id, fs);
        GLES30.glLinkProgram(id);
        int[] ok = new int[1];
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, ok, 0);
        String log = GLES30.glGetProgramInfoLog(id);
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
        if (ok[0] == 0) {
            GLES30.glDeleteProgram(id);
            throw new RuntimeException("Shader link failed: " + log);
        }
    }

    private static int compile(int kind, String source) {
        int shader = GLES30.glCreateShader(kind);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] ok = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return shader;
    }

    public int uniform(String name) { Integer result=uniforms.get(name);if(result==null){result=GLES30.glGetUniformLocation(id,name);uniforms.put(name,result);}return result; }
    public void use() { GLES30.glUseProgram(id); }
    public void release() { GLES30.glDeleteProgram(id); }
}
