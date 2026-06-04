package com.nanackations2.papaya.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM llm_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM llm_memories ORDER BY importance DESC, lastUsedAt DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("SELECT * FROM llm_memories WHERE tag = :tag ORDER BY importance DESC, lastUsedAt DESC")
    suspend fun getByTag(tag: String): List<MemoryEntity>

    @Query("SELECT * FROM llm_memories ORDER BY importance DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM llm_memories WHERE content LIKE '%' || :keyword || '%' ORDER BY importance DESC")
    suspend fun search(keyword: String): List<MemoryEntity>

    @Query("UPDATE llm_memories SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM llm_memories")
    suspend fun count(): Int

    @Query("DELETE FROM llm_memories")
    suspend fun clearAll()
}
