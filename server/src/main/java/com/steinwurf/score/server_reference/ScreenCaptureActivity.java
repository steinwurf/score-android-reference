package com.steinwurf.score.server_reference;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.steinwurf.score.shared.BackgroundHandler;
import com.steinwurf.score.shared.NaluType;
import com.steinwurf.score_android_server_reference.R;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureActivity extends AppCompatActivity {

    private static final String TAG = ScreenCaptureActivity.class.getSimpleName();

    /**
     * Permission request for for capturing the screen.
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
     * The screen capture which feeds the video encoder
     */
    private final ScreenCapture screenRecorder = new ScreenCapture(videoEncoder);

    /**
     * The background handler for handling work in the background
     */
    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    /**
     * The button for starting and stopping the client
     */
    private ToggleButton startStopToggleButton;

    /**
     * The Media Projection Manager used for initializing the screen capture
     */
    private MediaProjectionManager mMediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        backgroundHandler.start();

    }

    @Override
    protected void onStart() {
        super.onStart();
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
    protected void onDestroy() {
        super.onDestroy();
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
