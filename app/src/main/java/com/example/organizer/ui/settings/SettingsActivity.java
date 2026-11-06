package com.example.organizer.ui.settings;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.BuildConfig;
import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.database.SampleDataGenerator;
import com.example.organizer.data.service.GoogleCalendarService;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "organizer_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";

    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchVibration;
    private TextView tvCalendarStatus;
    private TextView tvAiStatus;

    private ActivityResultLauncher<String[]> calendarPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchVibration = findViewById(R.id.switch_vibration);
        tvCalendarStatus = findViewById(R.id.tv_calendar_status);
        tvAiStatus = findViewById(R.id.tv_ai_status);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        boolean isNotif = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        boolean isVib = prefs.getBoolean(KEY_VIBRATION, true);

        switchDarkMode.setChecked(isDark);
        switchNotifications.setChecked(isNotif);
        switchVibration.setChecked(isVib);

        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            Toast.makeText(this, isChecked ? "Notificações ativadas" : "Notificações pausadas", Toast.LENGTH_SHORT).show();
        });

        switchVibration.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
            if (isChecked) {
                NotificationHelper.vibrateFeedback(this, 40);
            }
        });

        calendarPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    updateCalendarStatus();
                }
        );

        findViewById(R.id.btn_request_calendar_perm).setOnClickListener(v -> {
            if (!GoogleCalendarService.hasCalendarPermissions(this)) {
                calendarPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                });
            } else {
                Toast.makeText(this, "Permissões ativas. Sincronização pronta para uso.", Toast.LENGTH_SHORT).show();
            }
        });

        updateCalendarStatus();
        updateAiStatus();

        findViewById(R.id.btn_reload_sample_data).setOnClickListener(v -> reloadSampleData());
        findViewById(R.id.btn_clear_all_data).setOnClickListener(v -> clearAllData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCalendarStatus();
        updateAiStatus();
    }

    private void updateCalendarStatus() {
        if (GoogleCalendarService.hasCalendarPermissions(this)) {
            tvCalendarStatus.setText("Conectado");
            tvCalendarStatus.setTextColor(getColor(R.color.module_kaizen_dark));
            tvCalendarStatus.setBackgroundTintList(getColorStateList(R.color.module_kaizen_light));
        } else {
            tvCalendarStatus.setText("Não Autorizado");
            tvCalendarStatus.setTextColor(getColor(R.color.module_pomodoro_dark));
            tvCalendarStatus.setBackgroundTintList(getColorStateList(R.color.module_pomodoro_light));
        }
    }

    private void updateAiStatus() {
        String key = BuildConfig.GEMINI_API_KEY;
        if (key != null && !key.isEmpty() && !key.equals("MY_GEMINI_API_KEY")) {
            tvAiStatus.setText("Online (Gemini)");
            tvAiStatus.setTextColor(getColor(R.color.module_pareto));
        } else {
            tvAiStatus.setText("Modo Inteligente Offline");
            tvAiStatus.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void reloadSampleData() {
        new AlertDialog.Builder(this)
                .setTitle("Recarregar Dados de Exemplo")
                .setMessage("Deseja restaurar os dados de demonstração para todos os 8 módulos?")
                .setPositiveButton("Recarregar", (d, w) -> {
                    Toast.makeText(this, "Carregando dados de exemplo...", Toast.LENGTH_SHORT).show();
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        SampleDataGenerator.preloadSampleData(DatabaseClient.getInstance(this).getDatabase());
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.sample_data_loaded, Toast.LENGTH_SHORT).show();
                            NotificationHelper.vibrateFeedback(this, 40);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void clearAllData() {
        new AlertDialog.Builder(this)
                .setTitle("Limpar Todos os Dados")
                .setMessage("Atenção: todos os dados locais (quadros, metas, tarefas, notas, 5S e A3) serão apagados permanentemente.")
                .setPositiveButton("Excluir Tudo", (d, w) -> {
                    Toast.makeText(this, "Limpando dados...", Toast.LENGTH_SHORT).show();
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        com.example.organizer.data.database.OrganizerDatabase db = DatabaseClient.getInstance(this).getDatabase();
                        try {
                            java.util.List<com.example.organizer.data.entity.EisenhowerTask> tasks = db.eisenhowerDao().getTasksWithCalendarEventSync();
                            if (tasks != null) {
                                for (com.example.organizer.data.entity.EisenhowerTask task : tasks) {
                                    if (task.getGoogleCalendarEventId() > 0) {
                                        GoogleCalendarService.deleteTaskFromCalendar(SettingsActivity.this, task.getGoogleCalendarEventId());
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                        SampleDataGenerator.clearAllData(db);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.sample_data_cleared, Toast.LENGTH_SHORT).show();
                            NotificationHelper.vibrateFeedback(this, 80);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
