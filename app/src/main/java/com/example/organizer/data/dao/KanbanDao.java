package com.example.organizer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.organizer.data.entity.KanbanBoard;
import com.example.organizer.data.entity.KanbanCard;
import com.example.organizer.data.entity.KanbanList;

import java.util.List;

@Dao
public interface KanbanDao {
    // Boards
    @Query("SELECT * FROM kanban_boards ORDER BY createdAt DESC")
    LiveData<List<KanbanBoard>> getAllBoards();

    @Query("SELECT * FROM kanban_boards WHERE id = :boardId LIMIT 1")
    KanbanBoard getBoardById(long boardId);

    @Insert
    long insertBoard(KanbanBoard board);

    @Update
    void updateBoard(KanbanBoard board);

    @Delete
    void deleteBoard(KanbanBoard board);

    @Query("DELETE FROM kanban_cards WHERE listId IN (SELECT id FROM kanban_lists WHERE boardId = :boardId)")
    void deleteCardsByBoardId(long boardId);

    @Query("DELETE FROM kanban_lists WHERE boardId = :boardId")
    void deleteListsByBoardId(long boardId);

    @Query("DELETE FROM kanban_cards WHERE listId = :listId")
    void deleteCardsByListId(long listId);

    // Lists
    @Query("SELECT * FROM kanban_lists WHERE boardId = :boardId ORDER BY position ASC")
    LiveData<List<KanbanList>> getListsByBoard(long boardId);

    @Query("SELECT * FROM kanban_lists WHERE boardId = :boardId ORDER BY position ASC")
    List<KanbanList> getListsByBoardSync(long boardId);

    @Insert
    long insertList(KanbanList list);

    @Update
    void updateList(KanbanList list);

    @Delete
    void deleteList(KanbanList list);

    // Cards
    @Query("SELECT * FROM kanban_cards WHERE listId = :listId ORDER BY position ASC")
    LiveData<List<KanbanCard>> getCardsByList(long listId);

    @Query("SELECT * FROM kanban_cards WHERE listId = :listId ORDER BY position ASC")
    List<KanbanCard> getCardsByListSync(long listId);

    @Query("SELECT * FROM kanban_cards WHERE id = :cardId LIMIT 1")
    KanbanCard getCardById(long cardId);

    @Insert
    long insertCard(KanbanCard card);

    @Update
    void updateCard(KanbanCard card);

    @Delete
    void deleteCard(KanbanCard card);

    @Query("DELETE FROM kanban_boards")
    void deleteAllBoards();
}
