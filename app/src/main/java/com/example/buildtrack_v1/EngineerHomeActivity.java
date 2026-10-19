package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class EngineerHomeActivity extends AppCompatActivity {

    private TextView tvEmpty;
    private RecyclerView rvProjects;
    private ProjectAdapter adapter;

    private final DailyLogRepository repo = new DailyLogRepository();
    private FirebaseUser user;
    private String engineerName;
    private ListenerRegistration projectsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engineer_home);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(EngineerHomeActivity.this, "To Log out, Press the Menu button at the top", Toast.LENGTH_LONG).show();
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { goToLogin(); return; }

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnMenu = findViewById(R.id.btnMenu);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvProjects = findViewById(R.id.rvProjects);

        // the name travels here inside the Intent that MainActivity started this screen with
        engineerName = getIntent().getStringExtra("name");
        if (engineerName == null || engineerName.isEmpty()) engineerName = user.getEmail();
        tvWelcome.setText("Welcome, " + engineerName);

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(EngineerHomeActivity.this, btnMenu);
                menu.getMenuInflater().inflate(R.menu.menu_home, menu.getMenu());

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menuLogout) {
                            FirebaseAuth.getInstance().signOut();
                            goToLogin();
                        }
                        return true;
                    }
                });

                menu.show();
            }
        });

        adapter = new ProjectAdapter(new ProjectAdapter.Listener() {
            @Override
            public void onLog(DailyLogRepository.AssignedProject p) {
                open(DailyLogEntryActivity.class, p);
            }

            @Override
            public void onHistory(DailyLogRepository.AssignedProject p) {
                open(LogHistoryActivity.class, p);
            }
        });
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (user == null) return;

        // Live: a project the manager assigns appears here without reopening the app
        projectsListener = repo.observeAssignedProjects(user.getUid(),
                new DailyLogRepository.Callback<List<DailyLogRepository.AssignedProject>>() {
                    @Override
                    public void onSuccess(List<DailyLogRepository.AssignedProject> projects) {
                        adapter.setProjects(projects);
                        if (projects.isEmpty()) {
                            tvEmpty.setText("No projects assigned yet. Your manager will assign you to one.");
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            refreshTodayCounts();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        tvEmpty.setText("Could not load projects: "
                                + (e != null ? e.getMessage() : "unknown error"));
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) refreshTodayCounts(); // update counts after coming back from a log entry
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (projectsListener != null) {
            projectsListener.remove();
            projectsListener = null;
        }
    }

    private void refreshTodayCounts() {
        for (DailyLogRepository.AssignedProject p : adapter.getProjects()) {
            repo.countTodayEntries(p.id, user.getUid(), new DailyLogRepository.Callback<Integer>() {
                @Override
                public void onSuccess(Integer n) {
                    adapter.setTodayCount(p.id, n);
                }

                @Override
                public void onError(Exception e) { /* leave "Checking..." */ }
            });
        }
    }

    private void open(Class<?> target, DailyLogRepository.AssignedProject p) {
        Intent i = new Intent(this, target);
        i.putExtra("projectId", p.id);
        i.putExtra("projectName", p.name);
        i.putExtra("engineerName", engineerName);
        startActivity(i);
    }

    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}