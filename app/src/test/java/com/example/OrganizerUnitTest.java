package com.example;

import com.example.organizer.data.entity.EisenhowerTask;
import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.data.entity.PomodoroSession;

import org.junit.Test;
import static org.junit.Assert.*;

public class OrganizerUnitTest {

    @Test
    public void testEisenhowerTaskCreation() {
        EisenhowerTask task = new EisenhowerTask(
                "Finalizar Relatório",
                "Revisão técnica completa",
                "2026-09-25",
                "14:00",
                System.currentTimeMillis(),
                "Alta",
                1,
                false,
                0,
                false,
                0
        );

        assertEquals("Finalizar Relatório", task.getTitle());
        assertEquals(1, task.getQuadrant());
        assertFalse(task.isCompleted());

        task.setCompleted(true);
        assertTrue(task.isCompleted());
    }

    @Test
    public void testKaizenGoalProgression() {
        KaizenGoal goal = new KaizenGoal(
                "Leitura diária",
                "Desenvolver hábito",
                "Diária",
                "2 páginas",
                "20 páginas",
                10,
                System.currentTimeMillis(),
                true,
                "21:00",
                "Todos os dias"
        );

        assertEquals(10, goal.getProgress());
        goal.setProgress(50);
        assertEquals(50, goal.getProgress());
    }

    @Test
    public void testPomodoroSessionMetrics() {
        PomodoroSession session = new PomodoroSession(
                System.currentTimeMillis(),
                25,
                5,
                4,
                100,
                20
        );

        assertEquals(25, session.getWorkMinutes());
        assertEquals(5, session.getBreakMinutes());
        assertEquals(4, session.getCompletedCycles());
        assertEquals(100, session.getTotalWorkMinutes());
        assertEquals(20, session.getTotalBreakMinutes());
    }

    @Test
    public void testKaizenProgressOrderIndex() {
        com.example.organizer.data.entity.KaizenProgress step1 = new com.example.organizer.data.entity.KaizenProgress(
                1L, "Passo 1", System.currentTimeMillis(), false, ""
        );
        step1.setOrderIndex(0);

        com.example.organizer.data.entity.KaizenProgress step2 = new com.example.organizer.data.entity.KaizenProgress(
                1L, "Passo 2", System.currentTimeMillis(), false, ""
        );
        step2.setOrderIndex(1);

        assertTrue(step1.getOrderIndex() < step2.getOrderIndex());
        assertFalse(step1.isAchieved());
        step1.setAchieved(true);
        assertTrue(step1.isAchieved());
    }

    @Test
    public void testDayMatchesHelper() {
        assertTrue(com.example.organizer.data.service.NotificationHelper.isDayMatches(
                java.util.Calendar.MONDAY, "Seg, Qua, Sex"
        ));
        assertFalse(com.example.organizer.data.service.NotificationHelper.isDayMatches(
                java.util.Calendar.TUESDAY, "Seg, Qua, Sex"
        ));
        assertTrue(com.example.organizer.data.service.NotificationHelper.isDayMatches(
                java.util.Calendar.SUNDAY, "Todos os dias"
        ));
    }

    @Test
    public void testStreakContinuedDaily() {
        long now = System.currentTimeMillis();
        // Executed today: streak is maintained
        assertTrue(com.example.organizer.data.service.NotificationHelper.isStreakContinued(now, "Todos os dias"));

        // Executed yesterday: streak is maintained
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
        assertTrue(com.example.organizer.data.service.NotificationHelper.isStreakContinued(
                yesterday.getTimeInMillis(), "Todos os dias"
        ));

        // Executed 5 days ago: streak broken
        java.util.Calendar fiveDaysAgo = java.util.Calendar.getInstance();
        fiveDaysAgo.add(java.util.Calendar.DAY_OF_YEAR, -5);
        assertFalse(com.example.organizer.data.service.NotificationHelper.isStreakContinued(
                fiveDaysAgo.getTimeInMillis(), "Todos os dias"
        ));
    }

    @Test
    public void testTagNormalization() {
        String input1 = "  Trabalho  ";
        String input2 = "trabalho";
        String normalized1 = input1.trim().toLowerCase(java.util.Locale.getDefault());
        String normalized2 = input2.trim().toLowerCase(java.util.Locale.getDefault());
        assertEquals(normalized1, normalized2);
    }
}
