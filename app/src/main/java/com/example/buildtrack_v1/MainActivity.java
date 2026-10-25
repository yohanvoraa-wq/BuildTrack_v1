package com.example.buildtrack_v1;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnGoRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);
        RadioGroup rgRole = findViewById(R.id.rgRole);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        // Part A: Show password toggle
        cbShowPassword.setOnClickListener(v -> {
            if (cbShowPassword.isChecked()) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Part B: Login button
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (!email.matches("[a-zA-Z0-9]+([._%+-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.-][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,}")) {
                etEmail.requestFocus();
                etEmail.setError("Enter a valid email, e.g. name@gmail.com");
                return;
            }

            if (password.length() == 0) {
                etPassword.requestFocus();
                etPassword.setError("Password cannot be empty");
                return;
            }

            String role = "";
            int checkedId = rgRole.getCheckedRadioButtonId();
            if (checkedId == R.id.rbManager) {
                role = "manager";
            } else if (checkedId == R.id.rbEngineer) {
                role = "engineer";
            } else if (checkedId == R.id.rbBuyer) {
                role = "buyer";
            }

            if (role.length() == 0) {
                Toast.makeText(MainActivity.this, "Please select your role", Toast.LENGTH_LONG).show();
                return;
            }
            String chosenRole = role;

            setButtonsEnabled(false);
            Toast.makeText(MainActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.toLowerCase(Locale.ROOT), password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().getUser() != null) {
                            routeUser(task.getResult().getUser().getUid(), chosenRole);
                        } else {
                            setButtonsEnabled(true);
                            showError("Login failed",
                                    "Check your email and password, or tap Create an account if you haven't registered yet.\n\nDetails: "
                                            + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    });
        });

        // Part C: Forgot password
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Part D: Open Register screen
        btnGoRegister.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        // Auto-login if already signed in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            setButtonsEnabled(false);
            routeUser(FirebaseAuth.getInstance().getCurrentUser().getUid(), null);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnGoRegister.setEnabled(enabled);
    }

    /** Loads the saved profile and opens the matching home screen. */
    private void routeUser(String uid, String chosenRole) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        verifyAndOpen(doc.getString("role"), doc.getString("name"), chosenRole);
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        setButtonsEnabled(true);
                        showError("Profile not found", "This account has no saved profile. Please register again.");
                    }
                })
                .addOnFailureListener(e -> {
                    FirebaseAuth.getInstance().signOut();
                    setButtonsEnabled(true);
                    showError("Could not load profile", e.getMessage());
                });
    }

    private void verifyAndOpen(String savedRole, String name, String chosenRole) {
        if (chosenRole != null && !chosenRole.equalsIgnoreCase(savedRole)) {
            FirebaseAuth.getInstance().signOut();
            setButtonsEnabled(true);
            showError("Wrong role selected",
                    "This account is registered as \"" + savedRole + "\", not \"" + chosenRole + "\". Select the role you registered with.");
            return;
        }
        openHome(savedRole, name);
    }

    private void openHome(String role, String name) {
        Intent i;
        if ("manager".equalsIgnoreCase(role)) {
            i = new Intent(MainActivity.this, ManagerHomeActivity.class);
        } else if ("engineer".equalsIgnoreCase(role)) {
            i = new Intent(MainActivity.this, EngineerHomeActivity.class);
        } else if ("buyer".equalsIgnoreCase(role)) {
            i = new Intent(MainActivity.this, BuyerHomeActivity.class);
        } else {
            setButtonsEnabled(true);
            showError("Unknown role", "Saved role not recognised: " + role);
            return;
        }

        // Pass name under both keys so all activities receive it
        i.putExtra("name", name);
        i.putExtra("engineerName", name);
        i.putExtra("managerName", name);
        i.putExtra("buyerName", name);

        startActivity(i);
        finish();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.reset_password);
        builder.setMessage(R.string.reset_password_message);

        final EditText input = new EditText(MainActivity.this);
        input.setHint(R.string.email);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        builder.setView(input);

        builder.setPositiveButton(R.string.send_reset_link, null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = input.getText().toString().trim().toLowerCase(Locale.ROOT);
                if (!email.matches("[a-zA-Z0-9]+([._%+-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.-][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,}")) {
                    input.setError("Enter a valid email");
                    return;
                }
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                dialog.dismiss();
                                showError(getString(R.string.reset_password), getString(R.string.reset_email_sent));
                            } else {
                                String detail = task.getException() != null ? task.getException().getMessage() : "";
                                showError(getString(R.string.reset_password), getString(R.string.reset_email_failed) + "\n\n" + detail);
                            }
                        });
            });
        });
        dialog.show();
    }

    private void showError(String title, String message) {
        AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
        ad.setTitle(title);
        ad.setMessage(message);
        ad.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        ad.show();
    }
}