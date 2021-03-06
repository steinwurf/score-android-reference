package com.steinwurf.score.client_reference.audio;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.steinwurf.score.client_reference.Client;
import com.steinwurf.score.client_reference.KeepAlive;
import com.steinwurf.score.client_reference.R;
import com.steinwurf.score.shared.BackgroundHandler;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class AudioClientActivity extends AppCompatActivity {

    private static final String TAG = AudioClientActivity.class.getSimpleName();

    /**
     * Hardcoded IP string
     */
    private static final String ipString = "224.0.0.251";

    /**
     * Hardcoded Port string
     */
    private static final String portString = "9810";

    /**
     * The client
     */
    private final Client client = new Client(new ClientOnEventListener());

    /**
     * The video player for playing the incoming video data.
     */
    private final AudioPlayer audioPlayer = new AudioPlayer();

    /**
     * The background handler for handling work in the background
     */
    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    /**
     * The keep alive handler which prevents the device's wifi from sleeping
     */
    private KeepAlive keepAlive;

    /**
     * The button for starting and stopping the client
     */
    private ToggleButton startStopToggleButton;

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
        setContentView(R.layout.activity_audio_client);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        keepAlive = KeepAlive.createKeepAlive(wm, 20);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        lookingForSeverLinearLayout = findViewById(R.id.lookingForSeverLinearLayout);
        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);

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
                    if (audioPlayer.isPlaying())
                        audioPlayer.stop();
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

    private class ClientOnEventListener implements Client.OnEventListener {
        @Override
        public void onError(String reason) {
            Log.d(TAG, reason);
        }

        @Override
        public void onData(ByteBuffer buffer) {
            if (!audioPlayer.isPlaying()) {
                audioPlayer.start();
                runOnUiThread(() -> lookingForSeverLinearLayout.setVisibility(View.GONE));
            }
            audioPlayer.handleData(buffer);
        }
    }
}
