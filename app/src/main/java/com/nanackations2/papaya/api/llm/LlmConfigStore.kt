package com.nanackations2.papaya.api.llm

import android.content.Context

/**
 * 从开发者选项 / SharedPreferences 中读取 LLM 配置。
 *
 * 使用的 key（与 DeveloperOptionsScreen 中的字段对应）：
 * - llm_provider:   LlmProvider enum 名
 * - llm_api_key:    API Key（Ollama 可为空）
 * - llm_base_url:   可覆盖默认 baseUrl
 * - llm_model:      可覆盖默认模型
 * - llm_max_tokens: Int，可选
 */
object LlmConfigStore {
    private const val PREF = "dev_options"

    fun read(context: Context): LlmConfig {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val provider = LlmProvider.fromName(sp.getString("llm_provider", LlmProvider.OPENAI.name))
        val apiKey = sp.getString("llm_api_key", "").orEmpty()
        val baseUrl = sp.getString("llm_base_url", "")?.ifBlank { null } ?: provider.defaultBaseUrl
        val model = sp.getString("llm_model", "")?.ifBlank { null } ?: provider.defaultModel
        val maxTokens = sp.getInt("llm_max_tokens", 800).coerceIn(64, 8192)
        val temperature = sp.getFloat("llm_temperature", 0.7f).toDouble()
        return LlmConfig(
            provider = provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            maxTokens = maxTokens,
            temperature = temperature,
        )
    }

    fun save(
        context: Context,
        provider: LlmProvider? = null,
        apiKey: String? = null,
        baseUrl: String? = null,
        model: String? = null,
        maxTokens: Int? = null,
    ) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().apply {
            provider?.let { putString("llm_provider", it.name) }
            apiKey?.let { putString("llm_api_key", it) }
            baseUrl?.let { putString("llm_base_url", it) }
            model?.let { putString("llm_model", it) }
            maxTokens?.let { putInt("llm_max_tokens", it) }
            apply()
        }
    }
}
