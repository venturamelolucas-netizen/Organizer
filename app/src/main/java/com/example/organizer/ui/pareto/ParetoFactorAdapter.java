package com.example.organizer.ui.pareto;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.service.GeminiService;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ParetoFactorAdapter extends RecyclerView.Adapter<ParetoFactorAdapter.FactorViewHolder> {

    private final Context context;
    private List<GeminiService.ParetoFactor> factors = new ArrayList<>();

    public ParetoFactorAdapter(Context context) {
        this.context = context;
    }

    public void setFactors(List<GeminiService.ParetoFactor> newFactors) {
        this.factors = (newFactors != null) ? newFactors : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FactorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pareto_factor, parent, false);
        return new FactorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FactorViewHolder holder, int position) {
        GeminiService.ParetoFactor factor = factors.get(position);

        holder.tvTitle.setText(factor.title);
        holder.pbImpact.setProgress(factor.impactPercentage);

        if (factor.isTopTwenty) {
            // Highlighted in 20% vital
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.module_pareto_light));
            holder.cardRoot.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.module_pareto)));
            holder.cardRoot.setStrokeWidth(2);

            holder.tvBadge.setText("20% Vital • " + factor.impactPercentage + "%");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.module_pareto)));

            holder.pbImpact.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.module_pareto)));
        } else {
            // 80% secondary factors in neutral tone
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_light));
            holder.cardRoot.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.card_stroke_light)));
            holder.cardRoot.setStrokeWidth(1);

            holder.tvBadge.setText("Secundário • " + factor.impactPercentage + "%");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_200)));

            holder.pbImpact.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_400)));
        }
    }

    @Override
    public int getItemCount() {
        return factors.size();
    }

    static class FactorViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvTitle;
        TextView tvBadge;
        ProgressBar pbImpact;

        public FactorViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_factor_root);
            tvTitle = itemView.findViewById(R.id.tv_factor_title);
            tvBadge = itemView.findViewById(R.id.tv_factor_badge);
            pbImpact = itemView.findViewById(R.id.pb_factor_impact);
        }
    }
}
