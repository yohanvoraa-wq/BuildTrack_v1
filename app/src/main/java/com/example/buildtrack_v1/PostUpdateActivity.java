package com.example.buildtrack_v1;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class PostUpdateActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private EditText etCaption;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ivPreview.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_update);

        ivPreview = findViewById(R.id.ivPreview);
        etCaption = findViewById(R.id.etCaption);
        Button btnPickPhoto = findViewById(R.id.btnPickPhoto);
        Button btnPublishUpdate = findViewById(R.id.btnPublishUpdate);
        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnPickPhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnPublishUpdate.setOnClickListener(v -> {
            String caption = etCaption.getText().toString().trim();
            if (caption.isEmpty()) {
                etCaption.setError("Enter a caption");
                return;
            }
            Toast.makeText(this, "Site update published for buyers!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}