package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "eisenhower_tasks")
public class EisenhowerTask {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String description;
    private String date; // "YYYY-MM-DD" or formatted date
    private String time; // "HH:mm"
    private long timestamp; // timestamp in millis for sorting & scheduling
    private String priority; // "Alta", "Média", "Baixa"
    private int quadrant; // 1: Urg+Imp, 2: Não Urg+Imp, 3: Urg+Não Imp, 4: Não Urg+Não Imp
    private boolean isCompleted;
    private long googleCalendarEventId; // Event ID if synced to Android/Google Calendar
    private boolean syncedWithCalendar;
    private int position;

    public EisenhowerTask(String title, String description, String date, String time, long timestamp, String priority, int quadrant, boolean isCompleted, long googleCalendarEventId, boolean syncedWithCalendar, int position) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.timestamp = timestamp;
        this.priority = priority;
        this.quadrant = quadrant;
        this.isCompleted = isCompleted;
        this.googleCalendarEventId = googleCalendarEventId;
        this.syncedWithCalendar = syncedWithCalendar;
        this.position = position;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getQuadrant() { return quadrant; }
    public void setQuadrant(int quadrant) { this.quadrant = quadrant; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public long getGoogleCalendarEventId() { return googleCalendarEventId; }
    public void setGoogleCalendarEventId(long googleCalendarEventId) { this.googleCalendarEventId = googleCalendarEventId; }

    public boolean isSyncedWithCalendar() { return syncedWithCalendar; }
    public void setSyncedWithCalendar(boolean syncedWithCalendar) { this.syncedWithCalendar = syncedWithCalendar; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
