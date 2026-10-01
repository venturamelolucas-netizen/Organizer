package com.example.organizer.ui.kanban;

import android.content.ClipData;
import android.content.ClipDescription;
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
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KanbanCardAdapter extends RecyclerView.Adapter<KanbanCardAdapter.CardViewHolder> {

    public interface CardListener {
        void onCardClick(KanbanCard card);
        void onToggleCompleted(KanbanCard card, boolean completed);
        void onDeleteCard(KanbanCard card);
        void onMoveCard(KanbanCard card);
    }

    private final Context context;
    private List<KanbanCard> cards = new ArrayList<>();
    private final CardListener listener;

    public KanbanCardAdapter(Context context, CardListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCards(List<KanbanCard> newCards) {
        this.cards = (newCards != null) ? newCards : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<KanbanCard> getCards() {
        return cards;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kanban_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        KanbanCard card = cards.get(position);

        holder.tvTitle.setText(card.getTitle());
        holder.tvDesc.setText(card.getDescription());

        // Strike through if completed
        if (card.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(card.isCompleted());
        holder.cbDone.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onToggleCompleted(card, isChecked);
        });

        // 4dp Priority Stripe
        int prioColorRes;
        if ("Alta".equalsIgnoreCase(card.getPriority())) {
            prioColorRes = R.color.eisenhower_q1;
        } else if ("Média".equalsIgnoreCase(card.getPriority())) {
            prioColorRes = R.color.eisenhower_q2;
        } else {
            prioColorRes = R.color.module_kaizen;
        }
        int prioColor = ContextCompat.getColor(context, prioColorRes);
        holder.viewPriorityStripe.setBackgroundColor(prioColor);
        holder.tvPriority.setText(card.getPriority());
        holder.tvPriority.setTextColor(prioColor);

        // Due date: only show if set (> 0)
        if (card.getDueDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDueDate.setText("Vencimento: " + sdf.format(new Date(card.getDueDate())));
            holder.tvDueDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDueDate.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(card);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteCard(card);
        });

        if (holder.btnMove != null) {
            holder.btnMove.setOnClickListener(v -> {
                if (listener != null) listener.onMoveCard(card);
            });
        }

        // Drag and Drop initiator on long press
        holder.itemView.setOnLongClickListener(v -> {
            ClipData.Item item = new ClipData.Item(String.valueOf(card.getId()));
            ClipData dragData = new ClipData("KANBAN_CARD", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(holder.itemView);
            holder.itemView.setElevation(14f);
            v.startDragAndDrop(dragData, shadow, card, 0);
            NotificationHelper.vibrateFeedback(context, 35);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        View viewPriorityStripe;
        CheckBox cbDone;
        TextView tvTitle;
        TextView tvDesc;
        TextView tvPriority;
        TextView tvDueDate;
        ImageButton btnDelete;
        ImageButton btnMove;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_kanban_root);
            viewPriorityStripe = itemView.findViewById(R.id.view_priority_stripe);
            cbDone = itemView.findViewById(R.id.cb_card_done);
            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvDesc = itemView.findViewById(R.id.tv_card_desc);
            tvPriority = itemView.findViewById(R.id.tv_card_priority);
            tvDueDate = itemView.findViewById(R.id.tv_card_due_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_card);
            btnMove = itemView.findViewById(R.id.btn_move_card);
        }
    }
}
