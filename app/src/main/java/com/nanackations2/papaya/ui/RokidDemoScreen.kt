package com.nanackations2.papaya.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.cxr.Caps
import com.nanackations2.papaya.rokid.RokidConstants
import com.nanackations2.papaya.rokid.RokidConnectionCallback
import com.nanackations2.papaya.rokid.RokidCredentials
import com.nanackations2.papaya.rokid.RokidDevice
import com.nanackations2.papaya.rokid.RokidDeviceInfo
import com.nanackations2.papaya.rokid.RokidManager
import com.nanackations2.papaya.rokid.RokidScanCallback
import com.nanackations2.papaya.ui.theme.AlertRed
import com.nanackations2.papaya.ui.theme.CreamBackground
import com.nanackations2.papaya.ui.theme.CreamCard
import com.nanackations2.papaya.ui.theme.DividerLight
import com.nanackations2.papaya.ui.theme.MintGreen
import com.nanackations2.papaya.ui.theme.MintGreenLight
import com.nanackations2.papaya.ui.theme.ShadowColor
import com.nanackations2.papaya.ui.theme.TextHint
import com.nanackations2.papaya.ui.theme.TextPrimary
import com.nanackations2.papaya.ui.theme.TextSecondary
import com.nanackations2.papaya.ui.theme.WarmOrange
import com.nanackations2.papaya.ui.theme.WarmOrangeLight
import com.nanackations2.papaya.ui.theme.PetPurple
import com.nanackations2.papaya.ui.theme.PetPurpleLight

