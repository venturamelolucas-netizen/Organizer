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
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(NotificationReceiver.EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId);

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
        try {
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
