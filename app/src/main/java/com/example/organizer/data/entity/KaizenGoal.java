package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "kaizen_goals")
public class KaizenGoal {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String description;
    private String frequency;
    private String currentState;
    private String targetState;
    private int progress; // 0 - 100
    private long startDate;
    private boolean notificationEnabled;
    private String notificationTime;
    private String notificationDays;

    public KaizenGoal(String title, String description, String frequency, String currentState, String targetState, int progress, long startDate, boolean notificationEnabled, String notificationTime, String notificationDays) {
        this.title = title;
        this.description = description;
        this.frequency = frequency;
        this.currentState = currentState;
        this.targetState = targetState;
        this.progress = progress;
        this.startDate = startDate;
        this.notificationEnabled = notificationEnabled;
        this.notificationTime = notificationTime;
        this.notificationDays = notificationDays;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    public String getTargetState() { return targetState; }
    public void setTargetState(String targetState) { this.targetState = targetState; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public boolean isNotificationEnabled() { return notificationEnabled; }
    public boolean isNotificationsEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    public void setNotificationsEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getNotificationTime() { return notificationTime; }
    public void setNotificationTime(String notificationTime) { this.notificationTime = notificationTime; }

    public String getNotificationDays() { return notificationDays; }
    public void setNotificationDays(String notificationDays) { this.notificationDays = notificationDays; }
}
