package com.nanackations2.papaya.service

import android.content.Context
import com.nanackations2.papaya.rokid.CxrSBridge
import com.nanackations2.papaya.rokid.RokidApi
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 手机端视觉讲解编排：
 * 眼镜拍照 -> 手机调用开发者选项里的多模态 LLM -> 结果通过 AI_RESULT 通道回传眼镜端 App。
 */
class RokidVisionNarrator(
    context: Context,
    private val rokidApi: RokidApi,
    private val scope: CoroutineScope,
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
            onStatus("视觉讲解启动失败：眼镜未连接")
            return false
        }

        CxrSBridge.sendAiResultToGlasses(rokidApi, "视觉讲解已开启，正在观察。")
        job = scope.launch {
            val describer = ImageDescriber.create(appContext)
            while (isActive) {
                narrateOnce(describer)
                delay(intervalMs)
            }
        }
        onStatus("视觉讲解已开启：每 ${intervalMs / 1000} 秒抽一帧分析")
        return true
    }

    fun stop(notifyGlasses: Boolean = true, logStatus: Boolean = true) {
        job?.cancel()
        job = null
        if (notifyGlasses && rokidApi.isBluetoothConnected) {
            CxrSBridge.sendAiResultToGlasses(rokidApi, "视觉讲解已停止。")
        }
        if (logStatus) {
            onStatus("视觉讲解已停止")
        }
    }

    private suspend fun narrateOnce(describer: ImageDescriber) {
        try {
            onStatus("正在从眼镜摄像头抽帧...")
            val photoBytes = withTimeoutOrNull(PHOTO_TIMEOUT_MS) { capturePhoto() }
            if (photoBytes == null || photoBytes.isEmpty()) {
                onStatus("抽帧失败：没有拿到眼镜图片")
                return
            }

            onStatus("已拿到眼镜图片，正在请求 LLM...")
            val result = withContext(Dispatchers.IO) {
                describer.describeBytes(photoBytes, VISION_PROMPT).trim()
            }.ifBlank {
                "没有识别到明确内容。"
            }.take(MAX_RESULT_CHARS)

            val pushed = CxrSBridge.sendAiResultToGlasses(rokidApi, result)
            onStatus(if (pushed) "视觉讲解已回传到眼镜端" else "视觉讲解回传失败：眼镜通道不可用")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onStatus("视觉讲解失败：${e.message ?: "请检查 LLM 配置"}")
            if (rokidApi.isBluetoothConnected) {
                CxrSBridge.sendAiResultToGlasses(rokidApi, "视觉讲解失败，请检查手机端开发者页面里的 LLM 配置。")
            }
        }
    }

    private suspend fun capturePhoto(): ByteArray? = suspendCancellableCoroutine { cont ->
        val resumed = AtomicBoolean(false)

        fun resumeOnce(value: ByteArray?) {
            if (resumed.compareAndSet(false, true) && cont.isActive) {
                cont.resume(value)
            }
        }

        val sent = CxrSBridge.takePhoto(
            api = rokidApi,
            width = PHOTO_WIDTH,
            height = PHOTO_HEIGHT,
            quality = PHOTO_QUALITY,
        ) { ok, data ->
            resumeOnce(if (ok && data.isNotEmpty()) data else null)
        }

        if (!sent) {
            resumeOnce(null)
        }

        cont.invokeOnCancellation {
            resumed.set(true)
        }
    }

    companion object {
        private const val DEFAULT_INTERVAL_MS = 15_000L
        private const val PHOTO_TIMEOUT_MS = 12_000L
        private const val PHOTO_WIDTH = 1280
        private const val PHOTO_HEIGHT = 720
        private const val PHOTO_QUALITY = 75
        private const val MAX_RESULT_CHARS = 600

        private const val VISION_PROMPT =
            "你是佩戴者的视觉助手。请用中文用 1-2 句描述这张眼镜摄像头画面里正在发生什么，优先说明人物、动作、物体和可能需要注意的风险；不要编造看不到的信息。"
    }
}
