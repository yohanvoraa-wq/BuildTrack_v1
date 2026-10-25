package com.example.buildtrack_v1;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Project documents (plans, permits, agreements, receipts...). The manager uploads them, the buyer
 * opens them. File bytes live in "documentFiles" (one doc each) and the list in "projectDocuments",
 * so listing the documents never downloads the files themselves.
 */
public class DocumentsActivity extends AppCompatActivity {

    /** Firestore documents are capped at 1 MiB; base64 adds a third, so keep files under this. */
    private static final long MAX_FILE_BYTES = 700 * 1024;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Doc> docs = new ArrayList<>();
    private DocAdapter adapter;
    private TextView tvEmpty;
    private Button btnAdd;
    private ListenerRegistration listener;
    private ActivityResultLauncher<String> filePicker;

    private String projectId, projectName;
    private boolean isManager;

    static class Doc {
        String id, title, fileName, mimeType, fileId;
        long size;
        Date createdAt;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Same screen for both roles, so pick the matching colour theme before anything inflates.
        isManager = "manager".equals(getIntent().getStringExtra("role"));
        setTheme(isManager ? R.style.Theme_BuildTrack_v1_Manager : R.style.Theme_BuildTrack_v1_Buyer);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        if (projectName != null) ((TextView) findViewById(R.id.tvSubtitle)).setText(projectName);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvEmpty = findViewById(R.id.tvEmpty);
        btnAdd = findViewById(R.id.btnAddDocument);

