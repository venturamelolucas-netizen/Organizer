package com.example.organizer.ui.eisenhower;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.google.android.material.appbar.MaterialToolbar;

public class EisenhowerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eisenhower);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_eisenhower);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.eisenhower_fragment_container, new EisenhowerFragment())
                    .commit();
        }
    }
}
