package com.example.organizer.ui.notes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.Note;
import com.example.organizer.data.entity.Tag;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotesActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener {

    private EditText etSearch;
    private ChipGroup chipGroupTags;
    private RecyclerView rvNotes;
    private TextView tvEmpty;
    private FloatingActionButton fabAddNote;

    private NotesAdapter adapter;
    private List<Note> allNotesCache = new ArrayList<>();
    private List<Tag> allTagsCache = new ArrayList<>();
    private String selectedTagFilter = "Todas";
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_notes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearch = findViewById(R.id.et_search_notes);
        chipGroupTags = findViewById(R.id.chip_group_note_tags);
        rvNotes = findViewById(R.id.rv_notes_masonry);
        tvEmpty = findViewById(R.id.tv_notes_empty);
        fabAddNote = findViewById(R.id.fab_add_note);

        setupRecyclerView();
        setupSearchAndFilters();

        fabAddNote.setOnClickListener(v -> showNoteDialog(null));

        observeNotes();
        observeTags();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_manage_tags) {
            showManageTagsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        adapter = new NotesAdapter(this, this);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        rvNotes.setLayoutManager(layoutManager);
        rvNotes.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                filterNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupTags.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedTagFilter = "Todas";
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                selectedTagFilter = (chip != null) ? chip.getText().toString() : "Todas";
            }
            filterNotes();
        });
    }

    private void observeNotes() {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .noteDao()
                .getAllNotes()
                .observe(this, notes -> {
                    allNotesCache = (notes != null) ? notes : new ArrayList<>();
                    updateTagsFilterChips();
                    filterNotes();
                });
    }

    private void observeTags() {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .noteDao()
                .getAllTags()
                .observe(this, tags -> {
                    allTagsCache = (tags != null) ? tags : new ArrayList<>();
                    updateTagsFilterChips();
                });
    }

    private void updateTagsFilterChips() {
        Set<String> uniqueTags = new LinkedHashSet<>();
        // 1. Tags explicitly stored in Tag table
        for (Tag t : allTagsCache) {
            if (t.getName() != null && !t.getName().trim().isEmpty()) {
                uniqueTags.add(t.getName().trim());
            }
        }
        // 2. Tags currently present on notes
        for (Note n : allNotesCache) {
            if (n.getTag() != null && !n.getTag().trim().isEmpty()) {
                uniqueTags.add(n.getTag().trim());
            }
        }

        chipGroupTags.removeAllViews();

        // Chip 'Todas'
        Chip chipAll = new Chip(this);
        chipAll.setText("Todas");
        chipAll.setCheckable(true);
        chipAll.setChecked(selectedTagFilter.equals("Todas"));
        chipGroupTags.addView(chipAll);

        // Chip for each Tag
        for (String tag : uniqueTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selectedTagFilter.equals(tag));
            chipGroupTags.addView(chip);
        }

        // Action Chip to Manage Tags
        Chip chipManage = new Chip(this);
        chipManage.setText("⚙ Gerenciar");
        chipManage.setCheckable(false);
        chipManage.setChipIconResource(R.drawable.ic_edit);
        chipManage.setChipIconTintResource(R.color.module_notes);
        chipManage.setOnClickListener(v -> showManageTagsDialog());
        chipGroupTags.addView(chipManage);
    }

    private void filterNotes() {
        List<Note> filtered = new ArrayList<>();
        for (Note note : allNotesCache) {
            // Tag filter
            if (!selectedTagFilter.equals("Todas")) {
                if (note.getTag() == null || !note.getTag().equalsIgnoreCase(selectedTagFilter)) {
                    continue;
                }
            }

            // Search filter
            if (!searchQuery.isEmpty()) {
                String title = note.getTitle().toLowerCase(Locale.getDefault());
                String content = (note.getContent() != null) ? note.getContent().toLowerCase(Locale.getDefault()) : "";
                if (!title.contains(searchQuery) && !content.contains(searchQuery)) {
                    continue;
                }
            }

            filtered.add(note);
        }

        adapter.setNotes(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNoteClick(Note note) {
        showNoteDialog(note);
    }

    private void showNoteDialog(@Nullable Note existingNote) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note_edit, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_note_title);
        EditText etTitle = dialogView.findViewById(R.id.et_note_edit_title);
        EditText etContent = dialogView.findViewById(R.id.et_note_edit_content);
        EditText etTag = dialogView.findViewById(R.id.et_note_edit_tag);
        ChipGroup chipGroupDialogTags = dialogView.findViewById(R.id.chip_group_dialog_tags);
        SwitchMaterial switchPin = dialogView.findViewById(R.id.switch_note_pin);
        View btnDelete = dialogView.findViewById(R.id.btn_delete_note);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_note);
        View btnSave = dialogView.findViewById(R.id.btn_save_note);

        final String[] selectedColor = {existingNote != null ? existingNote.getColor() : "#FEF08A"};

        // Color Palette Views
        View cYellow = dialogView.findViewById(R.id.color_yellow);
        View cGreen = dialogView.findViewById(R.id.color_green);
        View cBlue = dialogView.findViewById(R.id.color_blue);
        View cPurple = dialogView.findViewById(R.id.color_purple);
        View cPink = dialogView.findViewById(R.id.color_pink);
        View cOrange = dialogView.findViewById(R.id.color_orange);
        View cWhite = dialogView.findViewById(R.id.color_white);

        cYellow.setOnClickListener(v -> selectedColor[0] = "#FEF08A");
        cGreen.setOnClickListener(v -> selectedColor[0] = "#BBF7D0");
        cBlue.setOnClickListener(v -> selectedColor[0] = "#BAE6FD");
        cPurple.setOnClickListener(v -> selectedColor[0] = "#E9D5FF");
        cPink.setOnClickListener(v -> selectedColor[0] = "#FBCFE8");
        cOrange.setOnClickListener(v -> selectedColor[0] = "#FED7AA");
        cWhite.setOnClickListener(v -> selectedColor[0] = "#FFFFFF");

        // Quick Tag selector chips
        if (chipGroupDialogTags != null) {
            chipGroupDialogTags.removeAllViews();
            Set<String> tagSuggestions = new LinkedHashSet<>();
            for (Tag t : allTagsCache) {
                if (t.getName() != null && !t.getName().trim().isEmpty()) {
                    tagSuggestions.add(t.getName().trim());
                }
            }
            for (String tagSuggestion : tagSuggestions) {
                Chip chip = new Chip(this);
                chip.setText(tagSuggestion);
                chip.setCheckable(false);
                chip.setOnClickListener(v -> etTag.setText(tagSuggestion));
                chipGroupDialogTags.addView(chip);
            }
        }

        if (existingNote != null) {
            tvTitle.setText("Editar Nota");
            etTitle.setText(existingNote.getTitle());
            etContent.setText(existingNote.getContent());
            etTag.setText(existingNote.getTag());
            switchPin.setChecked(existingNote.isPinned());
            btnDelete.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            if (existingNote != null) {
                DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                    DatabaseClient.getInstance(this).getDatabase().noteDao().deleteNote(existingNote);
                });
                dialog.dismiss();
                NotificationHelper.vibrateFeedback(this, 50);
            }
        });

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                title = "Sem Título";
            }
            String content = etContent.getText().toString().trim();
            String tag = etTag.getText().toString().trim();
            boolean isPinned = switchPin.isChecked();

            final String fTitle = title;
            final String fContent = content;
            final String fTag = tag;
            final String fColor = selectedColor[0];

            DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                long now = System.currentTimeMillis();
                if (existingNote != null) {
                    existingNote.setTitle(fTitle);
                    existingNote.setContent(fContent);
                    existingNote.setTag(fTag);
                    existingNote.setColor(fColor);
                    existingNote.setPinned(isPinned);
                    existingNote.setUpdatedAt(now);
                    DatabaseClient.getInstance(this).getDatabase().noteDao().updateNote(existingNote);
                } else {
                    Note newNote = new Note(fTitle, fContent, fColor, fTag, isPinned, now, now);
                    DatabaseClient.getInstance(this).getDatabase().noteDao().insertNote(newNote);
                }

                // If new tag is typed, ensure it's recorded in the Tag table
                if (!fTag.isEmpty()) {
                    Tag existing = DatabaseClient.getInstance(this).getDatabase().noteDao().getTagByName(fTag);
                    if (existing == null) {
                        DatabaseClient.getInstance(this).getDatabase().noteDao().insertTag(new Tag(fTag, fColor));
                    }
                }
            });

            dialog.dismiss();
            Toast.makeText(this, "Nota salva!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }

    private void showManageTagsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_tags, null);

        RecyclerView rvManageTags = view.findViewById(R.id.rv_manage_tags);
        TextView tvNoTags = view.findViewById(R.id.tv_no_tags);
        MaterialButton btnCreateNewTag = view.findViewById(R.id.btn_create_new_tag);
        MaterialButton btnClose = view.findViewById(R.id.btn_close_manage_tags);

        rvManageTags.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        final TagAdapter tagAdapter = new TagAdapter(this, new TagAdapter.TagActionListener() {
            @Override
            public void onEditTag(Tag tag) {
                showEditTagDialog(tag);
            }

            @Override
            public void onDeleteTag(Tag tag) {
                confirmDeleteTag(tag);
            }
        });
        rvManageTags.setAdapter(tagAdapter);

        // Populate initially
        tagAdapter.setTags(allTagsCache);
        tvNoTags.setVisibility(allTagsCache.isEmpty() ? View.VISIBLE : View.GONE);

        // Live observation
        DatabaseClient.getInstance(this).getDatabase().noteDao().getAllTags().observe(this, tags -> {
            List<Tag> list = (tags != null) ? tags : new ArrayList<>();
            tagAdapter.setTags(list);
            tvNoTags.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        btnCreateNewTag.setOnClickListener(v -> showEditTagDialog(null));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditTagDialog(@Nullable Tag existingTag) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_tag, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_tag_title);
        EditText etName = dialogView.findViewById(R.id.et_tag_name);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_tag);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_tag);

        final String[] selectedColor = {existingTag != null ? existingTag.getColor() : "#FBBF24"};

        View cYellow = dialogView.findViewById(R.id.tag_color_yellow);
        View cGreen = dialogView.findViewById(R.id.tag_color_green);
        View cBlue = dialogView.findViewById(R.id.tag_color_blue);
        View cPurple = dialogView.findViewById(R.id.tag_color_purple);
        View cPink = dialogView.findViewById(R.id.tag_color_pink);
        View cOrange = dialogView.findViewById(R.id.tag_color_orange);
        View cCyan = dialogView.findViewById(R.id.tag_color_cyan);

        if (cYellow != null) cYellow.setOnClickListener(v -> selectedColor[0] = "#FBBF24");
        if (cGreen != null) cGreen.setOnClickListener(v -> selectedColor[0] = "#10B981");
        if (cBlue != null) cBlue.setOnClickListener(v -> selectedColor[0] = "#2563EB");
        if (cPurple != null) cPurple.setOnClickListener(v -> selectedColor[0] = "#8B5CF6");
        if (cPink != null) cPink.setOnClickListener(v -> selectedColor[0] = "#EC4899");
        if (cOrange != null) cOrange.setOnClickListener(v -> selectedColor[0] = "#F97316");
        if (cCyan != null) cCyan.setOnClickListener(v -> selectedColor[0] = "#06B6D4");

        if (existingTag != null) {
            tvTitle.setText("Editar Etiqueta");
            etName.setText(existingTag.getName());
        } else {
            tvTitle.setText("Nova Etiqueta");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Informe o nome da etiqueta");
                return;
            }

            final String newName = name;
            final String color = selectedColor[0];

            DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                if (existingTag != null) {
                    String oldName = existingTag.getName();
                    existingTag.setName(newName);
                    existingTag.setColor(color);
                    DatabaseClient.getInstance(this).getDatabase().noteDao().updateTag(existingTag);
                    // Update notes referencing old name
                    DatabaseClient.getInstance(this).getDatabase().noteDao().updateTagNameInNotes(oldName, newName);
                } else {
                    Tag newTag = new Tag(newName, color);
                    DatabaseClient.getInstance(this).getDatabase().noteDao().insertTag(newTag);
                }
            });

            dialog.dismiss();
            Toast.makeText(this, "Etiqueta salva!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }

    private void confirmDeleteTag(Tag tag) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Etiqueta")
                .setMessage("Deseja realmente excluir a etiqueta \"" + tag.getName() + "\"? As anotações vinculadas não serão apagadas, apenas terão essa etiqueta removida.")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().noteDao().deleteTag(tag);
                        DatabaseClient.getInstance(this).getDatabase().noteDao().removeTagFromNotes(tag.getName());
                    });
                    Toast.makeText(this, "Etiqueta excluída!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.vibrateFeedback(this, 40);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
