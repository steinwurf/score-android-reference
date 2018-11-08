package com.steinwurf.score.client_reference.video;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.steinwurf.mediaplayer.NaluType;
import com.steinwurf.mediaplayer.Utils;
import com.steinwurf.score.client_reference.Client;
import com.steinwurf.score.client_reference.KeepAlive;
import com.steinwurf.score.client_reference.R;
import com.steinwurf.score.shared.BackgroundHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class VideoClientActivity extends AppCompatActivity {

    private static final String TAG = VideoClientActivity.class.getSimpleName();

    /**
     * Hardcoded width of the incoming stream
     */
    private static final int WIDTH = 1280;

    /**
     * Hardcoded height of the incoming stream
     */
    private static final int HEIGHT = 720;

    /**
     * The client
     */
    private final Client client = new Client(new ClientOnEventListener());

    /**
     * The video player for playing the incoming video data.
     */
    private final VideoPlayer videoPlayer = new VideoPlayer(new VideoPlayerVideoEventListener());

    /**
     * The background handler for handling work in the background
     */
    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    /**
     * The keep alive handler which prevents the device's wifi from sleeping
     */
    private KeepAlive keepAlive;

    /**
     * The sps buffer
     */
    byte[] sps = null;

    /**
     * The pps buffer
     */
    byte[] pps = null;

    /**
     * The button for starting and stopping the client
     */
    private ToggleButton startStopToggleButton;

    /**
     * The view used for showing the video
     */
    private TextureView videoTextureView;

    /**
     * The view which covers the screen if no stream is playing
     */
    private View lookingForSeverLinearLayout;

    /**
     * The EditText which should contain the multicast ip to join
     */
    private EditText ipEditText;

    /**
     * The EditText which should contain the port
     */
    private EditText portEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_client);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideUI();

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        keepAlive = KeepAlive.createKeepAlive(wm, 20);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        videoTextureView = findViewById(R.id.videoTextureView);
        lookingForSeverLinearLayout = findViewById(R.id.lookingForSeverLinearLayout);
        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);

        videoTextureView.setSurfaceTextureListener(videoPlayer);

        backgroundHandler.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (keepAlive != null)
            keepAlive.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startStopToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.setEnabled(false);
            ipEditText.setEnabled(false);
            portEditText.setEnabled(false);

            if (isChecked) {
                String ipString = ipEditText.getText().toString();
                String portString = portEditText.getText().toString();
                backgroundHandler.post(
                        () -> client.start(ipString, portString),
                        () -> runOnUiThread(() -> buttonView.setEnabled(true)));
            } else {
                backgroundHandler.post(() -> {
                    client.stop();
                    if (videoPlayer.isRunning())
                        videoPlayer.stop();
                }, () -> runOnUiThread(() -> {
                    buttonView.setEnabled(true);
                    ipEditText.setEnabled(true);
                    portEditText.setEnabled(true);
                }));

                lookingForSeverLinearLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (keepAlive != null)
            keepAlive.stop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            client.stop();
            backgroundHandler.stop();
        }
    }

    /**
     * Hides the UI of the application
     */
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

        findViewById(R.id.mainRelativeLayout).setSystemUiVisibility(visibility);
    }

    /**
     * Class for handling the client's events.
     */
    private class ClientOnEventListener implements Client.OnEventListener
    {
        @Override
        public void onError(String reason) {
            Log.d(TAG, reason);
        }

        @Override
        public void onData(ByteBuffer buffer) {
            try {
                if (NaluType.parse(buffer.slice()) == NaluType.SequenceParameterSet) {
                    if (sps == null) {
                        Log.d(TAG, "Got sps");
                        // store the sps
                        sps = Arrays.copyOfRange(buffer.array(), 0, buffer.remaining());
                    }
                    return;
                }

                if (NaluType.parse(buffer.slice()) == NaluType.PictureParameterSet) {
                    if (pps == null) {
                        Log.d(TAG, "Got pps");
                        // store the pps
                        pps = Arrays.copyOfRange(buffer.array(), 0, buffer.remaining());
                    }
                    return;
                }
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
                return;
            }
            if (videoPlayer.isRunning()) {
                // The video is running so we can feed data to it
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.position(buffer.remaining() - Long.SIZE / Byte.SIZE);
                long presentationTimeUs = buffer.getLong();
                buffer.position(0);
                buffer.limit(buffer.remaining() - Long.SIZE / Byte.SIZE);
                videoPlayer.handleData(presentationTimeUs, buffer);
            } else if (pps != null && sps != null) {
                Log.d(TAG, "Starting video player");
                // We have both the sps and pps which means we are ready to start the playback.
                Point display  = Utils.getRealMetrics(getWindowManager().getDefaultDisplay());
                Matrix matrix = Utils.fitScale(WIDTH, HEIGHT, display.x, display.y).toMatrix();
                runOnUiThread(() -> videoTextureView.setTransform(matrix));
                try {
                    videoPlayer.start(WIDTH, HEIGHT, sps, pps);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Class for handling the video players events.
     */
    private class VideoPlayerVideoEventListener implements VideoPlayer.VideoEventListener {

        @Override
        public void onKeyFrameFound() {

            Log.d(TAG, "onKeyFrameFound");
            // The video is available - hide the overlay!
            runOnUiThread(() -> lookingForSeverLinearLayout.setVisibility(View.GONE));
        }
    }
}
