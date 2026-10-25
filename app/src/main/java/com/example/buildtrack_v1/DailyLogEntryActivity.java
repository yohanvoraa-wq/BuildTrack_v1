package com.example.buildtrack_v1;

import android.content.ActivityNotFoundException;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyLogEntryActivity extends AppCompatActivity {

    private static final String[] MATERIALS =
            {"Cement", "Sand", "Steel", "Bricks", "Aggregate", "Paints", "Tiles", "Other"};
    private static final String[] UNITS =
            {"bags", "kg", "tonnes", "pieces", "cubic metres", "litres"};
    private static final String[] TRADES =
            {"Mason", "Carpenter", "Electrician", "Plumber", "Helper", "Painter", "Welder", "Other"};

    private LinearLayout materialRows, labourRows;
    private Button btnSubmit, btnAddPhoto, btnTakePhoto, btnEditProgress;
    private EditText etWorkDescription, etRemarks;
    private Spinner spWeather;
    private TextView tvPhotoCount, tvProgressValue;
    private RecyclerView rvSitePhotos;

    private String projectId;
    private String projectName;
    private String engineerName;

    /** The stage average at the time of logging; null when nothing has been recorded yet. */
    private Integer stageOverall;

    private final List<Uri> selectedPhotoUris = new ArrayList<>();
    private PhotoAdapter photoAdapter;
    private ActivityResultLauncher<String> photoPickerLauncher;
    private ActivityResultLauncher<Intent> stageEditorLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraPhotoUri;

    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_log);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");
        engineerName = getIntent().getStringExtra("engineerName");

        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        if (tvSubtitle != null && projectName != null) {
            tvSubtitle.setText(projectName);
        }

        // Initialize UI Views
        materialRows = findViewById(R.id.materialRows);
        labourRows = findViewById(R.id.labourRows);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        etWorkDescription = findViewById(R.id.etWorkDescription);
        etRemarks = findViewById(R.id.etRemarks);
        spWeather = findViewById(R.id.spWeather);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        rvSitePhotos = findViewById(R.id.rvSitePhotos);
        tvProgressValue = findViewById(R.id.tvProgressValue);
        btnEditProgress = findViewById(R.id.btnEditProgress);

        // Setup Weather Spinner
        spWeather.setAdapter(adapterFor(getResources().getStringArray(R.array.weather_options)));

        stageEditorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int overall = result.getData().getIntExtra("overallPercent", 0);
                        stageOverall = overall;
                        showOverall(overall);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                saved -> {
                    if (saved && cameraPhotoUri != null) {
                        selectedPhotoUris.add(cameraPhotoUri);
                        photoAdapter.notifyDataSetChanged();
                        updatePhotoCount();
                    }
                });

        if (projectId != null) loadOverall();

        // Setup Photo RecyclerView
        photoAdapter = new PhotoAdapter();
        rvSitePhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSitePhotos.setAdapter(photoAdapter);

        // Multi-image picker launcher (as many photos as they want)
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedPhotoUris.addAll(uris);
                        photoAdapter.notifyDataSetChanged();
                        updatePhotoCount();
                    }
                }
        );

        btnAddPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnEditProgress.setOnClickListener(v -> openStageEditor());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddMaterial).setOnClickListener(v -> addMaterialRow());
        findViewById(R.id.btnAddLabour).setOnClickListener(v -> addLabourRow());
        btnSubmit.setOnClickListener(v -> submit());

        addMaterialRow();
        addLabourRow();
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

    private void openStageEditor() {        if (projectId == null) return;
        Intent intent = new Intent(this, PhaseProgressActivity.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("projectName", projectName);
        intent.putExtra("role", "engineer");
        stageEditorLauncher.launch(intent);
    }

    private void loadOverall() {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo p) {
                stageOverall = p.overallPercent;
                showOverall(p.overallPercent);
            }

            @Override public void onError(Exception e) { showOverall(null); }
        });
    }

    private void showOverall(Integer percent) {
        if (percent == null) {
            tvProgressValue.setText(R.string.stage_progress_unread);
        } else {
            tvProgressValue.setText(getString(R.string.percent_value, percent));
        }
    }

    private void updatePhotoCount() {
        int count = selectedPhotoUris.size();
        if (count == 0) {
            tvPhotoCount.setText(R.string.photos_zero);
        } else {
            tvPhotoCount.setText(getResources().getQuantityString(R.plurals.photo_count, count, count));
        }
    }

    private void addMaterialRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.activity_itemmaterial_row, materialRows, false);
        ((Spinner) row.findViewById(R.id.spMaterial)).setAdapter(adapterFor(MATERIALS));
        ((Spinner) row.findViewById(R.id.spUnit)).setAdapter(adapterFor(UNITS));
        row.findViewById(R.id.btnRemove).setOnClickListener(v -> materialRows.removeView(row));
        materialRows.addView(row);
    }

    private void addLabourRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.activity_itemlabour_row, labourRows, false);
        ((Spinner) row.findViewById(R.id.spTrade)).setAdapter(adapterFor(TRADES));
        row.findViewById(R.id.btnRemove).setOnClickListener(v -> labourRows.removeView(row));
        labourRows.addView(row);
    }

    private ArrayAdapter<String> adapterFor(String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void submit() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || projectId == null) {
            Toast.makeText(this, "Not signed in or no project selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String workDescription = etWorkDescription.getText().toString().trim();
        if (workDescription.isEmpty()) {
            etWorkDescription.setError("Please describe work completed today");
            etWorkDescription.requestFocus();
            return;
        }

        DailyLog log = new DailyLog();
        log.workDescription = workDescription;
        log.weather = spWeather.getSelectedItem().toString();
        log.remarks = etRemarks.getText().toString().trim();
        log.progressPercent = stageOverall;

        // Validate & read materials
        for (int i = 0; i < materialRows.getChildCount(); i++) {
            View row = materialRows.getChildAt(i);
            EditText etQty = row.findViewById(R.id.etQty);
            String qtyStr = etQty.getText().toString().trim();
            if (!qtyStr.isEmpty()) {
                try {
                    double qty = Double.parseDouble(qtyStr);
                    if (qty > 0) {
                        log.materials.add(new DailyLog.MaterialEntry(
                                ((Spinner) row.findViewById(R.id.spMaterial)).getSelectedItem().toString(),
                                qty,
                                ((Spinner) row.findViewById(R.id.spUnit)).getSelectedItem().toString()));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Validate & read labour
        for (int i = 0; i < labourRows.getChildCount(); i++) {
            View row = labourRows.getChildAt(i);
            EditText etCount = row.findViewById(R.id.etCount);
            String countStr = etCount.getText().toString().trim();
            if (!countStr.isEmpty()) {
                try {
                    int count = Integer.parseInt(countStr);
                    if (count > 0) {
                        log.labour.add(new DailyLog.LabourEntry(
                                ((Spinner) row.findViewById(R.id.spTrade)).getSelectedItem().toString(),
                                count));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Exact system date & time at moment of submission
        Date now = new Date();
        log.projectId = projectId;
        log.engineerId = user.getUid();
        log.engineerName = engineerName != null ? engineerName : user.getEmail();
        log.loggedAt = now;
        log.date = DailyLogRepository.dateString(now);
        log.time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now);

        btnSubmit.setEnabled(false);
        btnSubmit.setText(R.string.submitting_log);

        // Save log with all photos uploaded to Firebase
        repo.saveLogWithPhotos(log, selectedPhotoUris, new DailyLogRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(DailyLogEntryActivity.this, "Site log saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.submit_log);
                Toast.makeText(DailyLogEntryActivity.this, getString(R.string.could_not_save, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Photo preview list adapter
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log_photo, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Uri uri = selectedPhotoUris.get(position);
            holder.ivPhoto.setImageURI(uri);
            holder.btnRemovePhoto.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    selectedPhotoUris.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, selectedPhotoUris.size());
                    updatePhotoCount();
                }
            });
        }

        @Override
        public int getItemCount() {
            return selectedPhotoUris.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView btnRemovePhoto;

            ViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivPhoto);
                btnRemovePhoto = itemView.findViewById(R.id.btnRemovePhoto);
            }
        }
    }
}