package com.steinwurf.score.client_reference.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.steinwurf.mediaplayer.AudioDecoder;
import com.steinwurf.mediaplayer.SampleStorage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getSimpleName();

    private static final int SAMPLE_RATE = 44100;

    private AudioTrack audioTrack;

    public boolean isPlaying() {
        return audioTrack != null;
    }

    public void start() {
        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.play();

        Log.v(TAG, "Audio streaming started");
    }

    public void stop() {
        if (audioTrack == null)
            return;
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }

    void handleData(byte[] data)
    {
        if (!isPlaying())
            return;
        short[] shorts = new short[data.length/2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        audioTrack.write(shorts, 0, shorts.length);
    }
}
