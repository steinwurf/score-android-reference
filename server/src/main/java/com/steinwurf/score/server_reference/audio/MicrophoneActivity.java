package com.steinwurf.score.server_reference.audio;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.steinwurf.score.source.AutoSource;

import java.nio.ByteBuffer;

public class MicrophoneActivity extends AppCompatActivity {

    private static final String TAG = MicrophoneActivity.class.getSimpleName();

    /**
     * Permission request for for using the microphone
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
     * The audio recorder which feeds the server
     */
    private final AudioRecorder audioRecorder = new AudioRecorder(new AudioRecorderOnDataListener());

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
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        backgroundHandler.start();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
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
                    Log.d(TAG, "Starting");
                    AutoSource autoSource = new AutoSource();
                    // Configure the Score Source to handle the bufferSize of the audioRecorder.
                    // The library which splits up the messages into data packets, is called
                    // chunkie. Part of the symbol size is a header and checksum from this
                    // library. The size of this data is 16 bytes:
                    int headerBytes = 16;
                    int symbolSize = (audioRecorder.getBufferSize() / 4) + headerBytes;
                    autoSource.setSymbolSize(symbolSize);
                    autoSource.setGenerationSize(12);
                    server.start(autoSource, ipString, portString);
                    server.start(autoSource, ipString, portString);
                    audioRecorder.start();
                }, () -> runOnUiThread(() -> buttonView.setEnabled(true)));
            } else {
                Log.d(TAG, "Stopping");
                backgroundHandler.post(() -> {
                    audioRecorder.stop();
                    server.stop();
                }, () -> runOnUiThread(() -> buttonView.setEnabled(true)));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            audioRecorder.stop();
            server.stop();
            backgroundHandler.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Issue")
                        .setMessage("Required permissions not granted.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finish());
                builder.create().show();
            } else {
                setup();
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

    private class AudioRecorderOnDataListener implements AudioRecorder.OnDataListener {

        @Override
        public void onData(ByteBuffer buffer) {
            byte[] data = buffer.array();
            server.sendMessage(data);
        }
    }
}
