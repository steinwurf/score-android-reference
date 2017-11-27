package com.steinwurf.score.server_reference;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

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

    /**
     * Hardcoded width of the outgoing stream
     */
    private static final int WIDTH = 1280;

    /**
     * Hardcoded height of the outgoing stream
     */
    private static final int HEIGHT = 720;

    /**
     * Hardcoded density of the outgoing stream
     */
    private static final int DENSITY = 100;

    /**
     * Encoder for encoding the data from the screen capture
     */
    private final VideoEncoder videoEncoder;

    /**
     * MediaProjection object for handling the screen capture
     */
    private MediaProjection mMediaProjection;

    /**
     * The virtual display writing the data to the encoder's input surface
     */
    private VirtualDisplay mVirtualDisplay;

    /**
     * Creates a screen capture object
     * @param videoEncoder the video encoder to use for the video encoding.
     */
    ScreenCapture(VideoEncoder videoEncoder)
    {
        this.videoEncoder = videoEncoder;
    }

    /**
     * Starts the screen capture.
     * @param mediaProjection object for handling the screen capture
     * @throws IOException Throws if the video encoder is unable to start
     */
    void start(MediaProjection mediaProjection) throws IOException {
        if (mediaProjection == null)
            throw new IllegalArgumentException("mediaProjection must not be null");

        videoEncoder.start();

        mMediaProjection = mediaProjection;
        mMediaProjection.registerCallback(new MediaProjectionCallback(), null);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                TAG, WIDTH, HEIGHT, DENSITY,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                videoEncoder.getInputSurface(), null,null);
    }

    /**
     * Closes the screen capture
     */
    void stop() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        videoEncoder.stop();
    }

    /**
     * Class for handling the the callbacks of the @link {@link MediaProjection} object.
     */
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
        }
    }
}
