package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "fives_items",
    foreignKeys = @ForeignKey(
        entity = FiveSProject.class,
        parentColumns = "id",
        childColumns = "projectId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("projectId")}
)
public class FiveSItem {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long projectId;
    private String step; // "SEIRI", "SEITON", "SEISO", "SEIKETSU_SHITSUKE"
    private String name;
    private String description;

    // Seiri fields:
    private boolean isEssential;
    private boolean isDiscarded;

    // Seiton fields:
    private String category;
    private String location;
    private String priority;
    private String placementRule;
    private int orderIndex;

    // Seiso fields:
    private boolean isCompleted;

    // Seiketsu + Shitsuke fields:
    private String routineFrequency; // "Diário", "Semanal"
    private String routineDays;      // "Seg, Ter, Qua..."
    private String routineTime;      // "08:00"
    private boolean routineEnabled;
    private int streakCount;
    private long lastExecutionDate;
    private long nextExecutionDate;

    public FiveSItem(long projectId, String step, String name, String description) {
        this.projectId = projectId;
        this.step = step;
        this.name = name;
        this.description = description;
        this.isEssential = false;
        this.isDiscarded = false;
        this.category = "";
        this.location = "";
        this.priority = "Média";
        this.placementRule = "";
        this.orderIndex = 0;
        this.isCompleted = false;
        this.routineFrequency = "Diário";
        this.routineDays = "Todos os dias";
        this.routineTime = "09:00";
        this.routineEnabled = false;
        this.streakCount = 0;
        this.lastExecutionDate = 0;
        this.nextExecutionDate = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }

    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEssential() { return isEssential; }
    public void setEssential(boolean essential) { isEssential = essential; }

    public boolean isDiscarded() { return isDiscarded; }
    public void setDiscarded(boolean discarded) { isDiscarded = discarded; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getPlacementRule() { return placementRule; }
    public void setPlacementRule(String placementRule) { this.placementRule = placementRule; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getRoutineFrequency() { return routineFrequency; }
    public void setRoutineFrequency(String routineFrequency) { this.routineFrequency = routineFrequency; }

    public String getRoutineDays() { return routineDays; }
    public void setRoutineDays(String routineDays) { this.routineDays = routineDays; }

    public String getRoutineTime() { return routineTime; }
    public void setRoutineTime(String routineTime) { this.routineTime = routineTime; }

    public boolean isRoutineEnabled() { return routineEnabled; }
    public void setRoutineEnabled(boolean routineEnabled) { this.routineEnabled = routineEnabled; }

    public int getStreakCount() { return streakCount; }
    public void setStreakCount(int streakCount) { this.streakCount = streakCount; }

    public long getLastExecutionDate() { return lastExecutionDate; }
    public void setLastExecutionDate(long lastExecutionDate) { this.lastExecutionDate = lastExecutionDate; }

    public long getNextExecutionDate() { return nextExecutionDate; }
    public void setNextExecutionDate(long nextExecutionDate) { this.nextExecutionDate = nextExecutionDate; }
}
