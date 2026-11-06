package com.example.organizer.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.organizer.data.dao.A3Dao;
import com.example.organizer.data.dao.EisenhowerDao;
import com.example.organizer.data.dao.FiveSDao;
import com.example.organizer.data.dao.KaizenDao;
import com.example.organizer.data.dao.KanbanDao;
import com.example.organizer.data.dao.NoteDao;
import com.example.organizer.data.dao.PomodoroDao;
import com.example.organizer.data.entity.A3Document;
import com.example.organizer.data.entity.EisenhowerTask;
import com.example.organizer.data.entity.FiveSItem;
import com.example.organizer.data.entity.FiveSProject;
import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.data.entity.KaizenProgress;
import com.example.organizer.data.entity.KanbanBoard;
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.entity.KanbanList;
import com.example.organizer.data.entity.Note;
import com.example.organizer.data.entity.PomodoroSession;
import com.example.organizer.data.entity.Tag;

@Database(
    entities = {
        KanbanBoard.class,
        KanbanList.class,
        KanbanCard.class,
        KaizenGoal.class,
        KaizenProgress.class,
        PomodoroSession.class,
        A3Document.class,
        FiveSProject.class,
        FiveSItem.class,
        EisenhowerTask.class,
        Note.class,
        Tag.class
    },
    version = 2,
    exportSchema = false
)
public abstract class OrganizerDatabase extends RoomDatabase {

    private static volatile OrganizerDatabase INSTANCE;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE fives_items ADD COLUMN lastExecutionDate INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE fives_items ADD COLUMN nextExecutionDate INTEGER NOT NULL DEFAULT 0");
        }
    };

    public abstract KanbanDao kanbanDao();
    public abstract KaizenDao kaizenDao();
    public abstract PomodoroDao pomodoroDao();
    public abstract A3Dao a3Dao();
    public abstract FiveSDao fiveSDao();
    public abstract EisenhowerDao eisenhowerDao();
    public abstract NoteDao noteDao();

    public static OrganizerDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (OrganizerDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            OrganizerDatabase.class,
                            "organizer_master_db"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
