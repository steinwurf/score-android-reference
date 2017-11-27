package com.steinwurf.score_android_server_reference;

import android.Manifest;
import android.content.DialogInterface;
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
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS = 1;

    private static final String ipString = "224.0.0.251";
    private static final String portString = "9810";

    private final Server server = new Server(new ServerOnStateChangeListener());
    private final VideoEncoder videoEncoder = new VideoEncoder (new VideoEncoderOnDataListener());
    private final Camera camera = new Camera(videoEncoder);

    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

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
                            server.start(ipString, portString);
                            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
                            try {
                                camera.start(manager);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, onPostFinishedListener);
                } else {
                    backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            camera.stop();
                            server.stop();
                        }
                    }, onPostFinishedListener);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        camera.stop();
        server.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                finish();
                            }
                        });
                builder.create().show();
            }
        }
    }

    private class ServerOnStateChangeListener implements Server.OnStateChangeListener {
        @Override
        public void onError(String reason) {
            Log.d(TAG, reason);
        }
    }

    private class VideoEncoderOnDataListener implements VideoEncoder.OnDataListener {

        @Override
        public void onData(ByteBuffer buffer) {
            byte[] data = buffer.array();
            if (isIFrame(data)) {
                server.sendMessage(videoEncoder.getSPS());
                server.sendMessage(videoEncoder.getPPS());
            }
            server.sendMessage(data);
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "EOS");
        }

        private boolean isIFrame(byte[] data)
        {
            return (data.length > 5 &&
                    data[0] == 0x00 &&
                    data[1] == 0x00 &&
                    data[2] == 0x00 &&
                    data[3] == 0x01 &&
                    (data[4] & 0x1F) == 5);
        }
    }
}