        RecyclerView rv = findViewById(R.id.rvDocuments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DocAdapter();
        rv.setAdapter(adapter);

        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) askTitleThenUpload(uri);
        });

        btnAdd.setVisibility(isManager ? View.VISIBLE : View.GONE);
        btnAdd.setOnClickListener(v -> filePicker.launch("*/*"));

        if (projectId == null) {
            tvEmpty.setText("No project selected");
            return;
        }
        listen();
    }

    private void listen() {
        listener = db.collection("projectDocuments").whereEqualTo("projectId", projectId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Could not load documents");
                        return;
                    }
                    docs.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        Doc x = new Doc();
                        x.id = d.getId();
                        x.title = d.getString("title");
                        x.fileName = d.getString("fileName");
                        x.mimeType = d.getString("mimeType");
                        x.fileId = d.getString("fileId");
                        Long sz = d.getLong("size");
                        x.size = sz != null ? sz : 0;
                        com.google.firebase.Timestamp ts = d.getTimestamp("createdAt");
                        x.createdAt = ts != null ? ts.toDate() : new Date();
                        docs.add(x);
                    }
                    Collections.sort(docs, (a, b) -> b.createdAt.compareTo(a.createdAt));
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);
                    tvEmpty.setText(isManager
                            ? "No documents yet. Tap “Add document” to share plans, permits or receipts with the buyer."
                            : "Your manager hasn't shared any documents yet.");
                });
    }

    // ---------------------------------------------------------------- upload (manager)

    private void askTitleThenUpload(Uri uri) {
        String name = displayName(uri);
        EditText et = new EditText(this);
        et.setHint("Document title");
        et.setText(stripExtension(name));
        et.setSelectAllOnFocus(true);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Name this document")
                .setView(et)
                .setPositiveButton("Upload", (d, w) -> {
                    String title = et.getText().toString().trim();
                    upload(uri, name, title.isEmpty() ? stripExtension(name) : title);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void upload(Uri uri, String fileName, String title) {
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";
        final String mimeType = mime;
        final boolean isImage = mimeType.startsWith("image/");

        final byte[] bytes;
        try {
            if (isImage) {
                // Images are shrunk to a JPEG, exactly like site photos.
                File tmp = new File(getCacheDir(), "doc_" + System.currentTimeMillis() + ".jpg");
                DailyLogRepository.writeScaledJpeg(this, uri, tmp);
                bytes = readAll(new java.io.FileInputStream(tmp));
                tmp.delete();
            } else {
                bytes = readAll(getContentResolver().openInputStream(uri));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not read that file", Toast.LENGTH_LONG).show();
            return;
        }
        if (bytes.length > MAX_FILE_BYTES) {
            Toast.makeText(this, "File is too large. Keep documents under 700 KB (compress the PDF first).",
                    Toast.LENGTH_LONG).show();
            return;
        }

        btnAdd.setEnabled(false);
        btnAdd.setText("Uploading…");

        Map<String, Object> file = new HashMap<>();
        file.put("projectId", projectId);
        file.put("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
        file.put("createdAt", FieldValue.serverTimestamp());

        db.collection("documentFiles").add(file)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("projectId", projectId);
                    meta.put("title", title);
                    meta.put("fileName", fileName);
                    meta.put("mimeType", isImage ? "image/jpeg" : mimeType);
                    meta.put("size", (long) bytes.length);
                    meta.put("fileId", ref.getId());
                    meta.put("uploadedBy", FirebaseAuth.getInstance().getUid());
                    meta.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("projectDocuments").add(meta)
                            .addOnSuccessListener(r -> {
                                resetAddButton();
                                Toast.makeText(this, "Document shared with the buyer", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> uploadFailed(e));
                })
                .addOnFailureListener(this::uploadFailed);
    }

    private void uploadFailed(Exception e) {
        resetAddButton();
        Toast.makeText(this, "Upload failed: " + (e != null ? e.getMessage() : "unknown error"),
                Toast.LENGTH_LONG).show();
    }

    private void resetAddButton() {
        btnAdd.setEnabled(true);
        btnAdd.setText("Add document");
    }

    private void confirmDelete(Doc d) {
        new AlertDialog.Builder(this)
                .setTitle("Delete document?")
                .setMessage("“" + d.title + "” will disappear for the buyer too.")
                .setPositiveButton("Delete", (x, w) -> {
                    if (d.fileId != null) db.collection("documentFiles").document(d.fileId).delete();
                    db.collection("projectDocuments").document(d.id).delete();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------- open (everyone)

    private void open(Doc d) {
        if (d.fileId == null) return;
        if (d.mimeType != null && d.mimeType.startsWith("image/")) {
            PhotoViewerActivity.open(this, "doc:" + d.fileId);
            return;
        }
        Toast.makeText(this, "Opening…", Toast.LENGTH_SHORT).show();
        db.collection("documentFiles").document(d.fileId).get()
                .addOnSuccessListener(snap -> {
                    String data = snap.getString("data");
                    if (data == null) {
                        Toast.makeText(this, "This file is no longer available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        File dir = new File(getCacheDir(), "docs");
                        dir.mkdirs();
                        File out = new File(dir, safeName(d.fileName != null ? d.fileName : d.title));
                        try (FileOutputStream os = new FileOutputStream(out)) {
                            os.write(Base64.decode(data, Base64.NO_WRAP));
                        }
                        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", out);
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        view.setDataAndType(uri, d.mimeType != null ? d.mimeType : "*/*");
                        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(view, "Open with"));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "No app on this phone can open this file type", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not open file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not download the file", Toast.LENGTH_LONG).show());
    }

    // ---------------------------------------------------------------- helpers

    private static byte[] readAll(InputStream in) throws Exception {
        if (in == null) throw new Exception("unreadable");
        try (InputStream is = in) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 20 * 1024 * 1024) throw new Exception("too big");
            }
            return out.toByteArray();
        }
    }

    private String displayName(Uri uri) {
        String name = null;
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) name = c.getString(i);
            }
        } catch (Exception ignored) { }
        return name != null ? name : "document";
    }

    private static String stripExtension(String n) {
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String safeName(String n) {
        return n.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String icon(String mime) {
        if (mime == null) return "📄";
        if (mime.startsWith("image/")) return "🖼";
        if (mime.contains("pdf")) return "📕";
        if (mime.contains("sheet") || mime.contains("excel") || mime.contains("csv")) return "📊";
        if (mime.contains("word") || mime.contains("text")) return "📝";
        return "📄";
    }

    private static String sizeText(long b) {
        return b >= 1024 * 1024 ? String.format(Locale.getDefault(), "%.1f MB", b / 1048576.0)
                : Math.max(1, b / 1024) + " KB";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    private class DocAdapter extends RecyclerView.Adapter<DocAdapter.VH> {
        private final SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_document, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int i) {
            Doc d = docs.get(i);
            h.icon.setText(icon(d.mimeType));
            h.title.setText(d.title != null ? d.title : d.fileName);
            h.meta.setText(fmt.format(d.createdAt) + " • " + sizeText(d.size));
            h.itemView.setOnClickListener(v -> open(d));
            h.delete.setVisibility(isManager ? View.VISIBLE : View.GONE);
            h.delete.setOnClickListener(v -> confirmDelete(d));
        }

        @Override public int getItemCount() { return docs.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView icon, title, meta, delete;
            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.tvDocIcon);
                title = v.findViewById(R.id.tvDocTitle);
                meta = v.findViewById(R.id.tvDocMeta);
                delete = v.findViewById(R.id.btnDeleteDoc);
            }
        }
    }
}
