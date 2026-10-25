package com.example.buildtrack_v1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * The manager's hub for one site: how complete it is, and shortcuts to the team, the notes and
 * the logs. The overall number always comes from the stage bars, which the manager can edit here.
 */
public class ProjectDetailActivity extends AppCompatActivity {

    private final DailyLogRepository repo = new DailyLogRepository();

    private String projectId, projectName;
    private TextView tvOverallPercent, tvLastReport;
    private ProgressBar pbOverallDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project_detail);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        tvOverallPercent = findViewById(R.id.tvOverallPercent);
        tvLastReport = findViewById(R.id.tvLastReport);
        pbOverallDetail = findViewById(R.id.pbOverallDetail);

        if (projectName != null) {
            ((TextView) findViewById(R.id.tvProjectName)).setText(projectName);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        openOn(R.id.btnNavLogs, ManagerLogsActivity.class);
        openOn(R.id.btnNavAssign, AssignPeopleActivity.class);
        openOn(R.id.btnNavComments, CommentsActivity.class);
        openOn(R.id.btnNavPostUpdate, PostUpdateActivity.class);
        openOn(R.id.btnNavDocuments, DocumentsActivity.class);
        openOn(R.id.btnNavProgress, PhaseProgressActivity.class);

        if (projectId == null) {
            Toast.makeText(this, "No project selected", Toast.LENGTH_SHORT).show();
            return;
        }
        loadProgress();
    }

    private void openOn(int buttonId, Class<?> target) {
        Button btn = findViewById(buttonId);
        btn.setOnClickListener(v -> {
            Intent i = new Intent(this, target);
            i.putExtra("projectId", projectId);
            i.putExtra("projectName", projectName);
            i.putExtra("role", "manager");
            startActivity(i);
        });
    }

    private void loadProgress() {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo p) {
                bindOverall(p.overallPercent);
            }

            @Override public void onError(Exception e) { /* logs below still report */ }
        });

        repo.getAllLogsForProject(projectId,
                new DailyLogRepository.Callback<List<DailyLog>>() {
                    @Override public void onSuccess(List<DailyLog> logs) {
                        bindLastReport(logs);
                    }

                    @Override public void onError(Exception e) {
                        tvLastReport.setText(getString(R.string.error_loading_site_logs,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
                    }
                });
    }

    private void bindOverall(Integer work) {
        if (work == null) {
            tvOverallPercent.setText(R.string.no_stages_reported);
            pbOverallDetail.setProgress(0);
            pbOverallDetail.setProgressTintList(ColorStateList.valueOf(UiTheme.accent(this)));
        } else {
            tvOverallPercent.setText(getString(R.string.percent_value, work));
            pbOverallDetail.setProgress(work);
            // green only once the site is actually finished
            int tint = work >= 100 ? ProgressUtil.statusColor(this, "Completed")
                    : UiTheme.accent(this);
            pbOverallDetail.setProgressTintList(ColorStateList.valueOf(tint));
            tvOverallPercent.setTextColor(tint);
        }
    }

    private void bindLastReport(List<DailyLog> logs) {
        if (logs.isEmpty()) {
            tvLastReport.setText(R.string.no_daily_logs_yet);
            return;
        }
        DailyLog newest = logs.get(0);
        String by = newest.engineerName == null ? ""
                : getString(R.string.by_engineer, newest.engineerName);
        int n = logs.size();
        String count = getResources().getQuantityString(R.plurals.log_count, n, n);
        tvLastReport.setText(getString(R.string.last_log_line,
                ProgressUtil.formatDateTime(newest.loggedAt), by, count));
    }
}
