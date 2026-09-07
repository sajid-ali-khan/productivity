package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM task_lists ORDER BY isDefault DESC, id ASC")
    fun getAllLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: Long): TaskListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TaskListEntity): Long

    @Update
    suspend fun updateList(list: TaskListEntity)

    @Query("DELETE FROM task_lists WHERE id = :listId AND isDefault = 0")
    suspend fun deleteListById(listId: Long)

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY isCompleted ASC, orderIndex ASC, createdAt DESC")
    fun getTasksForList(listId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isStarred = 1 ORDER BY isCompleted ASC, orderIndex ASC, createdAt DESC")
    fun getStarredTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE listId = :listId AND isCompleted = 1")
    suspend fun deleteCompletedTasksForList(listId: Long)

    @Query("DELETE FROM tasks WHERE listId = :listId")
    suspend fun deleteTasksByListId(listId: Long)

    @Transaction
    suspend fun deleteListAndItsTasks(listId: Long) {
        deleteTasksByListId(listId)
        deleteListById(listId)
    }
}
