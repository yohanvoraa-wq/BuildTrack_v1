package com.example.buildtrack_v1;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

/**
 * Colours pulled from the activity's role theme, so shared screens can be drawn by the same
 * tokens the layouts use instead of hard-coded values.
 */
final class UiTheme {

    private UiTheme() {}

    static int accent(Context c) { return attr(c, androidx.appcompat.R.attr.colorPrimary); }

    static int onAccent(Context c) {
        return attr(c, com.google.android.material.R.attr.colorOnPrimary);
    }

    static int panel(Context c) {
        return attr(c, com.google.android.material.R.attr.colorSurfaceContainerHigh);
    }

    static int bodyText(Context c) {
        return attr(c, com.google.android.material.R.attr.colorOnSurface);
    }

    static int mutedText(Context c) {
        return attr(c, com.google.android.material.R.attr.colorOnSurfaceVariant);
    }

    /** Chooses the engineer or manager identity for a screen both roles open. */
    static int roleTheme(String role) {
        return "manager".equals(role)
                ? R.style.Theme_BuildTrack_v1_Manager
                : R.style.Theme_BuildTrack_v1_Engineer;
    }

    private static int attr(Context c, int attribute) {
        TypedArray a = c.obtainStyledAttributes(new int[]{attribute});
        try {
            return a.getColor(0, Color.TRANSPARENT);
        } finally {
            a.recycle();
        }
    }
}
