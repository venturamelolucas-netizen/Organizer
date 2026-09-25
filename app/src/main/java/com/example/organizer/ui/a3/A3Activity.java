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
    private A3Document currentDoc = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force landscape orientation per specification
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
                        clearFieldsForNewDoc();
                        return;
                    }

                    List<String> titles = new ArrayList<>();
                    for (A3Document d : docList) {
                        titles.add(d.getTitle());
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles);
                    spinnerDocs.setAdapter(spinnerAdapter);

                    spinnerDocs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            currentDoc = docList.get(position);
                            loadDocumentFields(currentDoc);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    if (currentDoc == null && !docList.isEmpty()) {
                        currentDoc = docList.get(0);
                        loadDocumentFields(currentDoc);
                    }
                });
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
        currentDoc = null;
        etTitle.setText("");
        etProblem.setText("");
        etContext.setText("");
        etCurrentConditions.setText("");
        etRootCause.setText("");
        etImprovementProposals.setText("");
        etActionPlan.setText("");
        etFollowupResults.setText("");
        etTitle.requestFocus();
        Toast.makeText(this, "Novo relatório A3 iniciado. Preencha os campos e salve.", Toast.LENGTH_SHORT).show();
    }

    private void saveCurrentDocument() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError("Informe o título do relatório A3");
            return;
        }

        String problem = etProblem.getText().toString().trim();
        String context = etContext.getText().toString().trim();
        String currentCond = etCurrentConditions.getText().toString().trim();
        String rootCause = etRootCause.getText().toString().trim();
        String improvements = etImprovementProposals.getText().toString().trim();
        String plan = etActionPlan.getText().toString().trim();
        String results = etFollowupResults.getText().toString().trim();

        DatabaseClient.getInstance(this).getExecutor().execute(() -> {
            if (currentDoc != null) {
                currentDoc.setTitle(title);
                currentDoc.setProblem(problem);
                currentDoc.setContext(context);
                currentDoc.setCurrentConditions(currentCond);
                currentDoc.setRootCause(rootCause);
                currentDoc.setImprovementProposals(improvements);
                currentDoc.setActionPlan(plan);
                currentDoc.setFollowUpResults(results);
                currentDoc.setUpdatedAt(System.currentTimeMillis());

                DatabaseClient.getInstance(this).getDatabase().a3Dao().updateDocument(currentDoc);
            } else {
                A3Document newDoc = new A3Document(
                        title, problem, context, currentCond, rootCause, improvements, plan, results, System.currentTimeMillis()
                );
                long id = DatabaseClient.getInstance(this).getDatabase().a3Dao().insertDocument(newDoc);
                newDoc.setId(id);
                currentDoc = newDoc;
            }
        });

        Toast.makeText(this, "Relatório A3 salvo com sucesso!", Toast.LENGTH_SHORT).show();
        NotificationHelper.vibrateFeedback(this, 30);
    }

    private void deleteCurrentDocument() {
        if (currentDoc == null) {
            Toast.makeText(this, "Nenhum documento selecionado para exclusão.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Excluir Relatório A3")
                .setMessage("Deseja realmente excluir \"" + currentDoc.getTitle() + "\"?")
                .setPositiveButton("Excluir", (d, w) -> {
                    DatabaseClient.getInstance(this).getExecutor().execute(() -> {
                        DatabaseClient.getInstance(this).getDatabase().a3Dao().deleteDocument(currentDoc);
                    });
                    currentDoc = null;
                    clearFieldsForNewDoc();
                    NotificationHelper.vibrateFeedback(this, 40);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
