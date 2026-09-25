package com.example.organizer.ui.kanban;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.KanbanBoard;
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.entity.KanbanList;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class KanbanActivity extends AppCompatActivity implements KanbanColumnAdapter.ColumnListener {

    private Spinner spinnerBoards;
    private RecyclerView rvColumns;
    private FloatingActionButton fabAddList;

    private KanbanColumnAdapter columnAdapter;
    private List<KanbanBoard> currentBoards = new ArrayList<>();
    private long selectedBoardId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kanban);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_kanban);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        spinnerBoards = findViewById(R.id.spinner_boards);
        rvColumns = findViewById(R.id.rv_kanban_columns);
        fabAddList = findViewById(R.id.fab_add_list);

        findViewById(R.id.btn_add_board).setOnClickListener(v -> showAddBoardDialog());
        fabAddList.setOnClickListener(v -> showAddListDialog());

        setupRecyclerView();
        observeBoards();
    }

    private void setupRecyclerView() {
        columnAdapter = new KanbanColumnAdapter(this, this, this);
        rvColumns.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvColumns.setAdapter(columnAdapter);
    }

    private void observeBoards() {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .kanbanDao()
                .getAllBoards()
                .observe(this, boards -> {
                    currentBoards = (boards != null) ? boards : new ArrayList<>();
                    if (currentBoards.isEmpty()) {
                        // Create default board if none
                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            long id = DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertBoard(
                                    new KanbanBoard("Meu Primeiro Quadro", "Quadro de gestão visual", System.currentTimeMillis())
                            );
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(id, "A Fazer", 0));
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(id, "Em Progresso", 1));
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(id, "Concluído", 2));
                        });
                        return;
                    }

                    List<String> titles = new ArrayList<>();
                    for (KanbanBoard b : currentBoards) {
                        titles.add(b.getTitle());
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles);
                    spinnerBoards.setAdapter(spinnerAdapter);

                    spinnerBoards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedBoardId = currentBoards.get(position).getId();
                            observeColumnsForBoard(selectedBoardId);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    if (selectedBoardId == -1 && !currentBoards.isEmpty()) {
                        selectedBoardId = currentBoards.get(0).getId();
                        observeColumnsForBoard(selectedBoardId);
                    }
                });
    }

    private void observeColumnsForBoard(long boardId) {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .kanbanDao()
                .getListsByBoard(boardId)
                .observe(this, lists -> {
                    columnAdapter.setLists(lists);
                });
    }

    private void showAddBoardDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nome do Quadro (ex: Marketing, Sprint 1)");
        new AlertDialog.Builder(this)
                .setTitle("Novo Quadro Kanban")
                .setView(input)
                .setPositiveButton("Criar", (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (!title.isEmpty()) {
                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            long bId = DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertBoard(
                                    new KanbanBoard(title, "", System.currentTimeMillis())
                            );
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(bId, "A Fazer", 0));
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(bId, "Concluído", 1));
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddListDialog() {
        if (selectedBoardId == -1) return;
        final EditText input = new EditText(this);
        input.setHint("Nome da Coluna (ex: Em Revisão)");
        new AlertDialog.Builder(this)
                .setTitle("Nova Coluna")
                .setView(input)
                .setPositiveButton("Adicionar", (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (!title.isEmpty()) {
                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(
                                    new KanbanList(selectedBoardId, title, 99)
                            );
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onAddCard(KanbanList list) {
        showCardDialog(list.getId(), null);
    }

    @Override
    public void onEditCard(KanbanCard card) {
        showCardDialog(card.getListId(), card);
    }

    @Override
    public void onDeleteList(KanbanList list) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Coluna")
                .setMessage("Deseja excluir a coluna \"" + list.getTitle() + "\" e todos os seus cartões?")
                .setPositiveButton("Excluir", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().kanbanDao().deleteList(list);
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCardDialog(long listId, @Nullable KanbanCard existingCard) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kanban_card, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_card_title);
        EditText etTitle = dialogView.findViewById(R.id.et_kanban_card_title);
        EditText etDesc = dialogView.findViewById(R.id.et_kanban_card_desc);
        ChipGroup chipGroupPrio = dialogView.findViewById(R.id.chip_group_kanban_prio);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_kanban_card);
        View btnSave = dialogView.findViewById(R.id.btn_save_kanban_card);

        if (existingCard != null) {
            tvTitle.setText("Editar Cartão");
            etTitle.setText(existingCard.getTitle());
            etDesc.setText(existingCard.getDescription());
            if ("Alta".equalsIgnoreCase(existingCard.getPriority())) {
                chipGroupPrio.check(R.id.chip_kanban_alta);
            } else if ("Baixa".equalsIgnoreCase(existingCard.getPriority())) {
                chipGroupPrio.check(R.id.chip_kanban_baixa);
            } else {
                chipGroupPrio.check(R.id.chip_kanban_media);
            }
        }

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
            String priority = "Média";
            if (chipGroupPrio.getCheckedChipId() == R.id.chip_kanban_alta) priority = "Alta";
            else if (chipGroupPrio.getCheckedChipId() == R.id.chip_kanban_baixa) priority = "Baixa";

            final String finalPrio = priority;
            DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                if (existingCard != null) {
                    existingCard.setTitle(title);
                    existingCard.setDescription(desc);
                    existingCard.setPriority(finalPrio);
                    DatabaseClient.getInstance(this).getDatabase().kanbanDao().updateCard(existingCard);
                } else {
                    DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertCard(
                            new KanbanCard(listId, title, desc, finalPrio, System.currentTimeMillis() + 86400000L, false, 0)
                    );
                }
            });

            dialog.dismiss();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }
}
