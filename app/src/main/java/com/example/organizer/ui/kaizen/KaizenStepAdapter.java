package com.example.organizer.ui.kaizen;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.entity.KaizenProgress;

import java.util.ArrayList;
import java.util.List;

public class KaizenStepAdapter extends RecyclerView.Adapter<KaizenStepAdapter.StepViewHolder> {

    public interface StepActionListener {
        void onToggleAchieved(KaizenProgress step, boolean achieved);
        void onDeleteStep(KaizenProgress step);
    }

    private final Context context;
    private List<KaizenProgress> steps = new ArrayList<>();
    private final StepActionListener listener;

    public KaizenStepAdapter(Context context, StepActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setSteps(List<KaizenProgress> newSteps) {
        this.steps = (newSteps != null) ? newSteps : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<KaizenProgress> getSteps() {
        return steps;
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kaizen_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        KaizenProgress step = steps.get(position);

        holder.tvTitle.setText(step.getStepTitle());

        // Find the first unachieved step index
        int firstUnachievedIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (!steps.get(i).isAchieved()) {
                firstUnachievedIndex = i;
                break;
            }
        }

        if (step.isAchieved()) {
            // CONCLUÍDA
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvStatusBadge.setText("Concluída ✓");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.module_kaizen));
            holder.itemView.setAlpha(0.85f);
            holder.cbAchieved.setEnabled(true);
        } else if (position == firstUnachievedIndex) {
            // DISPONÍVEL
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvStatusBadge.setText("Disponível ★");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.module_kanban));
            holder.itemView.setAlpha(1.0f);
            holder.cbAchieved.setEnabled(true);
        } else {
            // BLOQUEADA (Não pode ser concluída antecipadamente)
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvStatusBadge.setText("Bloqueada 🔒");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.app_text_tertiary));
            holder.itemView.setAlpha(0.5f);
            holder.cbAchieved.setEnabled(false);
        }

        holder.cbAchieved.setOnCheckedChangeListener(null);
        holder.cbAchieved.setChecked(step.isAchieved());
        holder.cbAchieved.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onToggleAchieved(step, isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteStep(step);
        });
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbAchieved;
        TextView tvTitle;
        TextView tvStatusBadge;
        ImageButton btnDelete;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            cbAchieved = itemView.findViewById(R.id.cb_step_achieved);
            tvTitle = itemView.findViewById(R.id.tv_step_title);
            tvStatusBadge = itemView.findViewById(R.id.tv_step_status_badge);
            btnDelete = itemView.findViewById(R.id.btn_delete_step);
        }
    }
}
