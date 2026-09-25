package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.organizer.data.entity.A3Document;

import java.util.List;

@Dao
public interface A3Dao {
    @Query("SELECT * FROM a3_documents ORDER BY updatedAt DESC")
    LiveData<List<A3Document>> getAllDocuments();

    @Query("SELECT * FROM a3_documents WHERE id = :id LIMIT 1")
    A3Document getDocumentById(long id);

    @Insert
    long insertDocument(A3Document document);

    @Update
    void updateDocument(A3Document document);

    @Delete
    void deleteDocument(A3Document document);

    @Query("DELETE FROM a3_documents")
    void deleteAllDocuments();
}
