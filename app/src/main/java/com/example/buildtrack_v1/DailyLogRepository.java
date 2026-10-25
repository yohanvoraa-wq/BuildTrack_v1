package com.example.buildtrack_v1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DailyLogRepository {

    private static final int PHOTO_MAX_SIDE = 1280;
    private static final int PHOTO_JPEG_QUALITY = 80;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /** A project (site) with its people, dates and helpline. */
    public static class ProjectInfo {
        public final String id;
        public final String name;
        public final List<String> assignedEngineers;
        public final List<String> assignedBuyers;
        public final Date startDate;        // optional
        public final Date endDate;          // optional
        public final String helplineNumber; // optional, reaches the on-site engineer
        public final String location;       // optional, shown on the engineer and buyer cards
        public final String siteNote;       // the manager's latest note, shown to the engineer
        public final String unitNumber;     // optional, the buyer's flat in this project

        /** Work-stage percentages the site team set; the single source of overall progress. */
        public Map<String, Integer> phaseProgress = new LinkedHashMap<>();
        /** Mean of phaseProgress, or the legacy stored number when no stages exist yet. */
        public Integer overallPercent;
        /** The manager who runs this site; the buyer's route to a phone number. */
        public String managerId;
        /** Flats each buyer owns in this project: buyer uid -> flat numbers. */
        public Map<String, List<String>> buyerUnits = new LinkedHashMap<>();

        public ProjectInfo(String id, String name, List<String> assignedEngineers,
                           List<String> assignedBuyers, Date startDate, Date endDate,
                           String helplineNumber, String location, String siteNote,
                           String unitNumber) {
            this.id = id;
            this.name = name;
            this.assignedEngineers = assignedEngineers;
            this.assignedBuyers = assignedBuyers;
            this.startDate = startDate;
            this.endDate = endDate;
            this.helplineNumber = helplineNumber;
            this.location = location;
            this.siteNote = siteNote;
            this.unitNumber = unitNumber;
        }
    }

    /** Numbers worked out from a project's logs. */
    public static class ProjectStats {
        public int logCount;
        public Date lastLogAt;
        public String lastEngineer;
    }

    /** A user account (from Firestore users/{uid}); used for engineers and buyers. */
    public static class Engineer {
        public final String uid;
        public final String name;
        public final String email;
        public final String phone;

        public Engineer(String uid, String name, String email) {
            this(uid, name, email, null);
        }

        public Engineer(String uid, String name, String email, String phone) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
    }

    /** One chat message in a project's chat. */
    public static class ChatMessage {
        public String id;
        public String senderId;
        public String senderName;
        public String senderRole;
        public String text;
        public Date createdAt;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** yyyy-MM-dd in the device's current time zone. */
    public static String dateString(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    // ---------------------------------------------------------------- projects

    private static List<String> stringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o != null) out.add(o.toString());
            }
        }
        return out;
    }

    private ProjectInfo toProjectInfo(DocumentSnapshot d) {
        String name = d.getString("name");
        ProjectInfo p = new ProjectInfo(d.getId(), name != null ? name : "Unnamed project",
                stringList(d.get("assignedEngineers")),
                stringList(d.get("assignedBuyers")),
                d.getDate("startDate"), d.getDate("endDate"),
                d.getString("helplineNumber"), d.getString("location"),
                d.getString("managerInstructions"), d.getString("unitNumber"));
        p.phaseProgress = readPhases(d.get("phaseProgress"));
        Integer legacy = d.get("overallProgress") != null
                ? ((Number) d.get("overallProgress")).intValue() : null;
        p.overallPercent = overallFrom(p.phaseProgress, legacy);
        p.managerId = d.getString("managerId");
        Object bu = d.get("buyerUnits");
        if (bu instanceof Map) {
            for (Map.Entry<?, ?> en : ((Map<?, ?>) bu).entrySet()) {
                p.buyerUnits.put(String.valueOf(en.getKey()), stringList(en.getValue()));
            }
        }
        return p;
    }

    /** Work stages a new project starts with. Existing projects keep whatever they already have. */
    public static final List<String> DEFAULT_PHASES = java.util.Collections.unmodifiableList(
            new ArrayList<>(java.util.Arrays.asList("Foundation", "Framing & Structure",
                    "Masonry", "Plumbing & Electrical", "Finishing", "Handover")));

    /** Overall = mean of the stage bars. Only falls back to a stored number when there are none. */
    public static Integer overallFrom(Map<String, Integer> phases, Integer legacy) {
        if (phases == null || phases.isEmpty()) return legacy;
        int total = 0, count = 0;
        for (Integer v : phases.values()) {
            if (v != null) { total += v; count++; }
        }
        return count == 0 ? legacy : Math.round((float) total / count);
    }

    private static Map<String, Integer> readPhases(Object raw) {
        Map<String, Integer> found = new HashMap<>();
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                if (e.getKey() != null && e.getValue() instanceof Number) {
                    found.put(e.getKey().toString(), ((Number) e.getValue()).intValue());
                }
            }
        }
        // Firestore hands back map keys in any order, so put the stages back in build sequence.
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String phase : DEFAULT_PHASES) {
            if (found.containsKey(phase)) out.put(phase, found.remove(phase));
        }
        List<String> custom = new ArrayList<>(found.keySet());
        Collections.sort(custom);
        for (String phase : custom) out.put(phase, found.get(phase));
        return out;
    }

    /** The stage list to show: the default build sequence, then any stage this project added. */
    public static Map<String, Integer> phasesForEditor(Map<String, Integer> stored) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String phase : DEFAULT_PHASES) {
            Integer value = stored == null ? null : stored.get(phase);
            out.put(phase, value == null ? 0 : value);
        }
        if (stored != null) {
            for (Map.Entry<String, Integer> e : stored.entrySet()) {
                if (!out.containsKey(e.getKey())) out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /** Saves the stage bars and re-points the overall number at their mean. */
    public void setPhaseProgress(String projectId, Map<String, Integer> phases, Callback<Void> cb) {
        Integer overall = overallFrom(phases, null);
        Map<String, Object> data = new HashMap<>();
        data.put("phaseProgress", new HashMap<>(phases));
        data.put("overallProgress", overall != null ? overall : 0);
        db.collection("projects").document(projectId)
                .update(data)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Live list of the projects this manager created. Call remove() in onStop(). */
    public ListenerRegistration observeManagerProjects(String managerUid,
                                                       Callback<List<ProjectInfo>> cb) {
        return observeProjectsFor(p -> p.managerId != null && p.managerId.equals(managerUid), cb);
    }

    /**
     * Live list of the projects this buyer owns a unit in. Matches the account uid or the email
     * an older assignment stored. The whole collection is filtered here so no Firestore index is
     * needed. Call remove() in onStop().
     */
    public ListenerRegistration observeBuyerProjects(String uid, String email,
                                                     Callback<List<ProjectInfo>> cb) {
        return observeProjectsFor(p -> listContains(p.assignedBuyers, uid, email), cb);
    }

    /** Live list of projects the manager put this engineer on, by uid or readable ENG- id. */
    public ListenerRegistration observeEngineerProjects(String uid, String engineerCode,
                                                        Callback<List<ProjectInfo>> cb) {
        return observeProjectsFor(p -> listContains(p.assignedEngineers, uid, engineerCode), cb);
    }

    private interface ProjectFilter {
        boolean matches(ProjectInfo p);
    }

    private ListenerRegistration observeProjectsFor(ProjectFilter filter,
                                                    Callback<List<ProjectInfo>> cb) {
        return db.collection("projects")
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) { cb.onError(e); return; }
                    List<ProjectInfo> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        ProjectInfo p = toProjectInfo(d);
                        if (filter.matches(p)) out.add(p);
                    }
                    cb.onSuccess(out);
                });
    }

    private static boolean listContains(List<String> values, String uid, String alias) {
        for (String v : values) {
            if (v == null) continue;
            String clean = v.trim();
            if (clean.equalsIgnoreCase(uid)) return true;
            if (alias != null && !alias.isEmpty() && clean.equalsIgnoreCase(alias.trim())) return true;
        }
        return false;
    }

    /** One project, once. */
    public void getProject(String projectId, Callback<ProjectInfo> cb) {
        db.collection("projects").document(projectId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onError(new Exception("Project not found")); return; }
                    cb.onSuccess(toProjectInfo(d));
                })
                .addOnFailureListener(cb::onError);
    }

    /** Live per-project numbers (log count, last update) from all logs. */
    public ListenerRegistration observeProjectStats(Callback<Map<String, ProjectStats>> cb) {
        return db.collection("dailyLogs")
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) { cb.onError(e); return; }
                    Map<String, ProjectStats> out = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        DailyLog l = d.toObject(DailyLog.class);
                        if (l == null || l.projectId == null) continue;
                        ProjectStats s = out.get(l.projectId);
                        if (s == null) { s = new ProjectStats(); out.put(l.projectId, s); }
                        s.logCount++;
                        if (l.loggedAt != null
                                && (s.lastLogAt == null || l.loggedAt.after(s.lastLogAt))) {
                            s.lastLogAt = l.loggedAt;
                            s.lastEngineer = l.engineerName;
                        }
                    }
                    cb.onSuccess(out);
                });
    }

    /** Creates a project with nobody assigned yet. Dates, helpline and location are optional. */
    public void createProject(String name, String managerId, Date startDate, Date endDate,
                              String helplineNumber, String location, Callback<Void> cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("managerId", managerId);
        data.put("assignedEngineers", new ArrayList<String>());
        data.put("assignedBuyers", new ArrayList<String>());
        Map<String, Integer> phases = new HashMap<>();
        for (String phase : DEFAULT_PHASES) phases.put(phase, 0);
        data.put("phaseProgress", phases);
        data.put("overallProgress", 0);
        data.put("createdAt", FieldValue.serverTimestamp());
        if (startDate != null) data.put("startDate", startDate);
        if (endDate != null) data.put("endDate", endDate);
        if (location != null && !location.trim().isEmpty()) {
            data.put("location", location.trim());
        }
        if (helplineNumber != null && !helplineNumber.trim().isEmpty()) {
            data.put("helplineNumber", helplineNumber.trim());
        }
        db.collection("projects")
                .add(data)
                .addOnSuccessListener(ref -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Replaces the project's engineer list (adds and removes). */
    public void setAssignedEngineers(String projectId, List<String> uids, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("assignedEngineers", uids)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Replaces the project's buyer list (adds and removes). */
    public void setAssignedBuyers(String projectId, List<String> uids, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("assignedBuyers", uids)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Sets the phone number buyers use to reach the on-site engineer. */
    public void setHelpline(String projectId, String number, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("helplineNumber", number)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Sets the flat number the buyer owns in this project. */
    public void setUnitNumber(String projectId, String unitNumber, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("unitNumber", unitNumber)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Adds one more flat to what this buyer owns in the project (a buyer can own several). */
    public void addBuyerUnit(String projectId, String buyerUid, String unit, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("buyerUnits." + buyerUid, FieldValue.arrayUnion(unit))
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Takes one flat away from a buyer. */
    public void removeBuyerUnit(String projectId, String buyerUid, String unit, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("buyerUnits." + buyerUid, FieldValue.arrayRemove(unit))
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Replaces the note every assigned engineer sees on their project card. */
    public void setSiteNote(String projectId, String note, Callback<Void> cb) {
        db.collection("projects").document(projectId)
                .update("managerInstructions", note)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** Looks up a saved account by its uid, email, or derived engineer id code. */
    public void findUser(String codeOrEmail, String role, Callback<Engineer> cb) {
        String needle = codeOrEmail.trim();
        loadUsersByRole(role, new Callback<List<Engineer>>() {
            @Override public void onSuccess(List<Engineer> people) {
                for (Engineer p : people) {
                    if (needle.equalsIgnoreCase(p.uid)
                            || (p.email != null && needle.equalsIgnoreCase(p.email.trim()))) {
                        cb.onSuccess(p);
                        return;
                    }
                }
                if (role.equals("engineer")) {
                    String normalized = EngineerIdentity.normalize(needle);
                    for (Engineer p : people) {
                        if (normalized.equalsIgnoreCase(EngineerIdentity.fromUid(p.uid))) {
                            cb.onSuccess(p);
                            return;
                        }
                    }
                }
                cb.onError(new Exception("No " + role + " account matches \"" + needle + "\""));
            }

            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    // ---------------------------------------------------------------- users

    /** All users whose saved role equals the given role ("engineer" or "buyer"). */
    public void loadUsersByRole(String role, Callback<List<Engineer>> cb) {
        db.collection("users").whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Engineer> out = new ArrayList<>();
                    for (DocumentSnapshot child : snapshot.getDocuments()) {
                        UserInfo u = child.toObject(UserInfo.class);
                        if (u != null && child.getId() != null) {
                            String name = u.getName() != null ? u.getName() : "(no name)";
                            String email = u.getEmail() != null ? u.getEmail() : "";
                            out.add(new Engineer(child.getId(), name, email, u.getPhone()));
                        }
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(cb::onError);
    }

    /** Profiles for a set of account uids, in the order given. Missing docs are skipped. */
    public void loadProfiles(List<String> uids, Callback<List<Engineer>> cb) {
        if (uids == null || uids.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }
        final Map<String, Engineer> found = new HashMap<>();
        final int[] remaining = {uids.size()};
        final Runnable finish = () -> {
            if (remaining[0] > 0) return;
            List<Engineer> out = new ArrayList<>();
            for (String uid : uids) {
                Engineer e = found.get(uid);
                if (e != null) out.add(e);
            }
            cb.onSuccess(out);
        };
        for (String uid : uids) {
            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        remaining[0]--;
                        DocumentSnapshot doc = task.getResult();
                        if (task.isSuccessful() && doc != null && doc.exists()) {
                            UserInfo u = doc.toObject(UserInfo.class);
                            if (u != null) {
                                found.put(uid, new Engineer(uid,
                                        u.getName() != null ? u.getName() : "(no name)",
                                        u.getEmail() != null ? u.getEmail() : "",
                                        u.getPhone()));
                            }
                        }
                        finish.run();
                    });
        }
    }

    /** Store the signed-in user's contact number. */
    public void savePhone(String uid, String phone, Callback<Void> cb) {
        db.collection("users").document(uid).update("phone", phone)
                .addOnSuccessListener(r -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /** All users whose saved role is "engineer". */
    public void loadEngineers(Callback<List<Engineer>> cb) {
        loadUsersByRole("engineer", cb);
    }

    /** The signed-in user's saved profile (name, email, role). */
    public void loadProfile(String uid, Callback<UserInfo> cb) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> cb.onSuccess(doc.exists() ? doc.toObject(UserInfo.class) : null))
                .addOnFailureListener(cb::onError);
    }

    // ---------------------------------------------------------------- chat

    /** Live chat messages for a project, oldest first. Call remove() in onStop(). */
    public ListenerRegistration observeMessages(String projectId, Callback<List<ChatMessage>> cb) {
        return db.collection("projects").document(projectId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) { cb.onError(e); return; }
                    List<ChatMessage> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        ChatMessage m = new ChatMessage();
                        m.id = d.getId();
                        m.senderId = d.getString("senderId");
                        m.senderName = d.getString("senderName");
                        m.senderRole = d.getString("senderRole");
                        m.text = d.getString("text");
                        Date t = d.getDate("createdAt",
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        m.createdAt = t != null ? t : new Date();
                        out.add(m);
                    }
                    cb.onSuccess(out);
                });
    }

    /** Sends one message to the project's chat. */
    public void sendMessage(String projectId, String senderId, String senderName,
                            String senderRole, String text, Callback<Void> cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", senderId);
        data.put("senderName", senderName);
        data.put("senderRole", senderRole);
        data.put("text", text);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection("projects").document(projectId).collection("messages")
                .add(data)
                .addOnSuccessListener(ref -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // ---------------------------------------------------------------- logs

    /** Every log from every engineer on this project, newest first (manager/buyer view). */
    public void getAllLogsForProject(String projectId, Callback<List<DailyLog>> cb) {
        db.collection("dailyLogs")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(qs -> cb.onSuccess(toSortedLogs(qs)))
                .addOnFailureListener(cb::onError);
    }

    // --------------------------------------------------------------- engineer side

    /**
     * Live entries this engineer filed on this project, newest first. Call remove() in onStop().
     */
    public ListenerRegistration observeLogsForEngineer(String projectId, String engineerId,
                                                       Callback<List<DailyLog>> cb) {
        // Two equality filters need no composite index; the sort happens in toSortedLogs.
        return observeLogs(db.collection("dailyLogs")
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("engineerId", engineerId), cb);
    }

    /** Live entries every engineer filed on this project, newest first. */
    public ListenerRegistration observeLogsForProject(String projectId, Callback<List<DailyLog>> cb) {
        return observeLogs(db.collection("dailyLogs").whereEqualTo("projectId", projectId), cb);
    }

    private ListenerRegistration observeLogs(Query base, Callback<List<DailyLog>> cb) {
        return base.addSnapshotListener((qs, e) -> {
            if (e != null || qs == null) { cb.onError(e); return; }
            cb.onSuccess(toSortedLogs(qs));
        });
    }

    private static List<DailyLog> toSortedLogs(QuerySnapshot qs) {
        List<DailyLog> out = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) {
            DailyLog l = d.toObject(DailyLog.class);
            if (l != null) { l.id = d.getId(); out.add(l); }
        }
        // sorted here so no Firestore index is needed
        Collections.sort(out, (a, b) -> {
            if (a.loggedAt == null && b.loggedAt == null) return 0;
            if (a.loggedAt == null) return 1;
            if (b.loggedAt == null) return -1;
            return b.loggedAt.compareTo(a.loggedAt);
        });
        return out;
    }

    /** Every submission gets its own auto-generated document id. */
    public void saveLog(DailyLog log, Callback<Void> cb) {
        db.collection("dailyLogs")
                .add(log)
                .addOnSuccessListener(ref -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Scales each chosen photo down, stores it as its own Firestore document, then saves the log
     * with "fs:<docId>" references. One document per photo keeps the log itself small.
     */
    public void saveLogWithPhotos(DailyLog log, List<Uri> photos, Callback<Void> cb) {
        if (photos == null || photos.isEmpty()) {
            log.photoUrls = new ArrayList<>();
            saveLog(log, cb);
            return;
        }

        Context ctx = FirebaseApp.getInstance().getApplicationContext();
        UploadBatch batch = new UploadBatch(photos.size(), log, cb);
        String stamp = String.valueOf(System.currentTimeMillis());

        for (int i = 0; i < photos.size(); i++) {
            final int index = i;
            File tmp = new File(ctx.getCacheDir(), "logphoto_" + stamp + "_" + index + ".jpg");
            String encoded;
            try {
                writeScaledJpeg(ctx, photos.get(index), tmp);
                encoded = encodeBase64(tmp);
            } catch (Exception e) {
                tmp.delete();
                batch.failed(e);
                continue;
            }

            // The project has no Cloud Storage bucket, so each photo becomes its own Firestore
            // document (well under the 1 MiB limit at this size) and the log stores "fs:<docId>".
            Map<String, Object> photo = new HashMap<>();
            photo.put("projectId", log.projectId);
            photo.put("data", encoded);
            photo.put("createdAt", FieldValue.serverTimestamp());
            db.collection("sitePhotos").add(photo)
                    .addOnSuccessListener(ref -> {
                        tmp.delete();
                        batch.succeeded("fs:" + ref.getId());
                    })
                    .addOnFailureListener(e -> {
                        tmp.delete();
                        batch.failed(e);
                    });
        }
    }

    static String encodeBase64(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int read = 0;
            while (read < bytes.length) {
                int n = in.read(bytes, read, bytes.length - read);
                if (n < 0) break;
                read += n;
            }
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /** Decodes a gallery/camera image once at reduced size and writes it out as a JPEG. */
    static void writeScaledJpeg(Context ctx, Uri src, File out) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = ctx.getContentResolver().openInputStream(src)) {
            if (in == null) throw new Exception("Photo could not be opened");
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > PHOTO_MAX_SIDE) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = ctx.getContentResolver().openInputStream(src)) {
            if (in == null) throw new Exception("Photo could not be opened");
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new Exception("Photo could not be decoded");

        try (OutputStream os = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, os);
        } finally {
            bmp.recycle();
        }
    }

    /** Waits for every photo upload to settle, then saves the log once. */
    private final class UploadBatch {
        private final int total;
        private final DailyLog log;
        private final Callback<Void> cb;
        private final List<String> urls = new ArrayList<>();
        private int done;
        private Exception firstError;

        UploadBatch(int total, DailyLog log, Callback<Void> cb) {
            this.total = total;
            this.log = log;
            this.cb = cb;
        }

        synchronized void succeeded(String url) {
            urls.add(url);
            if (++done == total) settle();
        }

        synchronized void failed(Exception e) {
            if (firstError == null) firstError = e;
            if (++done == total) settle();
        }

        private void settle() {
            log.photoUrls = new ArrayList<>(urls);
            if (!urls.isEmpty()) {
                saveLog(log, cb);
            } else {
                cb.onError(firstError != null ? firstError
                        : new Exception("Could not upload the photos"));
            }
        }
    }
}