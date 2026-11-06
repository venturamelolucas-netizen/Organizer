package com.example.organizer.ui.notes;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.entity.Note;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private final Context context;
    private List<Note> notes = new ArrayList<>();
    private final OnNoteClickListener listener;

    public NotesAdapter(Context context, OnNoteClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setNotes(List<Note> newNotes) {
        this.notes = (newNotes != null) ? newNotes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note_card, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);

        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        // Parse custom post-it color
        int bgColor;
        try {
            bgColor = Color.parseColor(note.getColor());
        } catch (Exception e) {
            bgColor = Color.parseColor("#FEF08A");
        }
        holder.cardRoot.setCardBackgroundColor(bgColor);

        // Dynamic text contrast based on background luminance
        double luminance = androidx.core.graphics.ColorUtils.calculateLuminance(bgColor);
        int titleColor = luminance > 0.45 ? Color.parseColor("#1F2937") : Color.parseColor("#F9FAFB");
        int subColor = luminance > 0.45 ? Color.parseColor("#4B5563") : Color.parseColor("#E5E7EB");

        holder.tvTitle.setTextColor(titleColor);
        holder.tvContent.setTextColor(subColor);
        holder.tvDate.setTextColor(subColor);

        // Tag
        if (note.getTag() != null && !note.getTag().isEmpty()) {
            holder.tvTag.setText(note.getTag());
            holder.tvTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvTag.setVisibility(View.GONE);
        }

        // Formatted Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(note.getUpdatedAt())));

        // Pin indicator
        holder.ivPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvTitle;
        TextView tvContent;
        TextView tvTag;
        TextView tvDate;
        ImageView ivPin;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_note_root);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvContent = itemView.findViewById(R.id.tv_note_content);
            tvTag = itemView.findViewById(R.id.tv_note_tag);
            tvDate = itemView.findViewById(R.id.tv_note_date);
            ivPin = itemView.findViewById(R.id.iv_note_pin);
        }
    }
}
