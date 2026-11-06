package com.example.organizer.data.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.ui.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_FIVES_ITEM_ID = "extra_fives_item_id";
    public static final String EXTRA_KAIZEN_GOAL_ID = "extra_kaizen_goal_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        // Handle device boot or time changed
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_TIME_CHANGED.equals(action) ||
            Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            final PendingResult pr = goAsync();
            DatabaseClient.getInstance(context.getApplicationContext()).getExecutor().execute(() -> {
                try {
                    NotificationHelper.rescheduleAllAlarms(context.getApplicationContext());
                } finally {
                    pr.finish();
                }
            });
            return;
        }

        // 1. Check Global Preference
        SharedPreferences prefs = context.getSharedPreferences("organizer_prefs", Context.MODE_PRIVATE);
        boolean notifEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notifEnabled) {
            Log.d(TAG, "Notification skipped: global notifications are disabled.");
            return;
        }

        // 2. Check Android 13+ POST_NOTIFICATIONS runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification suppressed.");
                return;
            }
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String channelId = intent.getStringExtra(EXTRA_CHANNEL_ID);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis());
        long fivesItemId = intent.getLongExtra(EXTRA_FIVES_ITEM_ID, -1L);
        long kaizenGoalId = intent.getLongExtra(EXTRA_KAIZEN_GOAL_ID, -1L);

        if (title == null) title = "Organizer";
        if (message == null) message = "Lembrete de rotina ou tarefa.";
        if (channelId == null) channelId = NotificationHelper.CHANNEL_TASKS;

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

        // 3. Haptic feedback
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

        // 4. Handle recurring reschedules safely using goAsync()
        if (fivesItemId > 0 || kaizenGoalId > 0) {
            final PendingResult pendingResult = goAsync();
            final Context appContext = context.getApplicationContext();
            final String fTitle = title;
            final String fMessage = message;
            final String fChannelId = channelId;
            final int fNotifId = notificationId;

            DatabaseClient.getInstance(appContext).getExecutor().execute(() -> {
                try {
                    // Recurring 5S routine
                    if (fivesItemId > 0) {
                        FiveSItem item = DatabaseClient.getInstance(appContext).getDatabase().fiveSDao().getItemById(fivesItemId);
                        if (item != null && item.isRoutineEnabled()) {
                            long nextOccurrence = NotificationHelper.getNextOccurrenceMillis(item.getRoutineTime(), item.getRoutineDays());
                            item.setNextExecutionDate(nextOccurrence);
                            DatabaseClient.getInstance(appContext).getDatabase().fiveSDao().updateItem(item);

                            NotificationHelper.scheduleRoutineNotification(
                                    appContext,
                                    fivesItemId,
                                    -1L,
                                    fNotifId,
                                    fTitle,
                                    fMessage,
                                    fChannelId,
                                    nextOccurrence
                            );
                        }
                    }

                    // Recurring Kaizen goal
                    if (kaizenGoalId > 0) {
                        KaizenGoal goal = DatabaseClient.getInstance(appContext).getDatabase().kaizenDao().getGoalById(kaizenGoalId);
                        if (goal != null && goal.isNotificationsEnabled()) {
                            long nextOccurrence = NotificationHelper.getNextOccurrenceMillis(goal.getNotificationTime(), goal.getNotificationDays());
                            NotificationHelper.scheduleRoutineNotification(
                                    appContext,
                                    -1L,
                                    kaizenGoalId,
                                    fNotifId,
                                    fTitle,
                                    fMessage,
                                    fChannelId,
                                    nextOccurrence
                            );
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling recurring routine: " + e.getMessage());
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
