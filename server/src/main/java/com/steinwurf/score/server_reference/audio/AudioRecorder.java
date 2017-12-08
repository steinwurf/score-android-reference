package com.steinwurf.score.server_reference.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getSimpleName();

    public interface OnDataListener {

        void onData(ByteBuffer buffer);
    }

    private static final int SAMPLE_RATE = 44100;

    private final OnDataListener onDataListener;

    public AudioRecorder(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }

    private boolean mIsRecording = false;
    private Thread mThread;

    public boolean isRecording() {
        return mThread != null;
    }

    public void start() {
        if (mThread != null)
            return;

        mIsRecording = true;
        mThread = new Thread(this::record);
        mThread.start();
    }

    public void stop() {
        if (mThread == null)
            return;
        mIsRecording = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;
    }

    private void record() {
        Log.v(TAG, "Start");

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];
        byte[] bytes = new byte[bufferSize];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(TAG, "Start recording");

        while (mIsRecording) {
            record.read(audioBuffer, 0, audioBuffer.length);

            // Notify waveform
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            buffer.asShortBuffer().put(audioBuffer);
            onDataListener.onData(buffer);
        }

        record.stop();
        record.release();
    }
}
