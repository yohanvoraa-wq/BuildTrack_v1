package com.example.buildtrack_v1;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<DailyLogRepository.ChatMessage> items = new ArrayList<>();
    private final String myUid;
    private final int accent;
    private final int onAccent;
    private final int panel;
    private final int bodyText;

    public ChatAdapter(String myUid, Context themeSource) {
        this.myUid = myUid;
        this.accent = UiTheme.accent(themeSource);
        this.onAccent = UiTheme.onAccent(themeSource);
        this.panel = UiTheme.panel(themeSource);
        this.bodyText = UiTheme.bodyText(themeSource);
    }

    public void setMessages(List<DailyLogRepository.ChatMessage> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailyLogRepository.ChatMessage m = items.get(position);
        boolean mine = myUid != null && myUid.equals(m.senderId);

        // mine on the right, theirs on the left
        h.root.setGravity(mine ? Gravity.END : Gravity.START);

        float density = h.itemView.getResources().getDisplayMetrics().density;
        GradientDrawable bubble = new GradientDrawable();
        bubble.setCornerRadius(16 * density);
        bubble.setColor(mine ? accent : panel);
        h.tvText.setBackground(bubble);
        h.tvText.setTextColor(mine ? onAccent : bodyText);
        h.tvText.setText(m.text != null ? m.text : "");

        if (mine) {
            h.tvSender.setVisibility(View.GONE);
        } else {
            h.tvSender.setVisibility(View.VISIBLE);
            String who = m.senderName != null ? m.senderName : "Someone";
            h.tvSender.setText(m.senderRole != null ? who + " (" + m.senderRole + ")" : who);
        }

        h.tvTime.setText(ProgressUtil.formatDateTime(m.createdAt));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView tvSender, tvText, tvTime;

        VH(@NonNull View v) {
            super(v);
            root = (LinearLayout) v;
            tvSender = v.findViewById(R.id.tvSender);
            tvText = v.findViewById(R.id.tvText);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}