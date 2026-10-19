package com.example.buildtrack_v1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProjectDetailActivity extends AppCompatActivity {

    private String projectId, projectName;
    private TextView tvProjectName, tvOverallPercent;
    private ProgressBar pbOverallDetail;
    private LinearLayout phasesContainer;
    private final Map<String, Integer> currentPhases = new HashMap<>();

    private final String[] PHASE_NAMES = {
            "Foundation",
            "Framing & Structure",
            "Masonry",
            "Plumbing & Electrical",
            "Finishing",
            "Handover"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project_detail);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        tvProjectName = findViewById(R.id.tvProjectName);
        tvOverallPercent = findViewById(R.id.tvOverallPercent);
        pbOverallDetail = findViewById(R.id.pbOverallDetail);
        phasesContainer = findViewById(R.id.phasesContainer);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnNavLogs = findViewById(R.id.btnNavLogs);
        Button btnNavAssign = findViewById(R.id.btnNavAssign);
        Button btnNavComments = findViewById(R.id.btnNavComments);
        Button btnNavPostUpdate = findViewById(R.id.btnNavPostUpdate);
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);

        tvProjectName.setText(projectName != null ? projectName : "Project Detail");

        btnBack.setOnClickListener(v -> finish());

        // Quick action navigation
        btnNavLogs.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogsViewActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });

        btnNavAssign.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignPeopleActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });

        btnNavComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });

        btnNavPostUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostUpdateActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });

        // Initialize sample phase values based on project
        if ("proj_02".equals(projectId)) {
            // 100% completed sample project
            currentPhases.put("Foundation", 100);
            currentPhases.put("Framing & Structure", 100);
            currentPhases.put("Masonry", 100);
            currentPhases.put("Plumbing & Electrical", 100);
            currentPhases.put("Finishing", 100);
            currentPhases.put("Handover", 100);
        } else if ("proj_01".equals(projectId)) {
            currentPhases.put("Foundation", 100);
            currentPhases.put("Framing & Structure", 100);
            currentPhases.put("Masonry", 80);
            currentPhases.put("Plumbing & Electrical", 68);
            currentPhases.put("Finishing", 60);
            currentPhases.put("Handover", 0);
        } else {
            // Newly created project starts at 0%
            currentPhases.put("Foundation", 0);
            currentPhases.put("Framing & Structure", 0);
            currentPhases.put("Masonry", 0);
            currentPhases.put("Plumbing & Electrical", 0);
            currentPhases.put("Finishing", 0);
            currentPhases.put("Handover", 0);
        }

        populatePhaseSliders();
        calculateOverall();

        // Save Progress Button (now visible right near the top)
        btnSaveChanges.setOnClickListener(v -> {
            int total = 0;
            for (String phase : PHASE_NAMES) {
                total += currentPhases.containsKey(phase) ? currentPhases.get(phase) : 0;
            }
            int overall = total / PHASE_NAMES.length;

            if (projectId != null) {
                FirebaseFirestore.getInstance().collection("projects").document(projectId)
                        .update("overallProgress", overall, "phaseProgress", currentPhases);
            }
            Toast.makeText(this, "Progress saved successfully (" + overall + "%)!", Toast.LENGTH_SHORT).show();
        });
    }

    private void populatePhaseSliders() {
        phasesContainer.removeAllViews();
        for (String phase : PHASE_NAMES) {
            int currentVal = currentPhases.containsKey(phase) ? currentPhases.get(phase) : 0;

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setBackgroundResource(R.drawable.bg_card);
            itemLayout.setPadding(30, 24, 30, 24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 16;
            itemLayout.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText(phase + ": " + currentVal + "%");
            title.setTextColor(ContextCompat.getColor(this, R.color.ink));
            title.setTextSize(15);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            SeekBar seekBar = new SeekBar(this);
            seekBar.setMax(100);
            seekBar.setProgress(currentVal);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    currentPhases.put(phase, progress);
                    title.setText(phase + ": " + progress + "%");
                    calculateOverall();
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });

            itemLayout.addView(title);
            itemLayout.addView(seekBar);
            phasesContainer.addView(itemLayout);
        }
    }

    private void calculateOverall() {
        int total = 0;
        for (String phase : PHASE_NAMES) {
            total += currentPhases.containsKey(phase) ? currentPhases.get(phase) : 0;
        }
        int avg = total / PHASE_NAMES.length;
        pbOverallDetail.setProgress(avg);

        // If 100% completed, show GREEN LINE and GREEN text
        if (avg == 100) {
            pbOverallDetail.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#27AE60")));
            tvOverallPercent.setTextColor(Color.parseColor("#27AE60"));
            tvOverallPercent.setText("100% Completed");
        } else {
            pbOverallDetail.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.manager_crimson)));
            tvOverallPercent.setTextColor(ContextCompat.getColor(this, R.color.manager_crimson));
            tvOverallPercent.setText(avg + "%");
        }
    }
}