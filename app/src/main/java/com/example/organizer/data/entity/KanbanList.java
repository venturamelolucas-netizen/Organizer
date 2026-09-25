package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "kanban_lists",
    foreignKeys = @ForeignKey(
        entity = KanbanBoard.class,
        parentColumns = "id",
        childColumns = "boardId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("boardId")}
)
public class KanbanList {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long boardId;
    private String title;
    private int position;

    public KanbanList(long boardId, String title, int position) {
        this.boardId = boardId;
        this.title = title;
        this.position = position;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBoardId() { return boardId; }
    public void setBoardId(long boardId) { this.boardId = boardId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
