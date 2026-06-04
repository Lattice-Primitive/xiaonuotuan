package com.nanackations2.papaya.api.llm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 支持的大模型服务商。
 *
 * 约定：所有实现都接收"文本 prompt + 可选图片"，返回文本。
 * 端点/Key 通过 [LlmConfig] 注入，不硬编码。
 */
enum class LlmProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
) {
    /** 包括 OpenAI 官方、任何 OpenAI 兼容代理（DeepSeek、Moonshot、SiliconFlow、oneapi 等） */
    OPENAI("OpenAI 兼容", "https://api.openai.com/", "gpt-4o-mini"),
    /** Anthropic Claude */
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com/", "claude-3-5-sonnet-20241022"),
    /** Google Gemini v1beta */
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/", "gemini-2.0-flash"),
    /** 阿里云 DashScope（通义千问）兼容模式 */
    DASHSCOPE("阿里通义", "https://dashscope.aliyuncs.com/compatible-mode/", "qwen-vl-plus"),
    /** 本地 Ollama（http://localhost:11434 或局域网地址） */
    OLLAMA("本地 Ollama", "http://localhost:11434/", "llava"),
    ;

    companion object {
        fun fromName(name: String?): LlmProvider =
            entries.firstOrNull { it.name == name } ?: OPENAI
    }
}

/**
 * 通用 LLM 配置。[baseUrl] 必须以 `/` 结尾。
 */
data class LlmConfig(
    val provider: LlmProvider,
    val apiKey: String,
    val baseUrl: String = provider.defaultBaseUrl,
    val model: String = provider.defaultModel,
    val temperature: Double? = null,
    val maxTokens: Int = 800,
) {
    val normalizedBaseUrl: String
        get() = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
}

/**
 * 统一的 LLM 服务接口。
 */
interface LlmClient {
    suspend fun chat(prompt: String, imageBase64: String? = null): String
    suspend fun chatStreaming(prompt: String, imageBase64: String? = null, onDelta: (String) -> Unit): String {
        // 默认实现退化为一次性返回
        val r = chat(prompt, imageBase64); onDelta(r); return r
    }
}

/** Bitmap → base64 JPEG（最大边 1024，质量 80），给多模态调用使用 */
fun bitmapToBase64(bitmap: Bitmap, maxSize: Int = 1024, quality: Int = 80): String {
    val resized = if (bitmap.width <= maxSize && bitmap.height <= maxSize) bitmap
    else {
        val r = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * r).toInt(), (bitmap.height * r).toInt(), true)
    }
    val out = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

fun bytesToBase64(bytes: ByteArray): String {
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalArgumentException("无法解码图片数据")
    return bitmapToBase64(bmp)
}
