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
}
