package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class LogsViewActivity extends AppCompatActivity {

    private String projectId, projectName;
    private RecyclerView rvLogs;
    private TextView tvEmptyLogs, tvTitle;
    private final List<DailyLog> logList = new ArrayList<>();
    private LogHistoryAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_history);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("Engineer Daily Logs");

        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        if (tvSubtitle != null && projectName != null) tvSubtitle.setText(projectName);

        tvEmptyLogs = findViewById(R.id.tvEmpty);
        rvLogs = findViewById(R.id.rvHistory);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LogHistoryAdapter(logList);
        rvLogs.setAdapter(adapter);

        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (projectId != null) {
            loadEngineerLogs(projectId);
        }
    }

    private void loadEngineerLogs(String pId) {
        if (tvEmptyLogs != null) {
            tvEmptyLogs.setVisibility(View.VISIBLE);
            tvEmptyLogs.setText("Loading engineer logs...");
        }

        db.collection("dailyLogs")
                .whereEqualTo("projectId", pId)
                .orderBy("loggedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        if (tvEmptyLogs != null) tvEmptyLogs.setText("Error: " + error.getMessage());
                        return;
                    }

                    logList.clear();
                    if (snapshots == null || snapshots.isEmpty()) {
                        if (tvEmptyLogs != null) {
                            tvEmptyLogs.setVisibility(View.VISIBLE);
                            tvEmptyLogs.setText("No daily logs recorded yet for this project.");
                        }
                    } else {
                        if (tvEmptyLogs != null) tvEmptyLogs.setVisibility(View.GONE);
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            DailyLog log = doc.toObject(DailyLog.class);
                            if (log != null) {
                                log.id = doc.getId();
                                logList.add(log);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}