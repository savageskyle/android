package com.example.lab4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnPlayLocalMedia;
    private Button btnDownloadMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPlayLocalMedia = findViewById(R.id.btnPlayLocalMedia);
        btnDownloadMedia = findViewById(R.id.btnDownloadMedia);

        btnPlayLocalMedia.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MediaPlayerActivity.class);
            startActivity(intent);
        });

        btnDownloadMedia.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
            startActivity(intent);
        });
    }
}