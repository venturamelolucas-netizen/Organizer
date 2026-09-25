package com.example.organizer.ui.dashboard;

public class DashboardModuleItem {
    public static final int ID_KANBAN = 1;
    public static final int ID_KAIZEN = 2;
    public static final int ID_POMODORO = 3;
    public static final int ID_A3 = 4;
    public static final int ID_FIVES = 5;
    public static final int ID_PARETO = 6;
    public static final int ID_EISENHOWER = 7;
    public static final int ID_NOTES = 8;

    private final int id;
    private final String title;
    private final String description;
    private final int iconRes;
    private final int accentColorRes;

    public DashboardModuleItem(int id, String title, String description, int iconRes, int accentColorRes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.accentColorRes = accentColorRes;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconRes() { return iconRes; }
    public int getAccentColorRes() { return accentColorRes; }
}
