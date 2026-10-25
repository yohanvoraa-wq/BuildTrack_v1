package com.example.buildtrack_v1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ManagerHomeActivity extends AppCompatActivity {

    private TextView tvEmpty;
    private RecyclerView rvProjects;
    private ManagerProjectAdapter adapter;

    private final DailyLogRepository repo = new DailyLogRepository();
    private FirebaseUser user;
    private ListenerRegistration projectsListener;
    private ListenerRegistration statsListener;

    private final Map<String, String> peopleNames = new HashMap<>();

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_home);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(ManagerHomeActivity.this, "To Log out, Press the Menu button at the top", Toast.LENGTH_LONG).show();
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { goToLogin(); return; }

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnMenu = findViewById(R.id.btnMenu);
        Button btnNewProject = findViewById(R.id.btnNewProject);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvProjects = findViewById(R.id.rvProjects);

        // the name travels here inside the Intent that MainActivity started this screen with
        String name = getIntent().getStringExtra("name");
        if (name == null || name.isEmpty()) name = user.getEmail();
        tvWelcome.setText(getString(R.string.welcome_user, name));

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(ManagerHomeActivity.this, btnMenu);
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

        btnNewProject.setOnClickListener(v -> showNewProjectDialog());

        adapter = new ManagerProjectAdapter(new ManagerProjectAdapter.Listener() {
            @Override
            public void onManageEngineers(DailyLogRepository.ProjectInfo project) {
                showAssignDialog(project, "engineer");
            }

            @Override
            public void onManageBuyers(DailyLogRepository.ProjectInfo project) {
                showAssignDialog(project, "buyer");
            }

            @Override
            public void onViewLogs(DailyLogRepository.ProjectInfo project) {
                Intent i = new Intent(ManagerHomeActivity.this, ManagerLogsActivity.class);
                i.putExtra("projectId", project.id);
                i.putExtra("projectName", project.name);
                i.putExtra("viewer", "manager");
                startActivity(i);
            }

            @Override
            public void onChat(DailyLogRepository.ProjectInfo project) {
                Intent i = new Intent(ManagerHomeActivity.this, ChatActivity.class);
                i.putExtra("projectId", project.id);
                i.putExtra("projectName", project.name);
                i.putExtra("role", "manager");
                startActivity(i);
            }

            @Override
            public void onOpenSiteHub(DailyLogRepository.ProjectInfo project) {
                Intent i = new Intent(ManagerHomeActivity.this, ProjectDetailActivity.class);
                i.putExtra("projectId", project.id);
                i.putExtra("projectName", project.name);
                startActivity(i);
            }
        });
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);
        ContactNumberSetup.askIfMissing(this, repo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (user == null) return;

        projectsListener = repo.observeManagerProjects(user.getUid(),
                new DailyLogRepository.Callback<List<DailyLogRepository.ProjectInfo>>() {
                    @Override
                    public void onSuccess(List<DailyLogRepository.ProjectInfo> projects) {
                        adapter.setProjects(projects);
                        if (projects.isEmpty()) {
                            tvEmpty.setText(R.string.no_projects_yet_tap_new);
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        tvEmpty.setText(getString(R.string.error_loading_projects,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });

        // live progress numbers worked out from the engineers' logs
        statsListener = repo.observeProjectStats(
                new DailyLogRepository.Callback<Map<String, DailyLogRepository.ProjectStats>>() {
                    @Override
                    public void onSuccess(Map<String, DailyLogRepository.ProjectStats> stats) {
                        adapter.setStats(stats);
                    }

                    @Override
                    public void onError(Exception e) { /* cards just show "No logs yet" */ }
                });

        // names of engineers and buyers for the cards
        loadNames("engineer");
        loadNames("buyer");
    }

    private void loadNames(String role) {
        repo.loadUsersByRole(role, new DailyLogRepository.Callback<List<DailyLogRepository.Engineer>>() {
            @Override
            public void onSuccess(List<DailyLogRepository.Engineer> people) {
                for (DailyLogRepository.Engineer p : people) peopleNames.put(p.uid, p.name);
                adapter.setPeopleNames(new HashMap<>(peopleNames));
            }

            @Override
            public void onError(Exception e) { /* cards fall back to a count */ }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (projectsListener != null) {
            projectsListener.remove();
            projectsListener = null;
        }
        if (statsListener != null) {
            statsListener.remove();
            statsListener = null;
        }
    }

    private void pickDate(boolean endOfDay, Consumer<Date> onPicked) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, day) -> {
            Calendar c = Calendar.getInstance();
            if (endOfDay) c.set(y, m, day, 23, 59, 59);
            else c.set(y, m, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            onPicked.accept(c.getTime());
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showNewProjectDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        EditText et = new EditText(this);
        et.setHint("Site / project name");
        et.setSingleLine(true);
        box.addView(et);

        EditText etPhone = new EditText(this);
        etPhone.setHint("On-site helpline number (optional)");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setSingleLine(true);
        box.addView(etPhone);

        EditText etLocation = new EditText(this);
        etLocation.setHint("Site location (optional)");
        etLocation.setSingleLine(true);
        box.addView(etLocation);

        Button btnStart = new Button(this);
        btnStart.setText(R.string.start_date_optional);
        btnStart.setAllCaps(false);
        box.addView(btnStart);

        Button btnEnd = new Button(this);
        btnEnd.setText(R.string.end_date_optional);
        btnEnd.setAllCaps(false);
        box.addView(btnEnd);

        final Date[] start = new Date[1];
        final Date[] end = new Date[1];

        btnStart.setOnClickListener(v -> pickDate(false, d -> {
            start[0] = d;
            btnStart.setText(getString(R.string.start_date_selected, ProgressUtil.formatDate(d)));
        }));
        btnEnd.setOnClickListener(v -> pickDate(true, d -> {
            end[0] = d;
            btnEnd.setText(getString(R.string.end_date_selected, ProgressUtil.formatDate(d)));
        }));

        new AlertDialog.Builder(this)
                .setTitle("New project")
                .setView(box)
                .setPositiveButton("Create", (d, which) -> {
                    String projectName = et.getText().toString().trim();
                    if (projectName.isEmpty()) {
                        Toast.makeText(this, "Enter a project name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (start[0] != null && end[0] != null && end[0].before(start[0])) {
                        Toast.makeText(this, "End date must be after the start date", Toast.LENGTH_LONG).show();
                        return;
                    }
                    repo.createProject(projectName, user.getUid(), start[0], end[0],
                            etPhone.getText().toString(), etLocation.getText().toString(),
                            new DailyLogRepository.Callback<Void>() {
                                @Override public void onSuccess(Void r) {
                                    Toast.makeText(ManagerHomeActivity.this, "Project created", Toast.LENGTH_SHORT).show();
                                }

                                @Override public void onError(Exception e) {
                                    Toast.makeText(ManagerHomeActivity.this,
                                            "Could not create: " + (e != null ? e.getMessage() : "unknown error"),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Tick = person is on the project (add), untick = removed from the project (remove).
    private void showAssignDialog(DailyLogRepository.ProjectInfo project, String role) {
        boolean forEngineers = role.equals("engineer");
        String word = forEngineers ? "engineers" : "buyers";
        List<String> current = forEngineers ? project.assignedEngineers : project.assignedBuyers;

        repo.loadUsersByRole(role, new DailyLogRepository.Callback<List<DailyLogRepository.Engineer>>() {
            @Override
            public void onSuccess(List<DailyLogRepository.Engineer> people) {
                if (people.isEmpty()) {
                    Toast.makeText(ManagerHomeActivity.this,
                            "No " + word + " have registered yet", Toast.LENGTH_LONG).show();
                    return;
                }

                String[] labels = new String[people.size()];
                boolean[] checked = new boolean[people.size()];
                for (int i = 0; i < people.size(); i++) {
                    DailyLogRepository.Engineer en = people.get(i);
                    labels[i] = en.email.isEmpty() ? en.name : en.name + " (" + en.email + ")";
                    checked[i] = current.contains(en.uid);
                }

                new AlertDialog.Builder(ManagerHomeActivity.this)
                        .setTitle("Add or remove " + word + ": " + project.name)
                        .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton("Save", (d, which) -> {
                            List<String> chosen = new ArrayList<>();
                            for (int i = 0; i < people.size(); i++) {
                                if (checked[i]) chosen.add(people.get(i).uid);
                            }
                            DailyLogRepository.Callback<Void> done = new DailyLogRepository.Callback<Void>() {
                                @Override public void onSuccess(Void r) {
                                    Toast.makeText(ManagerHomeActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                }

                                @Override public void onError(Exception e) {
                                    Toast.makeText(ManagerHomeActivity.this,
                                            "Could not save: " + (e != null ? e.getMessage() : "unknown error"),
                                            Toast.LENGTH_LONG).show();
                                }
                            };
                            if (forEngineers) repo.setAssignedEngineers(project.id, chosen, done);
                            else repo.setAssignedBuyers(project.id, chosen, done);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ManagerHomeActivity.this,
                        "Could not load " + word + ": " + (e != null ? e.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}