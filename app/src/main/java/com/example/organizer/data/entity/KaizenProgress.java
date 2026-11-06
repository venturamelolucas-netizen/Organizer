package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "kaizen_progress",
    foreignKeys = @ForeignKey(
        entity = KaizenGoal.class,
        parentColumns = "id",
        childColumns = "goalId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("goalId")}
)
public class KaizenProgress {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long goalId;
    private String stepTitle;
    private long date;
    private boolean achieved;
    private String notes;
    private int orderIndex;

    public KaizenProgress(long goalId, String stepTitle, long date, boolean achieved, String notes) {
        this.goalId = goalId;
        this.stepTitle = stepTitle;
        this.date = date;
        this.achieved = achieved;
        this.notes = notes;
        this.orderIndex = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getGoalId() { return goalId; }
    public void setGoalId(long goalId) { this.goalId = goalId; }

    public String getStepTitle() { return stepTitle; }
    public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public boolean isAchieved() { return achieved; }
    public void setAchieved(boolean achieved) { this.achieved = achieved; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
