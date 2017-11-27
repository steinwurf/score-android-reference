package com.steinwurf.score_android_server_reference;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureActivity extends AppCompatActivity {

    private static final String TAG = ScreenCaptureActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS = 1;

    private static final String ipString = "224.0.0.251";
    private static final String portString = "9810";

    private final Server server = new Server(new ServerOnStateChangeListener());
    private final VideoEncoder videoEncoder = new VideoEncoder (new VideoEncoderOnDataListener());
    private final ScreenCapture screenRecorder = new ScreenCapture(videoEncoder);

    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    private ToggleButton startStopToggleButton;

    private MediaProjectionManager mMediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        backgroundHandler.start();
        startStopToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                buttonView.setEnabled(false);
                if (isChecked) {
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_PERMISSIONS);
                } else {
                    backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            screenRecorder.stop();
                            server.stop();
                        }
                    }, new BackgroundHandler.OnPostFinishedListener() {
                        @Override
                        public void finished() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonView.setEnabled(true);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        screenRecorder.stop();
        server.stop();
        backgroundHandler.stop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PERMISSIONS) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            return;
        }
        final MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                server.start(ipString, portString);
                try {
                    screenRecorder.start(mediaProjection);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new BackgroundHandler.OnPostFinishedListener() {
            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startStopToggleButton.setEnabled(true);
                    }
                });
            }
        });
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
