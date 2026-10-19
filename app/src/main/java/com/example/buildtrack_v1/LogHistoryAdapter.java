package com.example.buildtrack_v1;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class LogHistoryAdapter extends RecyclerView.Adapter<LogHistoryAdapter.VH> {

    private final List<DailyLog> logs;

    public LogHistoryAdapter(List<DailyLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailyLog log = logs.get(position);
        Context ctx = h.itemView.getContext();

        // Date and time are formatted with the device's own locale and 12/24-hour setting
        if (log.loggedAt != null) {
            h.tvDate.setText(DateFormat.getMediumDateFormat(ctx).format(log.loggedAt)
                    + " · " + DateFormat.getTimeFormat(ctx).format(log.loggedAt));
        } else {
            h.tvDate.setText(log.date);
        }

        StringBuilder m = new StringBuilder("Materials: ");
        if (log.materials.isEmpty()) m.append("none");
        for (int i = 0; i < log.materials.size(); i++) {
            DailyLog.MaterialEntry e = log.materials.get(i);
            if (i > 0) m.append(", ");
            m.append(e.name).append(" ")
                    .append(String.format(Locale.US, "%.2f", e.quantity).replaceAll("\\.?0+$", ""))
                    .append(" ").append(e.unit);
        }
        h.tvMaterials.setText(m.toString());

        StringBuilder l = new StringBuilder("Labour: ");
        if (log.labour.isEmpty()) l.append("none");
        for (int i = 0; i < log.labour.size(); i++) {
            DailyLog.LabourEntry e = log.labour.get(i);
            if (i > 0) l.append(", ");
            l.append(e.trade).append(" ").append(e.count);
        }
        h.tvLabour.setText(l.toString());
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvMaterials, tvLabour;

        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvMaterials = v.findViewById(R.id.tvMaterials);
            tvLabour = v.findViewById(R.id.tvLabour);
        }
    }
}
