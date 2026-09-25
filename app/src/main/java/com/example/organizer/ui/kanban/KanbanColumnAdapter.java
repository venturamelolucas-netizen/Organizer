package com.example.organizer.ui.kanban;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.entity.KanbanList;
import com.example.organizer.data.service.NotificationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KanbanColumnAdapter extends RecyclerView.Adapter<KanbanColumnAdapter.ColumnViewHolder> {

    public interface ColumnListener {
        void onAddCard(KanbanList list);
        void onEditCard(KanbanCard card);
        void onDeleteList(KanbanList list);
    }

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private List<KanbanList> lists = new ArrayList<>();
    private final ColumnListener listener;

    public KanbanColumnAdapter(Context context, LifecycleOwner lifecycleOwner, ColumnListener listener) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.listener = listener;
    }

    public void setLists(List<KanbanList> newLists) {
        this.lists = (newLists != null) ? newLists : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ColumnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kanban_column, parent, false);
        return new ColumnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColumnViewHolder holder, int position) {
        KanbanList list = lists.get(position);
        holder.tvTitle.setText(list.getTitle());

        KanbanCardAdapter cardAdapter = new KanbanCardAdapter(context, new KanbanCardAdapter.CardListener() {
            @Override
            public void onCardClick(KanbanCard card) {
                if (listener != null) listener.onEditCard(card);
            }

            @Override
            public void onToggleCompleted(KanbanCard card, boolean completed) {
                card.setCompleted(completed);
                DatabaseClient.getInstance(context).getExecutor().execute(() -> {
                    DatabaseClient.getInstance(context).getDatabase().kanbanDao().updateCard(card);
                });
                NotificationHelper.vibrateFeedback(context, 40);
            }

            @Override
            public void onDeleteCard(KanbanCard card) {
                DatabaseClient.getInstance(context).getExecutor().execute(() -> {
                    DatabaseClient.getInstance(context).getDatabase().kanbanDao().deleteCard(card);
                });
                NotificationHelper.vibrateFeedback(context, 50);
            }
        });

        holder.rvCards.setLayoutManager(new LinearLayoutManager(context));
        holder.rvCards.setAdapter(cardAdapter);

        // Attach Drag-and-Drop ItemTouchHelper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                List<KanbanCard> currentCards = cardAdapter.getCards();
                if (fromPos < toPos) {
                    for (int i = fromPos; i < toPos; i++) {
                        Collections.swap(currentCards, i, i + 1);
                    }
                } else {
                    for (int i = fromPos; i > toPos; i--) {
                        Collections.swap(currentCards, i, i - 1);
                    }
                }
                cardAdapter.notifyItemMoved(fromPos, toPos);

                // Update positions in DB
                DatabaseClient.getInstance(context).getExecutor().execute(() -> {
                    for (int i = 0; i < currentCards.size(); i++) {
                        KanbanCard c = currentCards.get(i);
                        c.setPosition(i);
                        DatabaseClient.getInstance(context).getDatabase().kanbanDao().updateCard(c);
                    }
                });

                NotificationHelper.vibrateFeedback(context, 20);
                return true;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setElevation(12f);
                    NotificationHelper.vibrateFeedback(context, 35);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setElevation(2f);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        itemTouchHelper.attachToRecyclerView(holder.rvCards);

        // Observe cards for this list
        DatabaseClient.getInstance(context)
                .getDatabase()
                .kanbanDao()
                .getCardsByList(list.getId())
                .observe(lifecycleOwner, cards -> {
                    cardAdapter.setCards(cards);
                    holder.tvCount.setText(String.valueOf(cards != null ? cards.size() : 0));
                });

        holder.btnAddCard.setOnClickListener(v -> {
            if (listener != null) listener.onAddCard(list);
        });

        holder.btnDeleteList.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteList(list);
        });
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    static class ColumnViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvCount;
        ImageButton btnDeleteList;
        RecyclerView rvCards;
        View btnAddCard;

        public ColumnViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_column_title);
            tvCount = itemView.findViewById(R.id.tv_column_count);
            btnDeleteList = itemView.findViewById(R.id.btn_delete_list);
            rvCards = itemView.findViewById(R.id.rv_column_cards);
            btnAddCard = itemView.findViewById(R.id.btn_add_card_in_list);
        }
    }
}
