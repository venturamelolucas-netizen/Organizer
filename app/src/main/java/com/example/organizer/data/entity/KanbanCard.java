package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "kanban_cards",
    foreignKeys = @ForeignKey(
        entity = KanbanList.class,
        parentColumns = "id",
        childColumns = "listId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("listId")}
)
public class KanbanCard {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long listId;
    private String title;
    private String description;
    private String priority; // "Alta", "Média", "Baixa"
    private long dueDate;
    private boolean isCompleted;
    private int position;

    public KanbanCard(long listId, String title, String description, String priority, long dueDate, boolean isCompleted, int position) {
        this.listId = listId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
        this.position = position;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getListId() { return listId; }
    public void setListId(long listId) { this.listId = listId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
