package com.example.buildtrack_v1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AssignPeopleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_people);

        EditText etEngineerEmail = findViewById(R.id.etEngineerEmail);
        EditText etFlatNumber = findViewById(R.id.etFlatNumber);
        EditText etBuyerEmail = findViewById(R.id.etBuyerEmail);
        Button btnAssignEngineer = findViewById(R.id.btnAssignEngineer);
        Button btnAttachBuyer = findViewById(R.id.btnAttachBuyer);
        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnAssignEngineer.setOnClickListener(v -> {
            String email = etEngineerEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etEngineerEmail.setError("Enter engineer email");
                return;
            }
            Toast.makeText(this, "Engineer " + email + " attached to project!", Toast.LENGTH_SHORT).show();
            etEngineerEmail.setText("");
        });

        btnAttachBuyer.setOnClickListener(v -> {
            String flat = etFlatNumber.getText().toString().trim();
            String buyer = etBuyerEmail.getText().toString().trim();
            if (flat.isEmpty() || buyer.isEmpty()) {
                Toast.makeText(this, "Fill in both Flat and Buyer Email", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Buyer linked to flat " + flat + "!", Toast.LENGTH_SHORT).show();
            etFlatNumber.setText("");
            etBuyerEmail.setText("");
        });
    }
}
