package com.nanackations2.papaya.service

import android.content.Context
import com.nanackations2.papaya.rokid.CxrSBridge
import com.nanackations2.papaya.rokid.RokidApi
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * X4 视觉讲解编排：
 * X4 MJPEG bridge 最新帧 -> 手机调用开发者选项里的多模态 LLM -> 结果通过 AI_RESULT 通道回传眼镜端 App。
 */
class X4VisionNarrator(
    context: Context,
    private val rokidApi: RokidApi,
    private val scope: CoroutineScope,
    private val frameUrlProvider: () -> String,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val onStatus: (String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    fun start(): Boolean {
        if (isRunning) return true
        if (!rokidApi.isBluetoothConnected) {
            onStatus("X4 讲解启动失败：Rokid 眼镜未连接")
            return false
        }

        CxrSBridge.sendAiResultToGlasses(rokidApi, "X4 视觉讲解已开启，正在观察。")
        job = scope.launch {
            val describer = ImageDescriber.create(appContext)
            while (isActive) {
                narrateOnce(describer)
                delay(intervalMs)
            }
        }
        onStatus("X4 讲解已开启：每 ${intervalMs / 1000} 秒抽取一帧")
        return true
    }

    fun stop(notifyGlasses: Boolean = true, logStatus: Boolean = true) {
        job?.cancel()
        job = null
        if (notifyGlasses && rokidApi.isBluetoothConnected) {
            CxrSBridge.sendAiResultToGlasses(rokidApi, "X4 视觉讲解已停止。")
        }
        if (logStatus) {
            onStatus("X4 讲解已停止")
        }
    }

    private suspend fun narrateOnce(describer: ImageDescriber) {
        try {
            val frameUrl = latestFrameUrl(frameUrlProvider())
            onStatus("正在从 X4 桥接流抽帧...")
            val jpegBytes = withContext(Dispatchers.IO) { fetchLatestFrame(frameUrl) }
            if (jpegBytes.isEmpty()) {
                onStatus("X4 抽帧失败：没有拿到图片")
                return
            }

            onStatus("已拿到 X4 图片，正在请求 LLM...")
            val result = withContext(Dispatchers.IO) {
                describer.describeBytes(jpegBytes, VISION_PROMPT).trim()
            }.ifBlank {
                "没有识别到明确内容。"
            }.take(MAX_RESULT_CHARS)

            val pushed = CxrSBridge.sendAiResultToGlasses(rokidApi, result)
            onStatus(if (pushed) "X4 讲解已回传到眼镜端" else "X4 讲解回传失败：眼镜通道不可用")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "请检查 X4 桥接和 LLM 配置"
            onStatus("X4 讲解失败：$message")
            if (rokidApi.isBluetoothConnected) {
                CxrSBridge.sendAiResultToGlasses(rokidApi, "X4 视觉讲解失败，请检查 X4 桥接地址和手机端 LLM 配置。")
            }
        }
    }

    private fun fetchLatestFrame(frameUrl: String): ByteArray {
        val request = Request.Builder()
            .url(frameUrl)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-store")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            return response.body?.bytes() ?: ByteArray(0)
        }
    }

    companion object {
        private const val DEFAULT_INTERVAL_MS = 15_000L
        private const val MAX_RESULT_CHARS = 600

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        fun latestFrameUrl(streamUrl: String): String {
            val parsed = streamUrl.trim().toHttpUrlOrNull()
                ?: throw IllegalArgumentException("无效的 X4 桥接地址")
            val lastSegment = parsed.pathSegments.lastOrNull().orEmpty()
            return if (lastSegment.equals("latest.jpg", ignoreCase = true)) {
                parsed.toString()
            } else {
                parsed.newBuilder()
                    .removePathSegment(parsed.pathSize - 1)
                    .addPathSegment("latest.jpg")
                    .query(null)
                    .fragment(null)
                    .build()
                    .toString()
            }
        }

        private const val VISION_PROMPT =
            "你是佩戴者的视觉助手。请用中文用 1-2 句描述这张 Insta360 X4 画面里正在发生什么，优先说明人物、动作、物体和可能需要注意的风险；不要编造看不到的信息。"
    }
}
