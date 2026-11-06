package com.example.organizer.ui.eisenhower;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.EisenhowerTask;
import com.example.organizer.data.service.GoogleCalendarService;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EisenhowerFragment extends Fragment implements EisenhowerTaskAdapter.TaskActionListener {

    private TextView tvCountQ1, tvCountQ2, tvCountQ3, tvCountQ4;
    private MaterialCardView cardQ1, cardQ2, cardQ3, cardQ4;
    private ChipGroup chipGroupFilters;
    private Chip chipCalendarSync;
    private EditText etSearch;
    private RecyclerView rvTasks;
    private TextView tvTasksEmpty;
    private ExtendedFloatingActionButton fabAddTask;

    private EisenhowerTaskAdapter adapter;
    private List<EisenhowerTask> allTasksCache = new ArrayList<>();
    private int currentFilterMode = 0; // 0: All, 1: Q1, 2: Q2, 3: Q3, 4: Q4, 5: Pending, 6: Completed
    private String currentSearchQuery = "";

    private ActivityResultLauncher<String[]> calendarPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        calendarPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_CALENDAR)) &&
                            Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_CALENDAR));
                    if (granted) {
                        Toast.makeText(requireContext(), "Permissão de Agenda concedida!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Permissão necessária para sincronizar eventos com Google Agenda", Toast.LENGTH_SHORT).show();
                    }
                    updateCalendarSyncChip();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eisenhower, container, false);

        tvCountQ1 = view.findViewById(R.id.tv_count_q1);
        tvCountQ2 = view.findViewById(R.id.tv_count_q2);
        tvCountQ3 = view.findViewById(R.id.tv_count_q3);
        tvCountQ4 = view.findViewById(R.id.tv_count_q4);

        cardQ1 = view.findViewById(R.id.card_q1);
        cardQ2 = view.findViewById(R.id.card_q2);
        cardQ3 = view.findViewById(R.id.card_q3);
        cardQ4 = view.findViewById(R.id.card_q4);

        chipGroupFilters = view.findViewById(R.id.chip_group_filters);
        chipCalendarSync = view.findViewById(R.id.chip_calendar_sync);
        etSearch = view.findViewById(R.id.et_search_tasks);
        rvTasks = view.findViewById(R.id.rv_eisenhower_tasks);
        tvTasksEmpty = view.findViewById(R.id.tv_tasks_empty);
        fabAddTask = view.findViewById(R.id.fab_add_task);

        setupRecyclerView();
        setupMatrixInteractions();
        setupFiltersAndSearch();
        setupCalendarChip();

        fabAddTask.setOnClickListener(v -> showTaskDialog(null, 1));

        observeTasks();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new EisenhowerTaskAdapter(requireContext(), this);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);
    }

    private void setupMatrixInteractions() {
        cardQ1.setOnClickListener(v -> selectFilterChip(R.id.chip_filter_q1));
        cardQ2.setOnClickListener(v -> selectFilterChip(R.id.chip_filter_q2));
        cardQ3.setOnClickListener(v -> selectFilterChip(R.id.chip_filter_q3));
        cardQ4.setOnClickListener(v -> selectFilterChip(R.id.chip_filter_q4));
    }

    private void selectFilterChip(int chipId) {
        Chip chip = chipGroupFilters.findViewById(chipId);
        if (chip != null) {
            chip.setChecked(true);
        }
    }

    private void setupFiltersAndSearch() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilterMode = 0;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_filter_all) currentFilterMode = 0;
                else if (id == R.id.chip_filter_q1) currentFilterMode = 1;
                else if (id == R.id.chip_filter_q2) currentFilterMode = 2;
                else if (id == R.id.chip_filter_q3) currentFilterMode = 3;
                else if (id == R.id.chip_filter_q4) currentFilterMode = 4;
                else if (id == R.id.chip_filter_pending) currentFilterMode = 5;
                else if (id == R.id.chip_filter_completed) currentFilterMode = 6;
            }
            applyFilters();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCalendarChip() {
        updateCalendarSyncChip();
        chipCalendarSync.setOnClickListener(v -> {
            if (!GoogleCalendarService.hasCalendarPermissions(requireContext())) {
                calendarPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                });
            } else {
                Toast.makeText(requireContext(), "Google Agenda sincronizada nativamente para tarefas com data e hora.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateCalendarSyncChip() {
        if (!GoogleCalendarService.hasCalendarPermissions(requireContext())) {
            chipCalendarSync.setText("Google Agenda não conectado");
            chipCalendarSync.setChipIconResource(R.drawable.ic_calendar);
        } else {
            boolean hasSyncedTask = false;
            for (EisenhowerTask task : allTasksCache) {
                if (task.isSyncedWithCalendar() && task.getGoogleCalendarEventId() > 0) {
                    hasSyncedTask = true;
                    break;
                }
            }
            if (hasSyncedTask) {
                chipCalendarSync.setText("Google Agenda: Sincronizado");
            } else {
                chipCalendarSync.setText("Google Agenda: Permissão concedida (Pronto)");
            }
            chipCalendarSync.setChipIconResource(R.drawable.ic_check);
        }
    }

    private void observeTasks() {
        DatabaseClient.getInstance(requireContext())
                .getDatabase()
                .eisenhowerDao()
                .getAllTasks()
                .observe(getViewLifecycleOwner(), tasks -> {
                    allTasksCache = (tasks != null) ? tasks : new ArrayList<>();
                    updateQuadrantCounts(allTasksCache);
                    applyFilters();
                });
    }

    private void updateQuadrantCounts(List<EisenhowerTask> tasks) {
        int q1 = 0, q2 = 0, q3 = 0, q4 = 0;
        for (EisenhowerTask task : tasks) {
            if (!task.isCompleted()) {
                if (task.getQuadrant() == 1) q1++;
                else if (task.getQuadrant() == 2) q2++;
                else if (task.getQuadrant() == 3) q3++;
                else if (task.getQuadrant() == 4) q4++;
            }
        }
        tvCountQ1.setText(String.valueOf(q1));
        tvCountQ2.setText(String.valueOf(q2));
        tvCountQ3.setText(String.valueOf(q3));
        tvCountQ4.setText(String.valueOf(q4));
    }

    private void applyFilters() {
        List<EisenhowerTask> filtered = new ArrayList<>();
        for (EisenhowerTask task : allTasksCache) {
            // Mode filter
            boolean matchesMode = true;
            if (currentFilterMode >= 1 && currentFilterMode <= 4) {
                matchesMode = (task.getQuadrant() == currentFilterMode);
            } else if (currentFilterMode == 5) {
                matchesMode = !task.isCompleted();
            } else if (currentFilterMode == 6) {
                matchesMode = task.isCompleted();
            }

            if (!matchesMode) continue;

            // Search filter
            if (!currentSearchQuery.isEmpty()) {
                String title = task.getTitle().toLowerCase(Locale.getDefault());
                String desc = (task.getDescription() != null) ? task.getDescription().toLowerCase(Locale.getDefault()) : "";
                if (!title.contains(currentSearchQuery) && !desc.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filtered.add(task);
        }

        adapter.setTasks(filtered);
        tvTasksEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onToggleCompleted(EisenhowerTask task, boolean completed) {
        task.setCompleted(completed);
        DatabaseClient.getInstance(requireContext()).getExecutor().execute(() -> {
            DatabaseClient.getInstance(requireContext()).getDatabase().eisenhowerDao().updateTask(task);
        });
        NotificationHelper.vibrateFeedback(requireContext(), 40);
    }

    @Override
    public void onEditTask(EisenhowerTask task) {
        showTaskDialog(task, task.getQuadrant());
    }

    @Override
    public void onDeleteTask(EisenhowerTask task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir Tarefa")
                .setMessage("Deseja realmente remover \"" + task.getTitle() + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    DatabaseClient.getInstance(requireContext()).getExecutor().execute(() -> {
                        if (task.getGoogleCalendarEventId() > 0) {
                            GoogleCalendarService.deleteTaskFromCalendar(requireContext(), task.getGoogleCalendarEventId());
                        }
                        DatabaseClient.getInstance(requireContext()).getDatabase().eisenhowerDao().deleteTask(task);
                    });
                    NotificationHelper.vibrateFeedback(requireContext(), 60);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showTaskDialog(@Nullable EisenhowerTask existingTask, int defaultQuadrant) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_eisenhower_task, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etTitle = dialogView.findViewById(R.id.et_task_title);
        EditText etDesc = dialogView.findViewById(R.id.et_task_desc);
        RadioGroup rgQuadrants = dialogView.findViewById(R.id.rg_quadrants);
        View btnPickDate = dialogView.findViewById(R.id.btn_pick_date);
        View btnPickTime = dialogView.findViewById(R.id.btn_pick_time);
        SwitchMaterial switchCalendar = dialogView.findViewById(R.id.switch_calendar_sync);
        ChipGroup chipGroupPrio = dialogView.findViewById(R.id.chip_group_priority);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_task);
        View btnSave = dialogView.findViewById(R.id.btn_save_task);

        final Calendar calendar = Calendar.getInstance();
        final String[] selectedDate = {""};
        final String[] selectedTime = {""};

        if (existingTask != null) {
            tvDialogTitle.setText("Editar Tarefa");
            etTitle.setText(existingTask.getTitle());
            etDesc.setText(existingTask.getDescription());
            selectedDate[0] = existingTask.getDate();
            selectedTime[0] = existingTask.getTime();
            switchCalendar.setChecked(existingTask.isSyncedWithCalendar());

            int rbId = R.id.rb_q1;
            if (existingTask.getQuadrant() == 2) rbId = R.id.rb_q2;
            else if (existingTask.getQuadrant() == 3) rbId = R.id.rb_q3;
            else if (existingTask.getQuadrant() == 4) rbId = R.id.rb_q4;
            rgQuadrants.check(rbId);
        } else {
            int rbId = R.id.rb_q1;
            if (defaultQuadrant == 2) rbId = R.id.rb_q2;
            else if (defaultQuadrant == 3) rbId = R.id.rb_q3;
            else if (defaultQuadrant == 4) rbId = R.id.rb_q4;
            rgQuadrants.check(rbId);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate[0] = sdf.format(calendar.getTime());
                Toast.makeText(requireContext(), "Data selecionada: " + selectedDate[0], Toast.LENGTH_SHORT).show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPickTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                selectedTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                Toast.makeText(requireContext(), "Horário: " + selectedTime[0], Toast.LENGTH_SHORT).show();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError("Informe o título da tarefa");
                return;
            }

            String desc = etDesc.getText().toString().trim();
            int selectedQuad = 1;
            int checkedRb = rgQuadrants.getCheckedRadioButtonId();
            if (checkedRb == R.id.rb_q2) selectedQuad = 2;
            else if (checkedRb == R.id.rb_q3) selectedQuad = 3;
            else if (checkedRb == R.id.rb_q4) selectedQuad = 4;

            String priority = "Média";
            if (chipGroupPrio.getCheckedChipId() == R.id.chip_prio_high) priority = "Alta";
            else if (chipGroupPrio.getCheckedChipId() == R.id.chip_prio_low) priority = "Baixa";

            boolean shouldSyncCalendar = switchCalendar.isChecked();
            final int finalQuad = selectedQuad;
            final String finalPriority = priority;

            DatabaseClient.getInstance(requireContext()).getExecutor().execute(() -> {
                EisenhowerTask taskToSave;
                if (existingTask != null) {
                    taskToSave = existingTask;
                    taskToSave.setTitle(title);
                    taskToSave.setDescription(desc);
                    taskToSave.setQuadrant(finalQuad);
                    taskToSave.setPriority(finalPriority);
                    taskToSave.setDate(selectedDate[0]);
                    taskToSave.setTime(selectedTime[0]);
                    taskToSave.setTimestamp(calendar.getTimeInMillis());
                } else {
                    taskToSave = new EisenhowerTask(
                            title,
                            desc,
                            selectedDate[0],
                            selectedTime[0],
                            calendar.getTimeInMillis(),
                            finalPriority,
                            finalQuad,
                            false,
                            0,
                            false,
                            0
                    );
                }

                if (shouldSyncCalendar && GoogleCalendarService.hasCalendarPermissions(requireContext())) {
                    long eventId = GoogleCalendarService.syncTaskToCalendar(requireContext(), taskToSave);
                    if (eventId > 0) {
                        taskToSave.setGoogleCalendarEventId(eventId);
                        taskToSave.setSyncedWithCalendar(true);
                    }
                } else if (!shouldSyncCalendar && taskToSave.getGoogleCalendarEventId() > 0) {
                    // Desativar sincronização: remover evento do Google Agenda e limpar vínculo local (Seção 13.4)
                    GoogleCalendarService.deleteTaskFromCalendar(requireContext(), taskToSave.getGoogleCalendarEventId());
                    taskToSave.setGoogleCalendarEventId(0);
                    taskToSave.setSyncedWithCalendar(false);
                }

                if (existingTask != null) {
                    DatabaseClient.getInstance(requireContext()).getDatabase().eisenhowerDao().updateTask(taskToSave);
                } else {
                    DatabaseClient.getInstance(requireContext()).getDatabase().eisenhowerDao().insertTask(taskToSave);
                }
            });

            dialog.dismiss();
            Toast.makeText(requireContext(), "Tarefa salva com sucesso!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(requireContext(), 30);
        });

        dialog.show();
    }
}
