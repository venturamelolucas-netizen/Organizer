package com.example.organizer;

import android.app.Application;

import com.example.organizer.data.service.NotificationHelper;
import com.google.android.material.color.DynamicColors;

public class OrganizerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Material You Dynamic Colors across all activities on Android 12+
        DynamicColors.applyToActivitiesIfAvailable(this);
        // Initialize notification channels at application start
        NotificationHelper.createNotificationChannels(this);
    }
}
