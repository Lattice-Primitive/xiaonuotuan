package com.nanackations2.papaya.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.cxr.Caps
import com.nanackations2.papaya.rokid.CxrSBridge
import com.nanackations2.papaya.rokid.RokidAiEventListener
import com.nanackations2.papaya.rokid.RokidConstants
import com.nanackations2.papaya.rokid.RokidCustomCmdListener
import com.nanackations2.papaya.rokid.RokidGlassesEventListener
import com.nanackations2.papaya.rokid.RokidManager
import com.nanackations2.papaya.service.GlassPhotoLlmHandler
import com.nanackations2.papaya.ui.theme.Error
import com.nanackations2.papaya.ui.theme.Primary
import com.nanackations2.papaya.ui.theme.PrimaryDark
import com.nanackations2.papaya.ui.theme.Secondary
import com.nanackations2.papaya.ui.theme.SecondaryLight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 双向通信调试/控制界面。
 *
 * 功能：
 * 1. 实时显示从眼镜端收到的所有消息（事件日志）
 * 2. 发送自定义消息到眼镜
 * 3. 控制面板：亮度/音量滑块、快捷命令
 * 4. 心跳检测
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RokidBidirectionalScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val rokidManager = remember { RokidManager.getInstance() }
    val connectionState by rokidManager.connectionStateFlow.collectAsState()
    val runtimeStatus by rokidManager.runtimeStatusFlow.collectAsState()
    val isConnected = connectionState == RokidConstants.ConnectionState.CONNECTED

    // 消息日志
    data class BidiMessage(
        val direction: String, // "TX" or "RX"
        val channel: String,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    val messages = remember { mutableStateListOf<BidiMessage>() }
    var chatInput by remember { mutableStateOf("") }
    var heartbeatActive by remember { mutableStateOf(false) }
    var lastPongTime by remember { mutableStateOf<Long?>(null) }

    // 控制面板状态
    var brightnessValue by remember { mutableFloatStateOf(50f) }
    var volumeValue by remember { mutableFloatStateOf(50f) }

    // 同步运行时状态到滑块
    LaunchedEffect(runtimeStatus) {
        if (runtimeStatus.brightness >= 0) brightnessValue = runtimeStatus.brightness.toFloat()
        if (runtimeStatus.volume >= 0) volumeValue = runtimeStatus.volume.toFloat()
    }

    fun addMessage(direction: String, channel: String, content: String) {
        messages.add(0, BidiMessage(direction, channel, content))
        if (messages.size > 200) messages.removeRange(200, messages.size)
    }

    // 注册双向通信监听器
    DisposableEffect(isConnected) {
        if (isConnected) {
            // 自定义命令监听
            rokidManager.setCustomCmdListener(object : RokidCustomCmdListener {
                override fun onCustomCmd(channel: String, args: Caps?) {
                    val subCmd = CxrSBridge.capString(args, 0) ?: "?"
                    val data = CxrSBridge.capString(args, 1) ?: ""
                    addMessage("RX", channel, "[$subCmd] $data")

                    // 处理 Pong
                    if (channel == RokidConstants.Channel.PAPAYA_PONG) {
                        lastPongTime = System.currentTimeMillis()
                    }
                }
            })

            // AI 事件监听
            rokidManager.setAiEventListener(object : RokidAiEventListener {
                override fun onAiKeyDown() {
                    addMessage("RX", "ai_event", "AI 键按下")
                }
                override fun onAiKeyUp() {
                    addMessage("RX", "ai_event", "AI 键松开")
                }
                override fun onAiExit() {
                    addMessage("RX", "ai_event", "AI 退出")
                }
            })

            // 眼镜事件监听
            rokidManager.setGlassesEventListener(object : RokidGlassesEventListener {
                override fun onGlassesEvent(event: String, data: String) {
                    addMessage("RX", "glasses_event", "[$event] $data")
                }
            })
        }

        onDispose {
            rokidManager.setCustomCmdListener(null)
            rokidManager.setAiEventListener(null)
            rokidManager.setGlassesEventListener(null)
        }
    }

    // 发送消息辅助函数
    fun sendPing() {
        val caps = Caps().apply {
            write(RokidConstants.BidiCmd.REQUEST_STATUS)
            write(System.currentTimeMillis().toString())
        }
        val ok = rokidManager.sendCustomCmd(RokidConstants.Channel.PAPAYA_PING, caps)
        addMessage("TX", RokidConstants.Channel.PAPAYA_PING, if (ok) "Ping 已发送" else "发送失败")
    }

    fun sendChatMessage(text: String) {
        val caps = Caps().apply {
            write(RokidConstants.BidiCmd.CHAT_TEXT)
            write(text)
        }
        val ok = rokidManager.sendCustomCmd(RokidConstants.Channel.PAPAYA_CHAT, caps)
        addMessage("TX", RokidConstants.Channel.PAPAYA_CHAT, text)
    }

    fun sendCommand(cmd: String, payload: String = ""): Boolean {
        val caps = Caps().apply {
            write(cmd)
            write(payload)
        }
        val ok = rokidManager.sendCustomCmd(RokidConstants.Channel.PAPAYA_CMD, caps)
        addMessage("TX", RokidConstants.Channel.PAPAYA_CMD, "[$cmd] $payload (${if (ok) "✓" else "✗"})")
        return ok
    }

    fun sendNotification(title: String, body: String) {
        val caps = Caps().apply {
            write("NOTIFICATION")
            write("{\"title\":\"$title\",\"body\":\"$body\"}")
        }
        val ok = rokidManager.sendCustomCmd(RokidConstants.Channel.PAPAYA_NOTIFY, caps)
        addMessage("TX", RokidConstants.Channel.PAPAYA_NOTIFY, "$title: $body (${if (ok) "✓" else "✗"})")
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text("双向通信", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isConnected) "已连接 • ${messages.size} 条消息" else "未连接",
                        fontSize = 12.sp,
                        color = if (isConnected) Secondary else Error,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { messages.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { sendPing() }) {
                    Icon(Icons.Default.Sync, contentDescription = "Ping", tint = Primary)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // === 连接状态指示 ===
            item {
                if (!isConnected) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "眼镜未连接，请先在 Rokid 标签页连接设备",
                                color = Error,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // === 控制面板 ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("远程控制", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 亮度滑块
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LightMode, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("亮度", fontSize = 13.sp, modifier = Modifier.width(36.dp))
                            Slider(
                                value = brightnessValue,
                                onValueChange = { brightnessValue = it },
                                onValueChangeFinished = {
                                    val value = brightnessValue.toInt()
                                    val sdkOk = rokidManager.setGlassBrightness(value)
                                    val capsOk = sendCommand(RokidConstants.BidiCmd.SET_BRIGHTNESS, value.toString())
                                    addMessage("TX", "brightness", "SDK=${if (sdkOk) "✓" else "✗"} CMD=${if (capsOk) "✓" else "✗"}")
                                },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${brightnessValue.toInt()}%", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }

                        // 音量滑块
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("音量", fontSize = 13.sp, modifier = Modifier.width(36.dp))
                            Slider(
                                value = volumeValue,
                                onValueChange = { volumeValue = it },
                                onValueChangeFinished = {
                                    val value = volumeValue.toInt()
                                    val sdkOk = rokidManager.setGlassVolume(value)
                                    val capsOk = sendCommand(RokidConstants.BidiCmd.SET_VOLUME, value.toString())
                                    addMessage("TX", "volume", "SDK=${if (sdkOk) "✓" else "✗"} CMD=${if (capsOk) "✓" else "✗"}")
                                },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${volumeValue.toInt()}%", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 快捷命令按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CmdButton(
                                icon = Icons.Default.Chat,
                                label = "文本",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) {
                                val ok = CxrSBridge.sendTextToGlasses(rokidManager, "Papaya 通信测试")
                                addMessage("TX", "glass_text", if (ok) "Papaya 通信测试" else "文本发送失败")
                            }

                            CmdButton(
                                icon = Icons.Default.Notifications,
                                label = "通知",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) { sendNotification("Papaya", "来自手机的测试通知") }

                            CmdButton(
                                icon = Icons.Default.TextFields,
                                label = "Toast",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) {
                                CxrSBridge.glassToast(rokidManager, "Hello from Papaya!")
                                addMessage("TX", "global_toast", "Hello from Papaya!")
                            }

                            CmdButton(
                                icon = Icons.Default.Refresh,
                                label = "同步时间",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) {
                                val ok = rokidManager.syncGlassTime()
                                addMessage("TX", "system", if (ok) "时间同步" else "时间同步失败")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CmdButton(
                                icon = Icons.Default.Mic,
                                label = "TTS",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) {
                                CxrSBridge.glassSpeak(rokidManager, "你好，我是木瓜助手")
                                addMessage("TX", "global_tts", "你好，我是木瓜助手")
                            }

                            CmdButton(
                                icon = Icons.Default.BatteryFull,
                                label = "状态请求",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) { sendPing() }

                            CmdButton(
                                icon = Icons.Default.Notifications,
                                label = "系统消息",
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                            ) {
                                CxrSBridge.glassMessage(rokidManager, "Papaya 状态同步完成")
                                addMessage("TX", "global_message", "Papaya 状态同步完成")
                            }

                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // === 眼镜拍照 AI 识别 ===
            item {
                val glassPhotoHandler = remember {
                    GlassPhotoLlmHandler(
                        context = context,
                        rokidApi = rokidManager,
                        onStatus = { status -> addMessage("AI", "glass_photo", status) },
                    )
                }
                val isPhotoListening by glassPhotoHandler.isListening.collectAsState()
                val isPhotoProcessing by glassPhotoHandler.processing.collectAsState()
                val lastAiResult by glassPhotoHandler.lastResult.collectAsState()

                DisposableEffect(Unit) {
                    onDispose { glassPhotoHandler.stop() }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📷 眼镜拍照 AI 识别", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Switch(
                                checked = isPhotoListening,
                                onCheckedChange = { checked ->
                                    if (checked) glassPhotoHandler.start() else glassPhotoHandler.stop()
                                },
                                enabled = isConnected,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                !isConnected -> "需要先连接眼镜"
                                isPhotoProcessing -> "⏳ 正在处理图片..."
                                isPhotoListening -> "✅ 监听中，等待眼镜端拍照"
                                else -> "开启后，眼镜拍照将自动发送到手机进行 AI 分析"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (lastAiResult.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Secondary.copy(alpha = 0.08f),
                            ) {
                                Text(
                                    text = lastAiResult,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // === 聊天输入 ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("发送消息到眼镜", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("输入消息...", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        sendChatMessage(chatInput)
                                        // 同时推送到眼镜屏幕
                                        CxrSBridge.sendTextToGlasses(rokidManager, chatInput)
                                        chatInput = ""
                                    }
                                },
                                enabled = isConnected && chatInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // === 心跳状态 ===
            item {
                val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (lastPongTime != null && System.currentTimeMillis() - lastPongTime!! < 10000) Secondary else Error),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lastPongTime != null) "最后响应: ${sdf.format(Date(lastPongTime!!))}" else "尚未收到眼镜端响应",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "延迟: ${if (lastPongTime != null) "${System.currentTimeMillis() - lastPongTime!!}ms" else "N/A"}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // === 消息日志 ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("通信日志", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${messages.size} 条", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(messages.take(50)) { msg ->
                val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
                val isTx = msg.direction == "TX"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isTx) Primary.copy(alpha = 0.05f) else Secondary.copy(alpha = 0.05f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // 方向标识
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isTx) Primary.copy(alpha = 0.15f) else Secondary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = msg.direction,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTx) Primary else Secondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg.content,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp,
                        )
                        Text(
                            text = "${msg.channel} • ${sdf.format(Date(msg.timestamp))}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CmdButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Primary,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            fontWeight = FontWeight.Medium,
        )
    }
}
