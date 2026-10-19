package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;

public class ManagerHomeActivity extends AppCompatActivity {

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

        // New Project Buttons
        Button btnNewProject = findViewById(R.id.btnNewProject);
        CardView cardAddNewProject = findViewById(R.id.cardAddNewProject);

        View.OnClickListener createProjectListener = v -> showCreateProjectDialog();
        btnNewProject.setOnClickListener(createProjectListener);
        cardAddNewProject.setOnClickListener(createProjectListener);

        // Manage Project 1
        Button btnManageProject1 = findViewById(R.id.btnManageProject1);
        btnManageProject1.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerHomeActivity.this, ProjectDetailActivity.class);
            intent.putExtra("projectId", "proj_01");
            intent.putExtra("projectName", "Skyline Towers - Phase 1");
            startActivity(intent);
        });

        // Manage Project 2
        Button btnManageProject2 = findViewById(R.id.btnManageProject2);
        btnManageProject2.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerHomeActivity.this, ProjectDetailActivity.class);
            intent.putExtra("projectId", "proj_02");
            intent.putExtra("projectName", "Greenfield Residency");
            startActivity(intent);
        });
    }

    private void showCreateProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Project");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Project Name (e.g. Amber Woods)");
        layout.addView(etName);

        final EditText etLoc = new EditText(this);
        etLoc.setHint("Location (e.g. Block B, Metro Road)");
        layout.addView(etLoc);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String pName = etName.getText().toString().trim();
            if (pName.isEmpty()) {
                Toast.makeText(this, "Project name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Project '" + pName + "' created!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}