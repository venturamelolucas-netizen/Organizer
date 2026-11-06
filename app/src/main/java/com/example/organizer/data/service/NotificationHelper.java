package com.example.organizer.data.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Calendar;

public class NotificationHelper {

    public static final String CHANNEL_KAIZEN = "channel_kaizen";
    public static final String CHANNEL_POMODORO = "channel_pomodoro";
    public static final String CHANNEL_FIVES = "channel_fives";
    public static final String CHANNEL_TASKS = "channel_tasks";

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
        scheduleRoutineNotification(context, -1L, notificationId, title, message, channelId, triggerAtMillis);
    }

    public static void scheduleRoutineNotification(Context context, long fivesItemId, int notificationId, String title, String message, String channelId, long triggerAtMillis) {
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

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
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

    private static boolean isDayMatches(int calDay, String daysStr) {
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

    public static boolean isYesterday(long time) {
        if (time <= 0) return false;
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(time);
        return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    public static void cancelNotification(Context context, int notificationId) {
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

    public static void vibrateFeedback(Context context, long durationMillis) {
        if (context == null) return;
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("organizer_prefs", Context.MODE_PRIVATE);
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
