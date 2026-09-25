package com.example.organizer.ui.eisenhower;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.R;

public class EisenhowerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EisenhowerFragment())
                    .commit();
        }
    }
}
