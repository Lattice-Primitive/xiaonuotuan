package com.nanackations2.papaya.api.llm

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 根据 [LlmConfig.provider] 创建对应的客户端。
 */
object LlmClientFactory {
    fun create(config: LlmConfig): LlmClient = when (config.provider) {
        LlmProvider.OPENAI, LlmProvider.DASHSCOPE -> OpenAiCompatibleClient(config)
        LlmProvider.ANTHROPIC -> AnthropicClient(config)
        LlmProvider.GEMINI -> GeminiClient(config)
        LlmProvider.OLLAMA -> OllamaClient(config)
    }

    /**
     * 创建带长记忆功能的客户端。
     * 会自动在每次对话前注入历史记忆，对话后异步提取新记忆。
     */
    fun createWithMemory(
        config: LlmConfig,
        memoryManager: LlmMemoryManager,
        autoExtract: Boolean = true,
    ): LlmClient {
        val base = create(config)
        return MemoryAwareLlmClient(base, memoryManager, autoExtract)
    }
}

/**
 * 全局共享的 OkHttpClient 单例。
 * OkHttpClient 内部维护连接池和线程池，应在整个进程中复用同一实例，
 * 避免每次请求都创建新连接池带来的内存和连接开销。
 */
private val sharedHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

private fun OkHttpClient.executeJson(req: Request): JSONObject {
    newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            throw IOException("HTTP ${resp.code}: ${body.take(500)}")
        }
        return JSONObject(body)
    }
}

/* ---------- OpenAI 兼容（含 OpenAI / DashScope / DeepSeek / Moonshot / oneapi / 任意自建） ---------- */

internal class OpenAiCompatibleClient(private val cfg: LlmConfig) : LlmClient {
    private val client = sharedHttpClient

    override suspend fun chat(prompt: String, imageBase64: String?): String {
        val content = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", prompt))
            if (imageBase64 != null) {
                put(JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imageBase64")))
            }
        }
        val body = JSONObject().apply {
            put("model", cfg.model)
            put("max_tokens", cfg.maxTokens)
            cfg.temperature?.let { put("temperature", it) }
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", content)
            ))
        }

        val req = Request.Builder()
            .url("${cfg.normalizedBaseUrl}v1/chat/completions")
            .addHeader("Authorization", "Bearer ${cfg.apiKey}")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val json = client.executeJson(req)
        return json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
            .ifBlank { "无返回结果" }
    }
}

/* ---------- Anthropic Claude ---------- */

internal class AnthropicClient(private val cfg: LlmConfig) : LlmClient {
    private val client = sharedHttpClient

    override suspend fun chat(prompt: String, imageBase64: String?): String {
        val contentArr = JSONArray().apply {
            if (imageBase64 != null) {
                put(JSONObject()
                    .put("type", "image")
                    .put("source", JSONObject()
                        .put("type", "base64")
                        .put("media_type", "image/jpeg")
                        .put("data", imageBase64)))
            }
            put(JSONObject().put("type", "text").put("text", prompt))
        }
        val body = JSONObject().apply {
            put("model", cfg.model)
            put("max_tokens", cfg.maxTokens)
            cfg.temperature?.let { put("temperature", it) }
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", contentArr)
            ))
        }

        val req = Request.Builder()
            .url("${cfg.normalizedBaseUrl}v1/messages")
            .addHeader("x-api-key", cfg.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val json = client.executeJson(req)
        // { content: [ {type:"text", text:"..."} ] }
        val parts = json.optJSONArray("content") ?: return "无返回结果"
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONObject(i) ?: continue
            if (p.optString("type") == "text") sb.append(p.optString("text"))
        }
        return sb.toString().ifBlank { "无返回结果" }
    }
}

/* ---------- Google Gemini ---------- */

internal class GeminiClient(private val cfg: LlmConfig) : LlmClient {
    private val client = sharedHttpClient

    override suspend fun chat(prompt: String, imageBase64: String?): String {
        val parts = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            if (imageBase64 != null) {
                put(JSONObject().put("inline_data", JSONObject()
                    .put("mime_type", "image/jpeg").put("data", imageBase64)))
            }
        }
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("role", "user").put("parts", parts)
            ))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", cfg.maxTokens)
                cfg.temperature?.let { put("temperature", it) }
            })
        }

        // Gemini 官方用 query 参数传 key，也支持 x-goog-api-key header
        val url = "${cfg.normalizedBaseUrl}v1beta/models/${cfg.model}:generateContent?key=${cfg.apiKey}"
        val req = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val json = client.executeJson(req)
        val candidates = json.optJSONArray("candidates") ?: return "无返回结果"
        val first = candidates.optJSONObject(0) ?: return "无返回结果"
        val partsOut = first.optJSONObject("content")?.optJSONArray("parts") ?: return "无返回结果"
        val sb = StringBuilder()
        for (i in 0 until partsOut.length()) {
            partsOut.optJSONObject(i)?.optString("text")?.let { sb.append(it) }
        }
        return sb.toString().ifBlank { "无返回结果" }
    }
}

/* ---------- Ollama（/api/generate 或 /api/chat） ---------- */

internal class OllamaClient(private val cfg: LlmConfig) : LlmClient {
    private val client = sharedHttpClient

    override suspend fun chat(prompt: String, imageBase64: String?): String {
        // Ollama /api/chat 支持 messages + images，统一走它
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
                if (imageBase64 != null) {
                    put("images", JSONArray().put(imageBase64))
                }
            })
        }
        val body = JSONObject().apply {
            put("model", cfg.model)
            put("stream", false)
            put("messages", messages)
            put("options", JSONObject().apply {
                put("num_predict", cfg.maxTokens)
                cfg.temperature?.let { put("temperature", it) }
            })
        }

        val req = Request.Builder()
            .url("${cfg.normalizedBaseUrl}api/chat")
            .apply { if (cfg.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${cfg.apiKey}") }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val json = client.executeJson(req)
        // { message: { role:"assistant", content:"..." } }
        return json.optJSONObject("message")?.optString("content").orEmpty()
            .ifBlank { json.optString("response").ifBlank { "无返回结果" } }
    }
}
