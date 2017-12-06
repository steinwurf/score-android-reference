package com.steinwurf.score.server_reference.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

class Microphone {

    private final AudioEncoder audioEncoder;

    private AudioRecord audioRecord;

    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int rate = 44100;

    public Microphone(AudioEncoder audioEncoder) {
        this.audioEncoder = audioEncoder;
    }

    public void start() {
        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE)
            throw new RuntimeException("");

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);
        audioRecord.startRecording();
    }

    public void stop() {
        audioRecord.release();
    }
}
