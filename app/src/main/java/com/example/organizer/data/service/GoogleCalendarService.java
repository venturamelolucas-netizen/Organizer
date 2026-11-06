package com.example.organizer.data.service;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.core.content.ContextCompat;

import com.example.organizer.data.entity.EisenhowerTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GoogleCalendarService {

    public static boolean hasCalendarPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    public static long getPrimaryCalendarId(Context context) {
        if (!hasCalendarPermissions(context)) return -1;

        ContentResolver cr = context.getContentResolver();
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.IS_PRIMARY
        };

        try (Cursor cursor = cr.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID);
                int primaryCol = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY);
                int accountCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME);

                long fallbackId = cursor.getLong(idCol);
                do {
                    long id = cursor.getLong(idCol);
                    String account = accountCol != -1 ? cursor.getString(accountCol) : "";
                    int isPrimary = primaryCol != -1 ? cursor.getInt(primaryCol) : 0;

                    if (isPrimary == 1 || (account != null && account.contains("@gmail.com"))) {
                        return id;
                    }
                } while (cursor.moveToNext());

                return fallbackId;
            }
        } catch (Exception e) {
            // Permission or provider error
        }
        return -1;
    }

    public static boolean eventExists(Context context, long eventId) {
        if (!hasCalendarPermissions(context) || eventId <= 0) return false;
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            try (Cursor cursor = cr.query(uri, new String[]{CalendarContract.Events._ID}, null, null, null)) {
                return cursor != null && cursor.moveToFirst();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static long syncTaskToCalendar(Context context, EisenhowerTask task) {
        if (!hasCalendarPermissions(context)) {
            return -1;
        }

        long calendarId = getPrimaryCalendarId(context);
        if (calendarId == -1) {
            return -1;
        }

        long startMillis = calculateTaskStartTime(task);
        long endMillis = startMillis + (60 * 60 * 1000); // 1 hour duration default

        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, "[Organizer] " + task.getTitle());
        values.put(CalendarContract.Events.DESCRIPTION, task.getDescription() + "\n\n(Quadro de Eisenhower - Prioridade: " + task.getPriority() + ")");
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        try {
            if (task.getGoogleCalendarEventId() > 0) {
                if (eventExists(context, task.getGoogleCalendarEventId())) {
                    // Update existing event without duplicating
                    Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, task.getGoogleCalendarEventId());
                    cr.update(updateUri, values, null, null);
                    return task.getGoogleCalendarEventId();
                }
            }

            // Insert new event only if existing event does not exist
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            if (uri != null) {
                return Long.parseLong(uri.getLastPathSegment());
            }
        } catch (SecurityException | NumberFormatException e) {
            // Calendar write error
        }
        return -1;
    }

    public static void deleteTaskFromCalendar(Context context, long eventId) {
        if (!hasCalendarPermissions(context) || eventId <= 0) return;

        try {
            ContentResolver cr = context.getContentResolver();
            Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            cr.delete(deleteUri, null, null);
        } catch (Exception e) {
            // Ignore if already deleted
        }
    }

    private static long calculateTaskStartTime(EisenhowerTask task) {
        if (task.getDate() != null && !task.getDate().isEmpty()) {
            try {
                String dateTimeStr = task.getDate();
                String pattern = "yyyy-MM-dd";
                if (task.getTime() != null && !task.getTime().isEmpty()) {
                    dateTimeStr += " " + task.getTime();
                    pattern = "yyyy-MM-dd HH:mm";
                } else {
                    dateTimeStr += " 09:00";
                    pattern = "yyyy-MM-dd HH:mm";
                }
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                Date parsed = sdf.parse(dateTimeStr);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (Exception e) {
                // fallback to timestamp
            }
        }

        if (task.getTimestamp() > 0) {
            return task.getTimestamp();
        }

        return System.currentTimeMillis() + 3600000L;
    }
}
