package com.example.organizer.ui.pomodoro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.PomodoroSession;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "organizer_pomodoro_prefs";
    private static final String KEY_WORK_MIN = "pomo_work_min";
    private static final String KEY_BREAK_MIN = "pomo_break_min";
    private static final String KEY_LONG_BREAK_MIN = "pomo_long_break_min";
    private static final String KEY_CYCLES_COUNT = "pomo_cycles_count";

    private TextView tvCountdown;
    private TextView tvMode;
    private ProgressBar pbProgress;
    private FloatingActionButton fabToggle;
    private ImageButton btnReset;
    private View btnSkip;
    private View cardStats;
    private TextView tvTotalWork;
    private TextView tvTotalBreak;

    private CountDownTimer timer;
    private boolean isRunning = false;
    private boolean isWorkMode = true;
    private int currentCycle = 1;

    private int configWorkMin = 25;
    private int configBreakMin = 5;
    private int configLongBreakMin = 15;
    private int configCycles = 4;

    private long totalMillis;
    private long millisLeft;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_pomodoro);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        loadUserConfig();

        tvCountdown = findViewById(R.id.tv_pomodoro_countdown);
        tvMode = findViewById(R.id.tv_pomodoro_mode);
        pbProgress = findViewById(R.id.pb_pomodoro_progress);
        fabToggle = findViewById(R.id.fab_pomodoro_toggle);
        btnReset = findViewById(R.id.btn_pomodoro_reset);
        btnSkip = findViewById(R.id.btn_pomodoro_skip);
        cardStats = findViewById(R.id.card_pomodoro_stats);
        tvTotalWork = findViewById(R.id.tv_total_work);
        tvTotalBreak = findViewById(R.id.tv_total_break);

        totalMillis = configWorkMin * 60 * 1000L;
        millisLeft = totalMillis;

        updateTimerDisplay();

        fabToggle.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());
        btnSkip.setOnClickListener(v -> switchMode());

        findViewById(R.id.btn_pomodoro_config).setOnClickListener(v -> showConfigDialog());

        observeHistoricalStats();
    }

    private void loadUserConfig() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        configWorkMin = prefs.getInt(KEY_WORK_MIN, 25);
        configBreakMin = prefs.getInt(KEY_BREAK_MIN, 5);
        configLongBreakMin = prefs.getInt(KEY_LONG_BREAK_MIN, 15);
        configCycles = prefs.getInt(KEY_CYCLES_COUNT, 4);
    }

    private void showConfigDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pomodoro_config, null);
        EditText etWork = dialogView.findViewById(R.id.et_pomodoro_work_min);
        EditText etBreak = dialogView.findViewById(R.id.et_pomodoro_break_min);
        EditText etLongBreak = dialogView.findViewById(R.id.et_pomodoro_long_break_min);
        EditText etCycles = dialogView.findViewById(R.id.et_pomodoro_cycles_count);

        etWork.setText(String.valueOf(configWorkMin));
        etBreak.setText(String.valueOf(configBreakMin));
        etLongBreak.setText(String.valueOf(configLongBreakMin));
        etCycles.setText(String.valueOf(configCycles));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_cancel_pomodoro_config).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btn_save_pomodoro_config).setOnClickListener(v -> {
            try {
                int w = Integer.parseInt(etWork.getText().toString().trim());
                int b = Integer.parseInt(etBreak.getText().toString().trim());
                int lb = Integer.parseInt(etLongBreak.getText().toString().trim());
                int c = Integer.parseInt(etCycles.getText().toString().trim());

                if (w <= 0 || b <= 0 || lb <= 0 || c <= 0) {
                    Toast.makeText(this, "Valores devem ser maiores que zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                configWorkMin = w;
                configBreakMin = b;
                configLongBreakMin = lb;
                configCycles = c;

                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putInt(KEY_WORK_MIN, configWorkMin);
                editor.putInt(KEY_BREAK_MIN, configBreakMin);
                editor.putInt(KEY_LONG_BREAK_MIN, configLongBreakMin);
                editor.putInt(KEY_CYCLES_COUNT, configCycles);
                editor.apply();

                if (!isRunning) {
                    if (isWorkMode) {
                        totalMillis = configWorkMin * 60 * 1000L;
                    } else {
                        boolean isLong = (currentCycle % configCycles == 0);
                        totalMillis = (isLong ? configLongBreakMin : configBreakMin) * 60 * 1000L;
                    }
                    millisLeft = totalMillis;
                    updateTimerDisplay();
                }

                dialog.dismiss();
                Toast.makeText(this, "Tempos do Pomodoro atualizados!", Toast.LENGTH_SHORT).show();
                NotificationHelper.vibrateFeedback(this, 30);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void startTimer() {
        isRunning = true;
        fabToggle.setImageResource(R.drawable.ic_pause);

        cardStats.setVisibility(isWorkMode ? View.GONE : View.VISIBLE);

        timer = new CountDownTimer(millisLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisLeft = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                onTimerFinished();
            }
        }.start();
    }

    private void pauseTimer() {
        isRunning = false;
        fabToggle.setImageResource(R.drawable.ic_play);
        if (timer != null) {
            timer.cancel();
        }
        cardStats.setVisibility(View.VISIBLE);
    }

    private void resetTimer() {
        pauseTimer();
        millisLeft = totalMillis;
        updateTimerDisplay();
        NotificationHelper.vibrateFeedback(this, 30);
    }

    private void onTimerFinished() {
        isRunning = false;
        fabToggle.setImageResource(R.drawable.ic_play);
        cardStats.setVisibility(View.VISIBLE);

        NotificationHelper.vibrateFeedback(this, 500);

        if (isWorkMode) {
            final int completedWorkMin = configWorkMin;
            saveSessionRecord(completedWorkMin, 0, 1);

            NotificationHelper.scheduleNotification(
                    this,
                    999,
                    "Pomodoro: Hora de Descansar!",
                    "Você concluiu uma sessão de foco (" + completedWorkMin + " min). Faça uma pausa revigorante.",
                    NotificationHelper.CHANNEL_POMODORO,
                    System.currentTimeMillis() + 100
            );

            Toast.makeText(this, "Foco concluído (" + completedWorkMin + " min)! Hora de descansar.", Toast.LENGTH_LONG).show();
            switchMode();
        } else {
            boolean wasLongBreak = (currentCycle % configCycles == 0);
            final int completedBreakMin = wasLongBreak ? configLongBreakMin : configBreakMin;
            saveSessionRecord(0, completedBreakMin, 0);

            NotificationHelper.scheduleNotification(
                    this,
                    998,
                    "Pomodoro: Fim do Descanso!",
                    "Pronto para iniciar um novo bloco de foco?",
                    NotificationHelper.CHANNEL_POMODORO,
                    System.currentTimeMillis() + 100
            );

            Toast.makeText(this, "Pausa finalizada! Pronto para o próximo foco?", Toast.LENGTH_LONG).show();
            switchMode();
        }
    }

    private void switchMode() {
        pauseTimer();
        isWorkMode = !isWorkMode;

        if (isWorkMode) {
            currentCycle++;
            totalMillis = configWorkMin * 60 * 1000L;
            tvMode.setText("FOCO PROFUNDO");
            tvMode.setTextColor(getColor(R.color.module_pomodoro));
        } else {
            boolean isLongBreak = (currentCycle % configCycles == 0);
            int breakDuration = isLongBreak ? configLongBreakMin : configBreakMin;
            totalMillis = breakDuration * 60 * 1000L;
            tvMode.setText(isLongBreak ? "PAUSA LONGA (" + breakDuration + " min)" : "PAUSA CURTA (" + breakDuration + " min)");
            tvMode.setTextColor(getColor(R.color.module_kaizen));
        }

        millisLeft = totalMillis;
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (millisLeft / 1000) / 60;
        int seconds = (int) (millisLeft / 1000) % 60;
        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int progress = (int) ((millisLeft * 100) / totalMillis);
        pbProgress.setProgress(progress);
    }

    private void saveSessionRecord(int workMin, int breakMin, int cycles) {
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().pomodoroDao().insertSession(
                    new PomodoroSession(
                            System.currentTimeMillis(),
                            configWorkMin,
                            configBreakMin,
                            cycles,
                            workMin,
                            breakMin
                    )
            );
        });
    }

    private void observeHistoricalStats() {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .pomodoroDao()
                .getTotalWorkMinutes()
                .observe(this, totalWork -> {
                    int work = (totalWork != null) ? totalWork : 0;
                    tvTotalWork.setText(work + " min");
                });

        DatabaseClient.getInstance(this)
                .getDatabase()
                .pomodoroDao()
                .getAllSessions()
                .observe(this, sessions -> {
                    int totalBreak = 0;
                    if (sessions != null) {
                        for (PomodoroSession s : sessions) {
                            totalBreak += s.getTotalBreakMinutes();
                        }
                    }
                    tvTotalBreak.setText(totalBreak + " min");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
