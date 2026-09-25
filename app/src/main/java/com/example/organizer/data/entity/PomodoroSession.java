package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_sessions")
public class PomodoroSession {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long date;
    private int workMinutes;
    private int breakMinutes;
    private int completedCycles;
    private int totalWorkMinutes;
    private int totalBreakMinutes;

    public PomodoroSession(long date, int workMinutes, int breakMinutes, int completedCycles, int totalWorkMinutes, int totalBreakMinutes) {
        this.date = date;
        this.workMinutes = workMinutes;
        this.breakMinutes = breakMinutes;
        this.completedCycles = completedCycles;
        this.totalWorkMinutes = totalWorkMinutes;
        this.totalBreakMinutes = totalBreakMinutes;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public int getWorkMinutes() { return workMinutes; }
    public void setWorkMinutes(int workMinutes) { this.workMinutes = workMinutes; }

    public int getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(int breakMinutes) { this.breakMinutes = breakMinutes; }

    public int getCompletedCycles() { return completedCycles; }
    public void setCompletedCycles(int completedCycles) { this.completedCycles = completedCycles; }

    public int getTotalWorkMinutes() { return totalWorkMinutes; }
    public void setTotalWorkMinutes(int totalWorkMinutes) { this.totalWorkMinutes = totalWorkMinutes; }

    public int getTotalBreakMinutes() { return totalBreakMinutes; }
    public void setTotalBreakMinutes(int totalBreakMinutes) { this.totalBreakMinutes = totalBreakMinutes; }
}
