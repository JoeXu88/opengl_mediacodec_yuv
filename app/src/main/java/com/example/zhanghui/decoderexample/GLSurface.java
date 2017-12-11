package com.example.zhanghui.decoderexample;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by david on 11/12/2017.
 */

public class GLSurface  extends GLSurfaceView {

    final static String TAG = "GLSuface";

    public GLSurface(Context context) {
        super(context);
        setEGLContextClientVersion(2);
    }

    public GLSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(TAG,"surface onAttachedToWindow()");
        super.onAttachedToWindow();
        // setRenderMode() only takes effectd after SurfaceView attached to window!
        // note that on this mode, surface will not render util GLSurfaceView.requestRender() is
        // called, it's good and efficient -v-
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        Log.d(TAG,"surface setRenderMode RENDERMODE_WHEN_DIRTY");
    }
}
