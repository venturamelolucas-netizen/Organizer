package com.example.organizer.ui.fives;

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

import java.util.ArrayList;
import java.util.List;

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
        setupSwipeHelper();

        fabAdd.setOnClickListener(v -> showAddItemDialog());

        ensureProjectAndObserve();
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
                tvStepInstruction.setText("Classifique os itens analisados: deslize para a DIREITA para Essencial ou para a ESQUERDA para Zona de Descarte.");
                break;
            case 2:
                tvStepTitle.setText("Etapa 2: Seiton (Organização e Ordenação)");
                tvStepInstruction.setText("Organize os itens classificados como Essenciais. Defina local, categoria e regra de posicionamento.");
                break;
            case 3:
                tvStepTitle.setText("Etapa 3: Seiso (Limpeza e Inspeção)");
                tvStepInstruction.setText("Checklist de limpeza e manutenção preventiva. Marque as ações para monitorar a conclusão.");
                break;
            case 4:
                tvStepTitle.setText("Etapa 4: Seiketsu + Shitsuke (Padronização e Disciplina)");
                tvStepInstruction.setText("Transforme as ações em rotinas consolidadas. Ative lembretes e acompanhe a consistência e streaks.");
                break;
        }
    }

    private void setupSwipeHelper() {
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < currentStepItems.size() && currentStep == 1) {
                    FiveSItem item = currentStepItems.get(pos);
                    if (direction == ItemTouchHelper.RIGHT) {
                        onSetEssential(item, true);
                        Toast.makeText(FiveSActivity.this, "Item marcado como Essencial!", Toast.LENGTH_SHORT).show();
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

            runOnUiThread(() -> {
                selectStep(1);
            });
        });
    }

    private void loadStepItems() {
        if (projectId == -1) return;

        if (currentStep == 1) {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().getItemsByStep(projectId, "SEIRI")
                    .observe(this, items -> {
                        currentStepItems = (items != null) ? items : new ArrayList<>();
                        adapter.setData(currentStepItems, FiveSItemAdapter.VIEW_TYPE_SEIRI);
                    });
        } else if (currentStep == 2) {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().getEssentialItems(projectId)
                    .observe(this, items -> {
                        currentStepItems = (items != null) ? items : new ArrayList<>();
                        adapter.setData(currentStepItems, FiveSItemAdapter.VIEW_TYPE_SEITON);
                    });
        } else if (currentStep == 3) {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().getItemsByStep(projectId, "SEISO")
                    .observe(this, items -> {
                        currentStepItems = (items != null) ? items : new ArrayList<>();
                        adapter.setData(currentStepItems, FiveSItemAdapter.VIEW_TYPE_SEISO);
                        updateSeisoMetrics(currentStepItems);
                    });
        } else if (currentStep == 4) {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().getItemsByStep(projectId, "SEIKETSU_SHITSUKE")
                    .observe(this, items -> {
                        currentStepItems = (items != null) ? items : new ArrayList<>();
                        adapter.setData(currentStepItems, FiveSItemAdapter.VIEW_TYPE_ROUTINE);
                    });
        }
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
        if (enabled) {
            item.setStreakCount(item.getStreakCount() + 1);
            NotificationHelper.scheduleNotification(
                    this,
                    (int) item.getId(),
                    "Rotina 5S: " + item.getName(),
                    "Hora de executar sua rotina diária de organização.",
                    NotificationHelper.CHANNEL_FIVES,
                    System.currentTimeMillis() + 60000L
            );
            Toast.makeText(this, "Notificação de rotina ativada!", Toast.LENGTH_SHORT).show();
        } else {
            NotificationHelper.cancelNotification(this, (int) item.getId());
        }

        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().updateItem(item);
        });
        NotificationHelper.vibrateFeedback(this, 30);
    }

    @Override
    public void onDeleteItem(FiveSItem item) {
        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            DatabaseClient.getInstance(this).getDatabase().fiveSDao().deleteItem(item);
        });
    }

    private void showAddItemDialog() {
        final EditText inputName = new EditText(this);
        inputName.setHint("Nome do item ou ação");
        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Descrição / Detalhes");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(inputName);
        layout.addView(inputDesc);

        String title = "Adicionar Item (" + (currentStep == 1 ? "Seiri" : (currentStep == 2 ? "Seiton" : (currentStep == 3 ? "Seiso" : "Rotina"))) + ")";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Adicionar", (d, w) -> {
                    String name = inputName.getText().toString().trim();
                    if (!name.isEmpty() && projectId != -1) {
                        String stepName = (currentStep == 1) ? "SEIRI" : (currentStep == 2 ? "SEIRI" : (currentStep == 3 ? "SEISO" : "SEIKETSU_SHITSUKE"));
                        FiveSItem newItem = new FiveSItem(projectId, stepName, name, inputDesc.getText().toString().trim());
                        if (currentStep == 2) {
                            newItem.setEssential(true);
                            newItem.setLocation("Bancada");
                            newItem.setPlacementRule("Organizado");
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
