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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etPassword = findViewById(R.id.etPassword);
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);
        RadioGroup rgRole = findViewById(R.id.rgRole);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnGoLogin = findViewById(R.id.btnGoLogin);

        // Part A: Show password checkbox
        cbShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbShowPassword.isChecked()) {
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        // Part B: Register button = validate, confirm, then save
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                if (firstName.length() == 0) {
                    etFirstName.requestFocus();
                    etFirstName.setError("First name cannot be empty");
                    return;
                }

                if (!firstName.matches("[a-zA-Z]+([ '-][a-zA-Z]+)*")) {
                    etFirstName.requestFocus();
                    etFirstName.setError("Letters only (space, - and ' allowed inside)");
                    return;
                }

                if (lastName.length() > 0 && !lastName.matches("[a-zA-Z]+([ '-][a-zA-Z]+)*")) {
                    etLastName.requestFocus();
                    etLastName.setError("Letters only (space, - and ' allowed inside)");
                    return;
                }

                if (!email.matches("[a-zA-Z0-9]+([._%+-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.-][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,}")) {
                    etEmail.requestFocus();
                    etEmail.setError("Enter a valid email, e.g. name@gmail.com");
                    return;
                }

                String passwordError = getPasswordError(password);
                if (passwordError != null) {
                    etPassword.requestFocus();
                    etPassword.setError(passwordError);
                    return;
                }

                final String phone = MobileNumber.normalize(etPhone.getText().toString());
                if (phone == null) {
                    etPhone.requestFocus();
                    etPhone.setError(getString(R.string.phone_invalid));
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
                    Toast.makeText(RegisterActivity.this, "Please select your role", Toast.LENGTH_LONG).show();
                    return;
                }

                String fullName = firstName.toUpperCase(Locale.ROOT);
                if (lastName.length() > 0) {
                    fullName = fullName + " " + lastName.toUpperCase(Locale.ROOT);
                }
                String finalName = fullName;
                String finalRole = role;
                String finalEmail = email.toLowerCase(Locale.ROOT);

                AlertDialog.Builder ad = new AlertDialog.Builder(RegisterActivity.this);
                ad.setTitle("Confirm Registration");
                ad.setMessage("Create a " + finalRole + " account for " + finalName + " (" + finalEmail + ")?");

                ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        btnRegister.setEnabled(false);
                        Toast.makeText(RegisterActivity.this, "Creating account...", Toast.LENGTH_LONG).show();

                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        auth.createUserWithEmailAndPassword(finalEmail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    final FirebaseUser created = task.getResult().getUser();
                                    String uid = created.getUid();
                                    UserInfo info = new UserInfo(finalName, finalEmail, finalRole);
                                    info.setPhone(phone);
                                    if ("engineer".equals(finalRole)) {
                                        // Save the assignment ID at registration, not only after the
                                        // engineer happens to open their dashboard.
                                        info.setEngineerId(EngineerIdentity.fromUid(uid));
                                    }

                                    FirebaseFirestore.getInstance().collection("users").document(uid)
                                            .set(info)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task2) {
                                            btnRegister.setEnabled(true);
                                            if (task2.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "Account created", Toast.LENGTH_LONG).show();
                                                openHome(finalRole, finalName);
                                            } else {
                                                // An auth account with no profile can never log in, and it
                                                // would keep this email locked. Delete it so a retry works.
                                                created.delete().addOnCompleteListener(ignore ->
                                                        showError("Could not save profile",
                                                                task2.getException().getMessage()
                                                                        + "\n\nThe sign-in was rolled back, so you can register again."));
                                            }
                                        }
                                    });
                                } else {
                                    btnRegister.setEnabled(true);
                                    showError("Registration failed", task.getException().getMessage());
                                }
                            }
                        });
                    }
                });

                ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(RegisterActivity.this, "Registration cancelled", Toast.LENGTH_LONG).show();
                    }
                });

                ad.show();
            }
        });

        // Part C: back to the Login screen
        btnGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // opens the home screen for this role and clears every screen behind it
    private void openHome(String role, String name) {
        Intent i;
        if (role.equals("manager")) {
            i = new Intent(RegisterActivity.this, ManagerHomeActivity.class);
        } else if (role.equals("engineer")) {
            i = new Intent(RegisterActivity.this, EngineerHomeActivity.class);
        } else {
            i = new Intent(RegisterActivity.this, BuyerHomeActivity.class);
        }
        i.putExtra("name", name);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    private String getPasswordError(String p) {
        if (p.contains(" ")) {
            return "Password cannot contain spaces";
        }
        if (p.contains("_")) {
            return "Password cannot contain an underscore";
        }
        if (!p.matches("[a-zA-Z0-9@#$%&]*")) {
            return "Only these special characters are allowed: @ # $ % &";
        }
        if (p.matches("[@#$%&].*")) {
            return "Password cannot start with a special character";
        }
        if (p.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (!p.matches(".*[A-Z].*")) {
            return "Add at least 1 uppercase letter";
        }
        if (!p.matches(".*[a-z].*")) {
            return "Add at least 1 lowercase letter";
        }
        if (!p.matches(".*[0-9].*")) {
            return "Add at least 1 number";
        }
        if (!p.matches(".*[@#$%&].*")) {
            return "Add at least 1 special character (@ # $ % &)";
        }
        return null;
    }

    private void showError(String title, String message) {
        AlertDialog.Builder ad = new AlertDialog.Builder(RegisterActivity.this);
        ad.setTitle(title);
        ad.setMessage(message);
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.show();
    }
}
