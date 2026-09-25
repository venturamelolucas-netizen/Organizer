package com.example.organizer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.organizer.data.database.DatabaseClient;
import com.example.organizer.data.service.NotificationHelper;
import com.example.organizer.ui.dashboard.DashboardFragment;
import com.example.organizer.ui.eisenhower.EisenhowerFragment;
import com.example.organizer.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

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

        // Preload DB instance eagerly
        DatabaseClient.getInstance(this);

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_dashboard) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.menu_eisenhower) {
                loadFragment(new EisenhowerFragment());
                return true;
            } else if (itemId == R.id.menu_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return false;
            }
            return false;
        });
    }

    public void navigateToEisenhower() {
        bottomNav.setSelectedItemId(R.id.menu_eisenhower);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
