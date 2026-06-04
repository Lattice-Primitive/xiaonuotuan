package com.nanackations2.papaya.ui.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * MJPEG 流显示组件 —— 对齐 Web 端的 `<img src="http://host/x4.mjpeg">` 方案。
 *
 * 原理：
 * - 连接 MJPEG 服务器（multipart/x-mixed-replace 格式）
 * - 持续解析 JPEG 帧边界（FFD8 开始，FFD9 结束）
 * - 每收到完整帧就刷新画面
 *
 * 用法：
 * ```
 * MjpegStreamView(
 *     url = "http://192.168.1.100:8787/x4.mjpeg",
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 */
@Composable
fun MjpegStreamView(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onStreamReady: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var jobRef by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(url) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            connectMjpeg(url, onFrame = { bitmap ->
                frame = bitmap
                onStreamReady?.invoke()
            }, onError = { msg ->
                onError?.invoke(msg)
            })
        }
        jobRef = job

        onDispose {
            job.cancel()
            jobRef = null
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        frame?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = "实时画面",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

private suspend fun connectMjpeg(
    url: String,
    onFrame: (ImageBitmap) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "multipart/x-mixed-replace")
        }
        connection.connect()

        val code = connection.responseCode
        if (code != 200) {
            onError("HTTP $code")
            return
        }

        val inputStream = BufferedInputStream(connection.inputStream, 65536)
        val buffer = ByteArrayOutputStream(65536)
        var inJpeg = false
        var prev = -1

        val scope = CoroutineScope(Dispatchers.IO)

        while (scope.isActive) {
            val b = inputStream.read()
            if (b == -1) break

            if (!inJpeg) {
                // 寻找 JPEG SOI: FF D8
                if (prev == 0xFF && b == 0xD8) {
                    inJpeg = true
                    buffer.reset()
                    buffer.write(0xFF)
                    buffer.write(0xD8)
                }
                prev = b
                continue
            }

            buffer.write(b)

            // 检测 JPEG EOI: FF D9
            if (prev == 0xFF && b == 0xD9) {
                val jpegBytes = buffer.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                if (bitmap != null) {
                    onFrame(bitmap.asImageBitmap())
                }
                inJpeg = false
                buffer.reset()
            }

            prev = b
        }

        inputStream.close()
        connection.disconnect()
    } catch (e: Exception) {
        Log.e("MjpegStreamView", "MJPEG stream error", e)
        onError(e.message ?: "连接失败")
    }
}
