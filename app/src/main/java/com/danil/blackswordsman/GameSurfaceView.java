package com.danil.blackswordsman;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.lang.reflect.Method;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/** EGL surface that explicitly asks the driver for an OpenGL ES 3.2 context. */
public final class GameSurfaceView extends GLSurfaceView {
    private static final int EGL_CONTEXT_MAJOR_VERSION_KHR = 0x3098;
    private static final int EGL_CONTEXT_MINOR_VERSION_KHR = 0x30FB;

    public GameSurfaceView(Context context, GameRenderer renderer) {
        super(context);
        initialize(renderer);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void initialize(GameRenderer renderer) {
        setEGLConfigChooser(new QualityConfigChooser());
        setEGLContextFactory(new Context32Factory());
        setPreserveEGLContextOnPause(true);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){
        super.surfaceChanged(holder,format,width,height);
        try{
            float refresh=getDisplay()==null?60f:getDisplay().getRefreshRate();
            Method method=holder.getSurface().getClass().getMethod("setFrameRate",float.class,int.class);
            method.invoke(holder.getSurface(),Float.valueOf(refresh),Integer.valueOf(0));
        }catch(Throwable ignored){}
    }

    private static final class Context32Factory implements EGLContextFactory {
        @Override public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] request32 = {EGL_CONTEXT_MAJOR_VERSION_KHR,3,EGL_CONTEXT_MINOR_VERSION_KHR,2,EGL10.EGL_NONE};
            EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, request32);
            if (context == null || context == EGL10.EGL_NO_CONTEXT) {
                int[] request3 = {EGL_CONTEXT_MAJOR_VERSION_KHR,3,EGL10.EGL_NONE};
                context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, request3);
            }
            return context;
        }
        @Override public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }
    }

    private static final class QualityConfigChooser implements EGLConfigChooser {
        @Override public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int renderableEs3 = 0x40;
            int[] attrs = {
                EGL10.EGL_RED_SIZE,8,EGL10.EGL_GREEN_SIZE,8,EGL10.EGL_BLUE_SIZE,8,EGL10.EGL_ALPHA_SIZE,8,
                EGL10.EGL_DEPTH_SIZE,24,EGL10.EGL_STENCIL_SIZE,8,
                EGL10.EGL_RENDERABLE_TYPE,renderableEs3,
                EGL10.EGL_SAMPLE_BUFFERS,1,EGL10.EGL_SAMPLES,4,EGL10.EGL_NONE
            };
            EGLConfig config = first(egl,display,attrs);
            if(config!=null)return config;
            int[] fallback = {
                EGL10.EGL_RED_SIZE,8,EGL10.EGL_GREEN_SIZE,8,EGL10.EGL_BLUE_SIZE,8,EGL10.EGL_ALPHA_SIZE,8,
                EGL10.EGL_DEPTH_SIZE,24,EGL10.EGL_RENDERABLE_TYPE,renderableEs3,EGL10.EGL_NONE
            };
            return first(egl,display,fallback);
        }
        private EGLConfig first(EGL10 egl,EGLDisplay display,int[] attrs){
            int[] count=new int[1];if(!egl.eglChooseConfig(display,attrs,null,0,count)||count[0]==0)return null;
            EGLConfig[] configs=new EGLConfig[count[0]];egl.eglChooseConfig(display,attrs,configs,configs.length,count);return configs[0];
        }
    }
}
