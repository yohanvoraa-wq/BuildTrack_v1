package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class LogHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmpty, tvSubtitle;
    private LogHistoryAdapter adapter;
    private final List<DailyLog> logList = new ArrayList<>();
    private final DailyLogRepository repo = new DailyLogRepository();
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_history);

        String projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        tvSubtitle = findViewById(R.id.tvSubtitle);
        if (projectName != null && tvSubtitle != null) {
            tvSubtitle.setText(projectName);
        }

        tvEmpty = findViewById(R.id.tvEmpty);
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LogHistoryAdapter(logList);
        rvHistory.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (projectId != null) {
            loadLogs(projectId);
        } else {
            Toast.makeText(this, "No project specified", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLogs(String projectId) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(R.string.loading_site_logs);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvEmpty.setText(R.string.signed_out_see_logs);
            return;
        }

        listener = repo.observeLogsForEngineer(projectId, user.getUid(),
                new DailyLogRepository.Callback<List<DailyLog>>() {
                    @Override
                    public void onSuccess(List<DailyLog> logs) {
                        logList.clear();
                        logList.addAll(logs);
                        adapter.notifyDataSetChanged();

                        if (logs.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText(R.string.no_logs_posted_here);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        tvEmpty.setText(getString(R.string.error_prefix,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}