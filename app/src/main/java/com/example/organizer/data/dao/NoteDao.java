package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.organizer.data.entity.Note;
import com.example.organizer.data.entity.Tag;

import java.util.List;

@Dao
public interface NoteDao {
    // Notes
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM notes WHERE tag = :tag ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getNotesByTag(String tag);

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> searchNotes(String query);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getNoteById(long id);

    @Insert
    long insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<Tag> getAllTagsSync();

    @Insert
    long insertTag(Tag tag);

    @Delete
    void deleteTag(Tag tag);

    @Query("DELETE FROM notes")
    void deleteAllNotes();
}
