package com.example.buildtrack_v1;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/** Live text chat for one project, shared by the manager and the assigned engineers. */
public class ChatActivity extends AppCompatActivity {

    private final DailyLogRepository repo = new DailyLogRepository();

    private RecyclerView rvMessages;
    private TextView tvEmpty;
    private EditText etMessage;
    private ChatAdapter adapter;
    private ListenerRegistration listener;

    private String projectId;
    private String role;
    private String uid;
    private String myName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        role = getIntent().getStringExtra("role");
        if (role == null) role = "engineer";
        setTheme(UiTheme.roleTheme(role));
        setContentView(R.layout.activity_chat);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        if (user == null || projectId == null) {
            Toast.makeText(this, "Cannot open chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        myName = user.getEmail() != null ? user.getEmail() : "Me";

        // the real name comes from the saved profile
        repo.loadProfile(uid, new DailyLogRepository.Callback<UserInfo>() {
            @Override public void onSuccess(UserInfo info) {
                if (info != null && info.getName() != null && !info.getName().isEmpty()) {
                    myName = info.getName();
                }
            }

            @Override public void onError(Exception e) { /* keep the email */ }
        });

        int myColor = UiTheme.accent(this);

        ((TextView) findViewById(R.id.tvChatTitle)).setText(projectName != null ? projectName : "Chat");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Button btnSend = findViewById(R.id.btnSend);
        btnSend.setBackgroundTintList(ColorStateList.valueOf(myColor));

        rvMessages = findViewById(R.id.rvMessages);
        tvEmpty = findViewById(R.id.tvChatEmpty);
        etMessage = findViewById(R.id.etMessage);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        adapter = new ChatAdapter(uid, this);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> send());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid == null) return;

        listener = repo.observeMessages(projectId,
                new DailyLogRepository.Callback<List<DailyLogRepository.ChatMessage>>() {
                    @Override
                    public void onSuccess(List<DailyLogRepository.ChatMessage> messages) {
                        adapter.setMessages(messages);
                        if (messages.isEmpty()) {
                            tvEmpty.setText(R.string.chat_empty);
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvMessages.scrollToPosition(messages.size() - 1);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        tvEmpty.setText(getString(R.string.error_loading_chat,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    private void send() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");

        repo.sendMessage(projectId, uid, myName, role, text, new DailyLogRepository.Callback<Void>() {
            @Override public void onSuccess(Void r) { /* the live listener shows it */ }

            @Override public void onError(Exception e) {
                etMessage.setText(text); // give the text back so nothing is lost
                Toast.makeText(ChatActivity.this,
                        "Could not send: " + (e != null ? e.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}