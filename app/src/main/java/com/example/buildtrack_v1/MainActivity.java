package com.example.buildtrack_v1;

import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);
        RadioGroup rgRole = findViewById(R.id.rgRole);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Part A: Show password checkbox
        cbShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbShowPassword.isChecked()) {
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                // changing the method moves the cursor to the start, so move it back to the end
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        // Part B: Register button = validate, then confirm
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                // return after each error: only the first bad field gets the focus,
                // and nothing moves on with bad data
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

                // last name is optional, so check it only if something was typed
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

                // a RadioGroup has no setError, so a Toast reports a missing role
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

                // the system stores the name in UPPERCASE: "YohAN vOra" and "yohan VORA" become one name
                String fullName = firstName.toUpperCase(Locale.ROOT);
                if (lastName.length() > 0) {
                    fullName = fullName + " " + lastName.toUpperCase(Locale.ROOT);
                }
                String finalName = fullName;
                String finalRole = role;

                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setTitle("Confirm Registration");
                ad.setMessage("Create a " + finalRole + " account for " + finalName + " (" + email + ")?");

                ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Step 3 replaces this Toast with the Firebase save
                        Toast.makeText(MainActivity.this, "Valid: " + finalName + " / " + finalRole, Toast.LENGTH_LONG).show();
                    }
                });

                ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "Registration cancelled", Toast.LENGTH_LONG).show();
                    }
                });

                // the dialog stays hidden until show() is called
                ad.show();
            }
        });
    }

    // Returns null when the password is fine, otherwise a message for the FIRST rule broken
    private String getPasswordError(String p) {

        if (p.contains(" ")) {
            return "Password cannot contain spaces";
        }
        if (p.contains("_")) {
            return "Password cannot contain an underscore";
        }
        // only letters, digits and these five special characters may appear anywhere
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
}