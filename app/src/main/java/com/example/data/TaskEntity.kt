package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["listId"]),
        Index(value = ["isCompleted"]),
        Index(value = ["isStarred"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long = 1L,
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)
