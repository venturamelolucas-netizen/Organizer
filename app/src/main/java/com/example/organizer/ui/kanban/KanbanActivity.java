package com.example.organizer.ui.kanban;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.KanbanBoard;
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.entity.KanbanList;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KanbanActivity extends AppCompatActivity implements KanbanColumnAdapter.ColumnListener {

    private Spinner spinnerBoards;
    private ImageButton btnEditBoard;
    private ImageButton btnDeleteBoard;
    private View layoutEmptyBoards;
    private RecyclerView rvColumns;
    private FloatingActionButton fabAddList;

    private KanbanColumnAdapter columnAdapter;
    private List<KanbanBoard> currentBoards = new ArrayList<>();
    private long selectedBoardId = -1;
    private LiveData<List<KanbanList>> currentListsLiveData = null;

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
        btnEditBoard = findViewById(R.id.btn_edit_board);
        btnDeleteBoard = findViewById(R.id.btn_delete_board);
        layoutEmptyBoards = findViewById(R.id.layout_empty_boards);
        rvColumns = findViewById(R.id.rv_kanban_columns);
        fabAddList = findViewById(R.id.fab_add_list);

        findViewById(R.id.btn_add_board).setOnClickListener(v -> showAddBoardDialog());
        btnEditBoard.setOnClickListener(v -> showEditBoardDialog());
        btnDeleteBoard.setOnClickListener(v -> showDeleteBoardDialog());
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
                        selectedBoardId = -1;
                        spinnerBoards.setVisibility(View.GONE);
                        btnEditBoard.setVisibility(View.GONE);
                        btnDeleteBoard.setVisibility(View.GONE);
                        rvColumns.setVisibility(View.GONE);
                        fabAddList.setVisibility(View.GONE);
                        layoutEmptyBoards.setVisibility(View.VISIBLE);
                        columnAdapter.setLists(new ArrayList<>());
                        return;
                    }

                    layoutEmptyBoards.setVisibility(View.GONE);
                    spinnerBoards.setVisibility(View.VISIBLE);
                    btnEditBoard.setVisibility(View.VISIBLE);
                    btnDeleteBoard.setVisibility(View.VISIBLE);
                    rvColumns.setVisibility(View.VISIBLE);
                    fabAddList.setVisibility(View.VISIBLE);

                    List<String> titles = new ArrayList<>();
                    int selectedIndex = 0;
                    for (int i = 0; i < currentBoards.size(); i++) {
                        KanbanBoard b = currentBoards.get(i);
                        titles.add(b.getTitle());
                        if (b.getId() == selectedBoardId) {
                            selectedIndex = i;
                        }
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles);
                    spinnerBoards.setAdapter(spinnerAdapter);
                    spinnerBoards.setSelection(selectedIndex, false);

                    if (selectedBoardId == -1 || getBoardIndex(selectedBoardId) == -1) {
                        selectedBoardId = currentBoards.get(0).getId();
                        observeColumnsForBoard(selectedBoardId);
                    } else {
                        observeColumnsForBoard(selectedBoardId);
                    }

                    spinnerBoards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < currentBoards.size()) {
                                long newSelectedId = currentBoards.get(position).getId();
                                if (newSelectedId != selectedBoardId) {
                                    selectedBoardId = newSelectedId;
                                    observeColumnsForBoard(selectedBoardId);
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private int getBoardIndex(long boardId) {
        for (int i = 0; i < currentBoards.size(); i++) {
            if (currentBoards.get(i).getId() == boardId) return i;
        }
        return -1;
    }

    private KanbanBoard getSelectedBoard() {
        for (KanbanBoard b : currentBoards) {
            if (b.getId() == selectedBoardId) return b;
        }
        return null;
    }

    private void observeColumnsForBoard(long boardId) {
        if (currentListsLiveData != null) {
            currentListsLiveData.removeObservers(this);
        }
        currentListsLiveData = DatabaseClient.getInstance(this)
                .getDatabase()
                .kanbanDao()
                .getListsByBoard(boardId);
        currentListsLiveData.observe(this, lists -> {
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
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(bId, "Em Progresso", 1));
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(new KanbanList(bId, "Concluído", 2));
                            selectedBoardId = bId;
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showEditBoardDialog() {
        KanbanBoard board = getSelectedBoard();
        if (board == null) return;

        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null);
        EditText inputTitle = new EditText(this);
        inputTitle.setText(board.getTitle());
        inputTitle.setHint("Nome do Quadro");

        EditText inputDesc = new EditText(this);
        inputDesc.setText(board.getDescription() != null ? board.getDescription() : "");
        inputDesc.setHint("Descrição (opcional)");

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 16);
        container.addView(inputTitle);
        container.addView(inputDesc);

        new AlertDialog.Builder(this)
                .setTitle("Editar Quadro")
                .setView(container)
                .setPositiveButton("Salvar", (d, w) -> {
                    String title = inputTitle.getText().toString().trim();
                    if (!title.isEmpty()) {
                        board.setTitle(title);
                        board.setDescription(inputDesc.getText().toString().trim());
                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().updateBoard(board);
                        });
                        Toast.makeText(this, "Quadro atualizado!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteBoardDialog() {
        KanbanBoard board = getSelectedBoard();
        if (board == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Excluir Quadro Kanban")
                .setMessage("Deseja realmente excluir o quadro \"" + board.getTitle() + "\"?\n\nTodas as listas e cartões deste quadro serão removidos permanentemente.")
                .setPositiveButton("Excluir Definitivamente", (d, w) -> {
                    long bId = board.getId();
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().kanbanDao().deleteCardsByBoardId(bId);
                        DatabaseClient.getInstance(this).getDatabase().kanbanDao().deleteListsByBoardId(bId);
                        DatabaseClient.getInstance(this).getDatabase().kanbanDao().deleteBoard(board);
                    });
                    selectedBoardId = -1;
                    Toast.makeText(this, "Quadro excluído!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 50);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddListDialog() {
        if (selectedBoardId == -1) {
            Toast.makeText(this, "Selecione ou crie um quadro primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("Nome da Coluna (ex: Em Revisão, Impedimentos)");
        new AlertDialog.Builder(this)
                .setTitle("Nova Coluna")
                .setView(input)
                .setPositiveButton("Adicionar", (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (!title.isEmpty()) {
                        int nextPos = columnAdapter.getItemCount();
                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertList(
                                    new KanbanList(selectedBoardId, title, nextPos)
                            );
                        });
                        NotificationHelper.vibrateFeedback(this, 25);
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
    public void onEditList(KanbanList list) {
        final EditText input = new EditText(this);
        input.setText(list.getTitle());
        input.setHint("Nome da Coluna");
        new AlertDialog.Builder(this)
                .setTitle("Renomear Coluna")
                .setView(input)
                .setPositiveButton("Salvar", (d, w) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        list.setTitle(newTitle);
                        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                            DatabaseClient.getInstance(this).getDatabase().kanbanDao().updateList(list);
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDeleteList(KanbanList list) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Coluna")
                .setMessage("Deseja excluir a coluna \"" + list.getTitle() + "\" e todos os seus cartões?")
                .setPositiveButton("Excluir", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().kanbanDao().deleteCardsByListId(list.getId());
                        DatabaseClient.getInstance(this).getDatabase().kanbanDao().deleteList(list);
                    });
                    NotificationHelper.vibrateFeedback(this, 40);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onMoveCardToColumn(KanbanCard card, KanbanList targetList) {
        if (card.getListId() == targetList.getId()) {
            return;
        }

        long oldListId = card.getListId();
        long targetListId = targetList.getId();

        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            List<KanbanCard> targetCards = DatabaseClient.getInstance(this).getDatabase().kanbanDao().getCardsByListSync(targetListId);
            int nextPos = (targetCards != null) ? targetCards.size() : 0;

            card.setListId(targetListId);
            card.setPosition(nextPos);
            DatabaseClient.getInstance(this).getDatabase().kanbanDao().updateCard(card);

            // Re-order origin column cards
            List<KanbanCard> originCards = DatabaseClient.getInstance(this).getDatabase().kanbanDao().getCardsByListSync(oldListId);
            if (originCards != null) {
                for (int i = 0; i < originCards.size(); i++) {
                    KanbanCard c = originCards.get(i);
                    c.setPosition(i);
                    DatabaseClient.getInstance(this).getDatabase().kanbanDao().updateCard(c);
                }
            }
        });

        Toast.makeText(this, "Cartão movido para \"" + targetList.getTitle() + "\"", Toast.LENGTH_SHORT).show();
        NotificationHelper.vibrateFeedback(this, 30);
    }

    @Override
    public void onOpenMoveCardDialog(KanbanCard card) {
        List<KanbanList> allLists = columnAdapter.getLists();
        if (allLists.isEmpty()) return;

        List<KanbanList> targetOptions = new ArrayList<>();
        List<String> optionTitles = new ArrayList<>();
        for (KanbanList l : allLists) {
            if (l.getId() != card.getListId()) {
                targetOptions.add(l);
                optionTitles.add(l.getTitle());
            }
        }

        if (targetOptions.isEmpty()) {
            Toast.makeText(this, "Não há outras colunas neste quadro.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Mover Cartão Para...")
                .setItems(optionTitles.toArray(new String[0]), (d, which) -> {
                    KanbanList chosen = targetOptions.get(which);
                    onMoveCardToColumn(card, chosen);
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
        MaterialButton btnPickDueDate = dialogView.findViewById(R.id.btn_pick_due_date);
        MaterialButton btnClearDueDate = dialogView.findViewById(R.id.btn_clear_due_date);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_kanban_card);
        View btnSave = dialogView.findViewById(R.id.btn_save_kanban_card);

        final long[] selectedDueDate = {0L};
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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

            selectedDueDate[0] = existingCard.getDueDate();
            if (selectedDueDate[0] > 0) {
                btnPickDueDate.setText("Vencimento: " + sdf.format(new Date(selectedDueDate[0])));
            } else {
                btnPickDueDate.setText("Sem Vencimento");
            }
        } else {
            btnPickDueDate.setText("Sem Vencimento");
        }

        btnPickDueDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (selectedDueDate[0] > 0) {
                c.setTimeInMillis(selectedDueDate[0]);
            }
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, dayOfMonth, 23, 59, 59);
                selectedDueDate[0] = chosen.getTimeInMillis();
                btnPickDueDate.setText("Vencimento: " + sdf.format(chosen.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnClearDueDate.setOnClickListener(v -> {
            selectedDueDate[0] = 0L;
            btnPickDueDate.setText("Sem Vencimento");
        });

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
            final long dueDate = selectedDueDate[0];

            DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                if (existingCard != null) {
                    existingCard.setTitle(title);
                    existingCard.setDescription(desc);
                    existingCard.setPriority(finalPrio);
                    existingCard.setDueDate(dueDate);
                    DatabaseClient.getInstance(this).getDatabase().kanbanDao().updateCard(existingCard);
                } else {
                    List<KanbanCard> currentListCards = DatabaseClient.getInstance(this).getDatabase().kanbanDao().getCardsByListSync(listId);
                    int pos = (currentListCards != null) ? currentListCards.size() : 0;
                    DatabaseClient.getInstance(this).getDatabase().kanbanDao().insertCard(
                            new KanbanCard(listId, title, desc, finalPrio, dueDate, false, pos)
                    );
                }
            });

            dialog.dismiss();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }
}
