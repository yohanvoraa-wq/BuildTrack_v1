package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;

public class DailyLogEntryActivity extends AppCompatActivity {

    private static final String[] MATERIALS =
            {"Cement", "Sand", "Steel", "Bricks", "Aggregate", "Other"};
    private static final String[] UNITS =
            {"bags", "kg", "tonnes", "pieces", "cubic metres"};
    private static final String[] TRADES =
            {"Mason", "Carpenter", "Electrician", "Plumber", "Helper", "Other"};

    private LinearLayout materialRows, labourRows;
    private Button btnSubmit;
    private String projectId;
    private String engineerName;

    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_log);

        projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");
        engineerName = getIntent().getStringExtra("engineerName");
        ((TextView) findViewById(R.id.tvSubtitle)).setText(projectName != null ? projectName : "");

        // The date and time card (TextClock) ticks on its own from the device clock.
        // The saved values are read from the device again at the moment of submit.

        materialRows = findViewById(R.id.materialRows);
        labourRows = findViewById(R.id.labourRows);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddMaterial).setOnClickListener(v -> addMaterialRow());
        findViewById(R.id.btnAddLabour).setOnClickListener(v -> addLabourRow());
        btnSubmit.setOnClickListener(v -> submit());

        addMaterialRow();
        addLabourRow();
    }

    private void addMaterialRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.activity_itemmaterial_row, materialRows, false);
        ((Spinner) row.findViewById(R.id.spMaterial)).setAdapter(adapterFor(MATERIALS));
        ((Spinner) row.findViewById(R.id.spUnit)).setAdapter(adapterFor(UNITS));
        row.findViewById(R.id.btnRemove).setOnClickListener(v -> materialRows.removeView(row));
        materialRows.addView(row);
    }

    private void addLabourRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.activity_itemlabour_row, labourRows, false);
        ((Spinner) row.findViewById(R.id.spTrade)).setAdapter(adapterFor(TRADES));
        row.findViewById(R.id.btnRemove).setOnClickListener(v -> labourRows.removeView(row));
        labourRows.addView(row);
    }

    private ArrayAdapter<String> adapterFor(String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void submit() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || projectId == null) {
            Toast.makeText(this, "Not signed in or no project selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (materialRows.getChildCount() + labourRows.getChildCount() == 0) {
            Toast.makeText(this, "Add at least one material or labour row", Toast.LENGTH_SHORT).show();
            return;
        }

        DailyLog log = new DailyLog();

        // Materials: reject empty, non-numeric, zero or negative quantities
        for (int i = 0; i < materialRows.getChildCount(); i++) {
            View row = materialRows.getChildAt(i);
            EditText etQty = row.findViewById(R.id.etQty);
            double qty;
            try {
                qty = Double.parseDouble(etQty.getText().toString().trim());
            } catch (NumberFormatException e) {
                etQty.setError("Enter a quantity or remove this row");
                etQty.requestFocus();
                return;
            }
            if (qty <= 0) {
                etQty.setError("Must be greater than 0");
                etQty.requestFocus();
                return;
            }
            log.materials.add(new DailyLog.MaterialEntry(
                    ((Spinner) row.findViewById(R.id.spMaterial)).getSelectedItem().toString(),
                    qty,
                    ((Spinner) row.findViewById(R.id.spUnit)).getSelectedItem().toString()));
        }

        // Labour: whole numbers greater than 0
        for (int i = 0; i < labourRows.getChildCount(); i++) {
            View row = labourRows.getChildAt(i);
            EditText etCount = row.findViewById(R.id.etCount);
            int count;
            try {
                count = Integer.parseInt(etCount.getText().toString().trim());
            } catch (NumberFormatException e) {
                etCount.setError("Enter a headcount or remove this row");
                etCount.requestFocus();
                return;
            }
            if (count <= 0) {
                etCount.setError("Must be greater than 0");
                etCount.requestFocus();
                return;
            }
            log.labour.add(new DailyLog.LabourEntry(
                    ((Spinner) row.findViewById(R.id.spTrade)).getSelectedItem().toString(),
                    count));
        }

        Date now = new Date(); // device date and time at the moment of submit
        log.projectId = projectId;
        log.engineerId = user.getUid();
        log.engineerName = engineerName != null ? engineerName : user.getEmail();
        log.loggedAt = now;
        log.date = DailyLogRepository.dateString(now);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Saving...");

        repo.saveLog(log, new DailyLogRepository.Callback<Void>() {
            @Override public void onSuccess(Void r) {
                Toast.makeText(DailyLogEntryActivity.this, "Entry saved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override public void onError(Exception e) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit log");
                Toast.makeText(DailyLogEntryActivity.this,
                        "Could not save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}