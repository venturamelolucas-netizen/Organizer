package com.example.organizer.data.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.data.entity.KaizenGoal;

import java.util.Calendar;
import java.util.List;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String CHANNEL_KAIZEN = "channel_kaizen";
    public static final String CHANNEL_POMODORO = "channel_pomodoro";
    public static final String CHANNEL_FIVES = "channel_fives";
    public static final String CHANNEL_TASKS = "channel_tasks";

    // Stable ID offsets to prevent collision across modules
    public static final int OFFSET_KAIZEN = 10000;
    public static final int OFFSET_FIVES = 20000;
    public static final int OFFSET_POMODORO_WORK = 30001;
    public static final int OFFSET_POMODORO_BREAK = 30002;
    public static final int OFFSET_TASKS = 40000;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel channelKaizen = new NotificationChannel(
                    CHANNEL_KAIZEN,
                    "Kaizen - Metas e Hábitos",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channelKaizen.setDescription("Lembretes de evolução contínua e micro-hábitos");

            NotificationChannel channelPomodoro = new NotificationChannel(
                    CHANNEL_POMODORO,
                    "Pomodoro - Foco e Pausas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelPomodoro.setDescription("Alertas de término de foco e descanso");

            NotificationChannel channelFiveS = new NotificationChannel(
                    CHANNEL_FIVES,
                    "5S - Rotinas e Limpeza",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channelFiveS.setDescription("Lembretes de rotinas 5S e padronização");

            NotificationChannel channelTasks = new NotificationChannel(
                    CHANNEL_TASKS,
                    "Eisenhower - Tarefas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelTasks.setDescription("Notificações de tarefas prioritárias");

            manager.createNotificationChannel(channelKaizen);
            manager.createNotificationChannel(channelPomodoro);
            manager.createNotificationChannel(channelFiveS);
            manager.createNotificationChannel(channelTasks);
        }
    }

    public static void scheduleNotification(Context context, int notificationId, String title, String message, String channelId, long triggerAtMillis) {
        scheduleRoutineNotification(context, -1L, -1L, notificationId, title, message, channelId, triggerAtMillis);
    }

    public static void scheduleRoutineNotification(Context context, long fivesItemId, long kaizenGoalId, int notificationId, String title, String message, String channelId, long triggerAtMillis) {
        if (context == null || triggerAtMillis <= 0) return;

        SharedPreferences prefs = context.getSharedPreferences("organizer_prefs", Context.MODE_PRIVATE);
        boolean notifEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notifEnabled) {
            // Global notifications are disabled by the user
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(NotificationReceiver.EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        if (fivesItemId > 0) {
            intent.putExtra(NotificationReceiver.EXTRA_FIVES_ITEM_ID, fivesItemId);
        }
        if (kaizenGoalId > 0) {
            intent.putExtra(NotificationReceiver.EXTRA_KAIZEN_GOAL_ID, kaizenGoalId);
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Exact alarm safety check: fallback to setAndAllowWhileIdle or set() if not permitted
        try {
            boolean canExact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canExact = alarmManager.canScheduleExactAlarms();
            }

            if (canExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            }
        } catch (SecurityException se) {
            // Graceful fallback: inexact alarm without crashing
            Log.w(TAG, "Exact alarm permission denied, falling back to inexact alarm: " + se.getMessage());
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void cancelNotification(Context context, int notificationId) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    /**
     * Reschedules all active reminders (5S routines and Kaizen goals) from Room database.
     * Called on device boot, timezone/time changes, or when global notifications are toggled ON.
     */
    public static void rescheduleAllAlarms(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        DatabaseClient.getInstance(appContext).getExecutor().execute(() -> {
            try {
                // 1. Reschedule 5S Routines
                List<FiveSItem> activeRoutines = DatabaseClient.getInstance(appContext)
                        .getDatabase().fiveSDao().getAllEnabledRoutinesSync();
                if (activeRoutines != null) {
                    for (FiveSItem routine : activeRoutines) {
                        long nextTime = getNextOccurrenceMillis(routine.getRoutineTime(), routine.getRoutineDays());
                        routine.setNextExecutionDate(nextTime);
                        DatabaseClient.getInstance(appContext).getDatabase().fiveSDao().updateItem(routine);

                        scheduleRoutineNotification(
                                appContext,
                                routine.getId(),
                                -1L,
                                OFFSET_FIVES + (int) routine.getId(),
                                "Rotina 5S: " + routine.getName(),
                                "Hora de executar sua rotina de 5S (" + routine.getRoutineTime() + ").",
                                CHANNEL_FIVES,
                                nextTime
                        );
                    }
                }

                // 2. Reschedule Kaizen Goals
                List<KaizenGoal> activeGoals = DatabaseClient.getInstance(appContext)
                        .getDatabase().kaizenDao().getAllEnabledGoalsSync();
                if (activeGoals != null) {
                    for (KaizenGoal goal : activeGoals) {
                        long nextTime = getNextOccurrenceMillis(goal.getNotificationTime(), goal.getNotificationDays());
                        scheduleRoutineNotification(
                                appContext,
                                -1L,
                                goal.getId(),
                                OFFSET_KAIZEN + (int) goal.getId(),
                                "Kaizen: " + goal.getTitle(),
                                "Hora do seu micro-hábito diário de 1%!",
                                CHANNEL_KAIZEN,
                                nextTime
                        );
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling alarms: " + e.getMessage());
            }
        });
    }

    /**
     * Cancels all active alarms when global notifications are disabled.
     */
    public static void cancelAllAlarms(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        DatabaseClient.getInstance(appContext).getExecutor().execute(() -> {
            try {
                List<FiveSItem> activeRoutines = DatabaseClient.getInstance(appContext)
                        .getDatabase().fiveSDao().getAllEnabledRoutinesSync();
                if (activeRoutines != null) {
                    for (FiveSItem routine : activeRoutines) {
                        cancelNotification(appContext, OFFSET_FIVES + (int) routine.getId());
                    }
                }
                List<KaizenGoal> activeGoals = DatabaseClient.getInstance(appContext)
                        .getDatabase().kaizenDao().getAllEnabledGoalsSync();
                if (activeGoals != null) {
                    for (KaizenGoal goal : activeGoals) {
                        cancelNotification(appContext, OFFSET_KAIZEN + (int) goal.getId());
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public static long getNextOccurrenceMillis(String timeStr, String daysStr) {
        int hour = 9, minute = 0;
        try {
            if (timeStr != null && timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim());
            }
        } catch (Exception ignored) {}

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (daysStr == null || daysStr.isEmpty() || daysStr.equalsIgnoreCase("Todos os dias") || daysStr.equalsIgnoreCase("Diário")) {
            return next.getTimeInMillis();
        }

        for (int i = 0; i < 7; i++) {
            int dayOfWeek = next.get(Calendar.DAY_OF_WEEK);
            if (isDayMatches(dayOfWeek, daysStr)) {
                return next.getTimeInMillis();
            }
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        return next.getTimeInMillis();
    }

    public static boolean isDayMatches(int calDay, String daysStr) {
        if (daysStr == null || daysStr.isEmpty() || daysStr.equalsIgnoreCase("Todos os dias") || daysStr.equalsIgnoreCase("Diário")) {
            return true;
        }
        String lower = daysStr.toLowerCase();
        switch (calDay) {
            case Calendar.SUNDAY: return lower.contains("dom");
            case Calendar.MONDAY: return lower.contains("seg");
            case Calendar.TUESDAY: return lower.contains("ter");
            case Calendar.WEDNESDAY: return lower.contains("qua");
            case Calendar.THURSDAY: return lower.contains("qui");
            case Calendar.FRIDAY: return lower.contains("sex");
            case Calendar.SATURDAY: return lower.contains("sáb") || lower.contains("sab");
        }
        return true;
    }

    public static boolean isSameDay(long time1, long time2) {
        if (time1 <= 0 || time2 <= 0) return false;
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(time1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(time2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Checks if the last execution maintains streak according to the routine's scheduled days.
     * If the routine is Mon/Wed/Fri and today is Wednesday, executing on Monday maintains streak.
     */
    public static boolean isStreakContinued(long lastExecutionTime, String daysStr) {
        if (lastExecutionTime <= 0) return false;
        long now = System.currentTimeMillis();
        if (isSameDay(lastExecutionTime, now)) return true; // Already executed today

        Calendar check = Calendar.getInstance();
        check.setTimeInMillis(now);
        check.add(Calendar.DAY_OF_YEAR, -1);

        // Look back up to 7 days for the most recent scheduled day
        for (int i = 0; i < 7; i++) {
            int dayOfWeek = check.get(Calendar.DAY_OF_WEEK);
            if (isDayMatches(dayOfWeek, daysStr)) {
                // This was the expected previous execution day
                Calendar lastCal = Calendar.getInstance();
                lastCal.setTimeInMillis(lastExecutionTime);
                return check.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
                       check.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR);
            }
            check.add(Calendar.DAY_OF_YEAR, -1);
        }
        return false;
    }

    public static void vibrateFeedback(Context context, long durationMillis) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences("organizer_prefs", Context.MODE_PRIVATE);
            boolean isVibrationEnabled = prefs.getBoolean("vibration_enabled", true);
            if (!isVibrationEnabled) return;

            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMillis);
                }
            }
        } catch (Exception ignored) {}
    }
}
