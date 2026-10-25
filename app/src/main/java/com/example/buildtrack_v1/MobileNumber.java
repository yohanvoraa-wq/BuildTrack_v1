package com.example.buildtrack_v1;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/**
 * Mobile numbers are stored as bare digits so every dial/SMS link is built the same way,
 * however the user typed the number at registration.
 */
public final class MobileNumber {

    private MobileNumber() {}

    /** 10-digit Indian mobile number, or null when the input is not one. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 12 && digits.startsWith("00")) digits = digits.substring(2);
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        return digits.matches("[6-9][0-9]{9}") ? digits : null;
    }

    public static String format(String digits) {
        if (digits == null || digits.length() != 10) return digits;
        return "+91 " + digits.substring(0, 5) + " " + digits.substring(5);
    }

    public static boolean isCallable(String digits) {
        return normalize(digits) != null;
    }

    public static void dial(Context context, String digits) {
        open(context, Intent.ACTION_DIAL, "tel:" + normalize(digits));
    }

    public static void message(Context context, String digits) {
        open(context, Intent.ACTION_VIEW, "sms:" + normalize(digits));
    }

    private static void open(Context context, String action, String data) {
        try {
            context.startActivity(new Intent(action, Uri.parse(data))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_dialer_app, Toast.LENGTH_SHORT).show();
        }
    }
}
