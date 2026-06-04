package com.nanackations2.papaya.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.nanackations2.papaya.rokid.RokidConstants
import com.nanackations2.papaya.rokid.RokidConnectionCallback
import com.nanackations2.papaya.rokid.RokidCredentials
import com.nanackations2.papaya.rokid.RokidDevice
import com.nanackations2.papaya.rokid.RokidDeviceInfo
import com.nanackations2.papaya.rokid.RokidManager
import com.nanackations2.papaya.rokid.RokidRuntimeStatus
import com.nanackations2.papaya.rokid.RokidSceneStatus
import com.nanackations2.papaya.rokid.RokidScanCallback
import com.nanackations2.papaya.service.RokidVisionNarrator
import com.nanackations2.papaya.ui.theme.Error
import com.nanackations2.papaya.ui.theme.PetColor
import com.nanackations2.papaya.ui.theme.PetColorLight
import com.nanackations2.papaya.ui.theme.Primary
import com.nanackations2.papaya.ui.theme.PrimaryDark
import com.nanackations2.papaya.ui.theme.PrimaryLight
import com.nanackations2.papaya.ui.theme.Secondary
import com.nanackations2.papaya.ui.theme.SecondaryLight
import com.nanackations2.papaya.ui.theme.ShadowColor
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RokidTabScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val rokidManager = remember { RokidManager.getInstance() }

    var connectionState by remember { mutableStateOf(RokidConstants.ConnectionState.DISCONNECTED) }
    val discoveredDevices = remember { mutableStateListOf<RokidDevice>() }
    var selectedDevice by remember { mutableStateOf<RokidDevice?>(null) }
    var deviceInfo by remember { mutableStateOf<RokidDeviceInfo?>(null) }
    var logMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLogs by remember { mutableStateOf(false) }
    var scanErrorCode by remember { mutableStateOf<Int?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isCameraOpen by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isAiChatActive by remember { mutableStateOf(false) }
    var isTranslationActive by remember { mutableStateOf(false) }
    var isTeleprompterActive by remember { mutableStateOf(false) }
    var isVisionNarrationActive by remember { mutableStateOf(false) }
    var showRokidDoc by remember { mutableStateOf(false) }
    var pendingScanAfterBluetoothEnable by remember { mutableStateOf(false) }

    // 读取开发者选项中的"假装已连接"开关
    val devPrefs = remember { context.getSharedPreferences("dev_options", android.content.Context.MODE_PRIVATE) }
    var fakeConnected by remember { mutableStateOf(devPrefs.getBoolean("fake_connected", false)) }
    var papayaGlassesRemoteEnabled by remember { mutableStateOf(false) }

    // 假装已连接时生成随机电量和假设备信息
    val fakeBatteryLevel = remember { (30..95).random() }
    if (fakeConnected && deviceInfo == null) {
        deviceInfo = remember {
            RokidDeviceInfo(
                deviceName = "Rokid AR Lite (模拟)",
                serialNumber = "FAKE${(100000..999999).random()}",
                softwareVersion = "3.2.1-fake",
                hardwareVersion = "1.0",
                batteryLevel = fakeBatteryLevel,
                isCharging = false,
                glassesType = RokidConstants.GlassType.ROKID_AR_LITE,
            )
        }
    }

    val isConnected = fakeConnected || connectionState == RokidConstants.ConnectionState.CONNECTED
    val isScanning = connectionState == RokidConstants.ConnectionState.SCANNING
    val isConnecting = connectionState == RokidConstants.ConnectionState.CONNECTING ||
        connectionState == RokidConstants.ConnectionState.PAIRING

    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    var permissionsGranted by remember { mutableStateOf(checkBluetoothPermissions()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsGranted = checkBluetoothPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun addLog(message: String, expand: Boolean = false) {
        val timestamp = DateFormat.format("HH:mm:ss", System.currentTimeMillis()).toString()
        logMessages = (logMessages + "[$timestamp] $message").takeLast(200)
        if (expand) {
            showLogs = true
        }
    }

    fun printable(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: "<empty>"

    fun masked(value: String?): String {
        val raw = value?.takeIf { it.isNotBlank() } ?: return "<empty>"
        return if (raw.length <= 8) "***" else "${raw.take(4)}...${raw.takeLast(4)}"
    }

    fun logConnectionError(stage: String, code: Int, message: String) {
        addLog("$stage 错误 [$code] ${RokidConstants.bluetoothErrorMessage(code)}", expand = true)
        addLog("SDK 详情: $message", expand = true)
    }

    val coroutineScope = rememberCoroutineScope()
    val visionNarrator = remember(context, rokidManager, coroutineScope) {
        RokidVisionNarrator(
            context = context,
            rokidApi = rokidManager,
            scope = coroutineScope,
            onStatus = { addLog(it) },
        )
    }

    DisposableEffect(visionNarrator) {
        onDispose {
            visionNarrator.stop(notifyGlasses = false, logStatus = false)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (!allGranted) {
            addLog("需要蓝牙权限才能扫描设备")
        }
    }

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            addLog("蓝牙已开启，开始扫描...")
            pendingScanAfterBluetoothEnable = true
        } else {
            connectionState = RokidConstants.ConnectionState.FAILED
            addLog("需要开启蓝牙才能扫描设备")
        }
    }

    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    fun startDeviceScan() {
        if (!permissionsGranted) {
            requestBluetoothPermissions()
            return
        }

        if (!rokidManager.isBluetoothEnabled()) {
            val enableIntent = rokidManager.getEnableBluetoothIntent()
            if (enableIntent != null) {
                addLog("蓝牙未开启，请求开启蓝牙...")
                bluetoothEnableLauncher.launch(enableIntent)
                return
            }
        }

        scanErrorCode = null
        connectionState = RokidConstants.ConnectionState.SCANNING
        discoveredDevices.clear()
        addLog("正在扫描附近的 Rokid 设备...")
        rokidManager.startScan(object : RokidScanCallback {
            override fun onDeviceFound(device: RokidDevice) {
                if (discoveredDevices.none { it.macAddress == device.macAddress }) {
                    discoveredDevices.add(device)
                }
            }

            override fun onScanStarted() {}

            override fun onScanStopped() {
                connectionState = RokidConstants.ConnectionState.DISCONNECTED
                addLog("扫描结束，发现 ${discoveredDevices.size} 台设备")
            }

            override fun onScanFailed(errorCode: Int) {
                connectionState = RokidConstants.ConnectionState.FAILED
                scanErrorCode = errorCode
                when (errorCode) {
                    -100 -> addLog("扫描失败: 缺少蓝牙权限，请授权后重试")
                    -101 -> addLog("扫描失败: 权限被拒绝，请到设置中开启蓝牙权限")
                    -102 -> addLog("扫描失败: 蓝牙未开启，请开启蓝牙后重试")
                    else -> addLog("扫描失败: 错误码 $errorCode")
                }
            }
        })
    }

    LaunchedEffect(Unit) {
        rokidManager.initialize(context)
    }

    LaunchedEffect(pendingScanAfterBluetoothEnable) {
        if (pendingScanAfterBluetoothEnable) {
            pendingScanAfterBluetoothEnable = false
            startDeviceScan()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!isConnected) {
                rokidManager.release()
            }
        }
    }


    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    if (showRokidDoc) {
        RokidWebViewScreen(
            modifier = modifier.fillMaxSize(),
            onBack = { showRokidDoc = false },
        )
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            if (isConnected) {
                rokidManager.getGlassInfo()
                deviceInfo = rokidManager.connectedDeviceInfo
            }
            isRefreshing = false
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (onBack != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
            item {
                HeaderSection(
                    isConnected = isConnected,
                    isScanning = isScanning,
                    isConnecting = isConnecting,
                    deviceInfo = deviceInfo,
                    permissionsGranted = permissionsGranted,
                    onRequestPermission = { requestBluetoothPermissions() },
                )
            }

            // 启动 Papaya Glasses 远程应用开关（置顶，无需下滑）
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondaryLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phonelink,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Papaya Glasses 远程应用",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = if (papayaGlassesRemoteEnabled) "远程应用运行中" else "启动眼镜端远程应用服务",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (papayaGlassesRemoteEnabled) Secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = papayaGlassesRemoteEnabled,
                            onCheckedChange = { checked ->
                                papayaGlassesRemoteEnabled = checked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Secondary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }
                }
            }

            // 大电量卡片（Papaya Glasses 和快捷操作之间）
            item {
                BatteryStatusCard(
                    isConnected = isConnected,
                    deviceInfo = deviceInfo,
                    rokidManager = rokidManager,
                )
            }

            // 快捷操作
            item {
                AnimatedVisibility(visible = isConnected) {
                    QuickActionsCard(
                        isCameraOpen = isCameraOpen,
                        isRecording = isRecording,
                        isVisionNarrationActive = isVisionNarrationActive,
                        onTakePhoto = {
                            val success = rokidManager.takeGlassPhoto(width = 1920, height = 1080, quality = 80)
                            addLog(if (success) "拍照指令已发送" else "拍照失败：设备未连接或不支持")
                        },
                        onToggleCamera = {
                            if (isCameraOpen) {
                                val success = rokidManager.closeGlassCamera()
                                isCameraOpen = !success
                                addLog(if (success) "摄像头已关闭" else "关闭摄像头失败")
                            } else {
                                val success = rokidManager.openGlassCamera()
                                isCameraOpen = success
                                addLog(if (success) "摄像头已开启" else "开启摄像头失败")
                            }
                        },
                        onToggleVisionNarration = {
                            if (isVisionNarrationActive) {
                                visionNarrator.stop()
                                isVisionNarrationActive = false
                            } else {
                                isVisionNarrationActive = visionNarrator.start()
                            }
                        },
                        onToggleAudio = {
                            if (isRecording) {
                                val success = rokidManager.closeAudioRecord()
                                isRecording = !success
                                addLog(if (success) "录音已关闭" else "关闭录音失败")
                            } else {
                                val success = rokidManager.openAudioRecord()
                                isRecording = success
                                addLog(if (success) "录音已开启" else "开启录音失败")
                            }
                        },
                    )
                }
            }

            // AR 能力（电量卡片之前）
            item {
                AnimatedVisibility(visible = isConnected) {
                    FeatureShowcaseCard(
                        onToggleAiChat = {
                            val success = rokidManager.controlScene(RokidConstants.SceneType.AI_CHAT, !isAiChatActive)
                            isAiChatActive = if (success) !isAiChatActive else isAiChatActive
                            addLog(if (success) (if (isAiChatActive) "AI 对话已开启" else "AI 对话已关闭") else "AI 对话切换失败")
                        },
                        onToggleTranslation = {
                            val success = if (isTranslationActive) {
                                com.nanackations2.papaya.rokid.CxrSBridge.stopTranslation(rokidManager)
                            } else {
                                com.nanackations2.papaya.rokid.CxrSBridge.startTranslation(rokidManager, "zh", "en")
                            }
                            isTranslationActive = if (success) !isTranslationActive else isTranslationActive
                            addLog(if (success) (if (isTranslationActive) "翻译已开启 (中→英)" else "翻译已关闭") else "翻译切换失败")
                        },
                        onSendText = { text ->
                            val success = com.nanackations2.papaya.rokid.CxrSBridge.sendTextToGlasses(rokidManager, text)
                            addLog(if (success) "文字已推送到眼镜" else "推送失败：设备未连接")
                        },
                        onToggleTeleprompter = {
                            val success = if (isTeleprompterActive) {
                                com.nanackations2.papaya.rokid.CxrSBridge.stopTeleprompter(rokidManager)
                            } else {
                                com.nanackations2.papaya.rokid.CxrSBridge.startTeleprompter(
                                    api = rokidManager,
                                    script = "Papaya 提词器已开启。视觉讲解结果会实时显示在眼镜端。",
                                )
                            }
                            isTeleprompterActive = if (success) !isTeleprompterActive else isTeleprompterActive
                            addLog(if (success) (if (isTeleprompterActive) "提词器已开启" else "提词器已关闭") else "提词器切换失败")
                        },
                        isConnected = isConnected,
                        isAiChatActive = isAiChatActive,
                        isTranslationActive = isTranslationActive,
                        isTeleprompterActive = isTeleprompterActive,
                    )
                }
            }

            item {
                if (!isConnected && !permissionsGranted) {
                    PermissionRequestCard(onRequestPermission = { requestBluetoothPermissions() })
                } else if (!isConnected) {
                    DeviceDiscoveryCard(
                        isScanning = isScanning,
                        discoveredDevices = discoveredDevices,
                        selectedDevice = selectedDevice,
                        hasError = scanErrorCode != null,
                        errorCode = scanErrorCode,
                        onOpenSystemBluetooth = {
                            if (!permissionsGranted) {
                                addLog("请先授权蓝牙权限，再打开系统蓝牙连接")
                                requestBluetoothPermissions()
                            } else {
                                val device = selectedDevice
                                val opened = if (device != null) {
                                    rokidManager.requestSystemBluetoothPairing(context, device)
                                } else {
                                    rokidManager.openSystemBluetoothSettings(context)
                                }
                                addLog(
                                    if (opened) {
                                        if (device != null) {
                                            "已调用系统蓝牙配对/连接：${device.name}"
                                        } else {
                                            "已打开系统蓝牙连接界面"
                                        }
                                    } else {
                                        "系统蓝牙连接界面打开失败，请检查蓝牙权限"
                                    },
                                    expand = !opened,
                                )
                            }
                        },
                        onStartScan = { startDeviceScan() },
                        onStopScan = {
                            rokidManager.stopScan()
                            connectionState = RokidConstants.ConnectionState.DISCONNECTED
                        },
                        onDeviceSelected = { device ->
                            selectedDevice = device
                            addLog("已选择: ${device.name}")
                        },
                    )
                }
            }

            item {
                if (!isConnected && selectedDevice != null) {
                    ConnectButton(
                        isConnecting = isConnecting,
                        isScanning = isScanning,
                        onConnect = {
                            val device = selectedDevice ?: return@ConnectButton
                            showLogs = true
                            connectionState = RokidConstants.ConnectionState.PAIRING
                            addLog("正在配对 ${device.name}...")
                            addLog("设备信息: mac=${device.macAddress}, rssi=${device.rssi}")
                            addLog(
                                "系统蓝牙状态: ${
                                    if (rokidManager.isSystemBluetoothConnected(device.macAddress)) "已连接" else "未检测到连接，继续尝试 Papaya 通道"
                                }",
                            )
                            rokidManager.pairDevice(context, device, object : RokidConnectionCallback {
                                override fun onConnectionInfo(
                                    socketUuid: String?,
                                    macAddress: String?,
                                    rokidAccount: String?,
                                    glassesType: Int,
                                ) {
                                    val credentials = RokidCredentials.load(context)
                                    val snBytes = credentials.snEncrypt.size
                                    connectionState = RokidConstants.ConnectionState.CONNECTING
                                    addLog("已获取连接信息")
                                    addLog(
                                        "连接信息: socket=${printable(socketUuid)}, mac=${printable(macAddress ?: device.macAddress)}, type=${RokidConstants.glassTypeName(glassesType)}($glassesType), account=${masked(rokidAccount)}",
                                    )
                                    if (credentials.isUsable) {
                                        addLog(
                                            "使用开发者页面 Rokid 凭据连接: clientId=${masked(credentials.clientId)}, secretSet=${credentials.clientSecret.isNotBlank()}, accessKeyBytes=$snBytes",
                                        )
                                    } else {
                                        addLog(
                                            "未配置完整 Client Secret / Access Key，尝试无凭据连接: clientId=${masked(credentials.clientId)}, secretSet=${credentials.clientSecret.isNotBlank()}, accessKeyBytes=$snBytes",
                                            expand = true,
                                        )
                                    }
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
                                                addLog("已连接 ${device.name} ✓")
                                            }
                                            override fun onConnecting() {
                                                connectionState = RokidConstants.ConnectionState.CONNECTING
                                            }
                                            override fun onDisconnected() {
                                                connectionState = RokidConstants.ConnectionState.DISCONNECTED
                                                deviceInfo = null
                                                addLog("连接已断开（SDK onDisconnected）", expand = true)
                                            }
                                            override fun onError(code: Int, message: String) {
                                                connectionState = RokidConstants.ConnectionState.FAILED
                                                logConnectionError("连接", code, message)
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
                                    logConnectionError("配对", code, message)
                                }
                            })
                        },
                    )
                }
            }

            item {
                if (isConnected) {
                    OutlinedButton(
                        onClick = {
                            rokidManager.disconnect()
                            connectionState = RokidConstants.ConnectionState.DISCONNECTED
                            deviceInfo = null
                            selectedDevice = null
                            isCameraOpen = false
                            isRecording = false
                            isAiChatActive = false
                            isTranslationActive = false
                            isVisionNarrationActive = false
                            visionNarrator.stop(notifyGlasses = false, logStatus = false)
                            addLog("已断开连接")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("断开连接", fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                // Rokid 开发文档入口
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showRokidDoc = true },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rokid 开发文档",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "查看 Rokid 开放平台技术文档",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            item {
                LogToggleCard(
                    showLogs = showLogs,
                    logMessages = logMessages,
                    onToggle = { showLogs = !showLogs },
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HeaderSection(
    isConnected: Boolean,
    isScanning: Boolean,
    isConnecting: Boolean,
    deviceInfo: RokidDeviceInfo?,
    permissionsGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    val statusText = when {
        isConnected -> "已连接"
        isScanning -> "扫描中..."
        isConnecting -> "连接中..."
        !permissionsGranted -> "需要授权"
        else -> "未连接"
    }

    val statusColor = when {
        isConnected -> Secondary
        isScanning || isConnecting -> Primary
        !permissionsGranted -> Error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Primary, PrimaryDark))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isScanning) {
                ScanningAnimation()
            } else {
                Text(
                    text = if (isConnected) "⌐" else "R",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Rokid AR 眼镜",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = statusColor.copy(alpha = 0.08f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (isConnected && deviceInfo != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = deviceInfo.serialNumber.ifEmpty { "已配对" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScanningAnimation() {
    val transition = rememberInfiniteTransition(label = "scan")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
        ),
        label = "scan_rotation",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_alpha",
    )

    Icon(
        imageVector = Icons.Default.BluetoothSearching,
        contentDescription = null,
        tint = Color.White.copy(alpha = alpha),
        modifier = Modifier
            .size(36.dp)
            .rotate(rotation),
    )
}

@Composable
private fun PermissionRequestCard(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "需要蓝牙权限",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "授权蓝牙权限后即可扫描和连接\n附近的 Rokid AR 眼镜设备",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("授权蓝牙权限", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ConnectedDashboard(deviceInfo: RokidDeviceInfo) {
    val rokidManager = remember { RokidManager.getInstance() }
    val runtimeStatus by rokidManager.runtimeStatusFlow.collectAsState()
    val sceneStatus by rokidManager.sceneStatusFlow.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // === 主状态卡片 ===
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "设备已连接",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 佩戴状态指示
                    val wearingText = when (runtimeStatus.wearingStatus) {
                        "on" -> "佩戴中"
                        "off" -> "未佩戴"
                        else -> ""
                    }
                    if (wearingText.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (runtimeStatus.wearingStatus == "on") Secondary.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f),
                        ) {
                            Text(
                                text = wearingText,
                                fontSize = 11.sp,
                                color = if (runtimeStatus.wearingStatus == "on") Secondary else Error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 电量进度条
                val batteryLevel = if (runtimeStatus.batteryLevel >= 0) runtimeStatus.batteryLevel else deviceInfo.batteryLevel
                val isCharging = runtimeStatus.isCharging || deviceInfo.isCharging
                val batteryColor = when {
                    batteryLevel > 60 -> Secondary
                    batteryLevel > 20 -> Primary
                    else -> Error
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${batteryLevel}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = batteryColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCharging) "充电中" else "电量",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { batteryLevel / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = batteryColor,
                            trackColor = batteryColor.copy(alpha = 0.15f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 第一行指标：型号、固件、SN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DashboardMetric(
                        icon = Icons.Default.Devices,
                        value = when (deviceInfo.glassesType) {
                            RokidConstants.GlassType.ROKID_GLASSES -> "Glasses"
                            RokidConstants.GlassType.ROKID_AR_PRO -> "AR Pro"
                            RokidConstants.GlassType.ROKID_MAX -> "Max"
                            RokidConstants.GlassType.ROKID_AR_LITE -> "Lite"
                            RokidConstants.GlassType.ROKID_AR_STUDIO -> "Studio"
                            else -> "AR"
                        },
                        label = "型号",
                        color = PetColor,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetric(
                        icon = Icons.Default.Settings,
                        value = deviceInfo.softwareVersion.ifEmpty { "N/A" },
                        label = "固件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetric(
                        icon = Icons.Default.Info,
                        value = deviceInfo.serialNumber.take(8).ifEmpty { "N/A" },
                        label = "序列号",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 第二行指标：亮度、音量、屏幕状态
                val brightness = if (runtimeStatus.brightness >= 0) runtimeStatus.brightness else deviceInfo.brightness
                val volume = if (runtimeStatus.volume >= 0) runtimeStatus.volume else deviceInfo.volume

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DashboardMetric(
                        icon = Icons.Default.Sensors, // 亮度用 Sensors 图标替代
                        value = if (brightness >= 0) "${brightness}%" else "N/A",
                        label = "亮度",
                        color = Primary,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetric(
                        icon = Icons.Default.Headphones,
                        value = if (volume >= 0) "${volume}%" else "N/A",
                        label = "音量",
                        color = Primary,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetric(
                        icon = Icons.Default.Cast,
                        value = if (runtimeStatus.screenOn) "亮屏" else "灭屏",
                        label = "屏幕",
                        color = if (runtimeStatus.screenOn) Secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // === 场景状态卡片 ===
        val activeScenes = buildList {
            if (sceneStatus.aiAssistRunning) add("AI 助手")
            if (sceneStatus.aiChatRunning) add("AI 对话")
            if (sceneStatus.translateRunning) add("翻译")
            if (sceneStatus.wordTipsRunning) add("提词器")
            if (sceneStatus.audioRecordRunning) add("录音")
            if (sceneStatus.videoRecordRunning) add("录像")
            if (sceneStatus.navigationRunning) add("导航")
            if (sceneStatus.phoneCallRunning) add("通话")
            if (sceneStatus.otaRunning) add("OTA 升级")
            if (sceneStatus.notesRunning) add("笔记")
        }

        if (activeScenes.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = ShadowColor),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cast,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "运行中的场景",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        activeScenes.forEach { scene ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SecondaryLight,
                            ) {
                                Text(
                                    text = scene,
                                    fontSize = 11.sp,
                                    color = Secondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // === 设备详细信息（可展开） ===
        var showDetail by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = ShadowColor),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDetail = !showDetail }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "设备详情",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (showDetail) "收起" else "展开",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                    )
                }
                AnimatedVisibility(visible = showDetail) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        DetailRow("设备名称", deviceInfo.deviceName.ifEmpty { "N/A" })
                        DetailRow("设备面板", deviceInfo.devicePanel.ifEmpty { "N/A" })
                        DetailRow("硬件版本", deviceInfo.hardwareVersion.ifEmpty { "N/A" })
                        DetailRow("软件版本", deviceInfo.softwareVersion.ifEmpty { "N/A" })
                        DetailRow("序列号", deviceInfo.serialNumber.ifEmpty { "N/A" })
                        DetailRow("设备ID", deviceInfo.deviceId.ifEmpty { "N/A" })
                        DetailRow("设备类型ID", deviceInfo.deviceTypeId.ifEmpty { "N/A" })
                        DetailRow("助手版本", "${deviceInfo.assistVersionName} (${deviceInfo.assistVersionCode})")
                        DetailRow("SDK 版本", RokidConstants.ROKID_SDK_VERSION)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun DashboardMetric(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceDiscoveryCard(
    isScanning: Boolean,
    discoveredDevices: List<RokidDevice>,
    selectedDevice: RokidDevice?,
    hasError: Boolean,
    errorCode: Int?,
    onOpenSystemBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (RokidDevice) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "发现设备",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onOpenSystemBluetooth,
                    enabled = !isScanning,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "系统",
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = if (isScanning) onStopScan else onStartScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Error else Primary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

            if (isScanning) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Primary,
                    trackColor = PrimaryLight,
                )
            }

            if (hasError) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Error.copy(alpha = 0.06f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (errorCode) {
                                -100 -> "缺少蓝牙权限，请授权后重试"
                                -101 -> "权限被拒绝，请到系统设置中开启"
                                -102 -> "蓝牙未开启，请开启蓝牙后重试"
                                else -> "扫描失败 (错误码: $errorCode)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                        )
                    }
                }
            }

            if (discoveredDevices.isEmpty() && !isScanning && !hasError) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "先用「系统」打开 Android 蓝牙界面连接眼镜，或点击「扫描」查找附近设备",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            discoveredDevices.forEach { device ->
                val isSelected = device.macAddress == selectedDevice?.macAddress
                DeviceItem(
                    device = device,
                    isSelected = isSelected,
                    onClick = { onDeviceSelected(device) },
                )
                if (device != discoveredDevices.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: RokidDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Primary.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isSelected) Primary else MaterialTheme.colorScheme.outline,
                                if (isSelected) PrimaryDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        device.rssi > -60 -> Secondary
                        device.rssi > -80 -> Primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = when {
                        device.rssi > -60 -> "信号强"
                        device.rssi > -80 -> "信号中"
                        else -> "信号弱"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConnectButton(
    isConnecting: Boolean,
    isScanning: Boolean,
    onConnect: () -> Unit,
) {
    Button(
        onClick = onConnect,
        enabled = !isScanning && !isConnecting,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Secondary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("连接中...", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        } else {
            Icon(Icons.Default.BluetoothConnected, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("连接 Papaya 通道", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QuickActionsCard(
    isCameraOpen: Boolean,
    isRecording: Boolean,
    isVisionNarrationActive: Boolean,
    onTakePhoto: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleVisionNarration: () -> Unit,
    onToggleAudio: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Cast,
                    contentDescription = null,
                    tint = PetColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "快捷操作",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionItem(
                    icon = Icons.Default.CameraAlt,
                    label = if (isCameraOpen) "关闭相机" else "开启相机",
                    color = if (isCameraOpen) Error else Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleCamera,
                )
                QuickActionItem(
                    icon = Icons.Default.Videocam,
                    label = "拍照",
                    color = Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onTakePhoto,
                )
                QuickActionItem(
                    icon = Icons.Default.Mic,
                    label = if (isRecording) "停止录音" else "开启录音",
                    color = if (isRecording) Error else PetColor,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleAudio,
                )
                QuickActionItem(
                    icon = Icons.Default.Sensors,
                    label = if (isVisionNarrationActive) "停止讲解" else "视觉讲解",
                    color = if (isVisionNarrationActive) Error else Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleVisionNarration,
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FeatureShowcaseCard(
    isConnected: Boolean,
    isAiChatActive: Boolean,
    isTranslationActive: Boolean,
    isTeleprompterActive: Boolean = false,
    onToggleAiChat: () -> Unit,
    onToggleTranslation: () -> Unit,
    onSendText: (String) -> Unit,
    onToggleTeleprompter: () -> Unit = {},
) {
    var showTextInput by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AR 能力",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val features = listOf(
                RkFeatureItem("AI 对话", Icons.Default.Chat, if (isAiChatActive) "运行中" else "启动 AI 对话", true, isAiChatActive),
                RkFeatureItem("翻译", Icons.Default.Language, if (isTranslationActive) "运行中 (中→英)" else "启用语音翻译", true, isTranslationActive),
                RkFeatureItem("推送文字", Icons.Default.Chat, "向眼镜推送文字内容", true, false),
                RkFeatureItem("提词器", Icons.Default.Info, if (isTeleprompterActive) "运行中" else "在眼镜上显示提词内容", true, isTeleprompterActive),
            )

            val rows = features.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { feature ->
                            FeatureChip(
                                feature = feature,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when (feature.name) {
                                        "AI 对话" -> onToggleAiChat()
                                        "翻译" -> onToggleTranslation()
                                        "推送文字" -> showTextInput = !showTextInput
                                        "提词器" -> onToggleTeleprompter()
                                    }
                                },
                            )
                        }
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showTextInput) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    "输入要推送的文字...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        Button(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onSendText(textInput)
                                    textInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("发送", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class RkFeatureItem(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val available: Boolean,
    val isActive: Boolean = false,
)

@Composable
private fun FeatureChip(
    feature: RkFeatureItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    feature.isActive -> SecondaryLight
                    feature.available -> PetColorLight
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = when {
                feature.isActive -> Secondary
                feature.available -> PetColor
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feature.name,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                feature.isActive -> Secondary
                feature.available -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BatteryStatusCard(
    isConnected: Boolean,
    deviceInfo: RokidDeviceInfo?,
    rokidManager: RokidManager,
) {
    val runtimeStatus by rokidManager.runtimeStatusFlow.collectAsState()

    val batteryLevel = when {
        !isConnected -> -1
        runtimeStatus.batteryLevel >= 0 -> runtimeStatus.batteryLevel
        deviceInfo != null -> deviceInfo.batteryLevel
        else -> -1
    }
    val isCharging = isConnected && (runtimeStatus.isCharging || (deviceInfo?.isCharging ?: false))

    val batteryColor = when {
        batteryLevel < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        batteryLevel > 60 -> Secondary
        batteryLevel > 20 -> Primary
        else -> Error
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(batteryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = batteryColor,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (batteryLevel >= 0) "$batteryLevel" else "—",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = batteryColor,
                    )
                    if (batteryLevel >= 0) {
                        Text(
                            text = "%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = batteryColor,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            !isConnected -> "未连接"
                            isCharging -> "充电中"
                            batteryLevel <= 20 -> "电量低"
                            else -> "眼镜电量"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (batteryLevel >= 0) batteryLevel / 100f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = batteryColor,
                    trackColor = batteryColor.copy(alpha = 0.15f),
                )
            }
        }
    }
}

@Composable
private fun LogToggleCard(
    showLogs: Boolean,
    logMessages: List<String>,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggle)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "运行日志",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (showLogs) "收起" else "展开",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                )
            }

            if (!showLogs && logMessages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = logMessages.last(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                )
            }

            AnimatedVisibility(
                visible = showLogs,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (logMessages.isEmpty()) {
                        Text(
                            text = "暂无日志",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1A1A2E),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                logMessages.takeLast(30).forEach { message ->
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
    }
}
