package com.example.organizer.data.database;

import com.example.organizer.data.entity.A3Document;
import com.example.organizer.data.entity.EisenhowerTask;
import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.data.entity.FiveSProject;
import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.data.entity.KaizenProgress;
import com.example.organizer.data.entity.KanbanBoard;
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.entity.KanbanList;
import com.example.organizer.data.entity.Note;
import com.example.organizer.data.entity.PomodoroSession;
import com.example.organizer.data.entity.Tag;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SampleDataGenerator {

    public static void preloadSampleData(OrganizerDatabase db) {
        clearAllData(db);
        long now = System.currentTimeMillis();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdfDate.format(new Date(now));

        // 1. Kanban
        long boardId = db.kanbanDao().insertBoard(new KanbanBoard(
                "Desenvolvimento do Projeto",
                "Quadro de exemplo para gestão do fluxo de trabalho",
                now
        ));

        long listTodo = db.kanbanDao().insertList(new KanbanList(boardId, "A Fazer", 0));
        long listDoing = db.kanbanDao().insertList(new KanbanList(boardId, "Em Progresso", 1));
        long listDone = db.kanbanDao().insertList(new KanbanList(boardId, "Concluído", 2));

        db.kanbanDao().insertCard(new KanbanCard(listTodo, "Definir requisitos de UI", "Mapear fluxo de navegação e componentes visuais", "Alta", now + 86400000L, false, 0));
        db.kanbanDao().insertCard(new KanbanCard(listTodo, "Revisar testes unitários", "Cobrir regras de negócio principais", "Média", now + 172800000L, false, 1));
        db.kanbanDao().insertCard(new KanbanCard(listDoing, "Implementar banco Room", "Criar DAOs, entidades e migrações locais", "Alta", now + 43200000L, false, 0));
        db.kanbanDao().insertCard(new KanbanCard(listDone, "Configurar layout base", "Definir estilos XML e tema Material 3", "Baixa", now - 86400000L, true, 0));

        // 2. Kaizen
        long goalId = db.kaizenDao().insertGoal(new KaizenGoal(
                "Hábito de Leitura Diária",
                "Desenvolver leitura consistente e reflexiva todos os dias",
                "Diária",
                "Leio 5 páginas por semana",
                "Ler 30 páginas por dia",
                45,
                now - (7 * 86400000L),
                true,
                "21:30",
                "Todos os dias"
        ));

        db.kaizenDao().insertProgress(new KaizenProgress(goalId, "Etapa 1: Ler 5 páginas ao acordar", now - (5 * 86400000L), true, "Concluído sem interrupções"));
        db.kaizenDao().insertProgress(new KaizenProgress(goalId, "Etapa 2: Expandir para 12 páginas diárias", now - (2 * 86400000L), true, "Meta semanal atingida"));
        db.kaizenDao().insertProgress(new KaizenProgress(goalId, "Etapa 3: Chegar a 20 páginas com anotações", now, false, "Em andamento"));

        // 3. Pomodoro
        db.pomodoroDao().insertSession(new PomodoroSession(now - 3600000L, 25, 5, 4, 100, 20));

        // 4. Método A3
        db.a3Dao().insertDocument(new A3Document(
                "Otimização do Tempo de Resposta a Clientes",
                "Clientes aguardam em média 48h para primeiro retorno",
                "Empresa em crescimento com equipe de atendimento sobrecarregada",
                "Triagem manual sem categorização prévia de urgência",
                "Falta de templates e processo de despacho automatizado",
                "Criar sistema de triagem com 3 níveis e respostas padronizadas",
                "1. Definir matriz de SLA\n2. Treinar time de suporte\n3. Medir tempo semanal",
                "Tempo médio de resposta reduzido de 48h para 6h",
                now
        ));

        // 5. Método 5S
        long projId = db.fiveSDao().insertProject(new FiveSProject(
                "Organização do Espaço de Trabalho",
                "Aplicação dos 5S para estação de trabalho e ambiente digital",
                now
        ));

        // 5S Items:
        // Fluxo correto: Seiri decide o que é essencial.
        // Itens essenciais do Seiri são então organizados no Seiton com local e regras.
        FiveSItem itemEssential1 = new FiveSItem(projId, "SEIRI", "Notebook e Carregador", "Equipamento principal de trabalho");
        itemEssential1.setEssential(true);
        itemEssential1.setLocation("Mesa principal");
        itemEssential1.setCategory("Equipamento");
        itemEssential1.setPlacementRule("Posicionado sobre a base refrigerada centralizada");
        itemEssential1.setOrderIndex(0);
        db.fiveSDao().insertItem(itemEssential1);

        FiveSItem itemEssential2 = new FiveSItem(projId, "SEIRI", "Suporte ergonômico", "Posicionado na altura dos olhos");
        itemEssential2.setEssential(true);
        itemEssential2.setLocation("Mesa principal");
        itemEssential2.setCategory("Ergonomia");
        itemEssential2.setPlacementRule("Sempre alinhado com o monitor");
        itemEssential2.setOrderIndex(1);
        db.fiveSDao().insertItem(itemEssential2);

        FiveSItem itemDiscard = new FiveSItem(projId, "SEIRI", "Documentos impressos obsoletos", "Papéis de 2022 sem validade jurídica");
        itemDiscard.setDiscarded(true);
        itemDiscard.setEssential(false);
        db.fiveSDao().insertItem(itemDiscard);

        FiveSItem itemPending = new FiveSItem(projId, "SEIRI", "Cabos USB sobressalentes", "Gaveta lateral");
        itemPending.setEssential(false);
        itemPending.setDiscarded(false);
        db.fiveSDao().insertItem(itemPending);

        // Seiso items
        FiveSItem itemSeiso1 = new FiveSItem(projId, "SEISO", "Limpar teclado e tela com álcool isopropílico", "Checklist de manutenção");
        itemSeiso1.setCompleted(true);
        db.fiveSDao().insertItem(itemSeiso1);

        FiveSItem itemSeiso2 = new FiveSItem(projId, "SEISO", "Esvaziar lixeira física e digital", "Fim do expediente");
        itemSeiso2.setCompleted(false);
        db.fiveSDao().insertItem(itemSeiso2);

        // Seiketsu + Shitsuke
        FiveSItem itemRoutine = new FiveSItem(projId, "SEIKETSU_SHITSUKE", "Revisão e limpeza de 5 minutos", "Rotina diária ao encerrar o trabalho");
        itemRoutine.setRoutineFrequency("Diário");
        itemRoutine.setRoutineDays("Todos os dias");
        itemRoutine.setRoutineTime("18:00");
        itemRoutine.setRoutineEnabled(false);
        itemRoutine.setStreakCount(0);
        db.fiveSDao().insertItem(itemRoutine);

        // 6. Eisenhower Tasks (all 4 quadrants)
        // Q1: Urgente e Importante
        db.eisenhowerDao().insertTask(new EisenhowerTask(
                "Entregar relatório trimestral",
                "Finalizar métricas financeiras e enviar para diretoria",
                todayStr,
                "15:00",
                now + 7200000L,
                "Alta",
                1,
                false,
                0,
                false,
                0
        ));

        // Q2: Importante, Não Urgente
        db.eisenhowerDao().insertTask(new EisenhowerTask(
                "Planejar metas do próximo trimestre",
                "Definir OKRs e cronograma de entregas prioritárias",
                todayStr,
                "10:00",
                now + 86400000L,
                "Média",
                2,
                false,
                0,
                false,
                0
        ));

        // Q3: Urgente, Não Importante
        db.eisenhowerDao().insertTask(new EisenhowerTask(
                "Responder convites para reuniões de alinhamento",
                "Delegar ou confirmar presença para o comitê semanal",
                todayStr,
                "11:30",
                now + 3600000L,
                "Baixa",
                3,
                false,
                0,
                false,
                0
        ));

        // Q4: Não Urgente e Não Importante
        db.eisenhowerDao().insertTask(new EisenhowerTask(
                "Organizar pastas de downloads antigas",
                "Arquivar arquivos desnecessários quando houver tempo ocioso",
                "",
                "",
                now + (3 * 86400000L),
                "Baixa",
                4,
                false,
                0,
                false,
                0
        ));

        // 7. Notes & Tags
        db.noteDao().insertTag(new Tag("Ideias", "#FBBF24"));
        db.noteDao().insertTag(new Tag("Trabalho", "#2563EB"));
        db.noteDao().insertTag(new Tag("Pessoal", "#10B981"));

        db.noteDao().insertNote(new Note(
                "Insights de Produtividade",
                "O princípio de Pareto sugere que focar nas 20% das causas críticas resolve 80% das restrições. Aliado ao Pomodoro, o foco profundo evita alternância de contexto.",
                "#FEF08A",
                "Ideias",
                true,
                now - 3600000L,
                now
        ));

        db.noteDao().insertNote(new Note(
                "Checklist de Lançamento",
                "1. Validar fluxos de navegação\n2. Testar notificações locais\n3. Garantir funcionamento offline\n4. Revisar UI modular e responsiva",
                "#BBF7D0",
                "Trabalho",
                false,
                now - 7200000L,
                now
        ));
    }

    public static void clearAllData(OrganizerDatabase db) {
        db.kanbanDao().deleteAllBoards();
        db.kaizenDao().deleteAllGoals();
        db.pomodoroDao().deleteAllSessions();
        db.a3Dao().deleteAllDocuments();
        db.fiveSDao().deleteAllProjects();
        db.eisenhowerDao().deleteAllTasks();
        db.noteDao().deleteAllNotes();
        db.noteDao().deleteAllTags();
    }
}
