package com.nanackations2.papaya.service

import android.content.Context
import android.graphics.Bitmap
import com.nanackations2.papaya.api.llm.LlmClient
import com.nanackations2.papaya.api.llm.LlmClientFactory
import com.nanackations2.papaya.api.llm.LlmConfig
import com.nanackations2.papaya.api.llm.LlmConfigStore
import com.nanackations2.papaya.api.llm.LlmMemoryManager
import com.nanackations2.papaya.api.llm.LlmProvider
import com.nanackations2.papaya.api.llm.MemoryAwareLlmClient
import com.nanackations2.papaya.api.llm.bitmapToBase64
import com.nanackations2.papaya.api.llm.bytesToBase64

/**
 * 图片描述服务。内部委托给多 Provider 的 [LlmClient]，
 * 因此同一套 API 可以接 OpenAI / Claude / Gemini / 通义 / Ollama 等。
 *
 * 常用入口：
 * - [create]：根据开发者选项里的配置自动构造
 * - [forConfig]：显式传入 [LlmConfig]
 */
class ImageDescriber private constructor(
    private val client: LlmClient,
) {

    suspend fun describeBitmap(bitmap: Bitmap, prompt: String = DEFAULT_PROMPT): String {
        val base64 = bitmapToBase64(bitmap)
        return client.chat(prompt, base64)
    }

    suspend fun describeBytes(bytes: ByteArray, prompt: String = DEFAULT_PROMPT): String {
        val base64 = bytesToBase64(bytes)
        return client.chat(prompt, base64)
    }

    /**
     * 纯文本聊天（没有图片）。
     */
    suspend fun chat(prompt: String): String = client.chat(prompt, null)

    companion object {
        const val DEFAULT_PROMPT = "请详细描述这张图片中的内容"

        /** 读开发者选项里的配置，自动创建（不带记忆）。 */
        fun create(context: Context): ImageDescriber = forConfig(LlmConfigStore.read(context))

        /** 读开发者选项里的配置，创建带长记忆的实例。 */
        fun createWithMemory(context: Context): ImageDescriber {
            val config = LlmConfigStore.read(context)
            val baseClient = LlmClientFactory.create(config)
            val memoryManager = LlmMemoryManager.getInstance(context)
            val memoryClient = MemoryAwareLlmClient(baseClient, memoryManager)
            return ImageDescriber(memoryClient)
        }

        fun forConfig(config: LlmConfig): ImageDescriber =
            ImageDescriber(LlmClientFactory.create(config))

        /**
         * 用已有的 LlmClient 创建（可传入 MemoryAwareLlmClient）。
         */
        fun forClient(client: LlmClient): ImageDescriber = ImageDescriber(client)

        /**
         * 兼容旧签名：仅传 apiKey / baseUrl / model 时走 OpenAI 兼容实现。
         */
        fun legacyOpenAi(apiKey: String, baseUrl: String, model: String = "gpt-4o-mini"): ImageDescriber =
            forConfig(LlmConfig(provider = LlmProvider.OPENAI, apiKey = apiKey, baseUrl = baseUrl, model = model))
    }
}
