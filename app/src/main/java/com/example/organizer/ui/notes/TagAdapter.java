package com.example.organizer.ui.notes;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.entity.Tag;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {

    public interface TagActionListener {
        void onEditTag(Tag tag);
        void onDeleteTag(Tag tag);
    }

    private final Context context;
    private List<Tag> tags = new ArrayList<>();
    private final TagActionListener listener;

    public TagAdapter(Context context, TagActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTags(List<Tag> newTags) {
        this.tags = (newTags != null) ? newTags : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_tag, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        Tag tag = tags.get(position);
        holder.tvName.setText(tag.getName());

        try {
            holder.cardColor.setCardBackgroundColor(Color.parseColor(tag.getColor()));
        } catch (Exception e) {
            holder.cardColor.setCardBackgroundColor(context.getColor(R.color.module_notes));
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditTag(tag);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteTag(tag);
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardColor;
        TextView tvName;
        ImageButton btnEdit, btnDelete;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            cardColor = itemView.findViewById(R.id.card_tag_color);
            tvName = itemView.findViewById(R.id.tv_tag_name);
            btnEdit = itemView.findViewById(R.id.btn_edit_tag);
            btnDelete = itemView.findViewById(R.id.btn_delete_tag);
        }
    }
}
