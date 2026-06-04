package com.nanackations2.papaya.webrtc

import android.content.Context

/**
 * 读取开发者选项里的信令/推流配置。
 *
 * SharedPreferences key:
 * - signal_url:     WebSocket 信令服务地址（协议 [StreamProtocol.WEBSOCKET]）
 * - stream_protocol: WEBSOCKET / WHIP / WHEP，默认 WEBSOCKET
 * - whip_endpoint:  WHIP POST URL（推流）
 * - whep_endpoint:  WHEP POST URL（观看）
 * - stream_token:   可选 Bearer Token
 */
object StreamConfig {

    private const val PREF = "dev_options"

    fun readProtocol(context: Context): StreamProtocol {
        val name = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("stream_protocol", StreamProtocol.WEBSOCKET.name)
            ?: StreamProtocol.WEBSOCKET.name
        return runCatching { StreamProtocol.valueOf(name) }.getOrDefault(StreamProtocol.WEBSOCKET)
    }

    fun readSignalUrl(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("signal_url", "").orEmpty()
    }

    fun readWhipEndpoint(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("whip_endpoint", "").orEmpty()
    }

    fun readWhepEndpoint(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("whep_endpoint", "").orEmpty()
    }

    fun readToken(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("stream_token", "").orEmpty()
    }

    /**
     * 根据协议 + 房间 id 构造匹配的 [SignalingAdapter]。
     *
     * - WEBSOCKET：使用 [SignalingClient]
     * - WHIP：把 `{room}` 占位符替换为 roomId；若 endpoint 不含占位符，追加 `?stream=roomId`
     * - WHEP：同 WHIP
     */
    fun createAdapter(
        context: Context,
        protocol: StreamProtocol,
        roomId: String,
    ): SignalingAdapter {
        val token = readToken(context)
        return when (protocol) {
            StreamProtocol.WEBSOCKET -> {
                val url = readSignalUrl(context)
                SignalingClient(url)
            }
            StreamProtocol.WHIP -> {
                val endpoint = expandEndpoint(readWhipEndpoint(context), roomId)
                WhipSignalingClient(endpoint, token.ifBlank { null })
            }
            StreamProtocol.WHEP -> {
                val endpoint = expandEndpoint(readWhepEndpoint(context), roomId)
                WhepSignalingClient(endpoint, token.ifBlank { null })
            }
        }
    }

    private fun expandEndpoint(raw: String, roomId: String): String {
        if (raw.isBlank()) return raw
        if (raw.contains("{room}")) return raw.replace("{room}", roomId)
        val sep = if (raw.contains("?")) "&" else "?"
        return "$raw${sep}stream=$roomId"
    }
}
