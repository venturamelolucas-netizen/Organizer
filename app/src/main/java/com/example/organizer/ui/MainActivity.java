package com.example.organizer.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.service.NotificationHelper;
import com.example.organizer.ui.dashboard.DashboardFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply saved theme preference
        SharedPreferences prefs = getSharedPreferences("organizer_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this);

        // Request POST_NOTIFICATIONS on Android 13+ if not already granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        NotificationHelper.rescheduleAllAlarms(this);
                    }
                }).launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Preload DB instance eagerly
        DatabaseClient.getInstance(this);

        // Handle Edge-to-Edge System Bars Window Insets
        View root = findViewById(R.id.fragment_container);
        if (root != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars()
                );
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }
}
