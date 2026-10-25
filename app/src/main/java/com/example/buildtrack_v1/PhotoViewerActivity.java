package com.example.buildtrack_v1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Full-screen photo with pinch-to-zoom. Opened by tapping any site photo. */
public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "photo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        ZoomImageView zoom = findViewById(R.id.zoomImage);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        String photo = getIntent().getStringExtra(EXTRA_PHOTO);
        ImageHelper.showFull(zoom, photo);
    }

    public static void open(android.content.Context c, String stored) {
        android.content.Intent i = new android.content.Intent(c, PhotoViewerActivity.class);
        i.putExtra(EXTRA_PHOTO, stored);
        c.startActivity(i);
    }
}
