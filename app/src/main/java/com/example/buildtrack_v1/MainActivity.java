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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button btnLogin, btnGoRegister;

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

        // Part B: Login button = validate, sign in, check the chosen role, then route
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                Toast.makeText(MainActivity.this, "Logging in...", Toast.LENGTH_LONG).show();

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email.toLowerCase(Locale.ROOT), password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    routeUser(task.getResult().getUser().getUid(), chosenRole);
                                } else {
                                    setButtonsEnabled(true);
                                    showError("Login failed",
                                            "Check your email and password, or tap Create an account if you haven't registered yet.\n\nDetails: "
                                                    + task.getException().getMessage());
                                }
                            }
                        });
            }
        });

        // Part C: open the Register screen
        btnGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        // already signed in: skip the form and go straight to the home screen (no role to check)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            setButtonsEnabled(false);
            routeUser(FirebaseAuth.getInstance().getCurrentUser().getUid(), null);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnGoRegister.setEnabled(enabled);
    }

    // reads users/{uid}, compares the role with the one chosen on screen, then opens the home screen
    private void routeUser(String uid, String chosenRole) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserInfo info = snapshot.getValue(UserInfo.class);
                if (info == null || info.getRole() == null) {
                    FirebaseAuth.getInstance().signOut();
                    setButtonsEnabled(true);
                    showError("Profile not found", "This account has no saved profile. Delete it in Firebase Authentication and register again.");
                } else if (chosenRole != null && !chosenRole.equals(info.getRole())) {
                    // the password was right, but the wrong role card was selected
                    FirebaseAuth.getInstance().signOut();
                    setButtonsEnabled(true);
                    showError("Wrong role selected",
                            "This account is registered as \"" + info.getRole() + "\", not \"" + chosenRole
                                    + "\". Select the role you registered with.");
                } else {
                    openHome(info.getRole(), info.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                FirebaseAuth.getInstance().signOut();
                setButtonsEnabled(true);
                showError("Could not load profile", error.getMessage());
            }
        });
    }

    private void openHome(String role, String name) {
        Intent i;
        if (role.equals("manager")) {
            i = new Intent(MainActivity.this, ManagerHomeActivity.class);
        } else if (role.equals("engineer")) {
            i = new Intent(MainActivity.this, EngineerHomeActivity.class);
        } else if (role.equals("buyer")) {
            i = new Intent(MainActivity.this, BuyerHomeActivity.class);
        } else {
            setButtonsEnabled(true);
            showError("Unknown role", "Saved role not recognised: " + role);
            return;
        }
        i.putExtra("name", name);
        startActivity(i);
        finish();
    }

    private void showError(String title, String message) {
        AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
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