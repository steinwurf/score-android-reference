package com.steinwurf.score_android_client_reference;

import android.content.Context;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.steinwurf.mediaplayer.Utils;
import com.steinwurf.score.shared.BackgroundHandler;
import com.steinwurf.score.shared.NaluType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ClientActivity extends AppCompatActivity {

    private static final String TAG = ClientActivity.class.getSimpleName();

    private static final String ipString = "224.0.0.251";
    private static final String portString = "9810";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private final Client client = new Client(new ClientOnStateChangeListener());
    private final VideoPlayer videoPlayer = new VideoPlayer(new VideoPlayerVideoEventListener());

    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    private KeepAlive keepAlive;

    byte[] sps = null;
    byte[] pps = null;

    private ToggleButton startStopToggleButton;
    private TextureView videoTextureView;
    private View lookingForSeverLinearLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideUI();

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        keepAlive = KeepAlive.createKeepAlive(wm, 20);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        videoTextureView = findViewById(R.id.videoTextureView);
        lookingForSeverLinearLayout = findViewById(R.id.lookingForSeverLinearLayout);
        videoTextureView.setSurfaceTextureListener(videoPlayer);

        backgroundHandler.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startStopToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                buttonView.setEnabled(false);
                BackgroundHandler.OnPostFinishedListener onPostFinishedListener = new BackgroundHandler.OnPostFinishedListener() {
                    @Override
                    public void finished() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buttonView.setEnabled(true);
                            }
                        });
                    }
                };

                if (isChecked) {
                    backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            client.start(ipString, portString);
                        }
                    }, onPostFinishedListener);
                } else {
                    backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            client.stop();
                            if (videoPlayer.isRunning())
                                videoPlayer.stop();
                        }
                    }, onPostFinishedListener);

                    lookingForSeverLinearLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        if (keepAlive != null)
            keepAlive.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        client.stop();
        if (keepAlive != null)
            keepAlive.stop();
    }

    private void hideUI()
    {
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        findViewById(R.id.activity_main).setSystemUiVisibility(visibility);
    }


    private class ClientOnStateChangeListener implements Client.OnStateChangeListener {

        @Override
        public void onError(String reason) {
            Log.d(TAG, reason);
        }

        @Override
        public void onData(ByteBuffer buffer) {
            byte[] data = buffer.array();

            if (NaluType.parse(data) == NaluType.SequenceParameterSet) {
                if (sps == null) {
                    Log.d(TAG, "Got sps");
                    sps = data.clone();
                }
                return;
            }

            if (NaluType.parse(data) == NaluType.PictureParameterSet) {
                if (pps == null) {
                    Log.d(TAG, "Got pps");
                    pps = data.clone();
                }
                return;
            }

            if (videoPlayer.isRunning()) {
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.position(buffer.remaining() - Long.SIZE / Byte.SIZE);
                byte[] slice = Arrays.copyOfRange(data, 0, buffer.position());
                long presentationTimeUs = buffer.getLong();
                videoPlayer.handleData(presentationTimeUs, slice);
            } else if (pps != null && sps != null) {
                Log.d(TAG, "Starting video player");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Point displayMetrics  = Utils.getRealMetrics(ClientActivity.this);
                        videoTextureView.setTransform(
                                Utils.fitScale(
                                        WIDTH,
                                        HEIGHT,
                                        displayMetrics.x,
                                        displayMetrics.y).toMatrix());
                    }
                });
                try {
                    videoPlayer.start(WIDTH, HEIGHT, sps, pps);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class VideoPlayerVideoEventListener implements VideoPlayer.VideoEventListener {

        @Override
        public void onKeyFrameFound() {

            Log.d(TAG, "onKeyFrameFound");
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    lookingForSeverLinearLayout.setVisibility(View.GONE);
                }
            });
        }
    }
}
