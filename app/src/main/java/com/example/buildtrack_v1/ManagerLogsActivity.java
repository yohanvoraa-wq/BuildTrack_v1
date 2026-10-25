package com.example.buildtrack_v1;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detailed progress view for one site: overview, helpline, and every engineer's logs.
 * Used by the manager (viewer = "manager") and the buyer (viewer = "buyer", read-only).
 */
public class ManagerLogsActivity extends AppCompatActivity {

    private final DailyLogRepository repo = new DailyLogRepository();

    private TextView tvEmpty, tvSumStatus, tvSumWorkPct, tvSumSchedPct, tvSumDates,
            tvSumEngineers, tvSumStats, tvHelpline;
    private Button btnCall, btnSms, btnEditHelpline;
    private LinearProgressIndicator pbSumWork, pbSumSched;
    private View sumSchedRow;
    private LinearLayout logsContainer;

    private String projectId;
    private boolean isManager;
    private String helpline;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_progress);

        projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");
        isManager = !"buyer".equals(getIntent().getStringExtra("viewer"));

        ((TextView) findViewById(R.id.tvTitle)).setText(projectName != null ? projectName : "Project");
        ((Button) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);
        tvSumStatus = findViewById(R.id.tvSumStatus);
        tvSumWorkPct = findViewById(R.id.tvSumWorkPct);
        tvSumSchedPct = findViewById(R.id.tvSumSchedPct);
        tvSumDates = findViewById(R.id.tvSumDates);
        tvSumEngineers = findViewById(R.id.tvSumEngineers);
        tvSumStats = findViewById(R.id.tvSumStats);
        tvHelpline = findViewById(R.id.tvHelpline);
        btnCall = findViewById(R.id.btnCall);
        btnSms = findViewById(R.id.btnSms);
        btnEditHelpline = findViewById(R.id.btnEditHelpline);
        pbSumWork = findViewById(R.id.pbSumWork);
        pbSumSched = findViewById(R.id.pbSumSched);
        sumSchedRow = findViewById(R.id.sumSchedRow);
        logsContainer = findViewById(R.id.logsContainer);

        // only the manager can change the helpline number
        btnEditHelpline.setVisibility(isManager ? View.VISIBLE : View.GONE);
        btnEditHelpline.setOnClickListener(v -> showEditHelplineDialog());
        btnCall.setOnClickListener(v -> startNumberIntent(Intent.ACTION_DIAL, "tel:"));
        btnSms.setOnClickListener(v -> startNumberIntent(Intent.ACTION_SENDTO, "smsto:"));

        if (projectId == null) {
            tvEmpty.setText(R.string.no_project_selected);
            return;
        }

        loadProject();
    }

    // ------------------------------------------------------------------ helpline

    private void bindHelpline() {
        boolean has = helpline != null && !helpline.trim().isEmpty();
        tvHelpline.setText(has ? helpline : "Not set yet");
        btnCall.setEnabled(has);
        btnSms.setEnabled(has);
    }

    private void startNumberIntent(String action, String scheme) {
        if (helpline == null || helpline.trim().isEmpty()) return;
        try {
            startActivity(new Intent(action, Uri.parse(scheme + helpline.replace(" ", ""))));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to do this on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void showEditHelplineDialog() {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_PHONE);
        et.setHint("Phone number of the on-site engineer");
        et.setSingleLine(true);
        if (helpline != null) et.setText(helpline);

        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Site helpline")
                .setView(box)
                .setPositiveButton("Save", (d, which) -> {
                    String number = et.getText().toString().trim();
                    repo.setHelpline(projectId, number, new DailyLogRepository.Callback<Void>() {
                        @Override public void onSuccess(Void r) {
                            helpline = number;
                            bindHelpline();
                            Toast.makeText(ManagerLogsActivity.this, "Helpline saved", Toast.LENGTH_SHORT).show();
                        }

                        @Override public void onError(Exception e) {
                            Toast.makeText(ManagerLogsActivity.this,
                                    "Could not save: " + (e != null ? e.getMessage() : "unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------ loading

    private void loadProject() {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo project) {
                helpline = project.helplineNumber;
                bindHelpline();
                loadEngineers(project);
            }

            @Override public void onError(Exception e) {
                tvEmpty.setText(getString(R.string.error_loading_project,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
            }
        });
    }

    private void loadEngineers(DailyLogRepository.ProjectInfo project) {
        repo.loadEngineers(new DailyLogRepository.Callback<List<DailyLogRepository.Engineer>>() {
            @Override public void onSuccess(List<DailyLogRepository.Engineer> engineers) {
                Map<String, String> names = new HashMap<>();
                for (DailyLogRepository.Engineer en : engineers) names.put(en.uid, en.name);
                loadLogs(project, names);
            }

            @Override public void onError(Exception e) {
                loadLogs(project, new HashMap<>()); // names are optional
            }
        });
    }

    private void loadLogs(DailyLogRepository.ProjectInfo project, Map<String, String> names) {
        repo.getAllLogsForProject(project.id, new DailyLogRepository.Callback<List<DailyLog>>() {
            @Override public void onSuccess(List<DailyLog> logs) {
                bindSummary(project, names, logs);
                bindLogs(logs);
            }

            @Override public void onError(Exception e) {
                bindSummary(project, names, new ArrayList<>());
                tvEmpty.setText(getString(R.string.error_loading_logs,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
            }
        });
    }

    private void bindSummary(DailyLogRepository.ProjectInfo project, Map<String, String> names,
                             List<DailyLog> logs) {
        Integer work = project.overallPercent;
        Integer sched = ProgressUtil.schedulePercent(project.startDate, project.endDate);

        if (work == null) {
            tvSumWorkPct.setText(R.string.no_stages_reported);
            pbSumWork.setProgress(0);
        } else {
            tvSumWorkPct.setText(getString(R.string.percent_value, work));
            pbSumWork.setProgress(work);
        }

        if (sched == null) {
            sumSchedRow.setVisibility(View.GONE);
        } else {
            sumSchedRow.setVisibility(View.VISIBLE);
            tvSumSchedPct.setText(getString(R.string.percent_time_used, sched));
            pbSumSched.setProgress(sched);
            tvSumDates.setText(getString(R.string.date_range_to,
                    ProgressUtil.formatDate(project.startDate), ProgressUtil.formatDate(project.endDate)));
        }

        String status = ProgressUtil.status(work, sched);
        ProgressUtil.styleStatus(tvSumStatus, status, ProgressUtil.statusColor(this, status));

        if (project.assignedEngineers.isEmpty()) {
            tvSumEngineers.setText(R.string.engineers_none_assigned);
        } else {
            List<String> list = new ArrayList<>();
            for (String uid : project.assignedEngineers) {
                String n = names.get(uid);
                if (n != null) list.add(n);
            }
            tvSumEngineers.setText(list.isEmpty()
                    ? getResources().getQuantityString(R.plurals.engineer_assigned_count,
                            project.assignedEngineers.size(), project.assignedEngineers.size())
                    : getString(R.string.engineers_named, android.text.TextUtils.join(", ", list)));
        }

        if (logs.isEmpty()) {
            tvSumStats.setText(R.string.no_logs_submitted_yet);
        } else {
            DailyLog newest = logs.get(0);
            tvSumStats.setText(newest.engineerName != null
                    ? getString(R.string.logs_summary_by,
                            logCount(logs.size()), ProgressUtil.formatDateTime(newest.loggedAt), newest.engineerName)
                    : getString(R.string.logs_summary,
                            logCount(logs.size()), ProgressUtil.formatDateTime(newest.loggedAt)));
        }
    }

    private String logCount(int size) {
        return getResources().getQuantityString(R.plurals.log_count, size, size);
    }

    private void bindLogs(List<DailyLog> logs) {
        logsContainer.removeAllViews();
        if (logs.isEmpty()) {
            tvEmpty.setText(R.string.no_logs_on_this_project);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (DailyLog l : logs) {
            View card = inflater.inflate(R.layout.item_manager_log, logsContainer, false);

            String when = l.date != null ? l.date : ProgressUtil.formatDate(l.loggedAt);
            if (l.time != null && !l.time.isEmpty()) when += " • " + l.time;
            ((TextView) card.findViewById(R.id.tvLogWhen)).setText(when);

            ((TextView) card.findViewById(R.id.tvLogEngineer)).setText(
                    l.engineerName != null ? l.engineerName : "Engineer");

            TextView tvProg = card.findViewById(R.id.tvLogProgress);
            if (l.progressPercent != null) {
                ProgressUtil.styleChip(tvProg, getString(R.string.percent_complete, l.progressPercent),
                        ProgressUtil.statusColor(this, "On track"));
            } else {
                tvProg.setVisibility(View.GONE);
            }

            setLine((TextView) card.findViewById(R.id.tvLogWeather), R.string.weather_line,
                    l.weatherForDisplay());
            setLine((TextView) card.findViewById(R.id.tvLogWork), R.string.work_line, l.workDescription);
            setLine((TextView) card.findViewById(R.id.tvLogRemarks), R.string.remarks_line, l.remarks);

            // materials
            List<String> mats = new ArrayList<>();
            if (l.materials != null) {
                for (DailyLog.MaterialEntry m : l.materials) {
                    if (m == null) continue;
                    String qty = trimNumber(m.quantity) + (m.unit != null ? " " + m.unit : "");
                    boolean named = m.material != null && !m.material.trim().isEmpty();
                    mats.add(named ? getString(R.string.material_item, m.material.trim(), qty) : qty);
                }
            }
            setLine((TextView) card.findViewById(R.id.tvLogMaterials), R.string.materials_line,
                    mats.isEmpty() ? null : android.text.TextUtils.join(", ", mats));

            // labour
            List<String> lab = new ArrayList<>();
            if (l.labour != null) {
                for (DailyLog.LabourEntry le : l.labour) {
                    if (le == null) continue;
                    boolean named = le.trade != null && !le.trade.trim().isEmpty();
                    lab.add(named ? getString(R.string.labour_item, le.trade.trim(), le.count)
                            : getResources().getQuantityString(R.plurals.worker_count, le.count, le.count));
                }
            }
            setLine((TextView) card.findViewById(R.id.tvLogLabour), R.string.labour_line,
                    lab.isEmpty() ? null : android.text.TextUtils.join(", ", lab));

            addPhotos(card, l);
            logsContainer.addView(card);
        }
    }

    private void setLine(TextView tv, int labelRes, String value) {
        if (value == null || value.trim().isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(getString(labelRes, value.trim()));
        }
    }

    private String trimNumber(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private void addPhotos(View card, DailyLog l) {
        View hs = card.findViewById(R.id.hsPhotos);
        LinearLayout row = card.findViewById(R.id.photoRow);
        row.removeAllViews();
        int added = 0;
        if (l.photoUrls != null) {
            for (int i = 0; i < l.photoUrls.size() && added < 6; i++) {
                String s = l.photoUrls.get(i);
                if (s == null || s.isEmpty()) continue;
                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(84), dp(84));
                lp.setMarginEnd(dp(8));
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ImageHelper.showPhoto(iv, s);
                iv.setOnClickListener(v -> PhotoViewerActivity.open(this, s));
                row.addView(iv);
                added++;
            }
        }
        hs.setVisibility(added == 0 ? View.GONE : View.VISIBLE);
    }

    private void showFullPhoto(String stored) {
        ImageView iv = new ImageView(this);
        iv.setAdjustViewBounds(true);
        new AlertDialog.Builder(this)
                .setView(iv)
                .setPositiveButton("Close", null)
                .show();
        ImageHelper.showPhoto(iv, stored);
    }
}