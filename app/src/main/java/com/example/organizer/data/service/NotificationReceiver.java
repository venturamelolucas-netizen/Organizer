package com.example.organizer.data.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.ui.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_FIVES_ITEM_ID = "extra_fives_item_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String channelId = intent.getStringExtra(EXTRA_CHANNEL_ID);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis());
        long fivesItemId = intent.getLongExtra(EXTRA_FIVES_ITEM_ID, -1L);

        if (title == null) title = "Organizer";
        if (message == null) message = "Lembrete de rotina ou tarefa.";
        if (channelId == null) channelId = NotificationHelper.CHANNEL_TASKS;

        android.content.SharedPreferences prefs = context.getSharedPreferences("organizer_prefs", Context.MODE_PRIVATE);
        boolean notifEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notifEnabled) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationHelper.createNotificationChannels(context);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(notificationId, builder.build());

        // Auto-reschedule next occurrence for recurring 5S routines
        if (fivesItemId > 0) {
            final String fTitle = title;
            final String fMessage = message;
            final String fChannelId = channelId;
            final int fNotifId = notificationId;
            final Context appContext = context.getApplicationContext();

            DatabaseClient.getInstance(appContext).getExecutor().execute(() -> {
                try {
                    FiveSItem item = DatabaseClient.getInstance(appContext).getDatabase().fiveSDao().getItemById(fivesItemId);
                    if (item != null && item.isRoutineEnabled()) {
                        long nextOccurrence = NotificationHelper.getNextOccurrenceMillis(item.getRoutineTime(), item.getRoutineDays());
                        item.setNextExecutionDate(nextOccurrence);
                        DatabaseClient.getInstance(appContext).getDatabase().fiveSDao().updateItem(item);

                        NotificationHelper.scheduleRoutineNotification(
                                appContext,
                                fivesItemId,
                                fNotifId,
                                fTitle,
                                fMessage,
                                fChannelId,
                                nextOccurrence
                        );
                    }
                } catch (Exception ignored) {}
            });
        }

        // Haptic feedback
        boolean vibEnabled = prefs.getBoolean("vibration_enabled", true);
        if (vibEnabled) {
            try {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(200);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
