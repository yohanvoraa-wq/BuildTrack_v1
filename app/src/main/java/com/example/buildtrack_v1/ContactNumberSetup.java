package com.example.buildtrack_v1;

import android.app.Activity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Accounts created before mobile numbers were collected have none, so the contact links between
 * roles would be dead. Each home screen asks once, on entry, for the missing number.
 */
public final class ContactNumberSetup {

    private ContactNumberSetup() {}

    public static void askIfMissing(Activity host, DailyLogRepository repo) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;
        repo.loadProfile(me.getUid(), new DailyLogRepository.Callback<UserInfo>() {
            @Override public void onSuccess(UserInfo profile) {
                if (profile != null && MobileNumber.isCallable(profile.getPhone())) return;
                ask(host, repo, me.getUid());
            }

            @Override public void onError(Exception e) { /* profile is optional here */ }
        });
    }

    private static void ask(Activity host, DailyLogRepository repo, String uid) {
        EditText input = new EditText(host);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(R.string.your_number_title);

        new AlertDialog.Builder(host)
                .setTitle(R.string.your_number_title)
                .setMessage(R.string.your_number_body)
                .setView(input)
                .setPositiveButton(R.string.add_number, (dialog, which) ->
                        save(host, repo, uid, input.getText().toString()))
                .setNegativeButton(R.string.later, null)
                .show();
    }

    private static void save(Activity host, DailyLogRepository repo, String uid, String raw) {
        String digits = MobileNumber.normalize(raw);
        if (digits == null) {
            Toast.makeText(host, R.string.phone_invalid, Toast.LENGTH_LONG).show();
            ask(host, repo, uid);
            return;
        }
        repo.savePhone(uid, digits, new DailyLogRepository.Callback<Void>() {
            @Override public void onSuccess(Void r) {
                Toast.makeText(host, R.string.number_saved, Toast.LENGTH_SHORT).show();
            }

            @Override public void onError(Exception e) {
                Toast.makeText(host, host.getString(R.string.could_not_save,
                        e != null && e.getMessage() != null ? e.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
