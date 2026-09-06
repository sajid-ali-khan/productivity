package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: yyyy-MM-dd
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val subject: String = "",
    val notes: String = ""
)
