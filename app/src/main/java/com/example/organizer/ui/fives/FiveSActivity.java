package com.example.organizer.ui.fives;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.data.entity.FiveSProject;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FiveSActivity extends AppCompatActivity implements FiveSItemAdapter.FiveSActionListener {

    private MaterialButton btnPillSeiri, btnPillSeiton, btnPillSeiso, btnPillSeiketsu;
    private TextView tvStepTitle, tvStepInstruction;
    private View layoutSeisoProgress;
    private TextView tvSeisoPercent;
    private ProgressBar pbSeisoProgress;
    private RecyclerView rvItems;
    private FloatingActionButton fabAdd;

    private FiveSItemAdapter adapter;
    private int currentStep = 1; // 1: Seiri, 2: Seiton, 3: Seiso, 4: Seiketsu+Shitsuke
    private long projectId = -1;
    private List<FiveSItem> currentStepItems = new ArrayList<>();

    // Single active LiveData and Observer to prevent rogue background observers from cross-updating steps
    private LiveData<List<FiveSItem>> activeStepLiveData = null;
    private Observer<List<FiveSItem>> activeStepObserver = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fives);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_fives);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnPillSeiri = findViewById(R.id.btn_pill_seiri);
        btnPillSeiton = findViewById(R.id.btn_pill_seiton);
        btnPillSeiso = findViewById(R.id.btn_pill_seiso);
        btnPillSeiketsu = findViewById(R.id.btn_pill_seiketsu);

        tvStepTitle = findViewById(R.id.tv_fives_step_title);
        tvStepInstruction = findViewById(R.id.tv_fives_step_instruction);
        layoutSeisoProgress = findViewById(R.id.layout_seiso_progress);
        tvSeisoPercent = findViewById(R.id.tv_seiso_percent);
        pbSeisoProgress = findViewById(R.id.pb_seiso_progress);
        rvItems = findViewById(R.id.rv_fives_items);
        fabAdd = findViewById(R.id.fab_fives_add);

        setupRecyclerView();
        setupStepPills();
        setupItemTouchHelper();

        fabAdd.setOnClickListener(v -> showAddItemDialog());

        ensureProjectAndObserve();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeStepLiveData != null && activeStepObserver != null) {
            activeStepLiveData.removeObserver(activeStepObserver);
        }
    }

    private void setupRecyclerView() {
        adapter = new FiveSItemAdapter(this, this);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
    }

    private void setupStepPills() {
        btnPillSeiri.setOnClickListener(v -> selectStep(1));
        btnPillSeiton.setOnClickListener(v -> selectStep(2));
        btnPillSeiso.setOnClickListener(v -> selectStep(3));
        btnPillSeiketsu.setOnClickListener(v -> selectStep(4));
    }

    private void selectStep(int step) {
        currentStep = step;
        updatePillsAppearance();
        updateStepHeader();
        loadStepItems();
        NotificationHelper.vibrateFeedback(this, 25);
    }

    private void updatePillsAppearance() {
        resetPillStyle(btnPillSeiri, "1. Seiri", currentStep == 1);
        resetPillStyle(btnPillSeiton, "2. Seiton", currentStep == 2);
        resetPillStyle(btnPillSeiso, "3. Seiso", currentStep == 3);
        resetPillStyle(btnPillSeiketsu, "4. Rotinas", currentStep == 4);
    }

    private void resetPillStyle(MaterialButton btn, String text, boolean isSelected) {
        btn.setText(text);
        if (isSelected) {
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.module_5s));
            btn.setTextColor(ContextCompat.getColor(this, R.color.white));
            btn.setStrokeWidth(0);
        } else {
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            btn.setStrokeColorResource(R.color.card_stroke_light);
            btn.setStrokeWidth(2);
        }
    }

    private void updateStepHeader() {
        layoutSeisoProgress.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);
        switch (currentStep) {
            case 1:
                tvStepTitle.setText("Etapa 1: Seiri (Utilização e Descarte)");
                tvStepInstruction.setText("Classifique os itens analisados: marque como Essencial para enviar ao Seiton ou Zona de Descarte.");
                break;
            case 2:
                tvStepTitle.setText("Etapa 2: Seiton (Organização e Ordenação)");
                tvStepInstruction.setText("Organize os itens classificados como Essenciais. Defina local, categoria, prioridade e arraste para reordenar.");
                break;
            case 3:
                tvStepTitle.setText("Etapa 3: Seiso (Limpeza e Inspeção)");
                tvStepInstruction.setText("Checklist de limpeza e manutenção preventiva. Marque as ações para monitorar a conclusão.");
                break;
            case 4:
                tvStepTitle.setText("Etapa 4: Seiketsu + Shitsuke (Padronização e Disciplina)");
                tvStepInstruction.setText("Rotinas diárias e periódicas de manutenção 5S. Ative lembretes nos horários desejados e registre conclusões.");
                break;
        }
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (currentStep != 2) return false;

                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                if (fromPos < 0 || toPos < 0 || fromPos >= currentStepItems.size() || toPos >= currentStepItems.size()) {
                    return false;
                }

                Collections.swap(currentStepItems, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (currentStep == 2 && !currentStepItems.isEmpty()) {
                    // Persist updated positions in Room
                    DatabaseClient.getInstance(FiveSActivity.this).getExecutor().execute(() -> {
                        for (int i = 0; i < currentStepItems.size(); i++) {
                            FiveSItem item = currentStepItems.get(i);
                            item.setOrderIndex(i);
                            DatabaseClient.getInstance(FiveSActivity.this).getDatabase().fiveSDao().updateItem(item);
                        }
                    });
                }
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Only allow swipe in Step 1 (Seiri)
                if (currentStep == 1) {
                    return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                }
                return 0;
            }

            @Override
            public int getDragDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Only allow reordering drag in Step 2 (Seiton)
                if (currentStep == 2) {
                    return ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                }
                return 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < currentStepItems.size() && currentStep == 1) {
                    FiveSItem item = currentStepItems.get(pos);
                    if (direction == ItemTouchHelper.RIGHT) {
                        onSetEssential(item, true);
                        Toast.makeText(FiveSActivity.this, "Item marcado como Essencial → Visível no Seiton!", Toast.LENGTH_SHORT).show();
                    } else {
                        onSetDiscarded(item, true);
                        Toast.makeText(FiveSActivity.this, "Item movido para Zona de Descarte!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    adapter.notifyItemChanged(pos);
                }
            }
        });
        touchHelper.attachToRecyclerView(rvItems);
    }

    private void ensureProjectAndObserve() {
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            FiveSProject proj = DatabaseClient.getInstance(this).getDatabase().fiveSDao().getDefaultProject();
            if (proj == null) {
                long id = DatabaseClient.getInstance(this).getDatabase().fiveSDao().insertProject(
                        new FiveSProject("Meu Projeto 5S", "Aplicação dos 5 sensos", System.currentTimeMillis())
                );
                projectId = id;
            } else {
                projectId = proj.getId();
            }

            runOnUiThread(() -> selectStep(1));
        });
    }

    private void loadStepItems() {
        if (projectId == -1) return;

        final int targetStep = currentStep;

        // Clean up previous observer to prevent unintended step switches and cross-talk
        if (activeStepLiveData != null && activeStepObserver != null) {
            activeStepLiveData.removeObserver(activeStepObserver);
            activeStepLiveData = null;
            activeStepObserver = null;
        }

        LiveData<List<FiveSItem>> newLiveData;
        if (targetStep == 1) {
            newLiveData = DatabaseClient.getInstance(this).getDatabase().fiveSDao().getItemsByStep(projectId, "SEIRI");
        } else if (targetStep == 2) {
            newLiveData = DatabaseClient.getInstance(this).getDatabase().fiveSDao().getEssentialItems(projectId);
        } else if (targetStep == 3) {
            newLiveData = DatabaseClient.getInstance(this).getDatabase().fiveSDao().getItemsByStep(projectId, "SEISO");
        } else {
            newLiveData = DatabaseClient.getInstance(this).getDatabase().fiveSDao().getItemsByStep(projectId, "SEIKETSU_SHITSUKE");
        }

        activeStepLiveData = newLiveData;
        activeStepObserver = items -> {
            // Guard: ensure step has not changed while event was dispatching
            if (currentStep != targetStep) return;

            currentStepItems = (items != null) ? items : new ArrayList<>();
            int viewType;
            if (targetStep == 1) viewType = FiveSItemAdapter.VIEW_TYPE_SEIRI;
            else if (targetStep == 2) viewType = FiveSItemAdapter.VIEW_TYPE_SEITON;
            else if (targetStep == 3) {
                viewType = FiveSItemAdapter.VIEW_TYPE_SEISO;
                updateSeisoMetrics(currentStepItems);
            } else {
                viewType = FiveSItemAdapter.VIEW_TYPE_ROUTINE;
            }
            adapter.setData(currentStepItems, viewType);
        };

        activeStepLiveData.observe(this, activeStepObserver);
    }

    private void updateSeisoMetrics(List<FiveSItem> items) {
        if (items.isEmpty()) {
            pbSeisoProgress.setProgress(0);
            tvSeisoPercent.setText("0% concluído");
            return;
        }
        int completed = 0;
        for (FiveSItem i : items) {
            if (i.isCompleted()) completed++;
        }
        int percent = (completed * 100) / items.size();
        pbSeisoProgress.setProgress(percent);
        tvSeisoPercent.setText(percent + "% concluído (" + completed + "/" + items.size() + ")");
    }

    @Override
    public void onSetEssential(FiveSItem item, boolean essential) {
        item.setEssential(essential);
        item.setDiscarded(false);
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
        });
        NotificationHelper.vibrateFeedback(this, 40);
    }

    @Override
    public void onSetDiscarded(FiveSItem item, boolean discarded) {
        item.setDiscarded(discarded);
        item.setEssential(false);
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
        });
        NotificationHelper.vibrateFeedback(this, 40);
    }

    @Override
    public void onToggleCompleted(FiveSItem item, boolean completed) {
        item.setCompleted(completed);
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
        });
        NotificationHelper.vibrateFeedback(this, 40);
    }

    @Override
    public void onToggleRoutine(FiveSItem item, boolean enabled) {
        item.setRoutineEnabled(enabled);

        // Activating routine does NOT increase streak (Section 5)
        if (enabled) {
            long nextOccurrence = NotificationHelper.getNextOccurrenceMillis(item.getRoutineTime(), item.getRoutineDays());
            item.setNextExecutionDate(nextOccurrence);

            NotificationHelper.scheduleRoutineNotification(
                    this,
                    item.getId(),
                    (int) item.getId() + 6000,
                    "Rotina 5S: " + item.getName(),
                    "Hora de executar sua rotina de 5S (" + item.getRoutineTime() + ").",
                    NotificationHelper.CHANNEL_FIVES,
                    nextOccurrence
            );

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault());
            Toast.makeText(this, "Lembrete ativado para " + sdf.format(new Date(nextOccurrence)), Toast.LENGTH_SHORT).show();
        } else {
            NotificationHelper.cancelNotification(this, (int) item.getId() + 6000);
            Toast.makeText(this, "Lembrete desativado", Toast.LENGTH_SHORT).show();
        }

        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
        });
        NotificationHelper.vibrateFeedback(this, 30);
    }

    @Override
    public void onExecuteRoutine(FiveSItem item) {
        long now = System.currentTimeMillis();
        if (NotificationHelper.isSameDay(item.getLastExecutionDate(), now)) {
            Toast.makeText(this, "Esta rotina já foi executada hoje!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (NotificationHelper.isYesterday(item.getLastExecutionDate())) {
            item.setStreakCount(item.getStreakCount() + 1);
        } else {
            item.setStreakCount(1);
        }
        item.setLastExecutionDate(now);

        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
        });

        Toast.makeText(this, "Rotina concluída! Sequência: " + item.getStreakCount() + " dias 🔥", Toast.LENGTH_SHORT).show();
        NotificationHelper.vibrateFeedback(this, 40);
    }

    @Override
    public void onEditRoutine(FiveSItem item) {
        final EditText inputName = new EditText(this);
        inputName.setText(item.getName());
        inputName.setHint("Nome da rotina");

        final EditText inputDesc = new EditText(this);
        inputDesc.setText(item.getDescription());
        inputDesc.setHint("Descrição da rotina");

        final MaterialButton btnTime = new MaterialButton(this);
        final String[] currentTime = {item.getRoutineTime()};
        btnTime.setText("Horário: " + currentTime[0]);
        btnTime.setOnClickListener(v -> {
            int h = 18, m = 0;
            try {
                String[] p = currentTime[0].split(":");
                h = Integer.parseInt(p[0]);
                m = Integer.parseInt(p[1]);
            } catch (Exception ignored) {}
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                currentTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                btnTime.setText("Horário: " + currentTime[0]);
            }, h, m, true).show();
        });

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(inputName);
        layout.addView(inputDesc);
        layout.addView(btnTime);

        new AlertDialog.Builder(this)
                .setTitle("Editar Rotina 5S")
                .setView(layout)
                .setPositiveButton("Salvar", (d, w) -> {
                    String name = inputName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        item.setName(name);
                        item.setDescription(inputDesc.getText().toString().trim());
                        item.setRoutineTime(currentTime[0]);

                        if (item.isRoutineEnabled()) {
                            long nextOccurrence = NotificationHelper.getNextOccurrenceMillis(currentTime[0], item.getRoutineDays());
                            item.setNextExecutionDate(nextOccurrence);
                            NotificationHelper.scheduleRoutineNotification(
                                    this,
                                    item.getId(),
                                    (int) item.getId() + 6000,
                                    "Rotina 5S: " + item.getName(),
                                    "Hora de executar sua rotina de 5S.",
                                    NotificationHelper.CHANNEL_FIVES,
                                    nextOccurrence
                            );
                        }

                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
                        });
                        Toast.makeText(this, "Rotina atualizada!", Toast.LENGTH_SHORT).show();
                        NotificationHelper.vibrateFeedback(this, 30);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDeleteRoutine(FiveSItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Rotina")
                .setMessage("Deseja realmente excluir a rotina \"" + item.getName() + "\"?")
                .setPositiveButton("Excluir", (d, w) -> {
                    NotificationHelper.cancelNotification(this, (int) item.getId() + 6000);
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().fiveSDao().deleteItem(item);
                    });
                    Toast.makeText(this, "Rotina excluída", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 40);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onEditSeiton(FiveSItem item) {
        final EditText inputLoc = new EditText(this);
        inputLoc.setText(item.getLocation());
        inputLoc.setHint("Local demarcado (ex: Mesa principal)");

        final EditText inputCat = new EditText(this);
        inputCat.setText(item.getCategory());
        inputCat.setHint("Categoria (ex: Ferramentas, Ergonomia)");

        final EditText inputRule = new EditText(this);
        inputRule.setText(item.getPlacementRule());
        inputRule.setHint("Regra de posicionamento (ex: Sempre centralizado)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(inputLoc);
        layout.addView(inputCat);
        layout.addView(inputRule);

        new AlertDialog.Builder(this)
                .setTitle("Organização no Seiton: " + item.getName())
                .setView(layout)
                .setPositiveButton("Salvar", (d, w) -> {
                    item.setLocation(inputLoc.getText().toString().trim());
                    item.setCategory(inputCat.getText().toString().trim());
                    item.setPlacementRule(inputRule.getText().toString().trim());

                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
                    });
                    Toast.makeText(this, "Organização salva!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 30);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onRemoveFromSeiton(FiveSItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Remover do Seiton")
                .setMessage("Deseja desorganizar e remover \"" + item.getName() + "\" do Seiton? O item continuará existindo no Seiri como pendente de triagem.")
                .setPositiveButton("Remover", (d, w) -> {
                    item.setEssential(false);
                    item.setLocation("");
                    item.setCategory("");
                    item.setPlacementRule("");

                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
                    });
                    Toast.makeText(this, "Item removido do Seiton (mantido no Seiri)", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 35);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDeleteItem(FiveSItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Item do Seiri")
                .setMessage("Deseja realmente remover permanentemente \"" + item.getName() + "\" do 5S?")
                .setPositiveButton("Excluir", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().fiveSDao().deleteItem(item);
                    });
                    Toast.makeText(this, "Item excluído!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 40);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddItemDialog() {
        if (currentStep == 2) {
            new AlertDialog.Builder(this)
                    .setTitle("Itens no Seiton (Organização)")
                    .setMessage("No Seiton você organiza exclusivamente os itens classificados como Essenciais na etapa 1 (Seiri).\n\nPara organizar novos itens, cadastre-os no Seiri e marque-os como 'Essencial' (ou deslize o item para a direita). Eles aparecerão aqui automaticamente!")
                    .setPositiveButton("Ir para o Seiri", (d, w) -> selectStep(1))
                    .setNegativeButton("Fechar", null)
                    .show();
            return;
        }

        final EditText inputName = new EditText(this);
        inputName.setHint("Nome do item ou ação");
        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Descrição / Detalhes");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(inputName);
        layout.addView(inputDesc);

        String title = "Adicionar (" + (currentStep == 1 ? "Item para Seiri" : (currentStep == 3 ? "Ação de Limpeza (Seiso)" : "Rotina 5S")) + ")";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Adicionar", (d, w) -> {
                    String name = inputName.getText().toString().trim();
                    if (!name.isEmpty() && projectId != -1) {
                        String desc = inputDesc.getText().toString().trim();
                        FiveSItem newItem;

                        if (currentStep == 1) {
                            // Seiri: triagem
                            newItem = new FiveSItem(projectId, "SEIRI", name, desc);
                            newItem.setEssential(false);
                            newItem.setDiscarded(false);
                        } else if (currentStep == 3) {
                            // Seiso: checklist de limpeza
                            newItem = new FiveSItem(projectId, "SEISO", name, desc);
                        } else {
                            // Seiketsu + Shitsuke: rotina
                            newItem = new FiveSItem(projectId, "SEIKETSU_SHITSUKE", name, desc);
                            newItem.setRoutineFrequency("Diário");
                            newItem.setRoutineDays("Todos os dias");
                            newItem.setRoutineTime("18:00");
                            newItem.setRoutineEnabled(false);
                            newItem.setStreakCount(0);
                        }

                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            DatabaseClient.getInstance(this).getDatabase().fiveSDao().insertItem(newItem);
                        });
                        NotificationHelper.vibrateFeedback(this, 30);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
