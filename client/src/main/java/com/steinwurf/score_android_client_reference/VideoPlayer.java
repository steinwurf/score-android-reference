package com.steinwurf.score_android_client_reference;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.steinwurf.mediaplayer.SampleStorage;
import com.steinwurf.mediaplayer.VideoDecoder;

import org.jetbrains.annotations.NotNull;

public class VideoPlayer implements TextureView.SurfaceTextureListener
{
    private static final String TAG = VideoPlayer.class.getSimpleName();
    private final Object mVideoDecoderLock = new Object();

    private Surface mSurface = null;
    private VideoDecoder mVideoDecoder = null;

    private boolean mWaitingForKeyframe = true;
    private boolean mRunning = false;
    private SampleStorage mSampleStorage;
    private Long mFirstTimeStamp = null;

    public boolean start(int width, int height, byte[] sps, byte[] pps) {
        Log.d(TAG, "start");
        synchronized (mVideoDecoderLock)
        {
            mFirstTimeStamp = null;
            mSampleStorage = new SampleStorage();
            mVideoDecoder = VideoDecoder.build(width, height, sps, pps, mSampleStorage);
            if (mVideoDecoder == null)
                return false;
            if (mSurface != null)
            {
                Log.d(TAG, "mSurface.isValid: " + mSurface.isValid());
                mVideoDecoder.setSurface(mSurface);
                mVideoDecoder.start();
                mWaitingForKeyframe = true;
            }
        }
        mRunning = true;
        return true;
    }

    public interface VideoEventListener
    {
        void onKeyFrameFound();
    }
    private final VideoEventListener mVideoEventListener;

    public VideoPlayer(@NotNull VideoEventListener videoEventListener)
    {
        mVideoEventListener = videoEventListener;
    }

    public boolean isRunning()
    {
        return mRunning;
    }

    public void stop()
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

    public void handleData(long timestamp, byte[] sample)
    {
        if (mWaitingForKeyframe)
        {
            byte header = sample[3] == 0x01 ? sample[4] : sample[3];
            int type = header & 0x1F;
            if (type == 5)
            {
                // 5 is the NALU type we are looking for.
                mWaitingForKeyframe = false;
                Log.d(TAG, "Found keyframe!");
                mVideoEventListener.onKeyFrameFound();
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
