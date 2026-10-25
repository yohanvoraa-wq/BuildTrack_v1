package com.example.buildtrack_v1;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DailyLog {
    public String id;
    public String projectId;
    public String engineerId;
    public String engineerName;
    public Date loggedAt;
    public String date;
    public String time;
    public String workDescription;
    public String weather;
    public String remarks;
    public Integer progressPercent;   // site-wide stage average when this log was filed (null = none known)
    public List<String> photoUrls = new ArrayList<>();
    public List<MaterialEntry> materials = new ArrayList<>();
    public List<LabourEntry> labour = new ArrayList<>();

    public DailyLog() {} // Required for Firestore

    /** Weather as reported. Logs filed before the labels were cleaned up carry a symbol prefix. */
    public String weatherForDisplay() {
        if (weather == null) return "";
        String s = weather.trim();
        int i = 0;
        while (i < s.length() && !Character.isLetterOrDigit(s.charAt(i))) i++;
        return s.substring(i);
    }

    public static class MaterialEntry {
        public String material;
        public double quantity;
        public String unit;

        public MaterialEntry() {}
        public MaterialEntry(String material, double quantity, String unit) {
            this.material = material;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    public static class LabourEntry {
        public String trade;
        public int count;

        public LabourEntry() {}
        public LabourEntry(String trade, int count) {
            this.trade = trade;
            this.count = count;
        }
    }
}