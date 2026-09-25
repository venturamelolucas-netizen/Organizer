package com.example.organizer.data.database;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseClient {
    private static DatabaseClient instance;
    private final OrganizerDatabase database;
    private final ExecutorService executorService;

    private DatabaseClient(Context context) {
        database = OrganizerDatabase.getInstance(context);
        executorService = Executors.newFixedThreadPool(4);
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public OrganizerDatabase getDatabase() {
        return database;
    }

    public ExecutorService getExecutor() {
        return executorService;
    }
}
