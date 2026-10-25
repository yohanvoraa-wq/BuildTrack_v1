package com.example.buildtrack_v1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Writes the instruction note that every assigned engineer sees on their project card. */
public class CommentsActivity extends AppCompatActivity {

    private String projectId, projectName;
    private TextView tvProjectTitle, tvCurrentInstructions;
    private EditText etManagerNote;
    private final DailyLogRepository repo = new DailyLogRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        tvCurrentInstructions = findViewById(R.id.tvCurrentInstructions);
        etManagerNote = findViewById(R.id.etManagerNote);

        if (projectName != null) tvProjectTitle.setText(projectName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendComment).setOnClickListener(v -> sendNoteToEngineer());

        if (projectId == null) {
            Toast.makeText(this, "No project specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadCurrentNote();
    }

    private void loadCurrentNote() {
        repo.getProject(projectId, new DailyLogRepository.Callback<DailyLogRepository.ProjectInfo>() {
            @Override public void onSuccess(DailyLogRepository.ProjectInfo p) {
                tvCurrentInstructions.setText(p.siteNote == null || p.siteNote.isEmpty()
                        ? "No note is active on this site."
                        : "Active note:\n\"" + p.siteNote + "\"");
            }

            @Override public void onError(Exception e) {
                tvCurrentInstructions.setText(R.string.note_unavailable);
            }
        });
    }

    private void sendNoteToEngineer() {
        String note = etManagerNote.getText().toString().trim();
        if (note.isEmpty()) {
            Toast.makeText(this, "Type the instruction for the site team", Toast.LENGTH_SHORT).show();
            return;
        }

        Button btnSend = findViewById(R.id.btnSendComment);
        btnSend.setEnabled(false);
        repo.setSiteNote(projectId, note, new DailyLogRepository.Callback<Void>() {
            @Override public void onSuccess(Void r) {
                Toast.makeText(CommentsActivity.this,
                        "Note saved. It shows on the engineer's card for this site.",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override public void onError(Exception e) {
                btnSend.setEnabled(true);
                Toast.makeText(CommentsActivity.this,
                        "Could not save: " + (e != null ? e.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
