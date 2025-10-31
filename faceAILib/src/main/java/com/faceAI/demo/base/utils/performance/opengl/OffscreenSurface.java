package com.faceAI.demo.base.utils.performance.opengl;

public class OffscreenSurface extends EglSurfaceBase {
    /**
     * Creates an off-screen surface with the specified width and height.
     */
    public OffscreenSurface(EglCore eglCore, int width, int height) {
        super(eglCore);
        createOffscreenSurface(width, height);
    }

    /**
     * Releases any resources associated with the surface.
     */
    public void release() {
        releaseEglSurface();
    }
}
