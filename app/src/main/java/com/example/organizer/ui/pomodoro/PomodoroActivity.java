package com.example.organizer.ui.pomodoro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
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

    private static final int DEFAULT_WORK_MINUTES = 25;
    private static final int DEFAULT_BREAK_MINUTES = 5;
    private static final int TOTAL_CYCLES = 4;

    private TextView tvCountdown;
    private TextView tvMode;
    private TextView tvCycle;
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

    private long totalMillis = DEFAULT_WORK_MINUTES * 60 * 1000L;
    private long millisLeft = totalMillis;

    private int sessionWorkMinutes = 0;
    private int sessionBreakMinutes = 0;
    private int completedCycles = 0;

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

        tvCountdown = findViewById(R.id.tv_pomodoro_countdown);
        tvMode = findViewById(R.id.tv_pomodoro_mode);
        tvCycle = findViewById(R.id.tv_pomodoro_cycle);
        pbProgress = findViewById(R.id.pb_pomodoro_progress);
        fabToggle = findViewById(R.id.fab_pomodoro_toggle);
        btnReset = findViewById(R.id.btn_pomodoro_reset);
        btnSkip = findViewById(R.id.btn_pomodoro_skip);
        cardStats = findViewById(R.id.card_pomodoro_stats);
        tvTotalWork = findViewById(R.id.tv_total_work);
        tvTotalBreak = findViewById(R.id.tv_total_break);

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

        observeHistoricalStats();
    }

    private void startTimer() {
        isRunning = true;
        fabToggle.setImageResource(R.drawable.ic_pause);

        // Focus mode: Hide distraction stats card
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
            sessionWorkMinutes += (int) (totalMillis / 60000L);
            completedCycles++;

            NotificationHelper.scheduleNotification(
                    this,
                    999,
                    "Pomodoro: Hora de Descansar!",
                    "Você concluiu um ciclo de foco. Faça uma pausa revigorante.",
                    NotificationHelper.CHANNEL_POMODORO,
                    System.currentTimeMillis() + 100
            );

            Toast.makeText(this, "Ciclo de foco concluído! Hora de descansar.", Toast.LENGTH_LONG).show();
            saveSessionToDatabase();
            switchMode();
        } else {
            sessionBreakMinutes += (int) (totalMillis / 60000L);

            NotificationHelper.scheduleNotification(
                    this,
                    998,
                    "Pomodoro: Fim do Descanso!",
                    "Pronto para o próximo ciclo de produtividade?",
                    NotificationHelper.CHANNEL_POMODORO,
                    System.currentTimeMillis() + 100
            );

            Toast.makeText(this, "Pausa encerrada! Pronto para o próximo ciclo?", Toast.LENGTH_LONG).show();
            switchMode();
        }
    }

    private void switchMode() {
        pauseTimer();
        isWorkMode = !isWorkMode;

        if (isWorkMode) {
            if (currentCycle < TOTAL_CYCLES) {
                currentCycle++;
            } else {
                currentCycle = 1;
            }
            totalMillis = DEFAULT_WORK_MINUTES * 60 * 1000L;
            tvMode.setText("FOCO PROFUNDO");
            tvMode.setTextColor(getColor(R.color.module_pomodoro));
        } else {
            totalMillis = DEFAULT_BREAK_MINUTES * 60 * 1000L;
            tvMode.setText("PAUSA REPARADORA");
            tvMode.setTextColor(getColor(R.color.module_kaizen));
        }

        millisLeft = totalMillis;
        tvCycle.setText("Ciclo " + currentCycle + " de " + TOTAL_CYCLES);
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (millisLeft / 1000) / 60;
        int seconds = (int) (millisLeft / 1000) % 60;
        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int progress = (int) ((millisLeft * 100) / totalMillis);
        pbProgress.setProgress(progress);
    }

    private void saveSessionToDatabase() {
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().pomodoroDao().insertSession(
                    new PomodoroSession(System.currentTimeMillis(), DEFAULT_WORK_MINUTES, DEFAULT_BREAK_MINUTES, 1, DEFAULT_WORK_MINUTES, 0)
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
