package com.nanackations2.papaya.api.llm

import android.content.Context
import android.util.Log
import com.nanackations2.papaya.data.db.AppDatabase
import com.nanackations2.papaya.data.db.MemoryDao
import com.nanackations2.papaya.data.db.MemoryEntity

/**
 * LLM 长记忆管理器。
 *
 * 功能：
 * - 从对话中自动提取值得记住的信息并持久化
 * - 在发送 prompt 前注入相关记忆作为上下文
 * - 支持手动增删改查记忆条目
 *
 * 使用示例：
 * ```
 * val manager = LlmMemoryManager.getInstance(context)
 * // 手动添加记忆
 * manager.addMemory("用户喜欢简洁的回复风格", tag = "preference", importance = 7)
 * // 获取记忆上下文用于注入 prompt
 * val memoryContext = manager.buildMemoryContext(limit = 20)
 * ```
 */
class LlmMemoryManager private constructor(private val dao: MemoryDao) {

    companion object {
        private const val TAG = "LlmMemoryManager"
        /** 注入 prompt 时最多携带的记忆条数 */
        const val DEFAULT_MEMORY_LIMIT = 100
        /** 超过此数量时触发摘要压缩，将多条记忆合并为精简文本 */
        const val SUMMARIZE_THRESHOLD = 50
        /** 摘要后保留的最大字符数（约 2000 token） */
        const val MAX_CONTEXT_CHARS = 4000
        /** 记忆提取的 system prompt（发给 LLM 让它判断哪些该记住） */
        const val EXTRACT_PROMPT = """你是一个记忆提取助手。请分析以下对话，提取出值得长期记住的信息。
每条记忆单独一行，格式：[标签] 内容
标签可选：preference（用户偏好）、fact（事实）、instruction（指令）、context（背景）
如果没有值得记忆的内容，回复"无"。
只输出记忆条目，不要其他解释。"""

        /** 记忆摘要压缩 prompt */
        const val SUMMARIZE_PROMPT = """你是一个记忆压缩助手。请将以下用户记忆信息压缩为精简摘要，保留所有关键信息但去除冗余。
要求：
1. 合并相似/重复的记忆
2. 保留所有重要事实、偏好、指令
3. 使用简洁的要点格式
4. 按类别分组（偏好/事实/指令/背景）
5. 总字数控制在2000字以内
只输出压缩后的记忆摘要，不要其他解释。"""

        /** 记忆相关性筛选 prompt */
        const val RELEVANCE_FILTER_PROMPT = """你是一个记忆检索助手。根据用户当前的问题，从以下记忆列表中筛选出与问题相关的记忆。
规则：
1. 只选择与当前问题直接或间接相关的记忆
2. 间接相关也算（比如问题涉及饮食，与健康习惯相关的记忆也应该选入）
3. 输出格式：每行一个记忆编号（从1开始），只输出编号，用逗号分隔
4. 如果没有相关记忆，输出"无"
只输出编号列表或"无"，不要其他解释。"""

        /** 思维链联想 prompt */
        const val CHAIN_OF_THOUGHT_PROMPT = """你是一个记忆联想助手。基于以下已有记忆和当前对话，请进行思维链推理，发现记忆之间的隐含关联，并推导出新的有价值信息。
规则：
1. 分析现有记忆之间的逻辑关系和因果联系
2. 结合当前对话上下文，推导出未被明确记录但可合理推断的信息
3. 生成新的记忆条目，格式：[标签] 内容
4. 标签可选：inference（推理）、connection（关联）、prediction（预测）
5. 只输出有高置信度的推理结果，不要臆测
6. 最多输出5条新记忆
如果无法推导出有意义的新信息，回复"无"。
只输出新记忆条目，不要其他解释。"""

        @Volatile
        private var INSTANCE: LlmMemoryManager? = null

        fun getInstance(context: Context): LlmMemoryManager {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = LlmMemoryManager(db.memoryDao())
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * 手动添加一条记忆。
     */
    suspend fun addMemory(
        content: String,
        tag: String = "general",
        importance: Int = 5,
    ): Long {
        val entity = MemoryEntity(
            content = content.trim(),
            tag = tag,
            importance = importance.coerceIn(0, 10),
        )
        return dao.insert(entity).also {
            Log.d(TAG, "添加记忆 #$it: [${tag}] ${content.take(50)}")
        }
    }

    /**
     * 获取所有记忆。
     */
    suspend fun getAllMemories(): List<MemoryEntity> = dao.getAll()

    /**
     * 获取最重要的 N 条记忆。
     */
    suspend fun getTopMemories(limit: Int = DEFAULT_MEMORY_LIMIT): List<MemoryEntity> =
        dao.getTopMemories(limit)

    /**
     * 关键字搜索记忆。
     */
    suspend fun searchMemories(keyword: String): List<MemoryEntity> = dao.search(keyword)

    /**
     * 删除一条记忆。
     */
    suspend fun deleteMemory(id: Long) = dao.deleteById(id)

    /**
     * 清空所有记忆。
     */
    suspend fun clearAll() = dao.clearAll()

    /**
     * 更新记忆的 lastUsedAt 时间戳（表示被引用过）。
     */
    suspend fun touchMemory(id: Long) = dao.touch(id)

    /**
     * 构建记忆上下文文本块，用于注入到 system prompt 或 user prompt 前。
     * 当记忆数量较多时，会使用分层策略：高重要性记忆全文 + 低重要性记忆压缩摘要。
     * 返回格式化好的字符串，如果没有记忆则返回 null。
     */
    suspend fun buildMemoryContext(limit: Int = DEFAULT_MEMORY_LIMIT): String? {
        val memories = dao.getTopMemories(limit)
        if (memories.isEmpty()) return null

        // 更新 lastUsedAt
        memories.forEach { dao.touch(it.id) }

        val sb = StringBuilder()
        sb.appendLine("【长期记忆】以下是关于用户的重要信息，请在回复时参考：")

        // 如果有缓存的摘要且记忆数超过阈值，使用摘要+最新记忆的组合策略
        if (memories.size > SUMMARIZE_THRESHOLD && cachedSummary != null) {
            sb.appendLine(cachedSummary)
            sb.appendLine()
            // 补充最新的 10 条记忆（可能还未被摘要覆盖）
            val recentMemories = memories.take(10)
            sb.appendLine("【最近新增记忆】")
            recentMemories.forEach { m ->
                sb.appendLine("- [${m.tag}] ${m.content}")
            }
        } else {
            memories.forEach { m ->
                sb.appendLine("- [${m.tag}] ${m.content}")
            }
        }

        // 截断保护：防止超长上下文
        val result = sb.toString()
        return if (result.length > MAX_CONTEXT_CHARS) {
            result.take(MAX_CONTEXT_CHARS) + "\n...（更多记忆已省略）"
        } else {
            result
        }
    }

    /** 缓存的记忆摘要 */
    @Volatile
    private var cachedSummary: String? = null

    /**
     * 触发记忆摘要压缩。当记忆数量超过阈值时调用，
     * 将所有记忆压缩为精简摘要并缓存，后续 buildMemoryContext 会使用摘要。
     *
     * @param summarizer 用于执行摘要的 LlmClient
     */
    suspend fun summarizeMemories(summarizer: LlmClient) {
        try {
            val allMemories = dao.getAll()
            if (allMemories.size < SUMMARIZE_THRESHOLD) {
                Log.d(TAG, "记忆数 ${allMemories.size} 未达到摘要阈值 $SUMMARIZE_THRESHOLD，跳过")
                return
            }

            val memoryText = allMemories.joinToString("\n") { "- [${it.tag}] ${it.content}" }
            val summary = summarizer.chat("$SUMMARIZE_PROMPT\n\n---\n$memoryText")

            if (summary.isNotBlank() && summary != "无") {
                cachedSummary = summary
                Log.d(TAG, "记忆摘要完成，${allMemories.size} 条 → ${summary.length} 字")
            }
        } catch (e: Exception) {
            Log.w(TAG, "记忆摘要失败", e)
        }
    }

    /**
     * 获取当前记忆总数。
     */
    suspend fun memoryCount(): Int = dao.count()

    /**
     * 基于用户问题智能检索相关记忆。
     * 两步策略：
     * 1. 关键词预筛选（从问题中提取关键词做 DB LIKE 查询，快速缩小范围）
     * 2. LLM 相关性精筛（让 LLM 从候选列表中选出真正相关的）
     *
     * @param query 用户当前问题
     * @param retriever 用于执行相关性判断的 LlmClient
     * @return 筛选后的相关记忆列表
     */
    suspend fun retrieveRelevantMemories(
        query: String,
        retriever: LlmClient,
    ): List<MemoryEntity> {
        try {
            // Step 1: 关键词预筛选 — 提取问题中的关键词做模糊搜索
            val keywords = extractKeywords(query)
            val candidates = mutableSetOf<MemoryEntity>()

            // 关键词搜索
            for (kw in keywords) {
                if (kw.length >= 2) {
                    candidates.addAll(dao.search(kw))
                }
            }

            // 始终补充最重要的记忆作为兜底
            candidates.addAll(dao.getTopMemories(20))

            val candidateList = candidates.toList().distinctBy { it.id }
            if (candidateList.isEmpty()) return emptyList()
            if (candidateList.size <= 5) {
                // 数量很少时直接全部返回，不需要 LLM 筛选
                candidateList.forEach { dao.touch(it.id) }
                return candidateList
            }

            // Step 2: LLM 相关性精筛
            val numberedList = candidateList.mapIndexed { i, m ->
                "${i + 1}. [${m.tag}] ${m.content}"
            }.joinToString("\n")

            val filterPrompt = "$RELEVANCE_FILTER_PROMPT\n\n用户问题：$query\n\n记忆列表：\n$numberedList"
            val result = retriever.chat(filterPrompt)

            if (result.trim() == "无" || result.isBlank()) {
                // 如果LLM认为没有相关记忆，至少返回最重要的几条
                return candidateList.take(5).also { list -> list.forEach { dao.touch(it.id) } }
            }

            // 解析编号列表
            val indices = result.replace(Regex("[^0-9,]"), "")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..candidateList.size }
                .map { it - 1 }

            val selected = indices.mapNotNull { candidateList.getOrNull(it) }
            selected.forEach { dao.touch(it.id) }

            Log.d(TAG, "记忆检索：${candidateList.size} 条候选 → ${selected.size} 条相关")
            return selected.ifEmpty { candidateList.take(5) }
        } catch (e: Exception) {
            Log.w(TAG, "记忆检索失败，回退到 top 记忆", e)
            return dao.getTopMemories(10)
        }
    }

    /**
     * 基于相关记忆和对话进行思维链联想，推导记忆之间的隐含关联。
     * 生成的新推理记忆会自动保存。
     *
     * @param query 用户当前问题
     * @param reply AI的回复
     * @param relevantMemories 本次使用的相关记忆
     * @param thinker 用于执行推理的 LlmClient
     */
    suspend fun chainOfThoughtAssociate(
        query: String,
        reply: String,
        relevantMemories: List<MemoryEntity>,
        thinker: LlmClient,
    ) {
        try {
            if (relevantMemories.size < 2) return // 至少2条记忆才有联想价值

            val memoryText = relevantMemories.joinToString("\n") { "- [${it.tag}] ${it.content}" }
            val dialog = "用户：$query\nAI：$reply"

            val prompt = "$CHAIN_OF_THOUGHT_PROMPT\n\n已有记忆：\n$memoryText\n\n当前对话：\n$dialog"
            val result = thinker.chat(prompt)

            if (result.trim() == "无" || result.isBlank()) return

            val lines = result.lines().filter { it.isNotBlank() }
            var added = 0
            for (line in lines) {
                if (added >= 5) break // 最多5条
                val match = """\[(\w+)]\s*(.+)""".toRegex().find(line.trim())
                if (match != null) {
                    val tag = match.groupValues[1]
                    val content = match.groupValues[2]
                    // 推理记忆重要性稍低，避免污染核心记忆
                    addMemory(content, tag, importance = 4)
                    added++
                }
            }
            if (added > 0) {
                Log.d(TAG, "思维链联想生成 $added 条新推理记忆")
            }
        } catch (e: Exception) {
            Log.w(TAG, "思维链联想失败", e)
        }
    }

    /**
     * 构建基于问题相关性筛选后的记忆上下文。
     * 比 buildMemoryContext 更精准，只注入与当前问题相关的记忆。
     */
    suspend fun buildRelevantMemoryContext(
        query: String,
        retriever: LlmClient,
    ): String? {
        val relevant = retrieveRelevantMemories(query, retriever)
        if (relevant.isEmpty()) return null

        val sb = StringBuilder()
        sb.appendLine("【相关记忆】以下是与你当前问题相关的用户信息，请在回复时参考：")
        relevant.forEach { m ->
            sb.appendLine("- [${m.tag}] ${m.content}")
        }

        val result = sb.toString()
        return if (result.length > MAX_CONTEXT_CHARS) {
            result.take(MAX_CONTEXT_CHARS) + "\n...（更多记忆已省略）"
        } else {
            result
        }
    }

    /** 停用词集合，提升为 companion 级别避免每次调用 extractKeywords 时重复分配 */
    private val STOP_WORDS = setOf(
        "的", "了", "是", "在", "我", "你", "他", "她", "它", "们",
        "有", "不", "这", "那", "什么", "怎么", "为什么", "吗", "呢",
        "啊", "哦", "嗯", "和", "与", "或", "但", "还", "也", "都",
        "就", "会", "能", "可以", "要", "想", "请", "帮", "告诉",
        "the", "a", "an", "is", "are", "was", "were", "be", "been",
        "do", "does", "did", "have", "has", "had", "will", "would",
        "can", "could", "should", "may", "might", "i", "you", "he",
        "she", "it", "we", "they", "my", "your", "his", "her", "its",
        "what", "how", "why", "when", "where", "which", "who",
    )

    /** 预编译的标点清洗正则 */
    private val PUNCTUATION_REGEX = "[，。？！、；：\u201C\u201D\u2018\u2019（）\\[\\]{}…—·《》\\n\\r\\s]+".toRegex()
    private val NON_CJK_REGEX = Regex("[^\\u4e00-\\u9fff]")
    private val ENGLISH_WORD_REGEX = Regex("[a-zA-Z]{3,}")

    /** 从问题中提取关键词（简单中文/英文分词） */
    private fun extractKeywords(query: String): List<String> {
        // 移除常见停用词和标点，提取有意义的词
        val cleaned = query.replace(PUNCTUATION_REGEX, " ")
        val stopWords = STOP_WORDS

        // 中文按2-4字滑窗 + 英文按空格分词
        val keywords = mutableListOf<String>()

        // 英文单词
        val englishWords = ENGLISH_WORD_REGEX.findAll(cleaned).map { it.value.lowercase() }
        keywords.addAll(englishWords.filter { it !in stopWords }.toList())

        // 中文：提取2-4字的片段（简单滑窗，非精确分词但够用）
        val chineseChars = cleaned.replace(NON_CJK_REGEX, "")
        if (chineseChars.length >= 2) {
            for (len in 2..minOf(4, chineseChars.length)) {
                for (i in 0..chineseChars.length - len) {
                    val segment = chineseChars.substring(i, i + len)
                    if (segment.none { it.toString() in stopWords }) {
                        keywords.add(segment)
                    }
                }
            }
        }

        return keywords.distinct().take(10) // 最多10个关键词
    }

    /**
     * 从一段对话中自动提取记忆。
     * 需要传入一个 LlmClient 来做提取分析。
     *
     * @param userMessage 用户消息
     * @param assistantReply AI回复
     * @param extractor 用于提取记忆的 LlmClient
     */
    suspend fun extractAndSaveMemories(
        userMessage: String,
        assistantReply: String,
        extractor: LlmClient,
    ) {
        try {
            val dialog = "用户：$userMessage\nAI：$assistantReply"
            val result = extractor.chat("$EXTRACT_PROMPT\n\n---\n$dialog")

            if (result.trim() == "无" || result.isBlank()) return

            // 解析 [标签] 内容 格式
            val lines = result.lines().filter { it.isNotBlank() }
            for (line in lines) {
                val match = """\[(\w+)]\s*(.+)""".toRegex().find(line.trim())
                if (match != null) {
                    val tag = match.groupValues[1]
                    val content = match.groupValues[2]
                    addMemory(content, tag, importance = 6)
                } else if (line.trim().startsWith("-")) {
                    // 兼容 "- 内容" 格式
                    val content = line.trim().removePrefix("-").trim()
                    if (content.isNotBlank()) {
                        addMemory(content, "general", importance = 5)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "记忆提取失败", e)
        }
    }
}
