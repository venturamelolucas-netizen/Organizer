package com.example.organizer.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.organizer.ui.a3.A3Activity;
import com.example.organizer.ui.eisenhower.EisenhowerActivity;
import com.example.organizer.ui.fives.FiveSActivity;
import com.example.organizer.ui.kaizen.KaizenActivity;
import com.example.organizer.ui.kanban.KanbanActivity;
import com.example.organizer.ui.notes.NotesActivity;
import com.example.organizer.ui.pareto.ParetoActivity;
import com.example.organizer.ui.pomodoro.PomodoroActivity;
import com.example.organizer.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView tvGreeting;
    private RecyclerView rvModules;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        rvModules = view.findViewById(R.id.rv_dashboard_modules);

        ImageButton btnSettings = view.findViewById(R.id.btn_dashboard_settings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        updateGreeting();
        setupModulesGrid();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
    }

    private void updateGreeting() {
        if (getContext() == null || tvGreeting == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("organizer_prefs", Context.MODE_PRIVATE);
        String userName = prefs.getString("user_name", "Organizador");

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greetingPrefix;
        if (hour >= 5 && hour < 12) {
            greetingPrefix = "Bom dia";
        } else if (hour >= 12 && hour < 18) {
            greetingPrefix = "Boa tarde";
        } else {
            greetingPrefix = "Boa noite";
        }

        tvGreeting.setText(greetingPrefix + ", " + userName);
    }

    private void setupModulesGrid() {
        List<DashboardModuleItem> items = new ArrayList<>();
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_KANBAN,
                "Kanban",
                "Quadros visuais de fluxo de trabalho",
                R.drawable.ic_kanban,
                R.color.module_kanban
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_KAIZEN,
                "Kaizen",
                "Melhoria contínua e metas graduais",
                R.drawable.ic_kaizen,
                R.color.module_kaizen
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_POMODORO,
                "Pomodoro",
                "Foco temporizado e pausas produtivas",
                R.drawable.ic_pomodoro,
                R.color.module_pomodoro
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_A3,
                "Método A3",
                "Resolução estruturada de problemas",
                R.drawable.ic_a3,
                R.color.module_a3
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_FIVES,
                "Método 5S",
                "Organização, limpeza e rotinas",
                R.drawable.ic_fives,
                R.color.module_5s
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_PARETO,
                "Lei de Pareto",
                "Análise 80/20 com Inteligência Artificial",
                R.drawable.ic_pareto,
                R.color.module_pareto
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_EISENHOWER,
                "Quadro de Eisenhower",
                "Matriz de prioridades e tarefas",
                R.drawable.ic_eisenhower,
                R.color.eisenhower_q1
        ));
        items.add(new DashboardModuleItem(
                DashboardModuleItem.ID_NOTES,
                "Bloco de Notas",
                "Post-its digitais e anotações rápidas",
                R.drawable.ic_notes,
                R.color.module_notes
        ));

        rvModules.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        DashboardModuleAdapter adapter = new DashboardModuleAdapter(requireContext(), items, item -> {
            Intent intent = null;
            switch (item.getId()) {
                case DashboardModuleItem.ID_KANBAN:
                    intent = new Intent(requireContext(), KanbanActivity.class);
                    break;
                case DashboardModuleItem.ID_KAIZEN:
                    intent = new Intent(requireContext(), KaizenActivity.class);
                    break;
                case DashboardModuleItem.ID_POMODORO:
                    intent = new Intent(requireContext(), PomodoroActivity.class);
                    break;
                case DashboardModuleItem.ID_A3:
                    intent = new Intent(requireContext(), A3Activity.class);
                    break;
                case DashboardModuleItem.ID_FIVES:
                    intent = new Intent(requireContext(), FiveSActivity.class);
                    break;
                case DashboardModuleItem.ID_PARETO:
                    intent = new Intent(requireContext(), ParetoActivity.class);
                    break;
                case DashboardModuleItem.ID_EISENHOWER:
                    intent = new Intent(requireContext(), EisenhowerActivity.class);
                    break;
                case DashboardModuleItem.ID_NOTES:
                    intent = new Intent(requireContext(), NotesActivity.class);
                    break;
            }

            if (intent != null) {
                startActivity(intent);
            }
        });

        rvModules.setAdapter(adapter);
    }
}
