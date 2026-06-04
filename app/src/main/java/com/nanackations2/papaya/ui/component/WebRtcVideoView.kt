package com.nanackations2.papaya.ui.component

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * 渲染 WebRTC [VideoTrack] 的 Composable。
 *
 * 关键点：
 * - 如果外部传入了 [sharedEglContext]（推荐，和 WebRTCManager 共用），就不再自建 EglBase，
 *   避免出现多个 GL 上下文带来的黑屏/花屏。
 * - 使用可变引用保存"当前已绑定到 renderer 的 track"，在更新或 Dispose 时先 removeSink 再绑新的，
 *   杜绝多个 sink 叠加 / 旧 sink 在 dispose 后继续回调的崩溃。
 */
@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT,
    sharedEglContext: EglBase.Context? = null,
) {
    // 若未提供共享 EGL，退化为自己创建（只在没有 WebRTCManager 的预览场景）
    val fallbackEgl = remember(sharedEglContext) {
        if (sharedEglContext == null) EglBase.create() else null
    }
    val eglContext: EglBase.Context =
        sharedEglContext ?: fallbackEgl!!.eglBaseContext

    // renderer 与当前已绑定的 track 引用
    val rendererHolder = remember { arrayOfNulls<SurfaceViewRenderer>(1) }
    val boundTrack = remember { arrayOfNulls<VideoTrack>(1) }

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(eglContext, null)
                setEnableHardwareScaler(true)
                setScalingType(scalingType)
                setMirror(mirror)
                rendererHolder[0] = this
            }
        },
        modifier = modifier,
        update = { renderer ->
            renderer.setMirror(mirror)
            renderer.setScalingType(scalingType)

            val current = boundTrack[0]
            if (current !== videoTrack) {
                // 先把上一条 track 的 sink 摘掉
                current?.let { runCatching { it.removeSink(renderer) } }
                renderer.clearImage()
                videoTrack?.let {
                    runCatching { it.addSink(renderer) }
                }
                boundTrack[0] = videoTrack
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            val renderer = rendererHolder[0]
            val track = boundTrack[0]
            if (renderer != null) {
                track?.let { runCatching { it.removeSink(renderer) } }
                runCatching { renderer.release() }
            }
            rendererHolder[0] = null
            boundTrack[0] = null
            fallbackEgl?.let { runCatching { it.release() } }
        }
    }
}
