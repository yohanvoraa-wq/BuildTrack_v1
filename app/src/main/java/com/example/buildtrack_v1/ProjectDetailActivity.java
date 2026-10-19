package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        tvProjectName.setText(projectName != null ? projectName : "Project");

        btnBack.setOnClickListener(v -> finish());

        // Navigation to other Manager screens
        btnNavLogs.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogsViewActivity.class);
            intent.putExtra("projectId", projectId);
            startActivity(intent);
        });

        btnNavAssign.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignPeopleActivity.class);
            intent.putExtra("projectId", projectId);
            startActivity(intent);
        });

        btnNavComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("projectId", projectId);
            startActivity(intent);
        });

        btnNavPostUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostUpdateActivity.class);
            intent.putExtra("projectId", projectId);
            startActivity(intent);
        });

        // Initialize default sample phase values
        currentPhases.put("Foundation", 100);
        currentPhases.put("Framing & Structure", 100);
        currentPhases.put("Masonry", 80);
        currentPhases.put("Plumbing & Electrical", 60);
        currentPhases.put("Finishing", 40);
        currentPhases.put("Handover", 0);

        populatePhaseSliders();
        calculateOverall();

        btnSaveChanges.setOnClickListener(v -> {
            Toast.makeText(this, "Progress saved successfully!", Toast.LENGTH_SHORT).show();
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
            title.setTextColor(getResources().getColor(R.color.ink));
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
        tvOverallPercent.setText(avg + "%");
    }
}