package com.steinwurf.score_android_server_reference;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements Server.IServerHandler, Camera.OnDataListener {

    private static final String TAG = MainActivity.class.getSimpleName()        ;
    ToggleButton startStopToggleButton;
    TextView infoTextView;

    final Server server = new Server(this);
    final Camera camera = new Camera(this);
    private String ipString = "224.0.0.251";
    private String portString = "9810";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopToggleButton = findViewById(R.id.startStopToggleButton);
        infoTextView = findViewById(R.id.infoTextView);
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
                    server.start(ipString, portString);
                    CameraManager manager = (CameraManager) getApplicationContext().getSystemService(CAMERA_SERVICE);
                    try {
                        camera.start(manager);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    server.stop();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        server.stop();
    }

    @Override
    public void onServerStarted() {
        startStopToggleButton.setEnabled(true);
    }

    @Override
    public void onServerError(String reason) {
        Log.d(TAG, reason);
        startStopToggleButton.setEnabled(true);
    }

    @Override
    public void onData(ByteBuffer data) {
        server.sendData(data.array());
    }

    @Override
    public void onFinish() {
        Log.d(TAG, "EOS");
    }
}
