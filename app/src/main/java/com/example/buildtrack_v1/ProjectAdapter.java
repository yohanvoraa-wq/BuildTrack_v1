package com.example.buildtrack_v1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.VH> {

    public interface Listener {
        void onLog(DailyLogRepository.AssignedProject project);
        void onHistory(DailyLogRepository.AssignedProject project);
    }

    private final List<DailyLogRepository.AssignedProject> items = new ArrayList<>();
    private final Map<String, Integer> todayCounts = new HashMap<>();
    private final Listener listener;

    public ProjectAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setProjects(List<DailyLogRepository.AssignedProject> projects) {
        items.clear();
        items.addAll(projects);
        notifyDataSetChanged();
    }

    public List<DailyLogRepository.AssignedProject> getProjects() {
        return items;
    }

    public void setTodayCount(String projectId, int count) {
        todayCounts.put(projectId, count);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(projectId)) { notifyItemChanged(i); break; }
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_project_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailyLogRepository.AssignedProject p = items.get(position);
        h.tvName.setText(p.name);

        Integer n = todayCounts.get(p.id);
        if (n == null) h.tvToday.setText("Checking today's entries...");
        else if (n == 0) h.tvToday.setText("No entries today");
        else h.tvToday.setText(n + (n == 1 ? " entry today" : " entries today"));

        h.btnLog.setOnClickListener(v -> listener.onLog(p));
        h.btnHistory.setOnClickListener(v -> listener.onHistory(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvToday;
        Button btnLog, btnHistory;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvProjectName);
            tvToday = v.findViewById(R.id.tvEntriesToday);
            btnLog = v.findViewById(R.id.btnLog);
            btnHistory = v.findViewById(R.id.btnHistory);
        }
    }
}
