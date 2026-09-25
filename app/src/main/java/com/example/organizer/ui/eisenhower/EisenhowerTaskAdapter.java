package com.example.organizer.ui.eisenhower;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.entity.EisenhowerTask;

import java.util.ArrayList;
import java.util.List;

public class EisenhowerTaskAdapter extends RecyclerView.Adapter<EisenhowerTaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onToggleCompleted(EisenhowerTask task, boolean completed);
        void onEditTask(EisenhowerTask task);
        void onDeleteTask(EisenhowerTask task);
    }

    private final Context context;
    private List<EisenhowerTask> tasks = new ArrayList<>();
    private final TaskActionListener listener;

    public EisenhowerTaskAdapter(Context context, TaskActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTasks(List<EisenhowerTask> newTasks) {
        this.tasks = (newTasks != null) ? newTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_eisenhower_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        EisenhowerTask task = tasks.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription());

        // Strike through if completed
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        // Set CheckBox without triggering listener during binding
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(task.isCompleted());
        holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleCompleted(task, isChecked);
            }
        });

        // Quadrant color & badge
        int qColorRes;
        String qText;
        switch (task.getQuadrant()) {
            case 1:
                qColorRes = R.color.eisenhower_q1;
                qText = "Q1 • Urgente e Importante";
                break;
            case 2:
                qColorRes = R.color.eisenhower_q2;
                qText = "Q2 • Importante, Não Urgente";
                break;
            case 3:
                qColorRes = R.color.eisenhower_q3;
                qText = "Q3 • Urgente, Não Importante";
                break;
            case 4:
            default:
                qColorRes = R.color.eisenhower_q4;
                qText = "Q4 • Não Urgente e Não Importante";
                break;
        }

        int qColor = ContextCompat.getColor(context, qColorRes);
        holder.viewStripe.setBackgroundColor(qColor);
        holder.tvBadgeQuadrant.setText(qText);
        holder.tvBadgeQuadrant.setTextColor(qColor);

        // Date & Time badge
        String dateTime = "";
        if (task.getDate() != null && !task.getDate().isEmpty()) {
            dateTime = task.getDate();
            if (task.getTime() != null && !task.getTime().isEmpty()) {
                dateTime += " • " + task.getTime();
            }
        } else {
            dateTime = "Prioridade: " + task.getPriority();
        }
        holder.tvBadgeDateTime.setText(dateTime);

        // Calendar Synced Icon
        holder.ivCalendarSynced.setVisibility(task.isSyncedWithCalendar() ? View.VISIBLE : View.GONE);

        // Action Buttons
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditTask(task);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteTask(task);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewStripe;
        CheckBox cbCompleted;
        TextView tvTitle;
        TextView tvDesc;
        TextView tvBadgeQuadrant;
        TextView tvBadgeDateTime;
        ImageView ivCalendarSynced;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStripe = itemView.findViewById(R.id.view_quadrant_stripe);
            cbCompleted = itemView.findViewById(R.id.cb_task_completed);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDesc = itemView.findViewById(R.id.tv_task_desc);
            tvBadgeQuadrant = itemView.findViewById(R.id.tv_badge_quadrant);
            tvBadgeDateTime = itemView.findViewById(R.id.tv_badge_datetime);
            ivCalendarSynced = itemView.findViewById(R.id.iv_calendar_synced);
            btnEdit = itemView.findViewById(R.id.btn_edit_task);
            btnDelete = itemView.findViewById(R.id.btn_delete_task);
        }
    }
}
