package com.nanackations2.papaya.glass.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Papaya 眼镜端后台通信服务。
 *
 * 核心职责：
 * 1. 监听手机端通过 CXR 自定义通道发来的消息
 * 2. 响应 Ping → 返回 Pong + 设备状态
 * 3. 处理轻量命令（亮度/音量/通知等）
 * 4. 上报心跳状态给手机端
 *
 * 使用 Rokid CXR Service Bridge (com.rokid.cxr:cxr-service-bridge) 与手机端 CXR-M SDK 通信。
 * 不监听或重写 Rokid 系统保留的按键、触控板、拍照、录像和唤醒词交互。
 */
class PapayaGlassService : Service() {

    companion object {
        private const val TAG = "PapayaGlassService"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "papaya_glass_connection"

        @Volatile
        var instance: PapayaGlassService? = null
            private set

        // ===== 与 RokidConstants 保持一致的通道/命令定义 =====
        object Channel {
            const val PAPAYA_PING = "papaya_ping"
            const val PAPAYA_PONG = "papaya_pong"
            const val PAPAYA_STATUS_SYNC = "papaya_status_sync"
            const val PAPAYA_CMD = "papaya_cmd"
            const val PAPAYA_EVENT = "papaya_event"
            const val PAPAYA_CHAT = "papaya_chat"
            const val PAPAYA_NAV = "papaya_nav"
            const val PAPAYA_NOTIFY = "papaya_notify"
            const val GLASS_TEXT = "glass_text"
            const val AI_RESULT = "ai_result"
            const val CUSTOM_DATA = "custom_data"
        }

        object BidiCmd {
            const val REQUEST_STATUS = "REQ_STATUS"
            const val RESPONSE_STATUS = "RSP_STATUS"
            const val SET_BRIGHTNESS = "SET_BRIGHTNESS"
            const val SET_VOLUME = "SET_VOLUME"
            const val SHOW_TOAST = "SHOW_TOAST"
            const val SHOW_CARD = "SHOW_CARD"
            const val DISMISS_CARD = "DISMISS_CARD"
            const val BATTERY_UPDATE = "BATTERY_UPDATE"
            const val CHAT_TEXT = "CHAT_TEXT"
            const val CHAT_IMAGE = "CHAT_IMAGE"
        }

        private val _sharedIsPhoneConnected = MutableStateFlow(false)
        val sharedIsPhoneConnected: StateFlow<Boolean> = _sharedIsPhoneConnected.asStateFlow()

        private val _sharedIsNativeBluetoothConnected = MutableStateFlow(false)
        val sharedIsNativeBluetoothConnected: StateFlow<Boolean> =
            _sharedIsNativeBluetoothConnected.asStateFlow()

        private val _sharedNativeBluetoothLabel = MutableStateFlow("")
        val sharedNativeBluetoothLabel: StateFlow<String> =
            _sharedNativeBluetoothLabel.asStateFlow()

        private val _sharedLastReceivedMessage = MutableStateFlow("")
        val sharedLastReceivedMessage: StateFlow<String> = _sharedLastReceivedMessage.asStateFlow()

        private val _sharedPhoneBatteryRequest = MutableStateFlow(-1)
        val sharedPhoneBatteryRequest: StateFlow<Int> = _sharedPhoneBatteryRequest.asStateFlow()

        private val _sharedNotificationList = MutableStateFlow<List<String>>(emptyList())
        val sharedNotificationList: StateFlow<List<String>> = _sharedNotificationList.asStateFlow()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    // 可被 UI 订阅的状态
    val isPhoneConnected: StateFlow<Boolean> = sharedIsPhoneConnected

    val isNativeBluetoothConnected: StateFlow<Boolean> = sharedIsNativeBluetoothConnected

    val lastReceivedMessage: StateFlow<String> = sharedLastReceivedMessage

    val phoneBatteryRequest: StateFlow<Int> = sharedPhoneBatteryRequest

    val notificationList: StateFlow<List<String>> = sharedNotificationList

    private var serviceBridge: CXRServiceBridge? = null
    private var bluetoothReceiverRegistered = false

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.bluetoothDeviceExtra()
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    _sharedIsNativeBluetoothConnected.value = true
                    _sharedNativeBluetoothLabel.value = device?.safeBluetoothLabel() ?: "系统蓝牙已连接"
                    Log.d(TAG, "系统蓝牙已连接: ${_sharedNativeBluetoothLabel.value}")
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothAdapter.ACTION_STATE_CHANGED -> updateNativeBluetoothStatus()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "PapayaGlassService 已启动")

