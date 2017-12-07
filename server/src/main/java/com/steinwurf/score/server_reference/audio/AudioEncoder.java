package com.steinwurf.score.server_reference.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioEncoder {

    public interface OnDataListener {

        void onData(ByteBuffer buffer);

        void onFinish();
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = AudioEncoder.class.getSimpleName();

    /**
     * The MIME type for the video mEncoder
     */
    private static final String MIME_TYPE = "audio/mp4a-latm";

    private static final int[] SAMPLE_RATES = new int[]
            {
                    96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
                    16000, 12000, 11025, 8000
            };

    private static final int FREQUENCY_INDEX = 4;
    private static final int SAMPLE_RATE = SAMPLE_RATES[FREQUENCY_INDEX];
    private static final int CHANNEL_COUNT = 1;
    private static final int BIT_RATE = 64 * 1024;
    private static final int AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;

    /**
     * Encoder for encoding the data from the surface
     */
    private MediaCodec mEncoder;


    private final OnDataListener onDataListener;

    /**
     * Thread handling the encoding
     */
    private Thread mEncoderThread;


    public AudioEncoder(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }


    void start() throws IOException {
        Log.d(TAG, "start");

        Log.d(TAG, "creating encoder");
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);

        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MIME_TYPE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, AAC_PROFILE);
        Log.d(TAG, "configuring");
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Log.d(TAG, "configured start");
        mEncoder.start();
        Log.d(TAG, "started");
        mEncoderThread = new Thread(drainEncoder);
        mEncoderThread.start();
    }

    public void addData(byte[] data) {
        int index = mEncoder.dequeueInputBuffer(10000);
        if (index  >= 0)
        {
            ByteBuffer buffer = mEncoder.getInputBuffer(index);
            if (buffer == null)
                throw new RuntimeException("Input buffer was null");
            buffer.clear();
            buffer.put(data);
            mEncoder.queueInputBuffer(index, 0, data.length, 0, 0);
        }
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
            mEncoder.release();
            mEncoder = null;
        }
    }

    private Runnable drainEncoder = new Runnable() {

        @Override
        public void run() {

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "no output available, spinning");
                    bufferInfo = new MediaCodec.BufferInfo();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "output format changed");
                } else if (encoderStatus >= 0) {
                    Log.i(TAG, "output available");
                    // Normal flow: get output encoded buffer
                    ByteBuffer audioData = mEncoder.getOutputBuffer(encoderStatus);
                    assert audioData != null;
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        /*
                         * The codec config data was pulled out and fed to the VideoDataCallback
                         * when we got the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                         */
                        bufferInfo.size = 0;
                    }
                    if (bufferInfo.size != 0) {
                        // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                        audioData.position(bufferInfo.offset);
                        audioData.limit(bufferInfo.offset + bufferInfo.size);
                        ByteBuffer data = ByteBuffer.allocate(bufferInfo.size + ((Long.SIZE + Integer.SIZE * 3) / Byte.SIZE));
                        data.order(ByteOrder.BIG_ENDIAN);
                        data.put(audioData);
                        data.putLong(bufferInfo.presentationTimeUs);
                        data.putInt(AAC_PROFILE);
                        data.putInt(FREQUENCY_INDEX);
                        data.putInt(CHANNEL_COUNT);
                        onDataListener.onData(data);
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        onDataListener.onFinish();
                        return;
                    }
                } else {
                    Log.w(TAG, "unexpected result from mEncoder.dequeueOutputBuffer: " + encoderStatus);
                }
            }
        }
    };
}
