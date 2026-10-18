package com.example.buildtrack_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

// PLACEHOLDER: Ruddhima replaces this screen with the manager dashboard
public class ManagerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_home);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(ManagerHomeActivity.this, "To Log out, Press the Menu button at the top", Toast.LENGTH_LONG).show();
            }
        });

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnMenu = findViewById(R.id.btnMenu);

        // the name travels here inside the Intent that MainActivity started this screen with
        String name = getIntent().getStringExtra("name");
        tvWelcome.setText("Welcome, " + name);

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(ManagerHomeActivity.this, btnMenu);
                menu.getMenuInflater().inflate(R.menu.menu_home, menu.getMenu());

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menuLogout) {
                            FirebaseAuth.getInstance().signOut();
                            Intent i = new Intent(ManagerHomeActivity.this, MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                        }
                        return true;
                    }
                });

                menu.show();
            }
        });
    }
}