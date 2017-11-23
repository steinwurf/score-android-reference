package com.steinwurf.score_android_client_reference;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.net.wifi.WifiManager;
import android.support.annotation.Keep;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements Client.OnStateChangeListener, VideoPlayer.VideoEventListener {

    private static final String TAG = MainActivity.class.getSimpleName()        ;

    private static final String ipString = "224.0.0.251";
    private static final String portString = "9810";

    private final Client client = new Client(this);
    private final VideoPlayer videoPlayer = new VideoPlayer(this);

    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    private KeepAlive keepAlive;

    private ToggleButton startStopToggleButton;
    private TextureView videoTextureView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        keepAlive = KeepAlive.createKeepAlive(wm, 20);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        videoTextureView = findViewById(R.id.videoTextureView);
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

                if (isChecked)
                {
                    backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            client.start(ipString, portString);
                        }
                    }, onPostFinishedListener);
                }
                else
                {
                    backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            client.stop();
                        }
                    }, onPostFinishedListener);
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

    @Override
    public void onClientError(String reason) {
        Log.d(TAG, reason);
    }

    @Override
    public void onData(ByteBuffer data) {
        Log.d(TAG, "got some " + data.remaining());
    }

    @Override
    public void onKeyFrameFound() {

        Log.d(TAG, "onKeyFrameFound");
    }
}
