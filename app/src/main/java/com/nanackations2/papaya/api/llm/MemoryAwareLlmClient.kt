package com.nanackations2.papaya.api.llm

import android.util.Log
import com.nanackations2.papaya.data.db.MemoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 带长记忆能力的 LlmClient 包装器。
 *
 * 完整流程：
 * 1. **记忆检索**：根据用户问题，关键词预筛选 + LLM 相关性精筛，只注入相关记忆
 * 2. **上下文注入**：将筛选出的相关记忆注入到 prompt 中
 * 3. **调用 LLM**：执行实际对话
 * 4. **记忆提取**：从对话中自动提取新记忆并持久化
 * 5. **思维链联想**：基于相关记忆和对话，推导记忆间的隐含关联，生成推理记忆
 * 6. **摘要压缩**：记忆超过阈值时自动压缩
 *
 * 对外接口与 [LlmClient] 完全一致，可无缝替换。
 *
 * @param delegate 实际执行请求的底层 LlmClient
 * @param memoryManager 记忆管理器
 * @param autoExtract 是否在每次对话后自动提取记忆（默认 true）
 * @param useRelevanceRetrieval 是否使用基于问题的相关性检索（默认 true，false 则退化为全量注入）
 * @param useChainOfThought 是否启用思维链联想（默认 true）
 */
class MemoryAwareLlmClient(
    private val delegate: LlmClient,
    private val memoryManager: LlmMemoryManager,
    private val autoExtract: Boolean = true,
    private val useRelevanceRetrieval: Boolean = true,
    private val useChainOfThought: Boolean = true,
) : LlmClient {

    companion object {
        private const val TAG = "MemoryAwareLlmClient"
    }

    /** 使用 SupervisorJob 隔离子任务失败，避免单个记忆提取失败导致整个 scope 取消 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 记录本次对话使用的相关记忆（供异步思维链联想使用） */
    @Volatile
    private var lastRelevantMemories: List<MemoryEntity> = emptyList()

    override suspend fun chat(prompt: String, imageBase64: String?): String {
        // 1. 根据问题检索相关记忆
        val memoryContext = if (useRelevanceRetrieval && imageBase64 == null) {
            try {
                val relevant = memoryManager.retrieveRelevantMemories(prompt, delegate)
                lastRelevantMemories = relevant
                if (relevant.isNotEmpty()) {
                    buildContextFromMemories(relevant)
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "相关性检索失败，回退到全量记忆", e)
                memoryManager.buildMemoryContext()
            }
        } else {
            memoryManager.buildMemoryContext()
        }

        // 2. 构建带记忆的 prompt
        val enrichedPrompt = if (memoryContext != null) {
            "$memoryContext\n\n---\n$prompt"
        } else {
            prompt
        }

        // 3. 调用底层 LLM
        val reply = delegate.chat(enrichedPrompt, imageBase64)

        // 4. 异步后处理：提取记忆 + 思维链联想 + 摘要压缩
        if (autoExtract && imageBase64 == null) {
            val relevantForAssociation = lastRelevantMemories.toList()
            scope.launch {
                try {
                    // 提取新记忆
                    memoryManager.extractAndSaveMemories(prompt, reply, delegate)

                    // 思维链联想：基于相关记忆推导新关联
                    if (useChainOfThought && relevantForAssociation.size >= 2) {
                        memoryManager.chainOfThoughtAssociate(
                            prompt, reply, relevantForAssociation, delegate
                        )
                    }

                    // 记忆超过阈值时自动触发摘要压缩
                    if (memoryManager.memoryCount() >= LlmMemoryManager.SUMMARIZE_THRESHOLD) {
                        memoryManager.summarizeMemories(delegate)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "异步记忆处理失败", e)
                }
            }
        }

        return reply
    }

    override suspend fun chatStreaming(
        prompt: String,
        imageBase64: String?,
        onDelta: (String) -> Unit,
    ): String {
        // 1. 检索相关记忆
        val memoryContext = if (useRelevanceRetrieval && imageBase64 == null) {
            try {
                val relevant = memoryManager.retrieveRelevantMemories(prompt, delegate)
                lastRelevantMemories = relevant
                if (relevant.isNotEmpty()) {
                    buildContextFromMemories(relevant)
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "相关性检索失败，回退到全量记忆", e)
                memoryManager.buildMemoryContext()
            }
        } else {
            memoryManager.buildMemoryContext()
        }

        // 2. 注入记忆上下文
        val enrichedPrompt = if (memoryContext != null) {
            "$memoryContext\n\n---\n$prompt"
        } else {
            prompt
        }

        // 3. 流式调用
        val reply = delegate.chatStreaming(enrichedPrompt, imageBase64, onDelta)

        // 4. 异步后处理
        if (autoExtract && imageBase64 == null) {
            val relevantForAssociation = lastRelevantMemories.toList()
            scope.launch {
                try {
                    memoryManager.extractAndSaveMemories(prompt, reply, delegate)

                    if (useChainOfThought && relevantForAssociation.size >= 2) {
                        memoryManager.chainOfThoughtAssociate(
                            prompt, reply, relevantForAssociation, delegate
                        )
                    }

                    if (memoryManager.memoryCount() >= LlmMemoryManager.SUMMARIZE_THRESHOLD) {
                        memoryManager.summarizeMemories(delegate)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "异步记忆处理失败", e)
                }
            }
        }

        return reply
    }

    /** 将记忆列表格式化为上下文文本 */
    private fun buildContextFromMemories(memories: List<MemoryEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("【相关记忆】以下是与你当前问题相关的用户信息，请在回复时参考：")
        memories.forEach { m ->
            sb.appendLine("- [${m.tag}] ${m.content}")
        }
        val result = sb.toString()
        return if (result.length > LlmMemoryManager.MAX_CONTEXT_CHARS) {
            result.take(LlmMemoryManager.MAX_CONTEXT_CHARS) + "\n...（更多记忆已省略）"
        } else {
            result
        }
    }
}
