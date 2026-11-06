package com.example.organizer.ui.kaizen;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
                KaizenGoal newGoal = new KaizenGoal(
                        title, desc, "Diária", fCurrent, fTarget, 0,
                        System.currentTimeMillis(), notif, "20:00", "Todos os dias"
                );
                long goalId = DatabaseClient.getInstance(this).getDatabase().kaizenDao().insertGoal(newGoal);

                if (notif) {
                    scheduleKaizenNotification(goalId, title, 20, 0);
                }
            });

            dialog.dismiss();
            Toast.makeText(this, "Meta Kaizen criada!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }

    private void scheduleKaizenNotification(long goalId, String title, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        NotificationHelper.scheduleNotification(
                this,
                (int) goalId + 5000,
                "Kaizen: " + title,
                "Hora de dar um passo na sua evolução gradual diária.",
                NotificationHelper.CHANNEL_KAIZEN,
                cal.getTimeInMillis()
        );
    }

    @Override
    public void onSuggestAi(KaizenGoal goal) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_kaizen_ai, null);

        TextView tvContext = sheetView.findViewById(R.id.tv_ai_goal_context);
        TextView tvRationale = sheetView.findViewById(R.id.tv_ai_rationale);
        TextView tvSteps = sheetView.findViewById(R.id.tv_ai_steps);
        ProgressBar pbLoading = sheetView.findViewById(R.id.pb_ai_loading);
        TextView tvNotifSuggestion = sheetView.findViewById(R.id.tv_ai_notif_suggestion);
        SwitchMaterial switchNotif = sheetView.findViewById(R.id.switch_ai_apply_notif);
        MaterialButton btnReject = sheetView.findViewById(R.id.btn_reject_ai);
        MaterialButton btnEdit = sheetView.findViewById(R.id.btn_edit_ai);
        MaterialButton btnAccept = sheetView.findViewById(R.id.btn_accept_ai);

        tvContext.setText("Meta: \"" + goal.getTitle() + "\"\nSituação atual: " + goal.getCurrentState() + " → Meta final: " + goal.getTargetState());

        final String[] lastRejected = {null};
        final GeminiService.KaizenSuggestionResult[] currentResult = {null};
        final String[] customNotifTime = {"20:00"};
        final String[] customNotifDays = {"Todos os dias"};

        Runnable runAiCall = new Runnable() {
            @Override
            public void run() {
                pbLoading.setVisibility(View.VISIBLE);
                tvSteps.setText("Analisando meta e gerando micro-etapas personalizadas com Gemini...");
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                btnEdit.setEnabled(false);

                DatabaseClient.getInstance(KaizenActivity.this).getExecutor().execute(() -> {
                    List<KaizenProgress> existing = DatabaseClient.getInstance(KaizenActivity.this)
                            .getDatabase().kaizenDao().getProgressByGoalSync(goal.getId());

                    runOnUiThread(() -> {
                        geminiService.suggestKaizenProgression(
                                KaizenActivity.this,
                                goal,
                                existing,
                                lastRejected[0],
                                new GeminiService.ApiCallback<GeminiService.KaizenSuggestionResult>() {
                                    @Override
                                    public void onSuccess(GeminiService.KaizenSuggestionResult result) {
                                        pbLoading.setVisibility(View.GONE);
                                        btnAccept.setEnabled(true);
                                        btnReject.setEnabled(true);
                                        btnEdit.setEnabled(true);
                                        currentResult[0] = result;

                                        tvRationale.setText("Fundamento gradual:\n" + result.rationale);

                                        StringBuilder sb = new StringBuilder();
                                        for (int i = 0; i < result.steps.size(); i++) {
                                            sb.append(result.steps.get(i)).append("\n\n");
                                        }
                                        tvSteps.setText(sb.toString().trim());

                                        if (result.suggestedReminderTime != null) {
                                            customNotifTime[0] = result.suggestedReminderTime;
                                            customNotifDays[0] = (result.suggestedReminderDays != null) ? result.suggestedReminderDays : "Todos os dias";
                                            tvNotifSuggestion.setText("Horário sugerido: " + customNotifTime[0] + " (" + customNotifDays[0] + ") • Toque para alterar");
                                        }
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        pbLoading.setVisibility(View.GONE);
                                        btnReject.setEnabled(true);
                                        btnReject.setText("Tentar Novamente");
                                        tvSteps.setText(errorMessage);
                                    }
                                }
                        );
                    });
                });
            }
        };

        tvNotifSuggestion.setOnClickListener(v -> {
            int h = 20, m = 0;
            try {
                String[] p = customNotifTime[0].split(":");
                h = Integer.parseInt(p[0].trim());
                m = Integer.parseInt(p[1].trim());
            } catch (Exception ignored) {}

            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                customNotifTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                tvNotifSuggestion.setText("Horário configurado: " + customNotifTime[0] + " (" + customNotifDays[0] + ") • Toque para alterar");
            }, h, m, true).show();
        });

        runAiCall.run();

        btnReject.setOnClickListener(v -> {
            final EditText inputReason = new EditText(this);
            inputReason.setHint("Ex: Sugestão muito agressiva, passos mais graduais...");

            new AlertDialog.Builder(this)
                    .setTitle("Nova Sugestão da IA")
                    .setMessage("Deseja informar um motivo ou ajuste para a nova alternativa?")
                    .setView(inputReason)
                    .setPositiveButton("Gerar Alternativa", (d, w) -> {
                        String reason = inputReason.getText().toString().trim();
                        StringBuilder rejectionBuilder = new StringBuilder();
                        rejectionBuilder.append("Sugestão anterior rejeitada:\n")
                                .append(tvSteps.getText().toString());
                        if (!reason.isEmpty()) {
                            rejectionBuilder.append("\nMotivo do usuário: ").append(reason);
                        }
                        lastRejected[0] = rejectionBuilder.toString();
                        Toast.makeText(this, "Gerando nova sugestão alternativa...", Toast.LENGTH_SHORT).show();
                        runAiCall.run();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnEdit.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setText(tvSteps.getText());
            new AlertDialog.Builder(this)
                    .setTitle("Editar Etapas Sugeridas")
                    .setView(input)
                    .setPositiveButton("Salvar Edição", (d, w) -> {
                        String edited = input.getText().toString().trim();
                        if (!edited.isEmpty()) {
                            tvSteps.setText(edited);
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnAccept.setOnClickListener(v -> {
            String fullText = tvSteps.getText().toString().trim();
            if (fullText.isEmpty()) return;

            String[] rawLines = fullText.split("\n");
            List<String> finalSteps = new ArrayList<>();
            for (String line : rawLines) {
                String clean = line.trim();
                if (!clean.isEmpty()) {
                    finalSteps.add(clean);
                }
            }

            boolean applyNotif = switchNotif.isChecked();
            String notifTime = customNotifTime[0];
            String notifDays = customNotifDays[0];

            DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                for (String stepText : finalSteps) {
                    DatabaseClient.getInstance(this).getDatabase().kaizenDao().insertProgress(
                            new KaizenProgress(goal.getId(), stepText, System.currentTimeMillis(), false, "")
                    );
                }

                if (applyNotif) {
                    goal.setNotificationsEnabled(true);
                    goal.setNotificationTime(notifTime);
                    goal.setNotificationDays(notifDays);
                    DatabaseClient.getInstance(this).getDatabase().kaizenDao().updateGoal(goal);

                    try {
                        String[] parts = notifTime.split(":");
                        int h = Integer.parseInt(parts[0].trim());
                        int m = Integer.parseInt(parts[1].trim());
                        scheduleKaizenNotification(goal.getId(), goal.getTitle(), h, m);
                    } catch (Exception ignored) {}
                }
            });

            bottomSheet.dismiss();
            Toast.makeText(this, "Etapas aceitas e incorporadas à meta!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(this, 40);
        });

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    @Override
    public void onViewSteps(KaizenGoal goal) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kaizen_steps, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_steps_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_steps_dialog_subtitle);
        ProgressBar pbProgress = dialogView.findViewById(R.id.pb_steps_dialog);
        RecyclerView rvSteps = dialogView.findViewById(R.id.rv_kaizen_steps);
        EditText etNewStep = dialogView.findViewById(R.id.et_new_step_title);
        View btnAddManual = dialogView.findViewById(R.id.btn_add_step_manual);
        View btnClose = dialogView.findViewById(R.id.btn_close_steps_dialog);

        tvTitle.setText(goal.getTitle());
        tvSubtitle.setText("Progressão gradual: " + goal.getCurrentState() + " → " + goal.getTargetState());
        pbProgress.setProgress(goal.getProgress());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        KaizenStepAdapter stepAdapter = new KaizenStepAdapter(this, new KaizenStepAdapter.StepActionListener() {
            @Override
            public void onToggleAchieved(KaizenProgress step, boolean achieved) {
                step.setAchieved(achieved);
                DatabaseClient.getInstance(KaizenActivity.this).getExecutor().execute(() -> {
                    DatabaseClient.getInstance(KaizenActivity.this).getDatabase().kaizenDao().updateProgress(step);

                    // Recalculate goal progression based on completed steps
                    List<KaizenProgress> all = DatabaseClient.getInstance(KaizenActivity.this)
                            .getDatabase().kaizenDao().getProgressByGoalSync(goal.getId());
                    if (all != null && !all.isEmpty()) {
                        int achievedCount = 0;
                        for (KaizenProgress p : all) {
                            if (p.isAchieved()) achievedCount++;
                        }
                        int newProg = (achievedCount * 100) / all.size();
                        goal.setProgress(newProg);
                        DatabaseClient.getInstance(KaizenActivity.this).getDatabase().kaizenDao().updateGoal(goal);

                        runOnUiThread(() -> pbProgress.setProgress(newProg));
                    }
                });
                NotificationHelper.vibrateFeedback(KaizenActivity.this, 35);
            }

            @Override
            public void onDeleteStep(KaizenProgress step) {
                DatabaseClient.getInstance(KaizenActivity.this).getExecutor().execute(() -> {
                    DatabaseClient.getInstance(KaizenActivity.this).getDatabase().kaizenDao().deleteProgress(step);

                    List<KaizenProgress> all = DatabaseClient.getInstance(KaizenActivity.this)
                            .getDatabase().kaizenDao().getProgressByGoalSync(goal.getId());
                    int newProg = 0;
                    if (all != null && !all.isEmpty()) {
                        int achievedCount = 0;
                        for (KaizenProgress p : all) {
                            if (p.isAchieved()) achievedCount++;
                        }
                        newProg = (achievedCount * 100) / all.size();
                    }
                    goal.setProgress(newProg);
                    DatabaseClient.getInstance(KaizenActivity.this).getDatabase().kaizenDao().updateGoal(goal);
                    final int fProg = newProg;
                    runOnUiThread(() -> pbProgress.setProgress(fProg));
                });
            }
        });

        rvSteps.setLayoutManager(new LinearLayoutManager(this));
        rvSteps.setAdapter(stepAdapter);

        DatabaseClient.getInstance(this).getDatabase().kaizenDao().getProgressByGoal(goal.getId())
                .observe(this, steps -> {
                    stepAdapter.setSteps(steps);
                    if (steps != null && !steps.isEmpty()) {
                        int achievedCount = 0;
                        for (KaizenProgress p : steps) {
                            if (p.isAchieved()) achievedCount++;
                        }
                        int calculated = (achievedCount * 100) / steps.size();
                        pbProgress.setProgress(calculated);
                    }
                });

        btnAddManual.setOnClickListener(v -> {
            String stepTitle = etNewStep.getText().toString().trim();
            if (!stepTitle.isEmpty()) {
                DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                    DatabaseClient.getInstance(this).getDatabase().kaizenDao().insertProgress(
                            new KaizenProgress(goal.getId(), stepTitle, System.currentTimeMillis(), false, "")
                    );
                });
                etNewStep.setText("");
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onDeleteGoal(KaizenGoal goal) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Meta Kaizen")
                .setMessage("Deseja realmente remover \"" + goal.getTitle() + "\"?\nTodo o progresso e etapas serão excluídos.")
                .setPositiveButton("Excluir", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        NotificationHelper.cancelNotification(this, (int) goal.getId() + 5000);
                        DatabaseClient.getInstance(this).getDatabase().kaizenDao().deleteGoal(goal);
                    });
                    Toast.makeText(this, "Meta excluída!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 50);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
