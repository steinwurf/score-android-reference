package com.steinwurf.score.server_reference.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

class Microphone {

    private static final String TAG = Microphone.class.getSimpleName();
    private final AudioEncoder audioEncoder;

    private AudioRecord audioRecord;

    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int rate = 44100;

    /**
     * Thread handling the encoding
     */
    private Thread mRecorderThread;

    public Microphone(AudioEncoder audioEncoder) {
        this.audioEncoder = audioEncoder;
    }

    public void start() throws IOException {
        Log.d(TAG, "start");
        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE)
            throw new RuntimeException("ERROR_BAD_VALUE");

        Log.d(TAG, "bufferSize " + bufferSize);
        audioEncoder.start();
        Log.d(TAG, "audioEncoder started");
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);
        audioRecord.startRecording();

        mRecorderThread = new Thread(() -> {
            byte[] data = new byte[bufferSize];
            while (true)
            {
                audioRecord.read(data, 0, bufferSize);
                audioEncoder.addData(data);
            }

        });
        mRecorderThread.start();

    }

    public void stop() {
        audioRecord.stop();
        try {
            mRecorderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        audioRecord.release();
    }
}
