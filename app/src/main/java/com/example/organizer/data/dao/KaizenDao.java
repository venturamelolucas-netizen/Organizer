package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.data.entity.KaizenProgress;

import java.util.List;

@Dao
public interface KaizenDao {
    @Query("SELECT * FROM kaizen_goals ORDER BY startDate DESC")
    LiveData<List<KaizenGoal>> getAllGoals();

    @Query("SELECT * FROM kaizen_goals WHERE id = :goalId LIMIT 1")
    KaizenGoal getGoalById(long goalId);

    @Insert
    long insertGoal(KaizenGoal goal);

    @Update
    void updateGoal(KaizenGoal goal);

    @Delete
    void deleteGoal(KaizenGoal goal);

    // Progress
    @Query("SELECT * FROM kaizen_progress WHERE goalId = :goalId ORDER BY date DESC")
    LiveData<List<KaizenProgress>> getProgressByGoal(long goalId);

    @Query("SELECT * FROM kaizen_progress WHERE goalId = :goalId ORDER BY date DESC")
    List<KaizenProgress> getProgressByGoalSync(long goalId);

    @Insert
    long insertProgress(KaizenProgress progress);

    @Update
    void updateProgress(KaizenProgress progress);

    @Delete
    void deleteProgress(KaizenProgress progress);

    @Query("DELETE FROM kaizen_goals")
    void deleteAllGoals();
}
