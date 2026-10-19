package com.example.buildtrack_v1;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyLogRepository {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class AssignedProject {
        public final String id;
        public final String name;

        public AssignedProject(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** yyyy-MM-dd in the device's current time zone. */
    public static String dateString(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    /**
     * Live list of projects this engineer is assigned to. The callback fires again whenever the
     * manager assigns or removes him. Call remove() on the returned registration in onStop().
     */
    public ListenerRegistration observeAssignedProjects(String engineerId,
                                                        Callback<List<AssignedProject>> cb) {
        return db.collection("projects")
                .whereArrayContains("assignedEngineers", engineerId)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) { cb.onError(e); return; }
                    List<AssignedProject> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String name = d.getString("name");
                        out.add(new AssignedProject(d.getId(), name != null ? name : "Unnamed project"));
                    }
                    cb.onSuccess(out);
                });
    }

    /** How many entries this engineer has logged today (device date) for this project. */
    public void countTodayEntries(String projectId, String engineerId, Callback<Integer> cb) {
        db.collection("dailyLogs")
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("engineerId", engineerId)
                .whereEqualTo("date", dateString(new Date()))
                .get()
                .addOnSuccessListener(qs -> cb.onSuccess(qs.size()))
                .addOnFailureListener(cb::onError);
    }

    /** Every submission gets its own auto-generated document id. */
    public void saveLog(DailyLog log, Callback<Void> cb) {
        db.collection("dailyLogs")
                .add(log)
                .addOnSuccessListener(ref -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** This engineer's entries for this project, newest first. */
    public void getLogs(String projectId, String engineerId, Callback<List<DailyLog>> cb) {
        db.collection("dailyLogs")
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("engineerId", engineerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    List<DailyLog> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        DailyLog l = d.toObject(DailyLog.class);
                        if (l != null) { l.id = d.getId(); out.add(l); }
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(cb::onError);
    }
}
