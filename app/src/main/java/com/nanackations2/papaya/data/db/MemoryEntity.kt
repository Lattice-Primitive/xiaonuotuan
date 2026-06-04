package com.nanackations2.papaya.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LLM 长记忆条目。存储用户与 AI 交互中提取的重要信息片段，
 * 在后续对话中作为上下文注入 system prompt。
 */
@Entity(tableName = "llm_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 记忆内容摘要 */
    val content: String,
    /** 记忆类别标签，如 "preference"、"fact"、"instruction" */
    val tag: String = "general",
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
    /** 最后被引用/更新的时间 */
    val lastUsedAt: Long = System.currentTimeMillis(),
    /** 重要性评分 0~10，用于淘汰 */
    val importance: Int = 5,
)
