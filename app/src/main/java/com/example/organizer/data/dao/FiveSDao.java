package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.data.entity.FiveSProject;

import java.util.List;

@Dao
public interface FiveSDao {
    // Projects
    @Query("SELECT * FROM fives_projects ORDER BY createdAt DESC")
    LiveData<List<FiveSProject>> getAllProjects();

    @Query("SELECT * FROM fives_projects LIMIT 1")
    FiveSProject getDefaultProject();

    @Query("SELECT COUNT(*) FROM fives_projects")
    int getProjectCount();

    @Insert
    long insertProject(FiveSProject project);

    @Update
    void updateProject(FiveSProject project);

    @Delete
    void deleteProject(FiveSProject project);

    // Items
    @Query("SELECT * FROM fives_items WHERE projectId = :projectId")
    LiveData<List<FiveSItem>> getAllItems(long projectId);

    @Query("SELECT * FROM fives_items WHERE projectId = :projectId AND step = :step ORDER BY orderIndex ASC, id ASC")
    LiveData<List<FiveSItem>> getItemsByStep(long projectId, String step);

    @Query("SELECT * FROM fives_items WHERE projectId = :projectId AND step = 'SEIRI' AND isEssential = 1 ORDER BY orderIndex ASC")
    LiveData<List<FiveSItem>> getEssentialItems(long projectId);

    @Query("SELECT * FROM fives_items WHERE projectId = :projectId AND step = 'SEISO'")
    LiveData<List<FiveSItem>> getSeisoChecklist(long projectId);

    @Query("SELECT * FROM fives_items WHERE projectId = :projectId AND step = 'SEIKETSU_SHITSUKE'")
    LiveData<List<FiveSItem>> getRoutines(long projectId);

    @Query("SELECT * FROM fives_items WHERE step = 'SEIKETSU_SHITSUKE' AND routineEnabled = 1")
    List<FiveSItem> getAllEnabledRoutinesSync();

    @Query("SELECT * FROM fives_items WHERE id = :id LIMIT 1")
    FiveSItem getItemById(long id);

    @Insert
    long insertItem(FiveSItem item);

    @Update
    void updateItem(FiveSItem item);

    @Delete
    void deleteItem(FiveSItem item);

    @Query("DELETE FROM fives_projects")
    void deleteAllProjects();
}
