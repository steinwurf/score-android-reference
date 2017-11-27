package com.steinwurf.score_android_server_reference;

import android.hardware.camera2.CameraManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR;

class VideoEncoder {

    public interface OnDataListener {
        void onData(ByteBuffer data);
        void onFinish();
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = VideoEncoder.class.getSimpleName();

    /**
     * The MIME type for the video mEncoder
     */
    private static final String MIME_TYPE = "video/avc";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int BIT_RATE = 4000000;
    private static final int FRAME_RATE = 30;
    private static final int I_FRAME_INTERVAL = 1;
    private final OnDataListener onDataListener;

    /**
     * Encoder for encoding the data from the surface
     */
    private MediaCodec mEncoder;

    /**
     * Thread handling the encoding
     */
    private Thread mEncoderThread;

    /**
     * The encoder's input surface
     */
    private Surface mInputSurface;

    private byte[] mSPS = null;
    private byte[] mPPS = null;

    VideoEncoder(OnDataListener onDataListener)
    {
        this.onDataListener = onDataListener;
    }

    void start() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            format.setInteger(MediaFormat.KEY_PRIORITY, /*real time*/0);
        }
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
        mEncoderThread = new Thread(drainEncoder);
        mEncoderThread.start();
    }

    void stop() {
        if (mEncoder != null) {
            // Signal end of stream
            mEncoder.signalEndOfInputStream();
            try {
                mEncoderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mEncoder.stop();
            mEncoder.reset();
            if (mInputSurface != null)
            {
                mInputSurface.release();
                mInputSurface = null;
            }
            mEncoder.release();
            mEncoder = null;
        }
    }

    Surface getInputSurface()
    {
        return mInputSurface;
    }

    byte[] getSPS()
    {
        if (mSPS == null)
            throw new IllegalStateException("Must be called after first call to onData has been performed");
        return mSPS;
    }

    byte[] getPPS()
    {
        if (mPPS == null)
            throw new IllegalStateException("Must be called after first call to onData has been performed");
        return mPPS;
    }

    private Runnable drainEncoder = new Runnable() {
        @Override
        public void run() {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    Log.i(TAG, "no output available, spinning");
                    bufferInfo = new MediaCodec.BufferInfo();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    mSPS = newFormat.getByteBuffer("csd-0").array();
                    mPPS = newFormat.getByteBuffer("csd-1").array();
//                    Log.d(TAG, "output format changed");
                } else if (encoderStatus >= 0) {
//                    Log.i(TAG, "output available");
                    // Normal flow: get output encoded buffer, send to VideoDataCallback
                    ByteBuffer videoData = mEncoder.getOutputBuffer(encoderStatus);
                    assert videoData != null;
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        /*
                         * The codec config data was pulled out and fed to the VideoDataCallback
                         * when we got the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                         */
                        bufferInfo.size = 0;
                    }
                    if (bufferInfo.size != 0) {
                        // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                        videoData.position(bufferInfo.offset);
                        videoData.limit(bufferInfo.offset + bufferInfo.size);
                        ByteBuffer data = ByteBuffer.allocate(bufferInfo.size + Long.SIZE / Byte.SIZE);
                        data.order(ByteOrder.BIG_ENDIAN);
                        data.put(videoData);
                        data.putLong(bufferInfo.presentationTimeUs);
                        onDataListener.onData(data);
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        onDataListener.onFinish();
                        return;
                    }
                } else {
//                    Log.w(TAG, "unexpected result from mEncoder.dequeueOutputBuffer: " + encoderStatus);
                }
            }
        }
    };
}
