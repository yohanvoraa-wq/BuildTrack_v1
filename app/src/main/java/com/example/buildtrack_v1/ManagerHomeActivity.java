package com.example.buildtrack_v1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ManagerHomeActivity extends AppCompatActivity {

    private int projectCount = 2;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_home);

        // Back press behavior
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(ManagerHomeActivity.this, "To Log out, Press the Menu button at the top", Toast.LENGTH_SHORT).show();
            }
        });

        // Top bar views
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnMenu = findViewById(R.id.btnMenu);

        String name = getIntent().getStringExtra("name");
        tvWelcome.setText(name != null ? "Welcome, " + name : "Welcome, Ruddhema");

        // Menu Logout logic
        btnMenu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(ManagerHomeActivity.this, v);
            menu.getMenuInflater().inflate(R.menu.menu_home, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menuLogout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(ManagerHomeActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                }
                return true;
            });
            menu.show();
        });

        // "+ Assign Project" buttons
        Button btnNewProject = findViewById(R.id.btnNewProject);
        CardView cardAddNewProject = findViewById(R.id.cardAddNewProject);

        View.OnClickListener assignProjectListener = v -> showCreateProjectDialog();
        if (btnNewProject != null) btnNewProject.setOnClickListener(assignProjectListener);
        if (cardAddNewProject != null) cardAddNewProject.setOnClickListener(assignProjectListener);

        // Manage Project 1 (68% - Crimson)
        Button btnManageProject1 = findViewById(R.id.btnManageProject1);
        if (btnManageProject1 != null) {
            btnManageProject1.setOnClickListener(v -> {
                Intent intent = new Intent(ManagerHomeActivity.this, ProjectDetailActivity.class);
                intent.putExtra("projectId", "proj_01");
                intent.putExtra("projectName", "Skyline Towers - Phase 1");
                startActivity(intent);
            });
        }

        // Manage Project 2 (100% - Green line)
        Button btnManageProject2 = findViewById(R.id.btnManageProject2);
        if (btnManageProject2 != null) {
            btnManageProject2.setOnClickListener(v -> {
                Intent intent = new Intent(ManagerHomeActivity.this, ProjectDetailActivity.class);
                intent.putExtra("projectId", "proj_02");
                intent.putExtra("projectName", "Greenfield Residency");
                startActivity(intent);
            });
        }
    }

    /**
     * Dialog to create a project and assign Engineer & Buyer UIDs
     * Integrates directly with On-Site Engineer Firestore listener
     */
    private void showCreateProjectDialog() {
        FirebaseUser manager = FirebaseAuth.getInstance().getCurrentUser();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assign New Project to Engineer");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 24, 50, 16);

        final EditText etName = new EditText(this);
        etName.setHint("Project Name (e.g. Site Tower A)");
        layout.addView(etName);

        final EditText etEngineerUid = new EditText(this);
        etEngineerUid.setHint("Engineer Unique ID (e.g. ENG-103 or UID)");
        layout.addView(etEngineerUid);

        final EditText etBuyerUid = new EditText(this);
        etBuyerUid.setHint("Buyer UID / Email to link");
        layout.addView(etBuyerUid);

        final EditText etLoc = new EditText(this);
        etLoc.setHint("Project Location (e.g. Sector Site 1)");
        layout.addView(etLoc);

        builder.setView(layout);

        builder.setPositiveButton("Assign & Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String engineerId = etEngineerUid.getText().toString().trim();
            String buyerId = etBuyerUid.getText().toString().trim();
            String location = etLoc.getText().toString().trim();

            if (name.isEmpty() || engineerId.isEmpty()) {
                Toast.makeText(this, "Project Name and Engineer ID are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (location.isEmpty()) location = "Sector Site 1";

            // LINK: Writing to Firestore so On-Site Engineer can immediately observe it
            Map<String, Object> project = new HashMap<>();
            project.put("name", name);
            project.put("managerId", manager != null ? manager.getUid() : "mgr_default");
            project.put("managerName", manager != null ? manager.getEmail() : "Lead Manager");
            project.put("status", "Active");
            project.put("location", location);
            project.put("deadline", "30 Nov 2026");
            project.put("overallProgress", 0);
            project.put("assignedEngineers", Arrays.asList(engineerId)); // 👈 GIVES ACCESS TO ON-SITE ENGINEER
            project.put("assignedBuyers", Arrays.asList(buyerId));       // 👈 GIVES ACCESS TO BUYER

            String finalLocation = location;
            db.collection("projects").add(project)
                    .addOnSuccessListener(ref -> {
                        addNewProjectCard(name, engineerId, finalLocation, 0);
                        Toast.makeText(this, "Project assigned to Engineer ID: " + engineerId, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Fallback: still show card on UI so testing isn't blocked if offline
                        addNewProjectCard(name, engineerId, finalLocation, 0);
                        Toast.makeText(this, "Assigned locally: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Dynamically adds a new project card showing the unique Engineer ID
     * and sets green line if 100% completed
     */
    private void addNewProjectCard(String projectName, String engineerId, String location, int progress) {
        projectCount++;
        TextView tvSectionTitle = findViewById(R.id.tvSectionTitle);
        if (tvSectionTitle != null) {
            tvSectionTitle.setText("Ongoing Projects (" + projectCount + ")");
        }

        float density = getResources().getDisplayMetrics().density;

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = (int) (16 * density);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setRadius(16 * density);
        card.setCardElevation(3 * density);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * density);
        inner.setPadding(pad, pad, pad, pad);

        // Header row (Name + % badge)
        RelativeLayout headerRow = new RelativeLayout(this);
        headerRow.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvName = new TextView(this);
        tvName.setText(projectName);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.ink));
        tvName.setTextSize(18);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvBadge = new TextView(this);
        if (progress == 100) {
            tvBadge.setText("100% Completed");
            tvBadge.setTextColor(Color.parseColor("#27AE60"));
        } else {
            tvBadge.setText(progress + "%");
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.manager_crimson));
        }
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setBackgroundResource(R.drawable.bg_input);
        int padH = (int) (10 * density);
        int padV = (int) (4 * density);
        tvBadge.setPadding(padH, padV, padH, padV);
        RelativeLayout.LayoutParams badgeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        tvBadge.setLayoutParams(badgeParams);

        headerRow.addView(tvName);
        headerRow.addView(tvBadge);
        inner.addView(headerRow);

        // Unique Assigned Engineer ID
        TextView tvEng = new TextView(this);
        tvEng.setText("👷 Assigned Engineer ID: " + engineerId);
        tvEng.setTextColor(ContextCompat.getColor(this, R.color.ink));
        tvEng.setTextSize(13);
        tvEng.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams engParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        engParams.topMargin = (int) (6 * density);
        tvEng.setLayoutParams(engParams);
        inner.addView(tvEng);

        // Location
        TextView tvLoc = new TextView(this);
        tvLoc.setText("📍 " + location);
        tvLoc.setTextColor(ContextCompat.getColor(this, R.color.grey_text));
        tvLoc.setTextSize(14);
        LinearLayout.LayoutParams locParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        locParams.topMargin = (int) (2 * density);
        tvLoc.setLayoutParams(locParams);
        inner.addView(tvLoc);

        // Current phase
        TextView tvPhase = new TextView(this);
        tvPhase.setText(progress == 100 ? "Current Phase: Handover & Completed" : "Current Phase: Foundation Work");
        tvPhase.setTextColor(progress == 100 ? Color.parseColor("#27AE60") : ContextCompat.getColor(this, R.color.ink));
        tvPhase.setTextSize(13);
        tvPhase.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams phaseParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        phaseParams.topMargin = (int) (8 * density);
        tvPhase.setLayoutParams(phaseParams);
        inner.addView(tvPhase);

        // Progress bar (GREEN LINE IF 100%, CRIMSON OTHERWISE)
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (8 * density)
        );
        pbParams.topMargin = (int) (8 * density);
        pb.setLayoutParams(pbParams);
        pb.setProgress(progress);

        if (progress == 100) {
            pb.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#27AE60"))); // Green Line
        } else {
            pb.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.manager_crimson)));
        }
        inner.addView(pb);

        // Manage Button
        Button btnManage = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (44 * density)
        );
        btnParams.topMargin = (int) (14 * density);
        btnManage.setLayoutParams(btnParams);
        btnManage.setText("Manage Project");
        btnManage.setAllCaps(false);
        btnManage.setTextColor(ContextCompat.getColor(this, R.color.white));
        btnManage.setTypeface(null, Typeface.BOLD);
        btnManage.setBackgroundResource(R.drawable.bg_button_crimson);

        String generatedId = "proj_" + System.currentTimeMillis();
        btnManage.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerHomeActivity.this, ProjectDetailActivity.class);
            intent.putExtra("projectId", generatedId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });
        inner.addView(btnManage);

        card.addView(inner);

        // Insert right before "+ Assign Another Project" card
        LinearLayout container = findViewById(R.id.llProjectsContainer);
        if (container != null) {
            int insertIdx = Math.max(0, container.getChildCount() - 1);
            container.addView(card, insertIdx);
        }
    }
}