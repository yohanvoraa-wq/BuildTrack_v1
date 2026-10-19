package com.example.buildtrack_v1;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DailyLog {

    @Exclude public String id;          // Firestore document id, not stored as a field
    public String projectId;
    public String engineerId;
    public String engineerName;
    public String date;                 // yyyy-MM-dd from the device clock, for filtering/grouping by day
    public Date loggedAt;               // exact device date and time of submission
    @ServerTimestamp public Date createdAt; // server time, used for ordering and as a cross-check
    public List<MaterialEntry> materials = new ArrayList<>(); // used since the last entry
    public List<LabourEntry> labour = new ArrayList<>();      // headcount at the time of this entry

    public DailyLog() { }               // required by Firestore

    public static class MaterialEntry {
        public String name;
        public double quantity;
        public String unit;

        public MaterialEntry() { }

        public MaterialEntry(String name, double quantity, String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    public static class LabourEntry {
        public String trade;
        public int count;

        public LabourEntry() { }

        public LabourEntry(String trade, int count) {
            this.trade = trade;
            this.count = count;
        }
    }
}
