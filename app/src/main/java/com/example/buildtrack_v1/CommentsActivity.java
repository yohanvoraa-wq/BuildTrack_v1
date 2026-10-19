package com.example.buildtrack_v1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CommentsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        EditText etCommentInput = findViewById(R.id.etCommentInput);
        Button btnSendComment = findViewById(R.id.btnSendComment);
        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSendComment.setOnClickListener(v -> {
            String text = etCommentInput.getText().toString().trim();
            if (text.isEmpty()) return;
            Toast.makeText(this, "Comment sent to engineer!", Toast.LENGTH_SHORT).show();
            etCommentInput.setText("");
        });
    }
}