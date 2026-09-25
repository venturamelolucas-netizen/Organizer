package com.example.organizer.ui.fives;

import android.content.Context;
import android.content.res.ColorStateList;
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
import com.example.organizer.data.entity.FiveSItem;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class FiveSItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SEIRI = 1;
    public static final int VIEW_TYPE_SEITON = 2;
    public static final int VIEW_TYPE_SEISO = 3;
    public static final int VIEW_TYPE_ROUTINE = 4;

    public interface FiveSActionListener {
        void onSetEssential(FiveSItem item, boolean essential);
        void onSetDiscarded(FiveSItem item, boolean discarded);
        void onToggleCompleted(FiveSItem item, boolean completed);
        void onToggleRoutine(FiveSItem item, boolean enabled);
        void onDeleteItem(FiveSItem item);
    }

    private final Context context;
    private List<FiveSItem> items = new ArrayList<>();
    private int currentViewType = VIEW_TYPE_SEIRI;
    private final FiveSActionListener listener;

    public FiveSItemAdapter(Context context, FiveSActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<FiveSItem> newItems, int viewType) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        this.currentViewType = viewType;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return currentViewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_SEITON) {
            View v = inflater.inflate(R.layout.item_fives_seiton, parent, false);
            return new SeitonViewHolder(v);
        } else if (viewType == VIEW_TYPE_SEISO) {
            View v = inflater.inflate(R.layout.item_fives_seiso, parent, false);
            return new SeisoViewHolder(v);
        } else if (viewType == VIEW_TYPE_ROUTINE) {
            View v = inflater.inflate(R.layout.item_fives_routine, parent, false);
            return new RoutineViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_fives_seiri, parent, false);
            return new SeiriViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FiveSItem item = items.get(position);

        if (holder instanceof SeiriViewHolder) {
            bindSeiri((SeiriViewHolder) holder, item);
        } else if (holder instanceof SeitonViewHolder) {
            bindSeiton((SeitonViewHolder) holder, item);
        } else if (holder instanceof SeisoViewHolder) {
            bindSeiso((SeisoViewHolder) holder, item);
        } else if (holder instanceof RoutineViewHolder) {
            bindRoutine((RoutineViewHolder) holder, item);
        }
    }

    private void bindSeiri(SeiriViewHolder holder, FiveSItem item) {
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());

        if (item.isEssential()) {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.module_5s_light));
            holder.cardRoot.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.module_5s)));
            holder.tvBadge.setText("Essencial");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.module_5s_dark));
            holder.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
        } else if (item.isDiscarded()) {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_100));
            holder.cardRoot.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_300)));
            holder.tvBadge.setText("Descarte");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_200)));
        } else {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_light));
            holder.cardRoot.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.card_stroke_light)));
            holder.tvBadge.setText("Pendente");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        holder.btnEssential.setOnClickListener(v -> {
            if (listener != null) listener.onSetEssential(item, true);
        });

        holder.btnDiscard.setOnClickListener(v -> {
            if (listener != null) listener.onSetDiscarded(item, true);
        });
    }

    private void bindSeiton(SeitonViewHolder holder, FiveSItem item) {
        holder.tvName.setText(item.getName());
        holder.tvLocation.setText("Local: " + (item.getLocation().isEmpty() ? "Definir local" : item.getLocation()));
        holder.tvCategory.setText("Categoria: " + (item.getCategory().isEmpty() ? "Geral" : item.getCategory()));
        holder.tvRule.setText("Regra de posicionamento: " + (item.getPlacementRule().isEmpty() ? "Armazenar no local demarcado" : item.getPlacementRule()));
    }

    private void bindSeiso(SeisoViewHolder holder, FiveSItem item) {
        holder.tvTitle.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());

        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(item.isCompleted());
        holder.cbCompleted.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onToggleCompleted(item, isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteItem(item);
        });
    }

    private void bindRoutine(RoutineViewHolder holder, FiveSItem item) {
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());
        holder.tvTime.setText(item.getRoutineFrequency() + " às " + item.getRoutineTime());
        holder.tvStreak.setText("🔥 Sequência: " + item.getStreakCount() + " dias");

        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(item.isRoutineEnabled());
        holder.switchEnabled.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onToggleRoutine(item, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolders
    static class SeiriViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvName, tvDesc, tvBadge;
        View btnDiscard, btnEssential;
        public SeiriViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_seiri_root);
            tvName = itemView.findViewById(R.id.tv_seiri_name);
            tvDesc = itemView.findViewById(R.id.tv_seiri_desc);
            tvBadge = itemView.findViewById(R.id.tv_seiri_badge);
            btnDiscard = itemView.findViewById(R.id.btn_seiri_discard);
            btnEssential = itemView.findViewById(R.id.btn_seiri_essential);
        }
    }

    static class SeitonViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvCategory, tvRule;
        public SeitonViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_seiton_name);
            tvLocation = itemView.findViewById(R.id.tv_seiton_location);
            tvCategory = itemView.findViewById(R.id.tv_seiton_category);
            tvRule = itemView.findViewById(R.id.tv_seiton_rule);
        }
    }

    static class SeisoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbCompleted;
        TextView tvTitle, tvDesc;
        ImageButton btnDelete;
        public SeisoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompleted = itemView.findViewById(R.id.cb_seiso);
            tvTitle = itemView.findViewById(R.id.tv_seiso_title);
            tvDesc = itemView.findViewById(R.id.tv_seiso_desc);
            btnDelete = itemView.findViewById(R.id.btn_delete_seiso);
        }
    }

    static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvTime, tvStreak;
        SwitchMaterial switchEnabled;
        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_routine_name);
            tvDesc = itemView.findViewById(R.id.tv_routine_desc);
            tvTime = itemView.findViewById(R.id.tv_routine_time);
            tvStreak = itemView.findViewById(R.id.tv_routine_streak);
            switchEnabled = itemView.findViewById(R.id.switch_routine_enabled);
        }
    }
}
