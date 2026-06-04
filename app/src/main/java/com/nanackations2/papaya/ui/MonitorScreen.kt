package com.nanackations2.papaya.ui

import android.content.Context
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nanackations2.papaya.rokid.RokidManager
import com.nanackations2.papaya.service.X4VisionNarrator
import com.nanackations2.papaya.ui.component.MjpegStreamView
import com.nanackations2.papaya.ui.component.WebRtcVideoView
import com.nanackations2.papaya.ui.theme.CreamCard
import com.nanackations2.papaya.ui.theme.LiveRed
import com.nanackations2.papaya.ui.theme.MintGreen
import com.nanackations2.papaya.ui.theme.MintGreenLight
import com.nanackations2.papaya.ui.theme.PetPurple
import com.nanackations2.papaya.ui.theme.PetPurpleLight
import com.nanackations2.papaya.ui.theme.SafetyGreen
import com.nanackations2.papaya.ui.theme.ShadowColor
import com.nanackations2.papaya.ui.theme.TextHint
import com.nanackations2.papaya.ui.theme.TextPrimary
import com.nanackations2.papaya.ui.theme.WarmOrange
import com.nanackations2.papaya.ui.theme.WarmOrangeLight
import com.nanackations2.papaya.ui.theme.WarmOrangeDark
import com.nanackations2.papaya.webrtc.CameraManager
import com.nanackations2.papaya.webrtc.StreamConfig
import com.nanackations2.papaya.webrtc.StreamProtocol
import com.nanackations2.papaya.webrtc.WebRTCManager
import org.webrtc.VideoTrack

data class MonitorTab(
    val label: String,
    val icon: String,
    val scene: String,
    val roomId: String,
    val borderColor: Color,
    val lightColor: Color
)

enum class StreamMode(val label: String) {
    MJPEG("图传"),
    VIEW("观看"),
    PUSH("推流"),
    BIDI("双向"),
    X4("X4")
}

private const val DEFAULT_X4_MJPEG_URL = "http://192.168.42.3:8787/x4.mjpeg"

@Composable
fun SmileyIP(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "smiley")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smiley_scale"
    )

    Box(modifier = modifier.scale(scale)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val faceSize = size.minDimension
            val center = Offset(faceSize / 2, faceSize / 2)
            val radius = faceSize / 2 * 0.85f

            drawCircle(
                color = WarmOrange.copy(alpha = 0.15f),
                radius = radius * 1.15f,
                center = center,
            )

            drawCircle(
                color = WarmOrange,
                radius = radius,
                center = center,
            )

            drawCircle(
                color = WarmOrangeLight.copy(alpha = 0.4f),
                radius = radius * 0.88f,
                center = Offset(center.x - radius * 0.05f, center.y - radius * 0.05f),
            )

            val eyeOffsetX = radius * 0.32f
            val eyeOffsetY = radius * 0.18f
            val eyeRadius = radius * 0.1f

            drawCircle(
                color = Color.White,
                radius = eyeRadius * 1.5f,
                center = Offset(center.x - eyeOffsetX, center.y - eyeOffsetY),
            )
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 1.5f,
                center = Offset(center.x + eyeOffsetX, center.y - eyeOffsetY),
            )

            drawCircle(
                color = TextPrimary,
                radius = eyeRadius,
                center = Offset(center.x - eyeOffsetX, center.y - eyeOffsetY),
            )
            drawCircle(
                color = TextPrimary,
                radius = eyeRadius,
                center = Offset(center.x + eyeOffsetX, center.y - eyeOffsetY),
            )

            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.4f,
                center = Offset(
                    center.x - eyeOffsetX + eyeRadius * 0.3f,
                    center.y - eyeOffsetY - eyeRadius * 0.3f
                ),
            )
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.4f,
                center = Offset(
                    center.x + eyeOffsetX + eyeRadius * 0.3f,
                    center.y - eyeOffsetY - eyeRadius * 0.3f
                ),
            )

            val cheekOffsetX = radius * 0.52f
            val cheekOffsetY = radius * 0.15f
            drawCircle(
                color = WarmOrangeDark.copy(alpha = 0.25f),
                radius = radius * 0.15f,
                center = Offset(center.x - cheekOffsetX, center.y + cheekOffsetY),
            )
            drawCircle(
                color = WarmOrangeDark.copy(alpha = 0.25f),
                radius = radius * 0.15f,
                center = Offset(center.x + cheekOffsetX, center.y + cheekOffsetY),
            )

            val smileWidth = radius * 0.45f
            val smileHeight = radius * 0.22f
            val smileTopLeft = Offset(center.x - smileWidth, center.y + radius * 0.05f)
            drawArc(
                color = Color.White,
                topLeft = smileTopLeft,
                size = Size(smileWidth * 2, smileHeight * 2),
                startAngle = 5f,
                sweepAngle = 170f,
                useCenter = false,
                style = Stroke(width = radius * 0.08f, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun LiveIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LiveRed.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "实时画面",
            color = LiveRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SafetyBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = SafetyGreen.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SafetyGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SafetyGreen)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "安全",
                color = SafetyGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun MonitorViewTab(
    tab: MonitorTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) tab.borderColor else TextHint,
        animationSpec = tween(300),
        label = "tab_text"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) tab.borderColor else Color.Transparent,
        animationSpec = tween(300),
        label = "tab_indicator"
    )

    val avatarBgColor = if (isSelected) {
        Brush.linearGradient(colors = listOf(tab.borderColor, tab.borderColor.copy(alpha = 0.7f)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF0ECE7), Color(0xFFE8E4DF)))
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tab.icon,
                fontSize = 22.sp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tab.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(indicatorColor),
        )
    }
}

