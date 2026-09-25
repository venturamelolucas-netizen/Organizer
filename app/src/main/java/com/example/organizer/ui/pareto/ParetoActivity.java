package com.example.organizer.ui.pareto;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.service.GeminiService;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class ParetoActivity extends AppCompatActivity {

    private EditText etInput;
    private View btnAnalyze;
    private ProgressBar pbLoading;
    private View layoutResults;
    private TextView tvSummary;
    private TextView tvExplanation;
    private TextView tvRecommendations;
    private RecyclerView rvFactors;

    private ParetoFactorAdapter adapter;
    private GeminiService geminiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pareto);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_pareto);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etInput = findViewById(R.id.et_pareto_input);
        btnAnalyze = findViewById(R.id.btn_analyze_pareto);
        pbLoading = findViewById(R.id.pb_pareto_loading);
        layoutResults = findViewById(R.id.layout_pareto_results);
        tvSummary = findViewById(R.id.tv_pareto_summary);
        tvExplanation = findViewById(R.id.tv_pareto_explanation);
        tvRecommendations = findViewById(R.id.tv_pareto_recommendations);
        rvFactors = findViewById(R.id.rv_pareto_factors);

        adapter = new ParetoFactorAdapter(this);
        rvFactors.setLayoutManager(new LinearLayoutManager(this));
        rvFactors.setAdapter(adapter);

        geminiService = new GeminiService();

        // Run default initial demo analysis
        runAnalysis("Todo dia estudo 3 horas lendo apostilas, respondo e-mails por 1 hora, participo de reuniões de 2 horas e treino 40 minutos.");

        btnAnalyze.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (text.isEmpty()) {
                etInput.setError("Descreva sua rotina ou atividade para a IA analisar");
                return;
            }
            runAnalysis(text);
        });
    }

    private void runAnalysis(String report) {
        pbLoading.setVisibility(View.VISIBLE);
        NotificationHelper.vibrateFeedback(this, 30);

        geminiService.analyzePareto(this, report, new GeminiService.ApiCallback<GeminiService.ParetoAnalysisResult>() {
            @Override
            public void onSuccess(GeminiService.ParetoAnalysisResult result) {
                pbLoading.setVisibility(View.GONE);
                layoutResults.setVisibility(View.VISIBLE);

                tvSummary.setText(result.summary);
                tvExplanation.setText(result.explanation);
                adapter.setFactors(result.factors);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < result.recommendations.size(); i++) {
                    sb.append("• ").append(result.recommendations.get(i)).append("\n\n");
                }
                tvRecommendations.setText(sb.toString().trim());

                NotificationHelper.vibrateFeedback(ParetoActivity.this, 40);
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ParetoActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
