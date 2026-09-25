package com.example.organizer.ui.kaizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.data.entity.KaizenProgress;
import com.example.organizer.data.service.GeminiService;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class KaizenActivity extends AppCompatActivity implements KaizenGoalAdapter.KaizenListener {

    private RecyclerView rvGoals;
    private ExtendedFloatingActionButton fabAddGoal;
    private KaizenGoalAdapter adapter;
    private GeminiService geminiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kaizen);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_kaizen);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvGoals = findViewById(R.id.rv_kaizen_goals);
        fabAddGoal = findViewById(R.id.fab_add_kaizen_goal);

        geminiService = new GeminiService();

        setupRecyclerView();
        observeGoals();

        fabAddGoal.setOnClickListener(v -> showAddGoalDialog());
    }

    private void setupRecyclerView() {
        adapter = new KaizenGoalAdapter(this, this);
        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        rvGoals.setAdapter(adapter);
    }

    private void observeGoals() {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .kaizenDao()
                .getAllGoals()
                .observe(this, goals -> {
                    adapter.setGoals(goals);
                });
    }

    private void showAddGoalDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kaizen_goal, null);
        EditText etTitle = dialogView.findViewById(R.id.et_goal_title);
        EditText etDesc = dialogView.findViewById(R.id.et_goal_desc);
        EditText etCurrent = dialogView.findViewById(R.id.et_goal_current_state);
        EditText etTarget = dialogView.findViewById(R.id.et_goal_target_state);
        SwitchMaterial switchNotif = dialogView.findViewById(R.id.switch_goal_notif);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_goal);
        View btnSave = dialogView.findViewById(R.id.btn_save_goal);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError("Informe o título");
                return;
            }
            String desc = etDesc.getText().toString().trim();
            String current = etCurrent.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (current.isEmpty()) current = "Ponto de partida";
            if (target.isEmpty()) target = "Meta desejada";

            final String fCurrent = current;
            final String fTarget = target;
            final boolean notif = switchNotif.isChecked();

            DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                DatabaseClient.getInstance(this).getDatabase().kaizenDao().insertGoal(
                        new KaizenGoal(title, desc, "Diária", fCurrent, fTarget, 0, System.currentTimeMillis(), notif, "20:00", "Todos os dias")
                );
            });

            dialog.dismiss();
            Toast.makeText(this, "Meta Kaizen criada!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }

    @Override
    public void onSuggestAi(KaizenGoal goal) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_kaizen_ai, null);

        TextView tvContext = sheetView.findViewById(R.id.tv_ai_goal_context);
        TextView tvRationale = sheetView.findViewById(R.id.tv_ai_rationale);
        TextView tvSteps = sheetView.findViewById(R.id.tv_ai_steps);
        ProgressBar pbLoading = sheetView.findViewById(R.id.pb_ai_loading);
        View btnReject = sheetView.findViewById(R.id.btn_reject_ai);
        View btnEdit = sheetView.findViewById(R.id.btn_edit_ai);
        View btnAccept = sheetView.findViewById(R.id.btn_accept_ai);

        tvContext.setText("Meta: \"" + goal.getTitle() + "\"\nSituação atual: " + goal.getCurrentState() + " → Alvo: " + goal.getTargetState());

        pbLoading.setVisibility(View.VISIBLE);
        tvSteps.setText("Analisando com modelo Gemini...");

        geminiService.suggestKaizenProgression(this, goal.getTitle(), goal.getCurrentState(), goal.getTargetState(), new GeminiService.ApiCallback<GeminiService.KaizenSuggestionResult>() {
            @Override
            public void onSuccess(GeminiService.KaizenSuggestionResult result) {
                pbLoading.setVisibility(View.GONE);
                tvRationale.setText(result.rationale);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < result.steps.size(); i++) {
                    sb.append(result.steps.get(i)).append("\n\n");
                }
                tvSteps.setText(sb.toString().trim());

                btnAccept.setOnClickListener(v -> {
                    // Save steps into KaizenProgress
                    DatabaseClient.getInstance(KaizenActivity.this).getExecutor().execute(() -> {
                        for (String step : result.steps) {
                            DatabaseClient.getInstance(KaizenActivity.this).getDatabase().kaizenDao().insertProgress(
                                    new KaizenProgress(goal.getId(), step, System.currentTimeMillis(), false, "")
                            );
                        }
                    });
                    bottomSheet.dismiss();
                    Toast.makeText(KaizenActivity.this, "Etapas aceitas e incorporadas à meta!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(KaizenActivity.this, 40);
                });

                btnEdit.setOnClickListener(v -> {
                    final EditText input = new EditText(KaizenActivity.this);
                    input.setText(tvSteps.getText());
                    new AlertDialog.Builder(KaizenActivity.this)
                            .setTitle("Editar Etapas Sugeridas")
                            .setView(input)
                            .setPositiveButton("Salvar Etapas", (d, w) -> {
                                tvSteps.setText(input.getText().toString());
                                Toast.makeText(KaizenActivity.this, "Etapas ajustadas!", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

                btnReject.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    Toast.makeText(KaizenActivity.this, "Sugestão da IA descartada.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                tvSteps.setText("Não foi possível conectar ao Gemini. Tente novamente mais tarde.");
            }
        });

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    @Override
    public void onGoalClick(KaizenGoal goal) {
        // Dialog to update progress percentage
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(goal.getProgress());
        seekBar.setPadding(32, 32, 32, 32);

        final TextView label = new TextView(this);
        label.setPadding(32, 32, 32, 0);
        label.setText("Progresso atual: " + goal.getProgress() + "%");
        label.setTextSize(16);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                label.setText("Progresso atual: " + progress + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {}
            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.addView(label);
        container.addView(seekBar);

        new AlertDialog.Builder(this)
                .setTitle(goal.getTitle())
                .setMessage("Ajuste sua evolução gradual no objetivo:")
                .setView(container)
                .setPositiveButton("Salvar Progresso", (d, w) -> {
                    goal.setProgress(seekBar.getProgress());
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().kaizenDao().updateGoal(goal);
                    });
                    Toast.makeText(this, "Progresso atualizado para " + seekBar.getProgress() + "%", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 30);
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    @Override
    public void onDeleteGoal(KaizenGoal goal) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Meta Kaizen")
                .setMessage("Deseja realmente remover \"" + goal.getTitle() + "\"?")
                .setPositiveButton("Excluir", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().kaizenDao().deleteGoal(goal);
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
