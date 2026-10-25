package com.example.buildtrack_v1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LogHistoryAdapter extends RecyclerView.Adapter<LogHistoryAdapter.LogViewHolder> {

    private final List<DailyLog> logList;

    public LogHistoryAdapter(List<DailyLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        DailyLog log = logList.get(position);
        Context context = holder.itemView.getContext();

        // 1. Date & Time
        String dateDisplay = (log.date != null ? log.date : "") +
                (log.time != null ? " • " + log.time : "");
        holder.tvDate.setText(dateDisplay);

        // 2. Weather
        holder.tvWeather.setText(log.weatherForDisplay());

        // 3. Work Completed
        if (log.workDescription != null && !log.workDescription.isEmpty()) {
            holder.tvWorkDescription.setText(log.workDescription);
            holder.tvWorkDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvWorkDescription.setText(R.string.no_work_description);
        }

        // 4. Materials
        if (log.materials != null && !log.materials.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (DailyLog.MaterialEntry m : log.materials) {
                sb.append("• ").append(m.material).append(": ")
                        .append(m.quantity).append(" ").append(m.unit).append("\n");
            }
            holder.tvMaterials.setText(sb.toString().trim());
        } else {
            holder.tvMaterials.setText(R.string.none);
        }

        // 5. Labour
        if (log.labour != null && !log.labour.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (DailyLog.LabourEntry l : log.labour) {
                sb.append(l.trade).append(": ").append(l.count).append("  |  ");
            }
            String res = sb.toString();
            if (res.endsWith("  |  ")) {
                res = res.substring(0, res.length() - 5);
            }
            holder.tvLabour.setText(context.getString(R.string.bullet_line, res));
        } else {
            holder.tvLabour.setText(R.string.none);
        }

        // 6. Site Photos List
        if (log.photoUrls != null && !log.photoUrls.isEmpty()) {
            holder.tvPhotosLabel.setVisibility(View.VISIBLE);
            holder.rvItemPhotos.setVisibility(View.VISIBLE);
            holder.rvItemPhotos.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            holder.rvItemPhotos.setAdapter(new PhotoThumbnailAdapter(log.photoUrls));
        } else {
            holder.tvPhotosLabel.setVisibility(View.GONE);
            holder.rvItemPhotos.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvWeather, tvWorkDescription, tvMaterials, tvLabour, tvPhotosLabel;
        RecyclerView rvItemPhotos;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeather = itemView.findViewById(R.id.tvWeather);
            tvWorkDescription = itemView.findViewById(R.id.tvWorkDescription);
            tvMaterials = itemView.findViewById(R.id.tvMaterials);
            tvLabour = itemView.findViewById(R.id.tvLabour);
            tvPhotosLabel = itemView.findViewById(R.id.tvPhotosLabel);
            rvItemPhotos = itemView.findViewById(R.id.rvItemPhotos);
        }
    }

    // Horizontal photo viewer for history items
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
            holder.btnRemovePhoto.setVisibility(View.GONE); // No delete button in history view!
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