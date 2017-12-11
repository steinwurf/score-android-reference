package com.steinwurf.score.server_reference.video;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ToggleButton;

import com.steinwurf.score.server_reference.R;
import com.steinwurf.score.server_reference.Server;
import com.steinwurf.score.shared.BackgroundHandler;
import com.steinwurf.score.shared.NaluType;
import com.steinwurf.score.source.AutoSource;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    /**
     * Permission request for for using the camera
     */
    private static final int REQUEST_PERMISSIONS = 1;

    /**
     * Hardcoded IP string
     */
    private static final String ipString = "224.0.0.251";

    /**
     * Hardcoded Port string
     */
    private static final String portString = "9810";

    /**
     * The server
     */
    private final Server server = new Server(new ServerOnEventListener());

    /**
     * The video encoder which feeds the server
     */
    private final VideoEncoder videoEncoder = new VideoEncoder (new VideoEncoderOnDataListener());

    /**
     * The camera which feeds the video encoder
     */
    private final Camera camera = new Camera(videoEncoder);

    /**
     * The background handler for handling work in the background
     */
    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    /**
     * The button for starting and stopping the server
     */
    private ToggleButton startStopToggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        backgroundHandler.start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
            }
        }
        else {
            setup();
        }
    }

    private void setup() {
        startStopToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.setEnabled(false);
            if (isChecked) {
                backgroundHandler.post(() -> {
                    AutoSource autoSource = new AutoSource();
                    autoSource.setSymbolSize(750);
                    autoSource.setGenerationSize(50);
                    server.start(autoSource, ipString, portString);
                    CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
                    try {
                        camera.start(manager);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, () -> runOnUiThread(() -> buttonView.setEnabled(true)));
            } else {
                backgroundHandler.post(() -> {
                    camera.stop();
                    server.stop();
                }, () -> runOnUiThread(() -> buttonView.setEnabled(true)));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.stop();
        server.stop();
        backgroundHandler.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setup();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Issue")
                        .setMessage("Required permissions not granted.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finish());
                builder.create().show();
            }
        }
    }

    /**
     * Class for handling the server's events.
     */
    private class ServerOnEventListener implements Server.OnEventListener {
        @Override
        public void onError(String reason) {
            Log.d(TAG, reason);
        }
    }

    /**
     * Class for handling when the encoder has new data to feed to the server
     */
    private class VideoEncoderOnDataListener implements VideoEncoder.OnDataListener {

        @Override
        public void onData(ByteBuffer buffer) {
            byte[] data = buffer.array();
            if (NaluType.parse(data) == NaluType.IdrSlice) {
                server.sendMessage(videoEncoder.getSPS());
                server.sendMessage(videoEncoder.getPPS());
            }
            server.sendMessage(data);
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "EOS");
        }
    }
}
