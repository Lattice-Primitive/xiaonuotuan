package com.nanackations2.papaya.service

import android.content.Context
import android.util.Log
import com.nanackations2.papaya.rokid.CxrSBridge
import com.nanackations2.papaya.rokid.RokidApi
import com.nanackations2.papaya.rokid.RokidConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 监听眼镜端主动发来的拍照图片，调用 LLM 处理后将结果回传到眼镜端显示。
 *
 * 流程：
 * 1. 眼镜端拍照 → 将图片 Base64 通过 PAPAYA_CHAT / CHAT_IMAGE 通道发送到手机
 * 2. 手机端收到后，调用开发者选项里配置的多模态 LLM 进行分析
 * 3. LLM 返回的文本结果通过 AI_RESULT 通道推送回眼镜端
 *
 * 使用方法：
 * ```kotlin
 * val handler = GlassPhotoLlmHandler(context, rokidApi)
 * handler.start()  // 开始监听
 * handler.stop()   // 停止监听
 * ```
 */
class GlassPhotoLlmHandler(
    context: Context,
    private val rokidApi: RokidApi,
    private val prompt: String = DEFAULT_PROMPT,
    private val onStatus: (String) -> Unit = {},
) {
    companion object {
        private const val TAG = "GlassPhotoLlmHandler"
        private const val MAX_RESULT_CHARS = 600

        private const val DEFAULT_PROMPT =
            "你是佩戴者的视觉助手。请用中文用 1-2 句描述这张眼镜摄像头画面里正在发生什么，" +
                "优先说明人物、动作、物体和可能需要注意的风险；不要编造看不到的信息。"
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastResult = MutableStateFlow("")
    val lastResult: StateFlow<String> = _lastResult.asStateFlow()

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    /**
     * 开始监听眼镜端发来的图片。
     * 注册 CustomCmdListener，当收到 PAPAYA_CHAT + CHAT_IMAGE 时自动处理。
     */
    fun start() {
        if (_isListening.value) return

        CxrSBridge.registerGlassesMessageListener(rokidApi) { channel, subCmd, data ->
            Log.d(TAG, "收到眼镜消息: channel=$channel subCmd=$subCmd dataLen=${data?.length ?: 0}")
            when {
                channel == RokidConstants.Channel.PAPAYA_CHAT && subCmd == RokidConstants.BidiCmd.CHAT_IMAGE -> {
                    if (!data.isNullOrBlank()) {
                        handleGlassPhoto(data)
                    } else {
                        Log.w(TAG, "收到 CHAT_IMAGE 但 data 为空")
                    }
                }
            }
        }

        _isListening.value = true
        onStatus("已开始监听眼镜拍照")
        Log.d(TAG, "GlassPhotoLlmHandler 已启动")

        // 通知眼镜端已就绪
        if (rokidApi.isBluetoothConnected) {
            CxrSBridge.sendAiResultToGlasses(rokidApi, "手机端 AI 视觉助手已就绪，请在眼镜上拍照。")
        }
    }

    /**
     * 停止监听。
     */
    fun stop() {
        if (!_isListening.value) return
        rokidApi.setCustomCmdListener(null)
        _isListening.value = false
        onStatus("已停止监听眼镜拍照")
        Log.d(TAG, "GlassPhotoLlmHandler 已停止")
    }

    /**
     * 处理眼镜端发来的图片 Base64。
     */
    private fun handleGlassPhoto(imageBase64: String) {
        if (_processing.value) {
            Log.w(TAG, "上一张图片仍在处理中，跳过")
            onStatus("正在处理中，请稍后再拍")
            return
        }

        _processing.value = true
        onStatus("收到眼镜拍照，正在调用 LLM...")

        // 通知眼镜正在处理中
        if (rokidApi.isBluetoothConnected) {
            CxrSBridge.sendAiResultToGlasses(rokidApi, "正在分析图片...")
        }

        scope.launch {
            try {
                val describer = ImageDescriber.create(appContext)
                val result = describer.describeBytes(
                    bytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT),
                    prompt = prompt,
                ).trim().ifBlank {
                    "没有识别到明确内容。"
                }.take(MAX_RESULT_CHARS)

                _lastResult.value = result
                onStatus("LLM 分析完成，正在回传到眼镜...")

                // 将结果回传到眼镜端
                withContext(Dispatchers.Main) {
                    val pushed = CxrSBridge.sendAiResultToGlasses(rokidApi, result)
                    if (pushed) {
                        onStatus("AI 结果已回传到眼镜")
                        Log.d(TAG, "AI 结果已回传: ${result.take(50)}...")
                    } else {
                        onStatus("AI 结果回传失败：眼镜通道不可用")
                        Log.w(TAG, "sendAiResultToGlasses 返回 false")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = "分析失败：${e.message ?: "请检查 LLM 配置"}"
                _lastResult.value = errMsg
                onStatus(errMsg)
                Log.e(TAG, "LLM 处理失败", e)

                if (rokidApi.isBluetoothConnected) {
                    withContext(Dispatchers.Main) {
                        CxrSBridge.sendAiResultToGlasses(rokidApi, "视觉分析失败，请检查手机端 LLM 配置。")
                    }
                }
            } finally {
                _processing.value = false
            }
        }
    }
}
