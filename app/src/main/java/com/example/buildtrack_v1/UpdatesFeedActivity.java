package com.example.buildtrack_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class UpdatesFeedActivity extends AppCompatActivity {

    private RecyclerView rvUpdates;
    private TextView tvEmpty, tvSubtitle;
    private final List<DailyLog> logFeed = new ArrayList<>();
    private final DailyLogRepository repo = new DailyLogRepository();
    private ListenerRegistration listener;
    private UpdatesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updates_feed);

        String projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        tvSubtitle = findViewById(R.id.tvSubtitle);
        if (projectName != null) tvSubtitle.setText(projectName);

        tvEmpty = findViewById(R.id.tvEmpty);
        rvUpdates = findViewById(R.id.rvUpdatesFeed);
        rvUpdates.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UpdatesAdapter();
        rvUpdates.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (projectId != null) {
            loadUpdates(projectId);
        }
    }

    private void loadUpdates(String projectId) {
        // Read-only: shows what the site engineers and the manager posted, newest first.
        listener = repo.observeLogsForProject(projectId,
                new DailyLogRepository.Callback<List<DailyLog>>() {
                    @Override
                    public void onSuccess(List<DailyLog> logs) {
                        logFeed.clear();
                        logFeed.addAll(logs);
                        adapter.notifyDataSetChanged();

                        if (logs.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText(R.string.no_photo_updates_for_unit);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        tvEmpty.setText(getString(R.string.error_loading_updates,
                                e != null ? e.getMessage() : getString(R.string.unknown_error)));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    private static String dateOf(android.content.Context c, DailyLog log) {
        String day = log.date != null ? log.date : "";
        return log.time != null && !log.time.isEmpty()
                ? c.getString(R.string.date_time_dot, day, log.time) : day;
    }

    /** Materials are half of what an engineer files, so the buyer gets them spelled out. */
    private static String materialsLine(android.content.Context c, List<DailyLog.MaterialEntry> items) {
        if (items == null || items.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (DailyLog.MaterialEntry m : items) {
            if (m == null) continue;
            parts.add(c.getString(R.string.log_material_item,
                    m.material != null ? m.material : "",
                    quantity(m.quantity),
                    m.unit != null ? m.unit : ""));
        }
        return parts.isEmpty() ? ""
                : c.getString(R.string.log_line, c.getString(R.string.log_materials),
                        android.text.TextUtils.join(", ", parts));
    }

    private static String labourLine(android.content.Context c, List<DailyLog.LabourEntry> items) {
        if (items == null || items.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (DailyLog.LabourEntry l : items) {
            if (l == null) continue;
            parts.add(c.getString(R.string.log_labour_item,
                    l.trade != null ? l.trade : "", l.count));
        }
        return parts.isEmpty() ? ""
                : c.getString(R.string.log_line, c.getString(R.string.log_labour),
                        android.text.TextUtils.join(", ", parts));
    }

    /** 60.0 bags reads as "60 bags", not "60.0 bags". */
    private static String quantity(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private class UpdatesAdapter extends RecyclerView.Adapter<UpdatesAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buyer_update, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DailyLog log = logFeed.get(position);

            holder.tvDate.setText(dateOf(holder.itemView.getContext(), log));
            holder.tvWeather.setText(log.weatherForDisplay());
            holder.tvWorkDone.setText(log.workDescription != null && !log.workDescription.isEmpty()
                    ? log.workDescription : holder.itemView.getContext().getString(R.string.daily_progress_logged));

            android.content.Context c = holder.itemView.getContext();
            String materials = materialsLine(c, log.materials);
            holder.tvMaterials.setText(materials);
            holder.tvMaterials.setVisibility(materials.isEmpty() ? View.GONE : View.VISIBLE);

            String labour = labourLine(c, log.labour);
            holder.tvLabour.setText(labour);
            holder.tvLabour.setVisibility(labour.isEmpty() ? View.GONE : View.VISIBLE);

            if (log.photoUrls != null && !log.photoUrls.isEmpty()) {
                holder.tvPhotosLabel.setVisibility(View.VISIBLE);
                holder.rvPhotos.setVisibility(View.VISIBLE);
                holder.rvPhotos.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                holder.rvPhotos.setAdapter(new PhotoThumbnailAdapter(log.photoUrls));
            } else {
                holder.tvPhotosLabel.setVisibility(View.GONE);
                holder.rvPhotos.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return logFeed.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvWeather, tvWorkDone, tvMaterials, tvLabour, tvPhotosLabel;
            RecyclerView rvPhotos;

            ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvWeather = itemView.findViewById(R.id.tvWeather);
                tvWorkDone = itemView.findViewById(R.id.tvWorkDone);
                tvMaterials = itemView.findViewById(R.id.tvMaterials);
                tvLabour = itemView.findViewById(R.id.tvLabour);
                tvPhotosLabel = itemView.findViewById(R.id.tvPhotosLabel);
                rvPhotos = itemView.findViewById(R.id.rvPhotos);
            }
        }
    }

    private static class PhotoThumbnailAdapter extends RecyclerView.Adapter<PhotoThumbnailAdapter.ViewHolder> {
        private final List<String> urls;
        PhotoThumbnailAdapter(List<String> urls) { this.urls = urls; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log_photo, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.btnRemovePhoto.setVisibility(View.GONE); // Read-only
            final String stored = urls.get(position);
            ImageHelper.showPhoto(holder.ivPhoto, stored);
            holder.ivPhoto.setOnClickListener(v -> PhotoViewerActivity.open(v.getContext(), stored));
        }

        @Override
        public int getItemCount() { return urls.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView btnRemovePhoto;

            ViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivPhoto);
                btnRemovePhoto = itemView.findViewById(R.id.btnRemovePhoto);
            }
        }
    }
}