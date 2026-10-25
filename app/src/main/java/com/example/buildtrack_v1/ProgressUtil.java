package com.example.buildtrack_v1;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small helpers shared by the manager screens. */
public class ProgressUtil {

    /** % of the planned time that has passed (0-100), or null if the dates aren't set. */
    public static Integer schedulePercent(Date start, Date end) {
        if (start == null || end == null) return null;
        long total = end.getTime() - start.getTime();
        if (total <= 0) return null;
        long elapsed = System.currentTimeMillis() - start.getTime();
        int p = (int) Math.round(100.0 * elapsed / total);
        return Math.max(0, Math.min(100, p));
    }

    /** Short status label from work progress and schedule progress. */
    public static String status(Integer work, Integer sched) {
        if (work != null && work >= 100) return "Completed";
        if (work == null) {
            if (sched != null && sched >= 100) return "Overdue";
            return "Awaiting update";
        }
        if (sched == null) return "In progress";
        return (work + 10 < sched) ? "Behind schedule" : "On track";
    }

    public static int statusColor(Context c, String status) {
        switch (status) {
            case "Completed":
            case "On track":
            case "In progress":
            case "Active":
                return ContextCompat.getColor(c, R.color.status_ok);
            case "Awaiting update":
                return ContextCompat.getColor(c, R.color.status_wait);
            default:
                return ContextCompat.getColor(c, R.color.status_late);
        }
    }

    /** Status reads as a small dot plus a label, not a filled pill. */
    public static void styleStatus(TextView tv, String text, int color) {
        float density = tv.getResources().getDisplayMetrics().density;
        tv.setBackground(null);
        tv.setPadding(0, 0, 0, 0);
        tv.setCompoundDrawables(null, null, null, null);
        tv.setText(tv.getContext().getString(R.string.status_dot, text));
        tv.setTextColor(color);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
    }

    /** Turns a TextView into a coloured rounded chip. */
    public static void styleChip(TextView chip, String text, int color) {
        float density = chip.getResources().getDisplayMetrics().density;
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(14 * density);
        chip.setBackground(g);
        chip.setText(text);
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), R.color.on_accent));
    }

    public static String formatDate(Date d) {
        return d == null ? "-" : new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d);
    }

    public static String formatDateTime(Date d) {
        return d == null ? "-" : new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(d);
    }
}