@Composable
fun RokidDemoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rokidManager = remember { RokidManager.getInstance() }

    var connectionState by remember { mutableStateOf(RokidConstants.ConnectionState.DISCONNECTED) }
    var discoveredDevices = remember { mutableStateListOf<RokidDevice>() }
    var selectedDevice by remember { mutableStateOf<RokidDevice?>(null) }
    var deviceInfo by remember { mutableStateOf<RokidDeviceInfo?>(null) }
    var logMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var aiChatInput by remember { mutableStateOf("") }
    var aiChatResponse by remember { mutableStateOf("") }
    var isAiChatLoading by remember { mutableStateOf(false) }

    val isConnected = connectionState == RokidConstants.ConnectionState.CONNECTED
    val isScanning = connectionState == RokidConstants.ConnectionState.SCANNING
    val isConnecting = connectionState == RokidConstants.ConnectionState.CONNECTING ||
        connectionState == RokidConstants.ConnectionState.PAIRING

    fun addLog(message: String) {
        logMessages = (logMessages + message).take(100)
    }

    LaunchedEffect(Unit) {
        rokidManager.initialize(context)
        addLog("RokidManager initialized")
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!isConnected) {
                rokidManager.release()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CreamCard,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rokid AR 眼镜",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = when {
                            isConnected -> "已连接"
                            isScanning -> "扫描中..."
                            isConnecting -> "连接中..."
                            else -> "未连接"
                        },
                        fontSize = 12.sp,
                        color = if (isConnected) MintGreen else TextHint,
                    )
                }
                ConnectionStatusIndicator(isConnected = isConnected, isConnecting = isConnecting || isScanning)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Device Discovery Section
            DeviceDiscoverySection(
                isScanning = isScanning,
                isConnected = isConnected,
                discoveredDevices = discoveredDevices,
                selectedDevice = selectedDevice,
                onStartScan = {
                    connectionState = RokidConstants.ConnectionState.SCANNING
                    discoveredDevices.clear()
                    addLog("Starting BLE scan...")
                    rokidManager.startScan(object : RokidScanCallback {
                        override fun onDeviceFound(device: RokidDevice) {
                            if (discoveredDevices.none { it.macAddress == device.macAddress }) {
                                discoveredDevices.add(device)
                                addLog("Found device: ${device.name} (${device.macAddress})")
                            }
                        }

                        override fun onScanStarted() {
                            addLog("Scan started")
                        }

                        override fun onScanStopped() {
                            connectionState = RokidConstants.ConnectionState.DISCONNECTED
                            addLog("Scan stopped, found ${discoveredDevices.size} device(s)")
                        }

                        override fun onScanFailed(errorCode: Int) {
                            connectionState = RokidConstants.ConnectionState.FAILED
                            addLog("Scan failed: error code $errorCode")
                        }
                    })
                },
                onStopScan = {
                    rokidManager.stopScan()
                    connectionState = RokidConstants.ConnectionState.DISCONNECTED
                    addLog("Scan stopped by user")
                },
                onDeviceSelected = { device ->
                    selectedDevice = device
                    addLog("Selected device: ${device.name}")
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Connect/Disconnect Controls
            ConnectionControls(
                isConnected = isConnected,
                isConnecting = isConnecting,
                isScanning = isScanning,
                hasSelectedDevice = selectedDevice != null,
                onConnect = {
                    val device = selectedDevice ?: return@ConnectionControls
                    connectionState = RokidConstants.ConnectionState.PAIRING
                    addLog("Pairing with ${device.name}...")
                    rokidManager.pairDevice(context, device, object : RokidConnectionCallback {
                        override fun onConnectionInfo(
                            socketUuid: String?,
                            macAddress: String?,
                            rokidAccount: String?,
                            glassesType: Int,
                        ) {
                            val credentials = RokidCredentials.load(context)
                            addLog("Got connection info - socketUuid: $socketUuid")
                            addLog(if (credentials.isUsable) "Using Rokid developer credentials" else "Missing Client Secret / Access Key; trying without credentials")
                            connectionState = RokidConstants.ConnectionState.CONNECTING
                            rokidManager.connect(
                                context = context,
                                socketUuid = socketUuid ?: "",
                                macAddress = macAddress ?: device.macAddress,
                                callback = object : RokidConnectionCallback {
                                    override fun onConnectionInfo(
                                        socketUuid: String?,
                                        macAddress: String?,
                                        rokidAccount: String?,
                                        glassesType: Int,
                                    ) {}
                                    override fun onConnected() {
                                        connectionState = RokidConstants.ConnectionState.CONNECTED
                                        rokidManager.getGlassInfo()
                                        deviceInfo = rokidManager.connectedDeviceInfo
                                        addLog("Connected to ${device.name}!")
                                    }
                                    override fun onConnecting() {
                                        connectionState = RokidConstants.ConnectionState.CONNECTING
                                    }
                                    override fun onDisconnected() {
                                        connectionState = RokidConstants.ConnectionState.DISCONNECTED
                                        deviceInfo = null
                                        addLog("Disconnected")
                                    }
                                    override fun onError(code: Int, message: String) {
                                        connectionState = RokidConstants.ConnectionState.FAILED
                                        addLog("Connection error [$code]: ${RokidConstants.bluetoothErrorMessage(code)} ($message)")
                                    }
                                },
                                snEncrypt = credentials.snEncrypt.takeIf { credentials.isUsable },
                                clientSecret = credentials.clientSecret.takeIf { credentials.isUsable },
                            )
                        }
                        override fun onConnected() {}
                        override fun onConnecting() {}
                        override fun onDisconnected() {}
                        override fun onError(code: Int, message: String) {
                            connectionState = RokidConstants.ConnectionState.FAILED
                            addLog("Pairing error [$code]: ${RokidConstants.bluetoothErrorMessage(code)} ($message)")
                        }
                    })
                },
                onDisconnect = {
                    rokidManager.disconnect()
                    connectionState = RokidConstants.ConnectionState.DISCONNECTED
                    deviceInfo = null
                    addLog("Disconnected")
                },
                onRefreshInfo = {
                    rokidManager.getGlassInfo()
                    deviceInfo = rokidManager.connectedDeviceInfo
                    addLog("Refreshed device info")
                },
            )

            AnimatedVisibility(visible = isConnected && deviceInfo != null) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    DeviceInfoCard(deviceInfo = deviceInfo!!)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feature Extension Section
                    FeatureExtensionSection()

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Chat Section (as a demo feature)
                    AiChatSection(
                        input = aiChatInput,
                        response = aiChatResponse,
                        isLoading = isAiChatLoading,
                        onInputChange = { aiChatInput = it },
                        onSend = {
                            if (aiChatInput.isNotBlank()) {
                                isAiChatLoading = true
                                addLog("Sending custom cmd to glasses: ${aiChatInput.take(50)}...")
                                val caps = Caps().apply { write(aiChatInput) }
                                val sent = rokidManager.sendCustomCmd(
                                    RokidConstants.Channel.AI_RESULT,
                                    caps,
                                )
                                if (!sent) {
                                    aiChatResponse = "发送失败：请确保眼镜已连接"
                                    isAiChatLoading = false
                                }
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SDK Info & Setup Guide
            SdkSetupGuide()

            Spacer(modifier = Modifier.height(16.dp))

            // Log Section
            LogSection(logMessages = logMessages)
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(
    isConnected: Boolean,
    isConnecting: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "status_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                when {
                    isConnected -> MintGreenLight
                    isConnecting -> WarmOrangeLight
                    else -> Color(0xFFF5F5F5)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when {
                isConnected -> Icons.Default.BluetoothConnected
                isConnecting -> Icons.Default.Bluetooth
                else -> Icons.Default.BluetoothDisabled
            },
            contentDescription = null,
            tint = when {
                isConnected -> MintGreen
                isConnecting -> WarmOrange
                else -> TextHint
            },
            modifier = Modifier
                .size(20.dp)
                .then(
                    if (isConnecting) Modifier.alpha(pulseAlpha)
                    else Modifier
                ),
        )
    }
}

@Composable
private fun DeviceDiscoverySection(
    isScanning: Boolean,
    isConnected: Boolean,
    discoveredDevices: List<RokidDevice>,
    selectedDevice: RokidDevice?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (RokidDevice) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = WarmOrange,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设备发现",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!isConnected) {
                    Button(
                        onClick = if (isScanning) onStopScan else onStartScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) AlertRed else WarmOrange,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Cancel else Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isScanning) "停止" else "扫描",
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = WarmOrange,
                    trackColor = WarmOrangeLight,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (discoveredDevices.isEmpty() && !isScanning) {
                Text(
                    text = "点击「扫描」查找附近的Rokid眼镜设备",
                    fontSize = 13.sp,
                    color = TextHint,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            discoveredDevices.forEach { device ->
                val isSelected = device.macAddress == selectedDevice?.macAddress
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) WarmOrangeLight else Color.Transparent)
                        .clickable(enabled = !isConnected) { onDeviceSelected(device) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = if (isSelected) WarmOrange else TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) WarmOrange else TextPrimary,
                        )
                        Text(
                            text = device.macAddress,
                            fontSize = 11.sp,
                            color = TextHint,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${device.rssi} dBm",
                            fontSize = 11.sp,
                            color = if (device.rssi > -60) MintGreen else TextHint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionControls(
    isConnected: Boolean,
    isConnecting: Boolean,
    isScanning: Boolean,
    hasSelectedDevice: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshInfo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isConnected) {
            Button(
                onClick = onConnect,
                enabled = hasSelectedDevice && !isScanning && !isConnecting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen,
                    disabledContainerColor = DividerLight,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.BluetoothConnected, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnecting) "连接中..." else "连接设备",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
            ) {
                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("断开连接", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onRefreshInfo,
                modifier = Modifier.weight(0.6f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("刷新", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(deviceInfo: RokidDeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MintGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设备信息",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val glassTypeName = when (deviceInfo.glassesType) {
                RokidConstants.GlassType.ROKID_GLASSES -> "Rokid Glasses"
                RokidConstants.GlassType.ROKID_AR_PRO -> "Rokid AR Pro"
                RokidConstants.GlassType.ROKID_MAX -> "Rokid Max"
                else -> "Unknown"
            }

            InfoRow("眼镜型号", glassTypeName)
            InfoRow("硬件版本", deviceInfo.hardwareVersion)
            InfoRow("软件版本", deviceInfo.softwareVersion)
            InfoRow("序列号", deviceInfo.serialNumber)
            InfoRow("电量", "${deviceInfo.batteryLevel}%${if (deviceInfo.isCharging) " (充电中)" else ""}")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun FeatureExtensionSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cast, contentDescription = null, tint = PetPurple, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AR 应用扩展 (开发中)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "以下功能通过 CXR-M SDK 提供，连接设备后可扩展开发：",
                fontSize = 12.sp,
                color = TextHint,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureGrid(
                features = listOf(
                    FeatureItem("相机预览", Icons.Default.CameraAlt, "获取眼镜摄像头实时画面", true),
                    FeatureItem("语音录制", Icons.Default.Mic, "通过眼镜麦克风录音", true),
                    FeatureItem("音频播放", Icons.Default.Headphones, "推送音频到眼镜播放", true),
                    FeatureItem("视频通话", Icons.Default.Videocam, "WebRTC 实时视频通话", true),
                    FeatureItem("AI 对话", Icons.Default.Chat, "基于眼镜 AI 能力对话", true),
                    FeatureItem("翻译模式", Icons.Default.Language, "实时语音翻译", false),
                    FeatureItem("飞传文件", Icons.Default.SwapHoriz, "手机与眼镜间文件传输", false),
                    FeatureItem("屏幕镜像", Icons.Default.FlipToBack, "手机屏幕投射到眼镜", false),
                ),
            )
        }
    }
}

private data class FeatureItem(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val available: Boolean,
)

@Composable
private fun FeatureGrid(
    features: List<FeatureItem>,
    columns: Int = 4,
) {
    val rows = features.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (feature in row) {
                    FeatureItemCard(
                        feature = feature,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size < columns) {
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItemCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (feature.available) PetPurpleLight else Color(0xFFF5F5F5)
    val iconColor = if (feature.available) PetPurple else TextHint
    val textColor = if (feature.available) TextPrimary else TextHint

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feature.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AiChatSection(
    input: String,
    response: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = WarmOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 助手测试",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("输入测试消息...", fontSize = 13.sp, color = TextHint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = TextPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8F8F8),
                    unfocusedContainerColor = Color(0xFFF8F8F8),
                    focusedIndicatorColor = WarmOrange,
                    unfocusedIndicatorColor = DividerLight,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("处理中...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("发送", fontSize = 13.sp)
                }
            }

            if (response.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmOrangeLight,
                ) {
                    Text(
                        text = response,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SdkSetupGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SDK 配置说明",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "当前已集成 CXR-M SDK (com.rokid.cxr:client-m:1.0.8)。连接真实 Rokid 眼镜前，需确认以下配置：",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val steps = listOf(
                "1. 访问 ar.rokid.com/sdk 注册开发者账号",
                "2. 获取 Client ID 和 Client Secret",
                "3. 在 local.properties 中配置凭据:",
                "4. 确保蓝牙权限已授予 (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)",
                "5. 确保眼镜已开机并进入配对模式",
            )
            steps.forEach { step ->
                Text(
                    text = step,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5),
            ) {
                Text(
                    text = """rokid.clientId=your_client_id
|rokid.clientSecret=your_client_secret
|rokid.accessKey=your_access_key""".trimMargin(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun LogSection(logMessages: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "运行日志",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logMessages.isEmpty()) {
                Text(
                    text = "暂无日志",
                    fontSize = 12.sp,
                    color = TextHint,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1A2E),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        logMessages.takeLast(20).forEach { message ->
                            Text(
                                text = message,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFA0E8A0),
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
