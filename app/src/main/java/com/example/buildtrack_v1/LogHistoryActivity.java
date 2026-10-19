package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class LogHistoryActivity extends AppCompatActivity {

    private RecyclerView rvLogs;
    private TextView tvEmpty;
    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_history);

        String projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");
        ((TextView) findViewById(R.id.tvSubtitle)).setText(projectName != null ? projectName : "");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvLogs = findViewById(R.id.rvLogs);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || projectId == null) {
            tvEmpty.setText("No project assigned");
            return;
        }

        repo.getLogs(projectId, user.getUid(), new DailyLogRepository.Callback<List<DailyLog>>() {
            @Override public void onSuccess(List<DailyLog> logs) {
                if (logs.isEmpty()) {
                    tvEmpty.setText("No logs submitted yet");
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvLogs.setAdapter(new LogHistoryAdapter(logs));
                }
            }

            @Override public void onError(Exception e) {
                // First run usually fails here: Logcat shows a link to create the Firestore index
                tvEmpty.setText("Could not load logs: " + e.getMessage());
            }
        });
    }
}
