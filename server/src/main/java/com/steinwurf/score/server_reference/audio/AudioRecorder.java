package com.steinwurf.score.server_reference.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getSimpleName();

    public interface OnDataListener {

        void onData(ByteBuffer buffer);
    }

    private static final int SAMPLE_RATE = 44100;

    private final OnDataListener onDataListener;

    AudioRecorder(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }

    private Thread mThread;
    private boolean mRunning = false;

    public void start() {
        if (mThread != null)
            return;
        mRunning = true;
        mThread = new Thread(this::record);
        mThread.start();
    }

    public void stop() {
        if (mThread == null)
            return;

        mRunning = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;
    }

    /**
     * Get the buffer size in bytes
     * @return buffer size in bytes
     */
    public int getBufferSize()
    {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        return bufferSize;
    }

    private void record() {
        short[] audioBuffer = new short[getBufferSize() / 2];
        byte[] bytes = new byte[getBufferSize()];

        AudioRecord record = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                getBufferSize());

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new RuntimeException("Audio Record can't initialize!");
        }

        record.startRecording();
        while (mRunning) {
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
