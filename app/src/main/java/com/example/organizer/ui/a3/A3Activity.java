package com.example.organizer.ui.a3;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.A3Document;
import com.example.organizer.data.service.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

public class A3Activity extends AppCompatActivity {

    private Spinner spinnerDocs;
    private EditText etTitle;
    private EditText etProblem;
    private EditText etContext;
    private EditText etCurrentConditions;
    private EditText etRootCause;
    private EditText etImprovementProposals;
    private EditText etActionPlan;
    private EditText etFollowupResults;

    private List<A3Document> docList = new ArrayList<>();
    private long selectedDocId = -1;
    private boolean isUpdatingSpinner = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_a3);

        ImageButton btnBack = findViewById(R.id.btn_a3_back);
        btnBack.setOnClickListener(v -> finish());

        spinnerDocs = findViewById(R.id.spinner_a3_docs);
        etTitle = findViewById(R.id.et_a3_title);
        etProblem = findViewById(R.id.et_a3_problem);
        etContext = findViewById(R.id.et_a3_context);
        etCurrentConditions = findViewById(R.id.et_a3_current_conditions);
        etRootCause = findViewById(R.id.et_a3_root_cause);
        etImprovementProposals = findViewById(R.id.et_a3_improvement_proposals);
        etActionPlan = findViewById(R.id.et_a3_action_plan);
        etFollowupResults = findViewById(R.id.et_a3_followup_results);

        findViewById(R.id.btn_a3_new).setOnClickListener(v -> clearFieldsForNewDoc());
        findViewById(R.id.btn_a3_save).setOnClickListener(v -> saveCurrentDocument());
        findViewById(R.id.btn_a3_delete).setOnClickListener(v -> deleteCurrentDocument());

        observeDocuments();
    }

    private void observeDocuments() {
        DatabaseClient.getInstance(this)
                .getDatabase()
                .a3Dao()
                .getAllDocuments()
                .observe(this, docs -> {
                    docList = (docs != null) ? docs : new ArrayList<>();
                    if (docList.isEmpty()) {
                        selectedDocId = -1;
                        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Nenhum relatório"});
                        spinnerDocs.setAdapter(emptyAdapter);
                        clearFieldsForNewDoc();
                        return;
                    }

                    List<String> titles = new ArrayList<>();
                    int targetPosition = 0;
                    for (int i = 0; i < docList.size(); i++) {
                        A3Document d = docList.get(i);
                        titles.add(d.getTitle());
                        if (d.getId() == selectedDocId) {
                            targetPosition = i;
                        }
                    }

                    isUpdatingSpinner = true;
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles);
                    spinnerDocs.setAdapter(spinnerAdapter);
                    spinnerDocs.setSelection(targetPosition, false);
                    isUpdatingSpinner = false;

                    if (selectedDocId == -1 || getDocById(selectedDocId) == null) {
                        selectedDocId = docList.get(0).getId();
                        loadDocumentFields(docList.get(0));
                    }

                    spinnerDocs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (isUpdatingSpinner) return;
                            if (position >= 0 && position < docList.size()) {
                                A3Document chosen = docList.get(position);
                                if (chosen.getId() != selectedDocId) {
                                    selectedDocId = chosen.getId();
                                    loadDocumentFields(chosen);
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private A3Document getDocById(long docId) {
        for (A3Document d : docList) {
            if (d.getId() == docId) return d;
        }
        return null;
    }

    private void loadDocumentFields(A3Document doc) {
        if (doc == null) return;
        etTitle.setText(doc.getTitle());
        etProblem.setText(doc.getProblem());
        etContext.setText(doc.getContext());
        etCurrentConditions.setText(doc.getCurrentConditions());
        etRootCause.setText(doc.getRootCause());
        etImprovementProposals.setText(doc.getImprovementProposals());
        etActionPlan.setText(doc.getActionPlan());
        etFollowupResults.setText(doc.getFollowUpResults());
    }

    private void clearFieldsForNewDoc() {
        selectedDocId = -1;
        etTitle.setText("");
        etProblem.setText("");
        etContext.setText("");
        etCurrentConditions.setText("");
        etRootCause.setText("");
        etImprovementProposals.setText("");
        etActionPlan.setText("");
        etFollowupResults.setText("");
        etTitle.requestFocus();
        Toast.makeText(this, "Novo relatório A3. Preencha e toque em Salvar.", Toast.LENGTH_SHORT).show();
    }

    private void saveCurrentDocument() {
        final String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError("Informe o título do relatório A3");
            return;
        }

        final String problem = etProblem.getText().toString().trim();
        final String context = etContext.getText().toString().trim();
        final String currentCond = etCurrentConditions.getText().toString().trim();
        final String rootCause = etRootCause.getText().toString().trim();
        final String improvements = etImprovementProposals.getText().toString().trim();
        final String plan = etActionPlan.getText().toString().trim();
        final String results = etFollowupResults.getText().toString().trim();
        final long targetDocId = selectedDocId;

        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            A3Document existing = (targetDocId > 0) ? getDocById(targetDocId) : null;
            if (existing != null) {
                existing.setTitle(title);
                existing.setProblem(problem);
                existing.setContext(context);
                existing.setCurrentConditions(currentCond);
                existing.setRootCause(rootCause);
                existing.setImprovementProposals(improvements);
                existing.setActionPlan(plan);
                existing.setFollowUpResults(results);
                existing.setUpdatedAt(System.currentTimeMillis());

                DatabaseClient.getInstance(this).getDatabase().a3Dao().updateDocument(existing);
            } else {
                A3Document newDoc = new A3Document(
                        title, problem, context, currentCond, rootCause, improvements, plan, results, System.currentTimeMillis()
                );
                long newId = DatabaseClient.getInstance(this).getDatabase().a3Dao().insertDocument(newDoc);
                selectedDocId = newId;
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(A3Activity.this, "Relatório A3 salvo com sucesso!", Toast.LENGTH_SHORT).show();
                NotificationHelper.vibrateFeedback(A3Activity.this, 30);
            });
        });
    }

    private void deleteCurrentDocument() {
        A3Document toDelete = getDocById(selectedDocId);
        if (toDelete == null) {
            Toast.makeText(this, "Nenhum documento selecionado para exclusão.", Toast.LENGTH_SHORT).show();
            return;
        }

        final A3Document docToDelete = toDelete;
        new AlertDialog.Builder(this)
                .setTitle("Excluir Relatório A3")
                .setMessage("Deseja realmente excluir \"" + docToDelete.getTitle() + "\"?\nEsta ação é permanente.")
                .setPositiveButton("Excluir Definitivamente", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().a3Dao().deleteDocument(docToDelete);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            selectedDocId = -1;
                            clearFieldsForNewDoc();
                            Toast.makeText(A3Activity.this, "Relatório A3 excluído.", Toast.LENGTH_SHORT).show();
                            NotificationHelper.vibrateFeedback(A3Activity.this, 40);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
