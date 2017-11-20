package com.steinwurf.score_android_reference;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.steinwurf.score.sink.Sink;
import com.steinwurf.score.source.Source;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Source source = new Source();
        Sink sink = new Sink();
    }
}
