package com.example.buildtrack_v1;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostUpdateActivity extends AppCompatActivity {

    private String projectId, projectName;
    private EditText etWorkDescription;
    private ImageView ivPhotoPreview;
    private Button btnSubmitUpdate;
    private TextView tvProjectTitle;

    private Uri selectedPhotoUri = null;
    private ActivityResultLauncher<String> photoPicker;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraPhotoUri;
    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_update);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        if (tvProjectTitle != null && projectName != null) tvProjectTitle.setText(projectName);

        etWorkDescription = findViewById(R.id.etCaption);
        ivPhotoPreview = findViewById(R.id.ivPreview);
        btnSubmitUpdate = findViewById(R.id.btnPublishUpdate);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        photoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedPhotoUri = uri;
                ivPhotoPreview.setImageURI(uri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), saved -> {
            if (saved && cameraPhotoUri != null) {
                selectedPhotoUri = cameraPhotoUri;
                ivPhotoPreview.setImageURI(null); // force a refresh of the same-size preview
                ivPhotoPreview.setImageURI(cameraPhotoUri);
            }
        });

        findViewById(R.id.btnPickPhoto).setOnClickListener(v -> photoPicker.launch("image/*"));
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> takePhoto());

        btnSubmitUpdate.setOnClickListener(v -> postUpdate());
    }

    private void takePhoto() {
        try {
            File file = new File(getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
            cameraPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (IllegalArgumentException | SecurityException | ActivityNotFoundException e) {
            Toast.makeText(this, R.string.take_photo_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void postUpdate() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || projectId == null) {
            Toast.makeText(this, "Open a site first, then post the update", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = etWorkDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Describe what the photo shows", Toast.LENGTH_SHORT).show();
            return;
        }

        DailyLog log = new DailyLog();
        log.projectId = projectId;
        log.engineerId = user.getUid();
        log.engineerName = "Manager Update";
        log.workDescription = description;
        log.weather = getString(R.string.milestone_update);

        Date now = new Date();
        log.loggedAt = now;
        log.date = DailyLogRepository.dateString(now);
        log.time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now);

        List<Uri> photos = new ArrayList<>();
        if (selectedPhotoUri != null) {
            photos.add(selectedPhotoUri);
        }
        if (photos.isEmpty()) {
            Toast.makeText(this, "Select a site photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitUpdate.setEnabled(false);
        btnSubmitUpdate.setText(R.string.posting_update);

        repo.saveLogWithPhotos(log, photos, new DailyLogRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(PostUpdateActivity.this,
                        "Update published. The buyer sees it on their site feed.",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                btnSubmitUpdate.setEnabled(true);
                btnSubmitUpdate.setText(R.string.publish_update_to_buyers);
                Toast.makeText(PostUpdateActivity.this,
                        "Could not post: " + (e != null ? e.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}