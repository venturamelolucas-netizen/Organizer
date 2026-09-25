package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.organizer.data.entity.PomodoroSession;

import java.util.List;

@Dao
public interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY date DESC")
    LiveData<List<PomodoroSession>> getAllSessions();

    @Insert
    long insertSession(PomodoroSession session);

    @Query("SELECT SUM(totalWorkMinutes) FROM pomodoro_sessions")
    LiveData<Integer> getTotalWorkMinutes();

    @Query("SELECT SUM(completedCycles) FROM pomodoro_sessions")
    LiveData<Integer> getTotalCompletedCycles();

    @Query("DELETE FROM pomodoro_sessions")
    void deleteAllSessions();
}
