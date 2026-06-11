package com.englishpractice.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE createdDate LIKE :yearMonth || '%' ORDER BY id DESC")
    fun getNotesByMonth(yearMonth: String): Flow<List<Note>>

    @Query("SELECT createdDate, COUNT(*) as cnt FROM notes WHERE recordDate IS NOT NULL GROUP BY recordDate")
    fun getRecordDates(): Flow<List<RecordDateCount>>

    @Query("SELECT * FROM notes WHERE english = :english LIMIT 1")
    suspend fun findByEnglish(english: String): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class RecordDateCount(
    val createdDate: String,
    val cnt: Int
)
