package com.steinwurf.score.client_reference.audio;

import android.util.Log;

import com.steinwurf.mediaplayer.AudioDecoder;
import com.steinwurf.mediaplayer.SampleStorage;

class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getSimpleName();

    private final Object mAudioDecoderLock = new Object();

    private AudioDecoder mAudioDecoder;
    private SampleStorage mSampleStorage;

    private Long mFirstTimeStamp = null;
    private boolean mRunning = false;

    public void handleData(long timestamp, byte[] data) {

        synchronized (mAudioDecoderLock)
        {
            if (mSampleStorage != null)
            {
                if (mFirstTimeStamp == null)
                    mFirstTimeStamp = timestamp;
                Log.d(TAG, "Handle data " + data.length);
                mSampleStorage.addSample(timestamp - mFirstTimeStamp, data);
            }
        }
    }

    public void start(int mpegAudioObjectType, int frequencyIndex, int channelConfiguration) {

        synchronized (mAudioDecoderLock)
        {
            mFirstTimeStamp = null;
            mSampleStorage = new SampleStorage();
            Log.d(TAG, "Building " + mpegAudioObjectType + " " + frequencyIndex + " " + channelConfiguration);
            mAudioDecoder = AudioDecoder.build(
                    mpegAudioObjectType,
                    frequencyIndex,
                    channelConfiguration,
                    mSampleStorage);
            Log.d(TAG, "Starting");
            mAudioDecoder.start();
        }
        mRunning = true;
    }

    public void stop()
    {
        Log.d(TAG, "stop");
        mRunning = false;

        synchronized (mAudioDecoderLock)
        {
            if (mAudioDecoder != null)
            {
                mAudioDecoder.stop();
                mAudioDecoder = null;
            }
        }
    }

    public boolean isRunning() {
        return mRunning;
    }
}
