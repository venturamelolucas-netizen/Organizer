package com.example.organizer.ui.notes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.Note;
import com.example.organizer.data.entity.Tag;
import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
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
    }

    private void setupRecyclerView() {
        adapter = new NotesAdapter(this, this);
        // Staggered Masonry Grid Layout (2 columns)
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

    private void updateTagsFilterChips() {
        Set<String> uniqueTags = new HashSet<>();
        for (Note n : allNotesCache) {
            if (n.getTag() != null && !n.getTag().trim().isEmpty()) {
                uniqueTags.add(n.getTag().trim());
            }
        }

        // Keep 'Todas' chip and add custom tag chips
        chipGroupTags.removeAllViews();

        Chip chipAll = new Chip(this);
        chipAll.setText("Todas");
        chipAll.setCheckable(true);
        chipAll.setChecked(selectedTagFilter.equals("Todas"));
        chipGroupTags.addView(chipAll);

        for (String tag : uniqueTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selectedTagFilter.equals(tag));
            chipGroupTags.addView(chip);
        }
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

                // If new tag, insert into Tag table
                if (!fTag.isEmpty()) {
                    DatabaseClient.getInstance(this).getDatabase().noteDao().insertTag(new Tag(fTag, fColor));
                }
            });

            dialog.dismiss();
            Toast.makeText(this, "Nota salva!", Toast.LENGTH_SHORT).show();
            NotificationHelper.vibrateFeedback(this, 30);
        });

        dialog.show();
    }
}
