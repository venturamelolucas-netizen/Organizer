package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.organizer.data.entity.EisenhowerTask;

import java.util.List;

@Dao
public interface EisenhowerDao {
    @Query("SELECT * FROM eisenhower_tasks ORDER BY isCompleted ASC, timestamp ASC, position ASC")
    LiveData<List<EisenhowerTask>> getAllTasks();

    @Query("SELECT * FROM eisenhower_tasks WHERE quadrant = :quadrant ORDER BY isCompleted ASC, timestamp ASC, position ASC")
    LiveData<List<EisenhowerTask>> getTasksByQuadrant(int quadrant);

    @Query("SELECT * FROM eisenhower_tasks WHERE isCompleted = :completed ORDER BY timestamp ASC")
    LiveData<List<EisenhowerTask>> getTasksByStatus(boolean completed);

    @Query("SELECT * FROM eisenhower_tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY timestamp ASC")
    LiveData<List<EisenhowerTask>> searchTasks(String query);

    @Query("SELECT * FROM eisenhower_tasks WHERE id = :id LIMIT 1")
    EisenhowerTask getTaskById(long id);

    @Query("SELECT COUNT(*) FROM eisenhower_tasks WHERE quadrant = :quadrant AND isCompleted = 0")
    LiveData<Integer> getPendingCountByQuadrant(int quadrant);

    @Insert
    long insertTask(EisenhowerTask task);

    @Update
    void updateTask(EisenhowerTask task);

    @Delete
    void deleteTask(EisenhowerTask task);

    @Query("SELECT * FROM eisenhower_tasks WHERE googleCalendarEventId > 0")
    List<EisenhowerTask> getTasksWithCalendarEventSync();

    @Query("DELETE FROM eisenhower_tasks")
    void deleteAllTasks();
}
