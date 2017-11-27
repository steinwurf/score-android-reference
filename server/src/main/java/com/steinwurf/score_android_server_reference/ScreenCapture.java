package com.steinwurf.score_android_server_reference;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Log;

import java.io.IOException;

class ScreenCapture {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = ScreenCapture.class.getSimpleName();

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DENSITY = 100;

    /**
     * Encoder for encoding the data from the screen capture
     */
    private final VideoEncoder videoEncoder;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    ScreenCapture(VideoEncoder videoEncoder)
    {
        this.videoEncoder = videoEncoder;
    }

    void start(MediaProjection mediaProjection) throws IOException {
        if (mediaProjection == null)
            throw new IllegalArgumentException("mediaProjection must not be null");
        mMediaProjection = mediaProjection;
        videoEncoder.start();
        openMediaProjection();
    }

    void stop() {
        closeScreenCapture();
        videoEncoder.stop();
    }

    private void openMediaProjection() {
        mMediaProjection.registerCallback(new MediaProjectionCallback(), null);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                TAG, WIDTH, HEIGHT, DENSITY,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                videoEncoder.getInputSurface(), null,null);
    }

    /**
     * Closes the current screen capture.
     */
    private void closeScreenCapture() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
        }
    }
}
