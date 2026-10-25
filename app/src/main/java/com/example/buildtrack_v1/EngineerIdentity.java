package com.example.buildtrack_v1;

import java.util.Locale;

/** Generates the same readable ID for one Firebase account on every device. */
public final class EngineerIdentity {
    private EngineerIdentity() { }

    public static String fromUid(String uid) {
        if (uid == null || uid.isEmpty()) return "";
        int code = Math.abs(uid.hashCode() % 9000) + 1000;
        return "ENG-" + code;
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("\\d+")) return "ENG-" + normalized;
        return normalized;
    }
}