        startForeground(NOTIFICATION_ID, buildNotification())
        registerBluetoothStateReceiver()
        startNativeBluetoothMonitor()
        initGlassSdk()
        startHeartbeatReporter()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        unregisterBluetoothStateReceiver()
        scope.cancel()
        Log.d(TAG, "PapayaGlassService 已停止")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Papaya Glass",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Papaya Glass")
            .setContentText("正在等待手机端连接")
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    // ========== SDK 初始化 ==========

    private fun registerBluetoothStateReceiver() {
        if (bluetoothReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(bluetoothStateReceiver, filter)
            }
            bluetoothReceiverRegistered = true
            updateNativeBluetoothStatus()
        } catch (e: Exception) {
            Log.w(TAG, "注册蓝牙状态监听失败", e)
        }
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!bluetoothReceiverRegistered) return
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "注销蓝牙状态监听失败", e)
        } finally {
            bluetoothReceiverRegistered = false
        }
    }

    private fun startNativeBluetoothMonitor() {
        scope.launch {
            while (true) {
                updateNativeBluetoothStatus()
                delay(3_000)
            }
        }
    }

    private fun updateNativeBluetoothStatus() {
        if (!hasBluetoothConnectPermission()) {
            _sharedIsNativeBluetoothConnected.value = false
            _sharedNativeBluetoothLabel.value = "缺少蓝牙权限"
            return
        }

        val adapter = bluetoothAdapter()
        if (adapter?.isEnabled != true) {
            _sharedIsNativeBluetoothConnected.value = false
            _sharedNativeBluetoothLabel.value = "蓝牙未开启"
            return
        }

        val connectedDevice = connectedBondedDevice(adapter)
        val connected = connectedDevice != null || adapter.hasConnectedProfile()
        _sharedIsNativeBluetoothConnected.value = connected
        _sharedNativeBluetoothLabel.value = connectedDevice?.safeBluetoothLabel()
            ?: if (connected) "系统蓝牙已连接" else ""
    }

    private fun initGlassSdk() {
        try {
            serviceBridge = CXRServiceBridge().apply {
                setStatusListener(object : CXRServiceBridge.StatusListener {
                    override fun onConnected(socketUuid: String?, macAddress: String?, deviceType: Int) {
                        _sharedIsPhoneConnected.value = true
                        Log.d(TAG, "CXR 已连接: socket=$socketUuid mac=$macAddress type=$deviceType")
                    }

                    override fun onDisconnected() {
                        _sharedIsPhoneConnected.value = false
                        Log.d(TAG, "CXR 已断开")
                    }

                    override fun onConnecting(socketUuid: String?, macAddress: String?, deviceType: Int) {
                        Log.d(TAG, "CXR 连接中: socket=$socketUuid mac=$macAddress type=$deviceType")
                    }

                    override fun onARTCStatus(fps: Float, stable: Boolean) {
                        Log.d(TAG, "ARTC status: fps=$fps stable=$stable")
                    }

                    override fun onRokidAccountChanged(account: String?) {
                        Log.d(TAG, "Rokid account changed: $account")
                    }
                })
            }

            subscribeIncomingChannels()

            Log.d(TAG, "CXR Service Bridge 初始化成功，已订阅手机端消息")
        } catch (e: Exception) {
            Log.e(TAG, "CXR Service Bridge 初始化失败", e)
            _sharedIsPhoneConnected.value = false
        }
    }

    private fun subscribeIncomingChannels() {
        val bridge = serviceBridge ?: return
        listOf(
            Channel.PAPAYA_PING,
            Channel.PAPAYA_CMD,
            Channel.PAPAYA_CHAT,
            Channel.PAPAYA_NOTIFY,
            Channel.PAPAYA_STATUS_SYNC,
            Channel.PAPAYA_NAV,
            Channel.GLASS_TEXT,
            Channel.AI_RESULT,
            Channel.CUSTOM_DATA,
        ).forEach { channel ->
            val result = bridge.subscribe(channel) { topic, caps, _ ->
                handleIncomingMessage(topic, caps)
            }
            Log.d(TAG, "订阅通道 $channel result=$result")
        }
    }

    // ========== 消息处理 ==========

    private fun handleIncomingMessage(channel: String, caps: Caps?) {
        val subCmd = capString(caps, 0) ?: return
        val payload = capString(caps, 1) ?: ""

        Log.d(TAG, "收到消息: channel=$channel, cmd=$subCmd, payload=$payload")

        when (channel) {
            Channel.PAPAYA_PING -> handlePing(subCmd, payload)
            Channel.PAPAYA_CMD -> handleCommand(subCmd, payload)
            Channel.PAPAYA_CHAT -> handleChat(subCmd, payload)
            Channel.PAPAYA_NOTIFY -> handleNotification(subCmd, payload)
            Channel.PAPAYA_STATUS_SYNC -> handleStatusSync(subCmd, payload)
            Channel.GLASS_TEXT -> handleTextUpdate(subCmd, payload)
            Channel.AI_RESULT -> handleAiResult(subCmd, payload)
            Channel.CUSTOM_DATA -> handleCustomData(subCmd, payload)
            else -> Log.d(TAG, "未处理的通道: $channel")
        }
    }

    private fun handlePing(subCmd: String, payload: String) {
        // 收到 Ping，回复 Pong + 设备状态
        val statusJson = buildDeviceStatusJson()
        val caps = Caps().apply {
            write(BidiCmd.RESPONSE_STATUS)
            write(statusJson)
        }
        sendToPhone(Channel.PAPAYA_PONG, caps)
        _sharedIsPhoneConnected.value = true
        Log.d(TAG, "Ping 已响应，返回状态: $statusJson")
    }

    private fun handleCommand(cmd: String, payload: String) {
        when (cmd) {
            BidiCmd.SET_BRIGHTNESS -> {
                val level = payload.toIntOrNull() ?: return
                // Rokid Glass SDK 设置亮度
                try {
                    android.provider.Settings.System.putInt(
                        contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        (level * 255 / 100).coerceIn(0, 255)
                    )
                    Log.d(TAG, "亮度已设置为 $level%")
                } catch (e: Exception) {
                    Log.w(TAG, "设置亮度失败", e)
                }
            }
            BidiCmd.SET_VOLUME -> {
                val level = payload.toIntOrNull() ?: return
                val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    (level * maxVol / 100).coerceIn(0, maxVol),
                    0
                )
                Log.d(TAG, "音量已设置为 $level%")
            }
            BidiCmd.SHOW_TOAST -> {
                android.widget.Toast.makeText(this, payload, android.widget.Toast.LENGTH_SHORT).show()
            }
            else -> Log.d(TAG, "未处理的命令: $cmd")
        }
    }

    private fun handleChat(subCmd: String, payload: String) {
        when (subCmd) {
            BidiCmd.CHAT_TEXT -> {
                _sharedLastReceivedMessage.value = payload
                Log.d(TAG, "收到聊天消息: $payload")
                // 可以在眼镜上显示该消息（通过 Overlay 或 UI 更新）
            }
            BidiCmd.CHAT_IMAGE -> {
                Log.d(TAG, "收到图片消息 (base64 长度: ${payload.length})")
            }
        }
    }

    private fun handleNotification(subCmd: String, payload: String) {
        try {
            val json = gson.fromJson(payload, JsonObject::class.java)
            val title = json.get("title")?.asString ?: "通知"
            val body = json.get("body")?.asString ?: ""
            val notif = "$title: $body"
            _sharedNotificationList.value = (_sharedNotificationList.value + notif).takeLast(20)
            Log.d(TAG, "收到通知: $notif")
        } catch (e: Exception) {
            _sharedNotificationList.value = (_sharedNotificationList.value + payload).takeLast(20)
        }
    }

    private fun handleStatusSync(subCmd: String, payload: String) {
        if (subCmd == BidiCmd.REQUEST_STATUS) {
            val statusJson = buildDeviceStatusJson()
            val caps = Caps().apply {
                write(BidiCmd.RESPONSE_STATUS)
                write(statusJson)
            }
            sendToPhone(Channel.PAPAYA_STATUS_SYNC, caps)
        }
    }

    private fun handleTextUpdate(subCmd: String, payload: String) {
        // 手机端推送的文本更新
        _sharedLastReceivedMessage.value = payload
    }

    private fun handleAiResult(subCmd: String, payload: String) {
        // AI 结果展示
        _sharedLastReceivedMessage.value = payload
    }

    private fun handleCustomData(subCmd: String, payload: String) {
        Log.d(TAG, "自定义数据: [$subCmd] $payload")
    }

    // ========== 发送消息到手机端 ==========

    private fun sendToPhone(channel: String, caps: Caps): Boolean {
        return try {
            val result = serviceBridge?.sendMessage(channel, caps) ?: -1
            result == 0
        } catch (e: Exception) {
            Log.w(TAG, "发送消息失败: $channel", e)
            false
        }
    }

    /** 主动向手机端报告事件 */
    fun reportEvent(eventType: String, data: String) {
        val caps = Caps().apply {
            write(eventType)
            write(data)
        }
        sendToPhone(Channel.PAPAYA_EVENT, caps)
    }

    /** 发送聊天消息给手机端 */
    fun sendChatToPhone(text: String) {
        val caps = Caps().apply {
            write(BidiCmd.CHAT_TEXT)
            write(text)
        }
        sendToPhone(Channel.PAPAYA_CHAT, caps)
    }

    /**
     * 使用 CXR Service Bridge 拍照，将图片 Base64 编码后通过 PAPAYA_CHAT / CHAT_IMAGE 发送给手机端。
     * 手机端的 GlassPhotoLlmHandler 会收到后调用 LLM，并将结果通过 AI_RESULT 通道回传。
     */
    fun captureAndSendPhoto() {
        if (!_sharedIsPhoneConnected.value) {
            _sharedLastReceivedMessage.value = "无法拍照：手机未连接"
            Log.w(TAG, "captureAndSendPhoto: 手机未连接")
            return
        }

        _sharedLastReceivedMessage.value = "正在拍照..."
        Log.d(TAG, "开始拍照并发送到手机端")

        scope.launch {
            try {
                val photoBytes = capturePhotoFromCamera()
                if (photoBytes == null || photoBytes.isEmpty()) {
                    _sharedLastReceivedMessage.value = "拍照失败：无法获取图片"
                    Log.w(TAG, "captureAndSendPhoto: 拍照失败")
                    return@launch
                }

                val base64 = android.util.Base64.encodeToString(photoBytes, android.util.Base64.NO_WRAP)
                Log.d(TAG, "拍照成功，图片大小: ${photoBytes.size} 字节, Base64 长度: ${base64.length}")

                val caps = Caps().apply {
                    write(BidiCmd.CHAT_IMAGE)
                    write(base64)
                }
                val sent = sendToPhone(Channel.PAPAYA_CHAT, caps)
                if (sent) {
                    _sharedLastReceivedMessage.value = "照片已发送，等待 AI 分析..."
                    Log.d(TAG, "照片已发送到手机端")
                } else {
                    _sharedLastReceivedMessage.value = "照片发送失败"
                    Log.w(TAG, "sendToPhone 返回 false")
                }
            } catch (e: Exception) {
                _sharedLastReceivedMessage.value = "拍照出错：${e.message}"
                Log.e(TAG, "captureAndSendPhoto 异常", e)
            }
        }
    }

    /**
     * 通过 CXR Service Bridge 拍摄一帧照片。
     * 在 Rokid 眼镜上，Service Bridge 可以请求系统摄像头拍照。
     */
    private suspend fun capturePhotoFromCamera(): ByteArray? =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val resumed = java.util.concurrent.atomic.AtomicBoolean(false)

            fun resumeOnce(value: ByteArray?) {
                if (resumed.compareAndSet(false, true) && cont.isActive) {
                    cont.resume(value)
                }
            }

            try {
                val bridge = serviceBridge
                if (bridge == null) {
                    resumeOnce(null)
                    return@suspendCancellableCoroutine
                }

                // 通过 CXR Service Bridge 请求眼镜系统拍照
                val requestCaps = Caps().apply {
                    write("TAKE_PHOTO")
                    write("1280x720")
                }
                // 使用 sendMessage 请求手机端触发 takeGlassPhoto（协议约定）
                // 或者直接使用眼镜本地摄像头
                val photoData = captureLocalPhoto()
                resumeOnce(photoData)
            } catch (e: Exception) {
                Log.e(TAG, "capturePhotoFromCamera 异常", e)
                resumeOnce(null)
            }

            cont.invokeOnCancellation { resumed.set(true) }
        }

    /**
     * 使用 Android Camera2 API 在眼镜本地拍一张照片。
     * Rokid 眼镜运行的是 Android 系统，可以直接访问摄像头。
     */
    private fun captureLocalPhoto(): ByteArray? {
        return try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return null

            // 使用 ImageReader 拍一��� JPEG
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(android.graphics.ImageFormat.JPEG)
            val size = outputSizes?.firstOrNull { it.width <= 1280 }
                ?: outputSizes?.lastOrNull()
                ?: android.util.Size(1280, 720)

            val reader = android.media.ImageReader.newInstance(
                size.width, size.height, android.graphics.ImageFormat.JPEG, 1
            )

            val handlerThread = android.os.HandlerThread("CameraCapture").apply { start() }
            val handler = android.os.Handler(handlerThread.looper)
            val latch = java.util.concurrent.CountDownLatch(1)
            var resultBytes: ByteArray? = null

            reader.setOnImageAvailableListener({ imgReader ->
                val image = imgReader.acquireLatestImage()
                if (image != null) {
                    val buffer = image.planes[0].buffer
                    resultBytes = ByteArray(buffer.remaining())
                    buffer.get(resultBytes!!)
                    image.close()
                }
                latch.countDown()
            }, handler)

            cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
                override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                    try {
                        val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(reader.surface)
                            set(android.hardware.camera2.CaptureRequest.JPEG_QUALITY, 75.toByte())
                        }
                        camera.createCaptureSession(
                            listOf(reader.surface),
                            object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                                    session.capture(captureRequest.build(), object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {
                                        override fun onCaptureCompleted(
                                            session: android.hardware.camera2.CameraCaptureSession,
                                            request: android.hardware.camera2.CaptureRequest,
                                            result: android.hardware.camera2.TotalCaptureResult
                                        ) {
                                            // image available listener will handle the data
                                        }

                                        override fun onCaptureFailed(
                                            session: android.hardware.camera2.CameraCaptureSession,
                                            request: android.hardware.camera2.CaptureRequest,
                                            failure: android.hardware.camera2.CaptureFailure
                                        ) {
                                            latch.countDown()
                                        }
                                    }, handler)
                                }

                                override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                                    latch.countDown()
                                }
                            },
                            handler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera capture session error", e)
                        latch.countDown()
                        camera.close()
                    }
                }

                override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                    camera.close()
                    latch.countDown()
                }

                override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                    camera.close()
                    latch.countDown()
                }
            }, handler)

            // 等待拍照完成，最多 8 秒
            latch.await(8, java.util.concurrent.TimeUnit.SECONDS)
            handlerThread.quitSafely()
            reader.close()

            resultBytes
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "captureLocalPhoto failed", e)
            null
        }
    }

    // ========== 心跳上报 ==========

    private fun startHeartbeatReporter() {
        scope.launch {
            while (true) {
                delay(30_000) // 每 30 秒上报一次电量
                if (_sharedIsPhoneConnected.value) {
                    val batteryLevel = getBatteryLevel()
                    val caps = Caps().apply {
                        write(BidiCmd.BATTERY_UPDATE)
                        write(batteryLevel.toString())
                    }
                    sendToPhone(Channel.PAPAYA_EVENT, caps)
                }
            }
        }
    }

    // ========== 工具方法 ==========

    private fun buildDeviceStatusJson(): String {
        val status = JsonObject().apply {
            addProperty("battery", getBatteryLevel())
            addProperty("charging", isCharging())
            addProperty("brightness", getCurrentBrightness())
            addProperty("volume", getCurrentVolume())
            addProperty("native_bluetooth_connected", _sharedIsNativeBluetoothConnected.value)
            addProperty("native_bluetooth_label", _sharedNativeBluetoothLabel.value)
            addProperty("cxr_connected", _sharedIsPhoneConnected.value)
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("app_version", "0.1.0")
            addProperty("device", android.os.Build.MODEL)
        }
        return gson.toJson(status)
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }

    private fun getCurrentBrightness(): Int {
        return try {
            val raw = android.provider.Settings.System.getInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
            (raw * 100 / 255).coerceIn(0, 100)
        } catch (e: Exception) { 50 }
    }

    private fun getCurrentVolume(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        return if (max > 0) (current * 100 / max) else 50
    }

    private fun capString(caps: Caps?, index: Int): String? {
        if (caps == null) return null
        return try {
            if (index >= caps.size()) null
            else caps.at(index)?.getString()
        } catch (_: Throwable) { null }
    }

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun connectedBondedDevice(adapter: BluetoothAdapter): BluetoothDevice? {
        return try {
            adapter.bondedDevices?.firstOrNull { it.isConnectedCompat() }
        } catch (e: SecurityException) {
            Log.w(TAG, "读取已配对设备失败：缺少 BLUETOOTH_CONNECT 权限", e)
            null
        }
    }

    private fun BluetoothAdapter.hasConnectedProfile(): Boolean {
        return runCatching {
            getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED ||
                getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED ||
                getProfileConnectionState(BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED ||
                javaClass.getMethod("getConnectionState").invoke(this) == BluetoothProfile.STATE_CONNECTED
        }.getOrDefault(false)
    }

    private fun BluetoothDevice.isConnectedCompat(): Boolean {
        return runCatching {
            javaClass.getMethod("isConnected").invoke(this) == true
        }.getOrDefault(false)
    }

    private fun BluetoothDevice.safeBluetoothLabel(): String {
        return try {
            name?.takeIf { it.isNotBlank() } ?: address ?: "蓝牙设备"
        } catch (_: SecurityException) {
            "蓝牙设备"
        }
    }

    private fun Intent.bluetoothDeviceExtra(): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
