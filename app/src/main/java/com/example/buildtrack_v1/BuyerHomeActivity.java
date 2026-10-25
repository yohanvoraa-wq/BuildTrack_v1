package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class BuyerHomeActivity extends AppCompatActivity {

    private View cardOwnedUnit;
    private TextView tvEmpty, tvWelcome, tvUnitBadge, tvUnitStatus, tvProjectName, tvLocation, tvProgressPercent;
    private TextView tvSpecLogs, tvSpecLastReport, tvSpecHandover;
    private ProgressBar pbOverall;
    private Button btnViewProgress, btnMenu;
    private View btnViewUpdates, tileDocuments, tileLogs, tileContact;
    private TextView tvTileLogsSub;
    private View cardManagerContact;
    private TextView tvManagerName, tvManagerNumber;
    private Button btnManagerCall, btnManagerMessage;
    private String managerPhone;

    private android.widget.Spinner spUnits;
    private final List<DailyLogRepository.ProjectInfo> myUnits = new ArrayList<>();
    private final List<String> myFlats = new ArrayList<>();   // parallel to myUnits
    private String selectedUnitId;           // projectId|flat
    private String currentProjectId;
    private String currentProjectName;
    private ListenerRegistration buyerListener;
    private ListenerRegistration progressListener;
    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvEmpty = findViewById(R.id.tvEmpty);
        cardOwnedUnit = findViewById(R.id.cardOwnedUnit);
        spUnits = findViewById(R.id.spUnits);
        spUnits.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                if (pos < 0 || pos >= myUnits.size()) return;
                DailyLogRepository.ProjectInfo p = myUnits.get(pos);
                String key = p.id + "|" + myFlats.get(pos);
                if (key.equals(selectedUnitId)) return;
                selectedUnitId = key;
                bindUnit(p, myFlats.get(pos));
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        tvUnitBadge = findViewById(R.id.tvUnitBadge);
        tvUnitStatus = findViewById(R.id.tvUnitStatus);
        tvProjectName = findViewById(R.id.tvProjectName);
        tvLocation = findViewById(R.id.tvLocation);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        pbOverall = findViewById(R.id.pbOverall);

        tvSpecLogs = findViewById(R.id.tvSpecLogs);
        tvSpecLastReport = findViewById(R.id.tvSpecLastReport);
        tvSpecHandover = findViewById(R.id.tvSpecHandover);

        btnViewProgress = findViewById(R.id.btnViewProgress);
        btnViewUpdates = findViewById(R.id.btnViewUpdates);
        tileDocuments = findViewById(R.id.tileDocuments);
        tileLogs = findViewById(R.id.tileLogs);
        tileContact = findViewById(R.id.tileContact);
        tvTileLogsSub = findViewById(R.id.tvTileLogsSub);
        btnMenu = findViewById(R.id.btnMenu);

        cardManagerContact = findViewById(R.id.cardManagerContact);
        tvManagerName = findViewById(R.id.tvManagerName);
        tvManagerNumber = findViewById(R.id.tvManagerNumber);
        btnManagerCall = findViewById(R.id.btnManagerCall);
        btnManagerMessage = findViewById(R.id.btnManagerMessage);
        btnManagerCall.setOnClickListener(v -> MobileNumber.dial(this, managerPhone));
        btnManagerMessage.setOnClickListener(v -> MobileNumber.message(this, managerPhone));

        FirebaseUser buyer = FirebaseAuth.getInstance().getCurrentUser();
        if (buyer == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String buyerName = getIntent().getStringExtra("name");
        if (buyerName == null || buyerName.isEmpty()) {
            buyerName = buyer.getEmail() != null ? buyer.getEmail() : getString(R.string.buyer);
        }
        tvWelcome.setText(getString(R.string.welcome_user, buyerName));

        btnMenu.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(BuyerHomeActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        btnViewProgress.setOnClickListener(v -> {
            if (currentProjectId != null) {
                Intent i = new Intent(this, ProgressActivity.class);
                i.putExtra("projectId", currentProjectId);
                i.putExtra("projectName", currentProjectName);
                startActivity(i);
            }
        });

        btnViewUpdates.setOnClickListener(v -> {
            if (currentProjectId != null) {
                Intent i = new Intent(this, UpdatesFeedActivity.class);
                i.putExtra("projectId", currentProjectId);
                i.putExtra("projectName", currentProjectName);
                startActivity(i);
            }
        });

        tileDocuments.setOnClickListener(v -> {
            if (currentProjectId == null) {
                Toast.makeText(this, "No unit is linked to your account yet", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, DocumentsActivity.class);
            i.putExtra("projectId", currentProjectId);
            i.putExtra("projectName", currentProjectName);
            i.putExtra("role", "buyer");
            startActivity(i);
        });
        tileLogs.setOnClickListener(v ->
                Toast.makeText(this, tvTileLogsSub.getText(), Toast.LENGTH_SHORT).show());
        tileContact.setOnClickListener(v -> {
            if (managerPhone != null) MobileNumber.dial(this, managerPhone);
            else Toast.makeText(this, "Your manager's number is not available yet", Toast.LENGTH_SHORT).show();
        });

        loadBuyerUnit(buyer.getUid(), buyer.getEmail());
        ContactNumberSetup.askIfMissing(this, repo);
    }

    private void loadBuyerUnit(String buyerUid, String buyerEmail) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(R.string.loading_your_property_details);

        buyerListener = repo.observeBuyerProjects(buyerUid, buyerEmail,
                new DailyLogRepository.Callback<List<DailyLogRepository.ProjectInfo>>() {
                    @Override
                    public void onSuccess(List<DailyLogRepository.ProjectInfo> projects) {
                        if (projects.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText(R.string.no_unit_linked);
                            cardOwnedUnit.setVisibility(View.GONE);
                            return;
                        }

                        // One entry per flat: a buyer can own several flats in one project, or
                        // flats in different projects.
                        myUnits.clear();
                        myFlats.clear();
                        for (DailyLogRepository.ProjectInfo p : projects) {
                            List<String> mine = p.buyerUnits.get(buyerUid);
                            if (mine != null && !mine.isEmpty()) {
                                for (String flat : mine) { myUnits.add(p); myFlats.add(flat); }
                            } else {
                                myUnits.add(p);
                                myFlats.add(p.unitNumber != null ? p.unitNumber : "");
                            }
                        }
                        int idx = 0;
                        for (int i = 0; i < myUnits.size(); i++) {
                            if ((myUnits.get(i).id + "|" + myFlats.get(i)).equals(selectedUnitId)) idx = i;
                        }
                        selectedUnitId = myUnits.get(idx).id + "|" + myFlats.get(idx);

                        if (myUnits.size() > 1) {
                            List<String> labels = new ArrayList<>();
                            for (int i = 0; i < myUnits.size(); i++) {
                                String flat = myFlats.get(i);
                                labels.add((flat.isEmpty() ? "" : "Flat " + flat + " · ") + myUnits.get(i).name);
                            }
                            android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<>(
                                    BuyerHomeActivity.this, android.R.layout.simple_spinner_item, labels);
                            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spUnits.setAdapter(ad);
                            spUnits.setSelection(idx, false);
                            spUnits.setVisibility(View.VISIBLE);
                        } else {
                            spUnits.setVisibility(View.GONE);
                        }

                        tvEmpty.setVisibility(View.GONE);
                        cardOwnedUnit.setVisibility(View.VISIBLE);
                        bindUnit(myUnits.get(idx), myFlats.get(idx));
                    }

                    @Override
                    public void onError(Exception e) {
                        tvEmpty.setText(getString(R.string.error_loading_property,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
                    }
                });
    }

    private void bindUnit(DailyLogRepository.ProjectInfo p, String flat) {
                                                currentProjectId = p.id;
                        currentProjectName = p.name;

                        tvProjectName.setText(currentProjectName);
                        if (p.location == null || p.location.isEmpty()) {
                            tvLocation.setVisibility(View.GONE);
                        } else {
                            tvLocation.setVisibility(View.VISIBLE);
                            tvLocation.setText(getString(R.string.site_location, p.location));
                        }

                        tvUnitBadge.setText(flat != null && !flat.isEmpty()
                                ? getString(R.string.unit_badge, flat)
                                : getString(R.string.my_unit));
                        tvSpecHandover.setText(handoverText(p.endDate));

                        bindStageProgress(p, p.startDate, p.endDate);
                        watchSiteActivity(p.id);
                        showManagerContact(p.managerId);
    }

    /** The manager is the buyer's route to a phone call, so their number lives here. */
    private void showManagerContact(String managerId) {
        cardManagerContact.setVisibility(View.GONE);
        managerPhone = null;
        if (managerId == null || managerId.isEmpty()) return;

        List<String> one = new ArrayList<>();
        one.add(managerId);
        repo.loadProfiles(one, new DailyLogRepository.Callback<List<DailyLogRepository.Engineer>>() {
            @Override public void onSuccess(List<DailyLogRepository.Engineer> people) {
                if (people.isEmpty()) return;
                DailyLogRepository.Engineer manager = people.get(0);
                tvManagerName.setText(manager.name);
                if (MobileNumber.isCallable(manager.phone)) {
                    managerPhone = MobileNumber.normalize(manager.phone);
                    tvManagerNumber.setText(MobileNumber.format(managerPhone));
                    btnManagerCall.setEnabled(true);
                    btnManagerMessage.setEnabled(true);
                } else {
                    tvManagerNumber.setText(R.string.no_number_on_file);
                    btnManagerCall.setEnabled(false);
                    btnManagerMessage.setEnabled(false);
                }
                cardManagerContact.setVisibility(View.VISIBLE);
            }

            @Override public void onError(Exception e) { /* the unit details above still stand */ }
        });
    }

    /** The one overall number, always the mean of the stage bars the site team maintains. */
    private void bindStageProgress(DailyLogRepository.ProjectInfo p,
                                   java.util.Date start, java.util.Date end) {
        Integer work = p.overallPercent;
        Integer sched = ProgressUtil.schedulePercent(start, end);

        pbOverall.setProgress(work != null ? work : 0);
        tvProgressPercent.setText(work != null
                ? getString(R.string.percent_value, work)
                : getString(R.string.progress_not_started));
        tvUnitStatus.setText(ProgressUtil.status(work, sched));
    }

    /** How many logs the engineers have filed and when the newest one landed. */
    private void watchSiteActivity(String projectId) {
        if (progressListener != null) progressListener.remove();
        progressListener = repo.observeLogsForProject(projectId,
                new DailyLogRepository.Callback<List<DailyLog>>() {
                    @Override
                    public void onSuccess(List<DailyLog> logs) {
                        tvSpecLogs.setText(String.valueOf(logs.size()));
                        tvSpecLastReport.setText(latestDateOf(logs));
                        tvTileLogsSub.setText(logs.size() == 1 ? "1 log filed" : logs.size() + " logs filed");
                    }

                    @Override
                    public void onError(Exception e) { /* the unit details above still stand */ }
                });
    }

    private static String latestDateOf(List<DailyLog> logs) {
        java.util.Date newest = null;
        for (DailyLog l : logs) {
            if (l.loggedAt != null && (newest == null || l.loggedAt.after(newest))) newest = l.loggedAt;
        }
        return newest == null ? "No logs yet" : ProgressUtil.formatDate(newest);
    }

    private static String handoverText(java.util.Date end) {
        return end == null ? "Not set yet"
                : new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(end);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buyerListener != null) buyerListener.remove();
        if (progressListener != null) progressListener.remove();
    }
}