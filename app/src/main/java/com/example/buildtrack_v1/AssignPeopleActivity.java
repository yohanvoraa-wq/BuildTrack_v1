package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds people to one project. Assignments are stored as account uids in assignedEngineers /
 * assignedBuyers, which is the only form the engineer and buyer apps read.
 */
public class AssignPeopleActivity extends AppCompatActivity {

    private String projectId, projectName;
    private EditText etEngineerEmail, etBuyerEmail, etUnitNumber;
    private TextView tvProjectTitle;
    private Button btnSave;

    private final DailyLogRepository repo = new DailyLogRepository();
    private DailyLogRepository.ProjectInfo project;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_people);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        etEngineerEmail = findViewById(R.id.etEngineerEmail);
        etBuyerEmail = findViewById(R.id.etBuyerEmail);
        etUnitNumber = findViewById(R.id.etUnit);
        btnSave = findViewById(R.id.btnSaveAssignment);

        if (projectName != null) tvProjectTitle.setText(projectName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        if (projectId == null) {
            Toast.makeText(this, "No project specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProject();
    }

    private void loadProject() {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo p) {
                project = p;
                showTeam(p);
            }

            @Override public void onError(Exception e) {
                Toast.makeText(AssignPeopleActivity.this,
                        "Could not load the project: " + (e != null ? e.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showTeam(DailyLogRepository.ProjectInfo p) {
        renderTeam(findViewById(R.id.llEngineerTeam), p.assignedEngineers, false, null);
        renderTeam(findViewById(R.id.llBuyerTeam), p.assignedBuyers, true, p.buyerUnits);
    }

    /** Name and contact number for everyone on one side of the project. */
    private void renderTeam(LinearLayout container, List<String> uids, boolean allowMessage,
                            java.util.Map<String, List<String>> flats) {
        container.removeAllViews();
        if (uids == null || uids.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(R.string.nobody_assigned);
            none.setTextSize(13f);
            none.setTextColor(UiTheme.mutedText(this));
            container.addView(none);
            return;
        }

        repo.loadProfiles(uids, new DailyLogRepository.Callback<List<DailyLogRepository.Engineer>>() {
            @Override public void onSuccess(List<DailyLogRepository.Engineer> people) {
                for (DailyLogRepository.Engineer m : people) {
                    addRow(container, m, allowMessage, flats != null ? flats.get(m.uid) : null);
                }
            }

            @Override public void onError(Exception e) {
                for (String uid : uids) {
                    TextView fallback = new TextView(AssignPeopleActivity.this);
                    fallback.setText(uid);
                    fallback.setTextSize(13f);
                    container.addView(fallback);
                }
            }
        });
    }

    private void addRow(LinearLayout container, final DailyLogRepository.Engineer member,
                        boolean allowMessage, List<String> flats) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_team_member, container, false);
        ((TextView) row.findViewById(R.id.tvMemberName)).setText(member.name);
        if (flats != null && !flats.isEmpty()) {
            // Tap the name to remove a flat that was entered by mistake.
            TextView nameView = row.findViewById(R.id.tvMemberName);
            nameView.setText(member.name + " · Flat " + android.text.TextUtils.join(", ", flats));
            nameView.setOnClickListener(v -> askRemoveFlat(member, flats));
        }

        TextView number = row.findViewById(R.id.tvMemberNumber);
        Button call = row.findViewById(R.id.btnMemberCall);
        Button message = row.findViewById(R.id.btnMemberMessage);

        if (MobileNumber.isCallable(member.phone)) {
            number.setText(MobileNumber.format(MobileNumber.normalize(member.phone)));
            call.setOnClickListener(v -> MobileNumber.dial(this, member.phone));
            if (allowMessage) {
                message.setVisibility(View.VISIBLE);
                message.setOnClickListener(v -> MobileNumber.message(this, member.phone));
            }
        } else {
            number.setText(R.string.no_number_on_file);
            call.setEnabled(false);
            call.setAlpha(0.38f);
            if (allowMessage) message.setVisibility(View.GONE);
        }
        container.addView(row);
    }

    private void save() {
        String eng = etEngineerEmail.getText().toString().trim();
        String buyer = etBuyerEmail.getText().toString().trim();
        String unit = etUnitNumber.getText().toString().trim();

        if (eng.isEmpty() && buyer.isEmpty() && unit.isEmpty()) {
            Toast.makeText(this, "Add an engineer, a buyer or a unit number first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setBusy(true);
        addEngineer(eng, () -> addBuyer(buyer, unit, () -> saveUnit(buyer.isEmpty() ? unit : "", () -> {
            setBusy(false);
            Toast.makeText(this, "Team updated", Toast.LENGTH_SHORT).show();
            loadProject();
        })));
    }

    private void addEngineer(String input, Runnable done) {
        if (input.isEmpty()) { done.run(); return; }
        repo.findUser(input, "engineer", new DailyLogRepository.Callback<DailyLogRepository.Engineer>() {
            @Override public void onSuccess(DailyLogRepository.Engineer e) {
                List<String> next = withCurrent(project == null ? null : project.assignedEngineers, e.uid);
                repo.setAssignedEngineers(projectId, next, new DailyLogRepository.Callback<Void>() {
                    @Override public void onSuccess(Void r) {
                        etEngineerEmail.setText("");
                        if (project != null) project = copyWith(next, project.assignedBuyers);
                        done.run();
                    }

                    @Override public void onError(Exception err) { fail(err); }
                });
            }

            @Override public void onError(Exception e) { fail(e); }
        });
    }

    private void addBuyer(String input, String unit, Runnable done) {
        if (input.isEmpty()) { done.run(); return; }
        repo.findUser(input, "buyer", new DailyLogRepository.Callback<DailyLogRepository.Engineer>() {
            @Override public void onSuccess(DailyLogRepository.Engineer b) {
                List<String> next = withCurrent(project == null ? null : project.assignedBuyers, b.uid);
                repo.setAssignedBuyers(projectId, next, new DailyLogRepository.Callback<Void>() {
                    @Override public void onSuccess(Void r) {
                        etBuyerEmail.setText("");
                        if (project != null) project = copyWith(project.assignedEngineers, next);
                        if (unit.isEmpty()) { done.run(); return; }
                        repo.addBuyerUnit(projectId, b.uid, unit, new DailyLogRepository.Callback<Void>() {
                            @Override public void onSuccess(Void r2) {
                                etUnitNumber.setText("");
                                done.run();
                            }

                            @Override public void onError(Exception err) { fail(err); }
                        });
                    }

                    @Override public void onError(Exception err) { fail(err); }
                });
            }

            @Override public void onError(Exception e) { fail(e); }
        });
    }

    private void askRemoveFlat(DailyLogRepository.Engineer member, List<String> flats) {
        final String[] items = flats.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remove a flat from " + member.name)
                .setItems(items, (d, which) ->
                        repo.removeBuyerUnit(projectId, member.uid, items[which],
                                new DailyLogRepository.Callback<Void>() {
                                    @Override public void onSuccess(Void r) { loadProject(); }

                                    @Override public void onError(Exception e) { fail(e); }
                                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveUnit(String unit, Runnable done) {
        if (unit.isEmpty()) { done.run(); return; }
        repo.setUnitNumber(projectId, unit, new DailyLogRepository.Callback<Void>() {
            @Override public void onSuccess(Void r) { done.run(); }

            @Override public void onError(Exception e) { fail(e); }
        });
    }

    private void fail(Exception e) {
        setBusy(false);
        Toast.makeText(this, "Could not save: " + (e != null ? e.getMessage() : "unknown error"),
                Toast.LENGTH_LONG).show();
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? "Saving..." : "Save Team Assignment");
    }

    private static List<String> withCurrent(List<String> current, String uid) {
        List<String> out = new ArrayList<>();
        if (current != null) out.addAll(current);
        if (!out.contains(uid)) out.add(uid);
        return out;
    }

    /** Local copy so the on-screen team list updates before Firestore echoes back. */
    private DailyLogRepository.ProjectInfo copyWith(List<String> engineers, List<String> buyers) {
        return new DailyLogRepository.ProjectInfo(project.id, project.name, engineers, buyers,
                project.startDate, project.endDate, project.helplineNumber, project.location,
                project.siteNote, project.unitNumber);
    }
}