@Composable
private fun ConnectionBadge(state: WebRTCManager.State) {
    val (text, color) = when (state) {
        WebRTCManager.State.CONNECTED -> "已连接" to MintGreen
        WebRTCManager.State.CONNECTING -> "连接中" to WarmOrange
        WebRTCManager.State.FAILED -> "未连接" to Color(0xFF999999)
        WebRTCManager.State.DISCONNECTED -> "已断开" to Color(0xFF999999)
        WebRTCManager.State.IDLE -> "待机" to Color(0xFF999999)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StreamModeSelector(
    currentMode: StreamMode,
    onModeChange: (StreamMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0ECE7))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StreamMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            val bg = if (isSelected) Color.White else Color.Transparent
            val textColor = if (isSelected) TextPrimary else TextHint

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = mode.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** 监控标签页列表，提升为顶层常量避免每次重组都重新分配 */
private val MONITOR_TABS = listOf(
    MonitorTab("老人", "👴", "客厅 - 爷爷", "room_elderly", WarmOrange, WarmOrangeLight),
    MonitorTab("儿童", "👧", "儿童房 - 小美", "room_child", MintGreen, MintGreenLight),
    MonitorTab("宠物", "🐱", "阳台 - 咪咪", "room_pet", PetPurple, PetPurpleLight),
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MonitorFrame(modifier: Modifier = Modifier) {
    val tabs = MONITOR_TABS
    var selectedTab by remember { mutableIntStateOf(0) }
    var streamMode by remember { mutableStateOf(StreamMode.MJPEG) }
    val currentTab = tabs[selectedTab]

    val context = LocalContext.current

    // MJPEG 配置：从开发者选项读取，默认使用和 Web 端一致的地址
    val mjpegUrl = remember {
        context.getSharedPreferences("dev_options", Context.MODE_PRIVATE)
            .getString("mjpeg_url", "http://localhost:8787/x4.mjpeg") ?: "http://localhost:8787/x4.mjpeg"
    }
    var mjpegStreamReady by remember { mutableStateOf(false) }
    var mjpegError by remember { mutableStateOf<String?>(null) }

    val webrtc = remember { WebRTCManager(context) }
    val cameraManager = remember { CameraManager(context) }
    var remoteTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var connState by remember { mutableStateOf(WebRTCManager.State.IDLE) }
    var isMicOn by remember { mutableStateOf(true) }

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
        if (granted) {
            startLocalCapture(webrtc, cameraManager) { localTrack = it }
        }
    }

    // MJPEG 模式下重置状态
    LaunchedEffect(selectedTab, streamMode) {
        if (streamMode == StreamMode.MJPEG) {
            // MJPEG 模式：断开 WebRTC，MJPEG 流由 MjpegStreamView 自动管理
            webrtc.disconnect()
            cameraManager.stopCapture()
            remoteTrack = null
            localTrack = null
            connState = WebRTCManager.State.IDLE
            mjpegStreamReady = false
            mjpegError = null
            return@LaunchedEffect
        }

        // WebRTC 模式
        webrtc.disconnect()
        cameraManager.stopCapture()
        remoteTrack = null
        localTrack = null
        connState = WebRTCManager.State.IDLE
        mjpegStreamReady = false

        if (streamMode == StreamMode.X4) {
            return@LaunchedEffect
        }

        webrtc.initialize()
        webrtc.onRemoteVideoTrack = { remoteTrack = it }
        webrtc.onStateChange = { connState = it }

        val webrtcMode = when (streamMode) {
            StreamMode.VIEW -> WebRTCManager.Mode.VIEWER
            StreamMode.PUSH -> WebRTCManager.Mode.PUBLISHER
            StreamMode.BIDI -> WebRTCManager.Mode.BIDIRECTIONAL
            StreamMode.MJPEG -> return@LaunchedEffect // 不会到这里
            StreamMode.X4 -> WebRTCManager.Mode.VIEWER
        }

        val streamProtocol = StreamConfig.readProtocol(context)
        val protocolError = when {
            streamMode == StreamMode.VIEW && streamProtocol == StreamProtocol.WHIP ->
                "观看模式不能使用 WHIP，请在开发者选项切到 WebSocket 或 WHEP"
            streamMode == StreamMode.PUSH && streamProtocol == StreamProtocol.WHEP ->
                "推流模式不能使用 WHEP，请在开发者选项切到 WebSocket 或 WHIP"
            streamMode == StreamMode.BIDI && streamProtocol != StreamProtocol.WEBSOCKET ->
                "双向模式当前只支持 WebSocket 信令"
            streamProtocol == StreamProtocol.WHIP && StreamConfig.readWhipEndpoint(context).isBlank() ->
                "WHIP Endpoint 未配置"
            streamProtocol == StreamProtocol.WHEP && StreamConfig.readWhepEndpoint(context).isBlank() ->
                "WHEP Endpoint 未配置"
            else -> null
        }
        if (protocolError != null) {
            connState = WebRTCManager.State.FAILED
            return@LaunchedEffect
        }

        val signaling = StreamConfig.createAdapter(context, streamProtocol, currentTab.roomId)
        webrtc.connect(signaling, currentTab.roomId, webrtcMode)

        if (streamMode != StreamMode.VIEW) {
            if (hasCameraPermission) {
                startLocalCapture(webrtc, cameraManager) { localTrack = it }
            }
        }
    }

    LaunchedEffect(localTrack, connState) {
        if (localTrack != null && connState == WebRTCManager.State.CONNECTING && streamMode != StreamMode.VIEW) {
            val audioTrack = cameraManager.localAudioTrack
            webrtc.publish(localTrack!!, audioTrack)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.stopCapture()
            webrtc.dispose()
        }
    }

    val borderColor by animateColorAsState(
        targetValue = currentTab.borderColor.copy(alpha = 0.6f),
        animationSpec = tween(400),
        label = "frame_border"
    )

    val isMjpeg = streamMode == StreamMode.MJPEG
    val isLive = remoteTrack != null && connState == WebRTCManager.State.CONNECTED
    val isX4Preview = streamMode == StreamMode.X4
    val isPushing = localTrack != null && (streamMode == StreamMode.PUSH || streamMode == StreamMode.BIDI)
    var x4Url by remember { mutableStateOf(DEFAULT_X4_MJPEG_URL) }
    var x4StreamReady by remember { mutableStateOf(false) }
    var x4Error by remember { mutableStateOf<String?>(null) }
    val videoBg = if (isLive || isPushing || (isMjpeg && mjpegStreamReady) || (isX4Preview && x4StreamReady)) {
        Color(0xFF0A0A14)
    } else {
        Color(0xFF1A1A2E)
    }
    val currentX4Url = rememberUpdatedState(x4Url)
    val scope = rememberCoroutineScope()
    val rokidManager = remember { RokidManager.getInstance() }
    var x4NarrationActive by remember { mutableStateOf(false) }
    var x4NarrationStatus by remember { mutableStateOf("X4 讲解未开启") }
    val x4Narrator = remember {
        X4VisionNarrator(
            context = context,
            rokidApi = rokidManager,
            scope = scope,
            frameUrlProvider = { currentX4Url.value },
            onStatus = { x4NarrationStatus = it },
        )
    }
    val latestFrameUrl = remember(x4Url) {
        runCatching { X4VisionNarrator.latestFrameUrl(x4Url) }
            .getOrElse { "X4 桥接地址无效" }
    }

    LaunchedEffect(streamMode) {
        if (streamMode != StreamMode.X4 && x4NarrationActive) {
            x4Narrator.stop()
            x4NarrationActive = false
        }
    }

    LaunchedEffect(x4Url, streamMode) {
        if (streamMode == StreamMode.X4) {
            x4StreamReady = false
            x4Error = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            x4Narrator.stop(notifyGlasses = false, logStatus = false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor)
            .clip(RoundedCornerShape(24.dp))
            .background(CreamCard)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveIndicator()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConnectionBadge(connState)
                SafetyBadge()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        StreamModeSelector(currentMode = streamMode, onModeChange = { streamMode = it })

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                Box(modifier = Modifier.weight(1f)) {
                    MonitorViewTab(
                        tab = tab,
                        isSelected = index == selectedTab,
                        onClick = { selectedTab = index },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(videoBg),
            contentAlignment = Alignment.Center,
        ) {
            if (isMjpeg) {
                // MJPEG 图传模式 —— 对齐 Web 端的 <img src="x4.mjpeg">
                MjpegStreamView(
                    url = mjpegUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    onStreamReady = { mjpegStreamReady = true },
                    onError = { mjpegError = it },
                )
                if (!mjpegStreamReady) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📡", fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = mjpegError ?: "正在连接图传...",
                            fontSize = 14.sp,
                            color = if (mjpegError != null) LiveRed else TextHint,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else if (isX4Preview) {
                MjpegStreamView(
                    url = x4Url,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    onStreamReady = { x4StreamReady = true },
                    onError = { x4Error = it },
                )
                if (!x4StreamReady) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📡", fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = x4Error ?: "正在连接 X4...",
                            fontSize = 14.sp,
                            color = if (x4Error != null) LiveRed else TextHint,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else if (isLive) {
                WebRtcVideoView(
                    videoTrack = remoteTrack,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (isPushing && streamMode == StreamMode.PUSH) {
                WebRtcVideoView(
                    videoTrack = localTrack,
                    modifier = Modifier.fillMaxSize(),
                    mirror = cameraManager.isFrontFacing(),
                )
            } else if (isPushing && streamMode == StreamMode.BIDI) {
                WebRtcVideoView(
                    videoTrack = localTrack,
                    modifier = Modifier.fillMaxSize(),
                    mirror = cameraManager.isFrontFacing(),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val c = Color(0xFF2A2A4A)
                    for (x in 0..(w.toInt() / 40)) drawLine(c, Offset(x * 40f, 0f), Offset(x * 40f, h), 0.5f)
                    for (y in 0..(h.toInt() / 40)) drawLine(c, Offset(0f, y * 40f), Offset(w, y * 40f), 0.5f)
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "monitor_content"
                ) { tabIndex ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                connState == WebRTCManager.State.CONNECTING -> "⏳"
                                tabIndex == 0 -> "👤"
                                tabIndex == 1 -> "👧"
                                else -> "🐱"
                            },
                            fontSize = 52.sp,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when (connState) {
                                WebRTCManager.State.CONNECTING -> "连接中..."
                                WebRTCManager.State.FAILED -> "连接失败"
                                else -> "监控画面"
                            },
                            fontSize = 14.sp,
                            color = TextHint,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.5f),
            ) {
                Text(
                    text = if (isX4Preview) "Insta360 X4 · 无线桥接" else currentTab.scene,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            if (isLive || isPushing || (isMjpeg && mjpegStreamReady) || (isX4Preview && x4StreamReady)) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = LiveRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(LiveRed))
                        Spacer(Modifier.width(4.dp))
                        Text("LIVE", color = LiveRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isPushing && !hasCameraPermission) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = WarmOrange.copy(alpha = 0.9f),
                ) {
                    Text(
                        text = "📷 授予摄像头权限",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (isX4Preview) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = x4Url,
                onValueChange = { x4Url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("X4 MJPEG 桥接地址") },
                supportingText = { Text("Linux 端先运行：cd web && bun run x4:bridge") },
            )
            Spacer(modifier = Modifier.height(10.dp))
            X4NarrationPanel(
                isActive = x4NarrationActive,
                status = x4NarrationStatus,
                latestFrameUrl = latestFrameUrl,
                onToggle = {
                    if (x4NarrationActive) {
                        x4Narrator.stop()
                        x4NarrationActive = false
                    } else {
                        x4NarrationActive = x4Narrator.start()
                    }
                },
            )
        }

        if (isPushing && hasCameraPermission) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CameraControlButton(
                    icon = "🔄",
                    label = "翻转",
                    onClick = {
                        val ctx = webrtc.eglContext ?: return@CameraControlButton
                        cameraManager.switchCamera(webrtc.factory!!, ctx, isMicOn)
                    }
                )
                Spacer(modifier = Modifier.width(24.dp))
                CameraControlButton(
                    icon = if (isMicOn) "🎤" else "🔇",
                    label = if (isMicOn) "麦克风开" else "麦克风关",
                    onClick = {
                        isMicOn = !isMicOn
                        cameraManager.setAudioEnabled(isMicOn)
                    }
                )
            }
        }
    }
}

@Composable
private fun X4NarrationPanel(
    isActive: Boolean,
    status: String,
    latestFrameUrl: String,
    onToggle: () -> Unit,
) {
    val accent = if (isActive) SafetyGreen else WarmOrange
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBF4),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggle),
                shape = RoundedCornerShape(12.dp),
                color = accent,
            ) {
                Text(
                    text = if (isActive) "停止讲解" else "X4 讲解",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "抽帧地址：$latestFrameUrl",
                    color = TextHint,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun X4PreviewWebView(
    streamUrl: String,
    modifier: Modifier = Modifier,
) {
    var hasError by remember(streamUrl) { mutableStateOf(false) }
    val html = remember(streamUrl) {
        """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
              html, body {
                margin: 0;
                width: 100%;
                height: 100%;
                background: #070b12;
                overflow: hidden;
              }
              .stage {
                position: fixed;
                inset: 0;
                display: grid;
                place-items: center;
                background:
                  linear-gradient(135deg, rgba(12,18,30,.92), rgba(4,7,12,.98)),
                  repeating-linear-gradient(0deg, rgba(255,255,255,.05) 0 1px, transparent 1px 42px),
                  repeating-linear-gradient(90deg, rgba(255,255,255,.05) 0 1px, transparent 1px 42px);
              }
              img {
                width: 100%;
                height: 100%;
                object-fit: cover;
                display: block;
              }
              .badge {
                position: fixed;
                left: 14px;
                bottom: 14px;
                padding: 7px 10px;
                border: 1px solid rgba(255,255,255,.18);
                border-radius: 12px;
                color: #fff;
                background: rgba(0,0,0,.48);
                font: 600 12px system-ui, sans-serif;
              }
            </style>
          </head>
          <body>
            <div class="stage">
              <img src="$streamUrl" alt="Insta360 X4 live preview" />
              <div class="badge">X4 LIVE · MJPEG</div>
            </div>
          </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.apply {
                        javaScriptEnabled = false
                        domStorageEnabled = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                            }
                        }
                    }
                    loadDataWithBaseURL(streamUrl, html, "text/html", "utf-8", null)
                }
            },
            update = { webView ->
                hasError = false
                webView.loadDataWithBaseURL(streamUrl, html, "text/html", "utf-8", null)
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (hasError) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "X4 画面未连接",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "确认手机和 Linux 在 X4 Wi-Fi 下，并运行 bun run x4:bridge",
                    color = TextHint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun CameraControlButton(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextHint,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun startLocalCapture(
    webrtc: WebRTCManager,
    cameraManager: CameraManager,
    onTrackReady: (VideoTrack) -> Unit
) {
    val factory = webrtc.factory ?: return
    val eglContext = webrtc.eglContext ?: return

    cameraManager.onVideoTrackReady = onTrackReady
    cameraManager.startCapture(factory, eglContext)
}

@Composable
fun MonitorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "实时监控",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "随时查看家人状态",
            fontSize = 14.sp,
            color = TextHint,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        MonitorFrame()

        Spacer(modifier = Modifier.height(24.dp))
    }
}
