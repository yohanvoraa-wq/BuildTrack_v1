package com.example.buildtrack_v1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public static class ProjectItem {
        public String id;
        public String name;
        public String managerName;
        public String location;
        public String deadline;
        public String status;
        public String managerInstructions;

        public ProjectItem(String id, String name, String managerName, String location, String deadline, String status, String managerInstructions) {
            this.id = id;
            this.name = name;
            this.managerName = managerName;
            this.location = location;
            this.deadline = deadline;
            this.status = status;
            this.managerInstructions = managerInstructions;
        }
    }

    private final List<ProjectItem> projectList;
    private final String currentEngineerName;

    public ProjectAdapter(List<ProjectItem> projectList, String currentEngineerName) {
        this.projectList = projectList;
        this.currentEngineerName = currentEngineerName;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectItem project = projectList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvProjectName.setText(project.name != null ? project.name
                : context.getString(R.string.assigned_project));
        holder.tvAssignedBy.setText(context.getString(R.string.assigned_by,
                project.managerName != null ? project.managerName
                        : context.getString(R.string.manager_word)));
        holder.tvLocation.setText(context.getString(R.string.site_location,
                project.location != null ? project.location : context.getString(R.string.on_site)));
        holder.tvDueDate.setText(context.getString(R.string.due_date,
                project.deadline != null ? project.deadline
                        : context.getString(R.string.to_be_decided)));
        String status = project.status != null ? project.status : context.getString(R.string.status_active);
        ProgressUtil.styleStatus(holder.tvStatus, status, ProgressUtil.statusColor(context, status));

        if (holder.tvManagerDirectives != null) {
            if (project.managerInstructions != null && !project.managerInstructions.trim().isEmpty()) {
                holder.tvManagerDirectives.setVisibility(View.VISIBLE);
                holder.tvManagerDirectives.setText(
                        context.getString(R.string.manager_note, project.managerInstructions));
            } else {
                holder.tvManagerDirectives.setVisibility(View.GONE);
            }
        }

        View.OnClickListener openLogListener = v -> {
            Intent intent = new Intent(context, DailyLogEntryActivity.class);
            intent.putExtra("projectId", project.id);
            intent.putExtra("projectName", project.name);
            intent.putExtra("engineerName", currentEngineerName);
            intent.putExtra("managerInstructions", project.managerInstructions);
            context.startActivity(intent);
        };

        // Tapping "Log work" OR tapping anywhere on the card opens DailyLogEntryActivity
        if (holder.btnLog != null) holder.btnLog.setOnClickListener(openLogListener);
        holder.itemView.setOnClickListener(openLogListener);

        if (holder.btnMore != null) {
            holder.btnMore.setOnClickListener(v -> {
                android.widget.PopupMenu menu =
                        new android.widget.PopupMenu(context, holder.btnMore);
                menu.inflate(R.menu.menu_engineer_project_actions);
                menu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_edit_progress) {
                        Intent intent = new Intent(context, PhaseProgressActivity.class);
                        intent.putExtra("projectId", project.id);
                        intent.putExtra("projectName", project.name);
                        intent.putExtra("role", "engineer");
                        context.startActivity(intent);
                        return true;
                    }
                    if (id == R.id.action_history) {
                        Intent intent = new Intent(context, LogHistoryActivity.class);
                        intent.putExtra("projectId", project.id);
                        intent.putExtra("projectName", project.name);
                        context.startActivity(intent);
                        return true;
                    }
                    if (id == R.id.action_chat) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("projectId", project.id);
                        intent.putExtra("projectName", project.name);
                        intent.putExtra("role", "engineer");
                        context.startActivity(intent);
                        return true;
                    }
                    return false;
                });
                menu.show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectName, tvAssignedBy, tvStatus, tvLocation, tvDueDate, tvManagerDirectives;
        Button btnLog, btnMore;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvAssignedBy = itemView.findViewById(R.id.tvAssignedBy);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvManagerDirectives = itemView.findViewById(R.id.tvManagerDirectives);
            btnLog = itemView.findViewById(R.id.btnLog);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}