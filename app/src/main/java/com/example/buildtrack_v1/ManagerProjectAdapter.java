package com.example.buildtrack_v1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerProjectAdapter extends RecyclerView.Adapter<ManagerProjectAdapter.VH> {

    public interface Listener {
        void onManageEngineers(DailyLogRepository.ProjectInfo project);
        void onManageBuyers(DailyLogRepository.ProjectInfo project);
        void onViewLogs(DailyLogRepository.ProjectInfo project);
        void onChat(DailyLogRepository.ProjectInfo project);
        void onOpenSiteHub(DailyLogRepository.ProjectInfo project);
    }

    private final List<DailyLogRepository.ProjectInfo> items = new ArrayList<>();
    private Map<String, DailyLogRepository.ProjectStats> stats = new HashMap<>();
    private Map<String, String> peopleNames = new HashMap<>();   // uid -> name (engineers + buyers)
    private final Listener listener;

    public ManagerProjectAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setProjects(List<DailyLogRepository.ProjectInfo> projects) {
        items.clear();
        items.addAll(projects);
        notifyDataSetChanged();
    }

    public void setStats(Map<String, DailyLogRepository.ProjectStats> s) {
        stats = s != null ? s : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setPeopleNames(Map<String, String> names) {
        peopleNames = names != null ? names : new HashMap<>();
        notifyDataSetChanged();
    }

    private String describe(Context c, List<String> uids, int labelRes, int noneRes, int countRes) {
        if (uids.isEmpty()) return c.getString(noneRes);
        List<String> names = new ArrayList<>();
        for (String uid : uids) {
            String n = peopleNames.get(uid);
            if (n != null) names.add(n);
        }
        if (names.isEmpty()) {
            return c.getResources().getQuantityString(countRes, uids.size(), uids.size());
        }
        return c.getString(labelRes, android.text.TextUtils.join(", ", names));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_manager_project_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailyLogRepository.ProjectInfo p = items.get(position);
        DailyLogRepository.ProjectStats s = stats.get(p.id);
        Context c = h.tvName.getContext();

        h.tvName.setText(p.name);

        h.tvEngineers.setText(describe(c, p.assignedEngineers, R.string.engineers_named,
                R.string.no_engineers_assigned, R.plurals.engineer_assigned_count));
        h.tvBuyers.setText(describe(c, p.assignedBuyers, R.string.buyers_named,
                R.string.no_buyers_assigned, R.plurals.buyer_assigned_count));

        // overall completion always comes from the stage bars, not from a single log
        Integer work = p.overallPercent;
        if (work == null) {
            h.tvWorkPct.setText(R.string.no_stages_reported);
            h.tvWorkPct.setTextColor(themeColor(h.tvWorkPct, com.google.android.material.R.attr.colorOnSurfaceVariant));
            h.pbWork.setProgress(0);
        } else {
            h.tvWorkPct.setText(h.tvWorkPct.getContext().getString(R.string.percent_value, work));
            h.tvWorkPct.setTextColor(themeColor(h.tvWorkPct, androidx.appcompat.R.attr.colorPrimary));
            h.pbWork.setProgress(work);
        }

        // schedule progress (from the dates the manager set)
        Integer sched = ProgressUtil.schedulePercent(p.startDate, p.endDate);
        if (sched == null) {
            h.schedRow.setVisibility(View.GONE);
        } else {
            h.schedRow.setVisibility(View.VISIBLE);
            h.tvSchedPct.setText(c.getString(R.string.percent_time_used, sched));
            h.pbSched.setProgress(sched);
            h.tvDates.setText(c.getString(R.string.date_range_to,
                    ProgressUtil.formatDate(p.startDate), ProgressUtil.formatDate(p.endDate)));
        }

        // status chip
        String status = ProgressUtil.status(work, sched);
        ProgressUtil.styleStatus(h.tvStatus, status,
                ProgressUtil.statusColor(h.tvStatus.getContext(), status));

        // last update
        if (s == null || s.lastLogAt == null) {
            h.tvLastUpdate.setText(R.string.no_logs_from_engineers);
        } else {
            String count = c.getResources().getQuantityString(R.plurals.log_count, s.logCount, s.logCount);
            h.tvLastUpdate.setText(s.lastEngineer != null
                    ? c.getString(R.string.last_update_by,
                            ProgressUtil.formatDateTime(s.lastLogAt), s.lastEngineer, count)
                    : c.getString(R.string.last_update,
                            ProgressUtil.formatDateTime(s.lastLogAt), count));
        }

        h.btnSiteHub.setOnClickListener(v -> listener.onOpenSiteHub(p));
        h.btnMore.setOnClickListener(v -> showActions(h.btnMore, p));
    }

    private static int themeColor(View v, int attr) {
        android.util.TypedValue out = new android.util.TypedValue();
        v.getContext().getTheme().resolveAttribute(attr, out, true);
        return out.data;
    }

    /** Secondary destinations live here so the card has one obvious action. */
    private void showActions(View anchor, DailyLogRepository.ProjectInfo p) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(anchor.getContext(), anchor);
        menu.inflate(R.menu.menu_project_actions);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_progress) { listener.onViewLogs(p); return true; }
            if (id == R.id.action_chat) { listener.onChat(p); return true; }
            if (id == R.id.action_engineers) { listener.onManageEngineers(p); return true; }
            if (id == R.id.action_buyers) { listener.onManageBuyers(p); return true; }
            return false;
        });
        menu.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvEngineers, tvBuyers, tvWorkPct, tvSchedPct, tvDates, tvLastUpdate;
        LinearProgressIndicator pbWork, pbSched;
        View schedRow;
        Button btnSiteHub, btnMore;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvProjectName);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvEngineers = v.findViewById(R.id.tvEngineers);
            tvBuyers = v.findViewById(R.id.tvBuyers);
            tvWorkPct = v.findViewById(R.id.tvWorkPct);
            tvSchedPct = v.findViewById(R.id.tvSchedPct);
            tvDates = v.findViewById(R.id.tvDates);
            tvLastUpdate = v.findViewById(R.id.tvLastUpdate);
            pbWork = v.findViewById(R.id.pbWork);
            pbSched = v.findViewById(R.id.pbSched);
            schedRow = v.findViewById(R.id.schedRow);
            btnSiteHub = v.findViewById(R.id.btnSiteHub);
            btnMore = v.findViewById(R.id.btnMore);
        }
    }
}