package com.nanackations2.papaya.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY hour ASC, minute ASC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE personId = :personId ORDER BY hour ASC, minute ASC")
    fun getTodosByPerson(personId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY hour ASC, minute ASC")
    suspend fun getAllTodosOnce(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("UPDATE todos SET isDone = :isDone WHERE id = :id")
    suspend fun updateDoneStatus(id: Long, isDone: Boolean)

    @Query("UPDATE todos SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)
}
