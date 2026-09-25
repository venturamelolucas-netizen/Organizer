package com.example.organizer.ui.dashboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class DashboardModuleAdapter extends RecyclerView.Adapter<DashboardModuleAdapter.ModuleViewHolder> {

    public interface OnModuleClickListener {
        void onModuleClick(DashboardModuleItem item);
    }

    private final Context context;
    private final List<DashboardModuleItem> items;
    private final OnModuleClickListener listener;

    public DashboardModuleAdapter(Context context, List<DashboardModuleItem> items, OnModuleClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dashboard_module, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        DashboardModuleItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDesc.setText(item.getDescription());
        holder.ivIcon.setImageResource(item.getIconRes());

        int color = ContextCompat.getColor(context, item.getAccentColorRes());
        holder.accentIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
        holder.cardRoot.setStrokeColor(ColorStateList.valueOf(color));
        holder.cardRoot.setStrokeWidth(2);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModuleClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        ImageView ivIcon;
        View accentIndicator;
        TextView tvTitle;
        TextView tvDesc;

        public ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_module);
            ivIcon = itemView.findViewById(R.id.iv_module_icon);
            accentIndicator = itemView.findViewById(R.id.view_accent_indicator);
            tvTitle = itemView.findViewById(R.id.tv_module_title);
            tvDesc = itemView.findViewById(R.id.tv_module_desc);
        }
    }
}
