package com.steinwurf.score.client_reference.video;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.steinwurf.mediaplayer.SampleStorage;
import com.steinwurf.mediaplayer.VideoDecoder;
import com.steinwurf.score.shared.NaluType;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class VideoPlayer implements TextureView.SurfaceTextureListener
{
    private static final String TAG = VideoPlayer.class.getSimpleName();
    private final Object mVideoDecoderLock = new Object();

    private Surface mSurface = null;
    private VideoDecoder mVideoDecoder = null;

    private boolean mWaitingForKeyframe = true;
    private boolean mRunning = false;
    private SampleStorage mSampleStorage;
    private Long mFirstTimeStamp = null;

    void start(int width, int height, byte[] sps, byte[] pps) throws IOException {
        Log.d(TAG, "start");
        synchronized (mVideoDecoderLock)
        {
            mFirstTimeStamp = null;
            mSampleStorage = new SampleStorage();
            mVideoDecoder = VideoDecoder.build(width, height, sps, pps, mSampleStorage);
            if (mSurface != null)
            {
                Log.d(TAG, "mSurface.isValid: " + mSurface.isValid());
                mVideoDecoder.setSurface(mSurface);
                mVideoDecoder.start();
                mWaitingForKeyframe = true;
            }
        }
        mRunning = true;
    }

    public interface VideoEventListener
    {
        void onKeyFrameFound();
    }
    private final VideoEventListener mVideoEventListener;

    VideoPlayer(@NotNull VideoEventListener videoEventListener)
    {
        mVideoEventListener = videoEventListener;
    }

    boolean isRunning()
    {
        return mRunning;
    }

    void stop()
    {
        Log.d(TAG, "stop");
        mRunning = false;

        synchronized (mVideoDecoderLock)
        {
            if (mVideoDecoder != null)
            {
                mVideoDecoder.stop();
                mVideoDecoder = null;
            }
        }
        Log.d(TAG, "mVideoEventListener.onStopped()");
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        Log.d(TAG, "onSurfaceTextureAvailable");
        synchronized (mVideoDecoderLock)
        {
            mSurface = new Surface(surface);
            if (mVideoDecoder != null)
            {
                mVideoDecoder.setSurface(mSurface);
                mVideoDecoder.start();
                Log.d(TAG, "video decoder started");
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        if (mSurface != null)
        {
            mSurface.release();
            mSurface = null;
        }
        return true;
    }

    void handleData(long timestamp, byte[] sample)
    {
        if (mWaitingForKeyframe)
        {
            if (NaluType.parse(sample) == NaluType.IdrSlice)
            {
                Log.d(TAG, "Found keyframe!");
                mWaitingForKeyframe = false;
                mVideoEventListener.onKeyFrameFound();
            }
            else
            {
                return;
            }
        }
        synchronized (mVideoDecoderLock)
        {
            if (mSampleStorage != null)
            {
                if (mFirstTimeStamp == null)
                    mFirstTimeStamp = timestamp;
                mSampleStorage.addSample(timestamp - mFirstTimeStamp, sample);
            }
        }
    }
}
