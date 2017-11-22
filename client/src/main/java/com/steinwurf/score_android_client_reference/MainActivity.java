package com.steinwurf.score_android_client_reference;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements Client.IClientHandler {

    private static final String TAG = MainActivity.class.getSimpleName()        ;

    ToggleButton startStopToggleButton;
    SurfaceView videoSurfaceView;

    final Client client = new Client(this);
    private String ipString = "224.0.0.251";
    private String portString = "9810";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        videoSurfaceView = findViewById(R.id.videoSurfaceView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startStopToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    buttonView.setEnabled(false);
                    client.start(ipString, portString);
                }
                else
                {
                    client.stop();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        client.stop();
    }

    @Override
    public void onClientStarted() {
        startStopToggleButton.setEnabled(true);
    }

    @Override
    public void onClientError(String reason) {
        Log.d(TAG, reason);
        startStopToggleButton.setEnabled(true);
    }
}
