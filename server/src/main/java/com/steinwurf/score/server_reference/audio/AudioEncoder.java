package com.steinwurf.score.server_reference.audio;

import java.nio.ByteBuffer;

public class AudioEncoder {

    private final OnDataListener onDataListener;

    public AudioEncoder(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }

    public interface OnDataListener {

        void onData(ByteBuffer buffer);

        void onFinish();
    }
}
