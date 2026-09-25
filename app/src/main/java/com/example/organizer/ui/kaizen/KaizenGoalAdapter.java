package com.example.organizer.ui.kaizen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.entity.KaizenGoal;

import java.util.ArrayList;
import java.util.List;

public class KaizenGoalAdapter extends RecyclerView.Adapter<KaizenGoalAdapter.GoalViewHolder> {

    public interface KaizenListener {
        void onSuggestAi(KaizenGoal goal);
        void onGoalClick(KaizenGoal goal);
        void onDeleteGoal(KaizenGoal goal);
    }

    private final Context context;
    private List<KaizenGoal> goals = new ArrayList<>();
    private final KaizenListener listener;

    public KaizenGoalAdapter(Context context, KaizenListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setGoals(List<KaizenGoal> newGoals) {
        this.goals = (newGoals != null) ? newGoals : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kaizen_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        KaizenGoal goal = goals.get(position);

        holder.tvTitle.setText(goal.getTitle());
        holder.tvDesc.setText(goal.getDescription());
        holder.tvCurrentState.setText(goal.getCurrentState());
        holder.tvTargetState.setText(goal.getTargetState());

        holder.pbProgress.setProgress(goal.getProgress());
        holder.tvProgressPercent.setText(goal.getProgress() + "%");

        holder.btnSuggestAi.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestAi(goal);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGoalClick(goal);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteGoal(goal);
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDesc;
        ProgressBar pbProgress;
        TextView tvProgressPercent;
        TextView tvCurrentState;
        TextView tvTargetState;
        View btnSuggestAi;
        ImageButton btnDelete;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_goal_title);
            tvDesc = itemView.findViewById(R.id.tv_goal_desc);
            pbProgress = itemView.findViewById(R.id.pb_circular_progress);
            tvProgressPercent = itemView.findViewById(R.id.tv_progress_percent);
            tvCurrentState = itemView.findViewById(R.id.tv_current_state);
            tvTargetState = itemView.findViewById(R.id.tv_target_state);
            btnSuggestAi = itemView.findViewById(R.id.btn_suggest_ai);
            btnDelete = itemView.findViewById(R.id.btn_delete_goal);
        }
    }
}
