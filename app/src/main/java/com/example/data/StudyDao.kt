package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE date = :date")
    fun getTotalDurationForDate(date: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @androidx.room.Update
    suspend fun updateSession(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): StudySessionEntity?

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
