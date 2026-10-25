package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The stage bars behind a site's overall number. Both the on-site engineer and the manager open
 * this; the overall bar shown everywhere else is always the mean of these values.
 */
public class PhaseProgressActivity extends AppCompatActivity {

    private final DailyLogRepository repo = new DailyLogRepository();
    private final Map<String, Integer> values = new LinkedHashMap<>();

    private LinearLayout llPhases;
    private TextView tvOverall;
    private ProgressBar pbOverall;
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(UiTheme.roleTheme(getIntent().getStringExtra("role")));
        setContentView(R.layout.activity_phase_progress);

        projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        llPhases = findViewById(R.id.llPhases);
        tvOverall = findViewById(R.id.tvOverall);
        pbOverall = findViewById(R.id.pbOverall);

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> save());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvProjectName = findViewById(R.id.tvProjectName);
        tvProjectName.setText(projectName == null ? "" : projectName);

        if (projectId == null) {
            Toast.makeText(this, "No project specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        load();
    }

    private void load() {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo p) {
                buildRows(DailyLogRepository.phasesForEditor(p.phaseProgress));
            }

            @Override public void onError(Exception e) {
                Toast.makeText(PhaseProgressActivity.this,
                        getString(R.string.could_not_save, message(e)), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void buildRows(Map<String, Integer> phases) {
        LayoutInflater inflater = LayoutInflater.from(this);
        values.clear();
        llPhases.removeAllViews();

        for (Map.Entry<String, Integer> entry : phases.entrySet()) {
            final String phaseName = entry.getKey();
            int start = entry.getValue() == null ? 0 : entry.getValue();
            values.put(phaseName, start);

            View row = inflater.inflate(R.layout.item_phase_row, llPhases, false);
            ((TextView) row.findViewById(R.id.tvPhaseName)).setText(phaseName);
            TextView tvValue = row.findViewById(R.id.tvPhaseValue);
            tvValue.setText(getString(R.string.percent_value, start));

            SeekBar bar = row.findViewById(R.id.sbPhase);
            bar.setProgress(start);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    values.put(phaseName, progress);
                    tvValue.setText(getString(R.string.percent_value, progress));
                    refreshOverall();
                }

                @Override public void onStartTrackingTouch(SeekBar s) { }

                @Override public void onStopTrackingTouch(SeekBar s) { }
            });
            llPhases.addView(row);
        }
        refreshOverall();
    }

    private void refreshOverall() {
        Integer overall = DailyLogRepository.overallFrom(values, null);
        int percent = overall == null ? 0 : overall;
        tvOverall.setText(getString(R.string.percent_value, percent));
        pbOverall.setProgress(percent);
    }

    private void save() {
        if (projectId == null) return;
        repo.setPhaseProgress(projectId, values, new DailyLogRepository.Callback<Void>() {
            @Override public void onSuccess(Void r) {
                int overall = DailyLogRepository.overallFrom(values, 0);
                Intent result = new Intent();
                result.putExtra("overallPercent", (int) overall);
                setResult(RESULT_OK, result);
                Toast.makeText(PhaseProgressActivity.this,
                        R.string.progress_updated, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override public void onError(Exception e) {
                Toast.makeText(PhaseProgressActivity.this,
                        getString(R.string.could_not_save, message(e)), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String message(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : "unknown error";
    }
}
