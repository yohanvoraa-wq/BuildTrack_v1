package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * The buyer's progress view. The overall figure is the mean of the construction stages, which the
 * site engineer and manager maintain; nothing here is estimated or pre-filled.
 */
public class ProgressActivity extends AppCompatActivity {

    private static final int MAX_ROWS = 12;

    private TextView tvOverallPct, tvEstimate, tvNoProgress;
    private ProgressBar pbOverall;
    private LinearLayout rows;
    private String handover, logSummary;

    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        String projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        ((TextView) findViewById(R.id.tvSubtitle)).setText(
                projectName != null ? projectName : "Reported by your site engineer");

        tvOverallPct = findViewById(R.id.tvOverallPct);
        tvEstimate = findViewById(R.id.tvEstimateCalculation);
        tvNoProgress = findViewById(R.id.tvNoProgress);
        pbOverall = findViewById(R.id.pbOverall);
        rows = findViewById(R.id.progressRows);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (projectId == null) {
            tvEstimate.setText(R.string.no_project_selected);
            return;
        }

        loadStages(projectId);
        loadReports(projectId);
    }

    /** Planned handover date is the end date the manager set on the project. */
    private void loadStages(String projectId) {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo project) {
                handover = project.endDate == null ? null
                        : "Planned handover: " + ProgressUtil.formatDate(project.endDate);
                bindStages(project.phaseProgress, project.overallPercent);
                updateEstimate();
            }

            @Override public void onError(Exception e) {
                tvNoProgress.setVisibility(View.VISIBLE);
                tvNoProgress.setText(getString(R.string.error_loading_stage_progress,
                        e != null ? e.getMessage() : getString(R.string.unknown_error)));
            }
        });
    }

    private void loadReports(String projectId) {
        repo.getAllLogsForProject(projectId, new DailyLogRepository.Callback<List<DailyLog>>() {
            @Override public void onSuccess(List<DailyLog> logs) {
                logSummary = logs.isEmpty() ? null
                        : logs.size() + (logs.size() == 1 ? " site log" : " site logs") + " so far";
                updateEstimate();
            }

            @Override public void onError(Exception e) { /* the stage bars above still render */ }
        });
    }

    private void bindStages(java.util.Map<String, Integer> phases, Integer overall) {
        if (overall == null) {
            tvOverallPct.setText(R.string.no_stages_reported);
            pbOverall.setProgress(0);
        } else {
            tvOverallPct.setText(getString(R.string.percent_value, overall));
            pbOverall.setProgress(overall);
        }

        rows.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int shown = 0;
        for (java.util.Map.Entry<String, Integer> e :
                DailyLogRepository.phasesForEditor(phases).entrySet()) {
            if (shown == MAX_ROWS) break;
            int value = e.getValue() == null ? 0 : e.getValue();
            View row = inflater.inflate(R.layout.item_progress_row, rows, false);
            ((TextView) row.findViewById(R.id.tvRowWhen)).setText(e.getKey());
            ((TextView) row.findViewById(R.id.tvRowPct)).setText(getString(R.string.percent_value, value));
            ((ProgressBar) row.findViewById(R.id.pbRow)).setProgress(value);
            rows.addView(row);
            shown++;
        }

        tvNoProgress.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }

    private void updateEstimate() {
        StringBuilder sb = new StringBuilder();
        if (handover != null) sb.append(handover);
        if (logSummary != null) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(logSummary);
        }
        tvEstimate.setText(sb.length() == 0 ? "Waiting for the first site log" : sb);
    }
}
