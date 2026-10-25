package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class EngineerHomeActivity extends AppCompatActivity {

    private RecyclerView rvProjects;
    private TextView tvEmpty, tvWelcome, tvRole, tvEngineerIdBadge;
    private ProjectAdapter adapter;
    private final List<ProjectAdapter.ProjectItem> projectList = new ArrayList<>();
    private ListenerRegistration projectsListener;
    private final DailyLogRepository repo = new DailyLogRepository();

    private String myEngineerId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engineer_home);

        tvRole = findViewById(R.id.tvRole);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvEngineerIdBadge = findViewById(R.id.tvEngineerIdBadge);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvProjects = findViewById(R.id.rvProjects);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Derived from the account id, so a manager can assign work with this code before the
        // engineer ever opens the app.
        myEngineerId = EngineerIdentity.fromUid(user.getUid());

        if (tvRole != null) {
            tvRole.setText(R.string.on_site_engineer);
        }
        String name = getIntent().getStringExtra("name");
        String displayName = (name != null && !name.isEmpty()) ? name
                : (user.getEmail() != null ? user.getEmail() : getString(R.string.engineer_word));
        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.welcome_user, displayName));
        }

        if (tvEngineerIdBadge != null) {
            tvEngineerIdBadge.setText(getString(R.string.engineer_id_badge, myEngineerId));
            tvEngineerIdBadge.setOnClickListener(v ->
                    Toast.makeText(this, getString(R.string.your_id_is, myEngineerId), Toast.LENGTH_SHORT).show()
            );
        }

        if (tvEmpty != null) {
            tvEmpty.setOnClickListener(v ->
                    Toast.makeText(this, "Waiting for the manager to assign a site...",
                            Toast.LENGTH_SHORT).show());
        }

        adapter = new ProjectAdapter(projectList, displayName);
        rvProjects.setAdapter(adapter);

        Button btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent i = new Intent(EngineerHomeActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }

        loadAssignedProjects(user.getUid());
        ContactNumberSetup.askIfMissing(this, repo);
    }

    private void loadAssignedProjects(String uid) {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.loading_assigned_projects);
        }

        // Live listener: the card list refreshes the moment the manager assigns or removes a site.
        projectsListener = repo.observeEngineerProjects(uid, myEngineerId,
                new DailyLogRepository.Callback<List<DailyLogRepository.ProjectInfo>>() {
                    @Override
                    public void onSuccess(List<DailyLogRepository.ProjectInfo> projects) {
                        projectList.clear();
                        for (DailyLogRepository.ProjectInfo p : projects) {
                            projectList.add(new ProjectAdapter.ProjectItem(
                                    p.id,
                                    p.name,
                                    null,
                                    p.location,
                                    p.endDate != null ? ProgressUtil.formatDate(p.endDate) : null,
                                    null,
                                    p.siteNote));
                        }

                        if (projects.isEmpty()) {
                            if (tvEmpty != null) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText(getString(R.string.no_site_assigned_id, myEngineerId));
                            }
                        } else if (tvEmpty != null) {
                            tvEmpty.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText(getString(R.string.error_loading_your_projects,
                                    e != null ? e.getMessage() : getString(R.string.unknown_error)));
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (projectsListener != null) projectsListener.remove();
    }
}
