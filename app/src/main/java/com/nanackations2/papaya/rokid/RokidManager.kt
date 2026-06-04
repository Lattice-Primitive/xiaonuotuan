package com.nanackations2.papaya.rokid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.rokid.cxr.Caps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class RokidManager private constructor() : RokidApi {

    companion object {
        private const val TAG = "RokidManager"

        @Volatile
        private var instance: RokidManager? = null

        fun getInstance(): RokidManager {
            return instance ?: synchronized(this) {
                instance ?: RokidManager().also { instance = it }
            }
        }
    }

    override var isBluetoothConnected: Boolean = false
        private set
    override var isInitialized: Boolean = false
        private set
    override var connectedDeviceInfo: RokidDeviceInfo? = null
        private set

    private var appContext: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: android.bluetooth.le.BluetoothLeScanner? = null

    private var scanCallback: ScanCallback? = null
    private var externalScanCallback: RokidScanCallback? = null
    private var connectionCallback: RokidConnectionCallback? = null
    private var dataCallback: RokidDataCallback? = null
    private var cameraCallback: RokidCameraCallback? = null
    private var audioCallback: RokidAudioCallback? = null
    private var customCmdListener: RokidCustomCmdListener? = null
    private var aiEventListener: RokidAiEventListener? = null
    private var glassesEventListener: RokidGlassesEventListener? = null

    private val scanHandler = Handler(Looper.getMainLooper())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isScanning = false

    private val discoveredDevices = CopyOnWriteArrayList<RokidDevice>()

    private var currentSocketUuid: String? = null
    private var currentMacAddress: String? = null
    private var currentConnectionState: String = RokidConstants.ConnectionState.DISCONNECTED
    private var connectionAttemptId = 0
    private var connectionTimeoutRunnable: Runnable? = null

    // ===== StateFlows =====
    private val _connectionStateFlow = MutableStateFlow(RokidConstants.ConnectionState.DISCONNECTED)
    val connectionStateFlow: StateFlow<String> = _connectionStateFlow.asStateFlow()

    private val _runtimeStatusFlow = MutableStateFlow(RokidRuntimeStatus())
    val runtimeStatusFlow: StateFlow<RokidRuntimeStatus> = _runtimeStatusFlow.asStateFlow()

    private val _sceneStatusFlow = MutableStateFlow(RokidSceneStatus())
    val sceneStatusFlow: StateFlow<RokidSceneStatus> = _sceneStatusFlow.asStateFlow()

    // Mock devices for development without real hardware
    private val mockDevices = listOf(
        RokidDevice(name = "Rokid Glasses Demo", macAddress = "AA:BB:CC:DD:EE:01", rssi = -65),
        RokidDevice(name = "Rokid AR Pro Demo", macAddress = "AA:BB:CC:DD:EE:02", rssi = -72),
    )

    override fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        isInitialized = true
        Log.d(TAG, "RokidManager initialized")
    }

    override fun release() {
        stopScan()
        disconnect()
        bluetoothLeScanner = null
        bluetoothAdapter = null
        appContext = null
        isInitialized = false
        Log.d(TAG, "RokidManager released")
    }

    override fun startScan(callback: RokidScanCallback) {
        if (!isInitialized || isScanning) return
        externalScanCallback = callback
        discoveredDevices.clear()
        isScanning = true

        val context = appContext
        if (context == null) {
            isScanning = false
            mainHandler.post { callback.onScanFailed(-1) }
            Log.e(TAG, "scan failed: context is null")
            return
        }

        if (!hasBluetoothPermissions(context)) {
            isScanning = false
            mainHandler.post { callback.onScanFailed(-100) }
            Log.e(TAG, "scan failed: missing Bluetooth permissions")
            return
        }

        try {
            val scanner = bluetoothLeScanner
            if (scanner != null && bluetoothAdapter?.isEnabled == true) {
                val filter = try {
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(RokidConstants.ROKID_SERVICE_UUID))
                        .build()
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid service UUID, scanning without filter", e)
                    null
                }
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        try {
                            val device = result.device
                            val name = device.name ?: "Unknown Rokid"
                            val rokidDevice = RokidDevice(
                                name = name,
                                macAddress = device.address,
                                rssi = result.rssi,
                            )
                            if (discoveredDevices.none { it.macAddress == rokidDevice.macAddress }) {
                                discoveredDevices.add(rokidDevice)
                                mainHandler.post {
                                    externalScanCallback?.onDeviceFound(rokidDevice)
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.w(TAG, "Security exception in scan result", e)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        mainHandler.post {
                            externalScanCallback?.onScanFailed(errorCode)
                        }
                    }
                }

                val scanFilters = if (filter != null) listOf(filter) else listOf()
                try {
                    scanner.startScan(scanFilters, settings, scanCallback!!)
                } catch (e: SecurityException) {
                    isScanning = false
                    mainHandler.post { callback.onScanFailed(-101) }
                    Log.e(TAG, "scan failed: SecurityException - missing runtime permissions", e)
                    return
                }
                callback.onScanStarted()
            } else {
                Log.w(TAG, "BLE not available, using mock devices for development")
                callback.onScanStarted()
                var delay = 0L
                for (mockDevice in mockDevices) {
                    delay += 500
                    scanHandler.postDelayed({
                        if (isScanning) {
                            discoveredDevices.add(mockDevice)
                            callback.onDeviceFound(mockDevice)
                        }
                    }, delay)
                }
                scanHandler.postDelayed({
                    if (isScanning) {
                        stopScan()
                    }
                }, delay + 1000)
            }

            scanHandler.postDelayed({
                if (isScanning) {
                    stopScan()
                }
            }, RokidConstants.SCAN_TIMEOUT_MS)
        } catch (e: Exception) {
            isScanning = false
            mainHandler.post { callback.onScanFailed(-102) }
            Log.e(TAG, "Unexpected error during scan start", e)
        }
    }

    override fun stopScan() {
        if (!isScanning) return
        isScanning = false
        scanHandler.removeCallbacksAndMessages(null)
        scanCallback?.let { cb ->
            try {
                bluetoothLeScanner?.stopScan(cb)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping scan", e)
            }
        }
        scanCallback = null
        externalScanCallback?.onScanStopped()
    }

    override fun pairDevice(context: Context, device: RokidDevice, callback: RokidConnectionCallback) {
        connectionCallback = callback
        currentConnectionState = RokidConstants.ConnectionState.PAIRING
        _connectionStateFlow.value = currentConnectionState
        callback.onConnecting()

        if (!hasBluetoothPermissions(context)) {
            markConnectionFailed(
                callback,
                RokidConstants.ScanError.NO_PERMISSION,
                "配对失败：缺少蓝牙运行时权限 BLUETOOTH_SCAN/BLUETOOTH_CONNECT",
            )
            return
        }

        scheduleConnectionTimeout(
            delayMs = RokidConstants.PAIR_TIMEOUT_MS,
            callback = callback,
            message = "配对超时：手机已发现眼镜，但没有拿到连接信息",
        )

        val bluetoothDevice = findBluetoothDevice(device.macAddress)
        if (bluetoothDevice != null && bluetoothAdapter?.isEnabled == true) {
            Log.d(TAG, "Initiating real Bluetooth pairing with ${device.name}")
            CxrSdkProxy.initBluetooth(context, bluetoothDevice, object : RokidConnectionCallback {
                override fun onConnectionInfo(
                    socketUuid: String?,
                    macAddress: String?,
                    rokidAccount: String?,
                    glassesType: Int,
                ) {
                    currentSocketUuid = socketUuid
                    currentMacAddress = macAddress ?: device.macAddress
                    RokidCredentials.rememberConnection(context, currentSocketUuid, currentMacAddress)
                    cancelConnectionTimeout()
                    callback.onConnectionInfo(socketUuid, macAddress, rokidAccount, glassesType)
                }

                override fun onConnected() {
                    markConnected(device.macAddress)
                    callback.onConnected()
                }

                override fun onConnecting() {
                    currentConnectionState = RokidConstants.ConnectionState.CONNECTING
                    _connectionStateFlow.value = currentConnectionState
                    callback.onConnecting()
                }

                override fun onDisconnected() {
                    if (isConnectionPending()) {
                        markConnectionFailed(
                            callback,
                            RokidConstants.ScanError.CONNECTION_INTERRUPTED,
                            "配对阶段 SDK 返回 onDisconnected: device=${device.name}, mac=${device.macAddress}, socket=${currentSocketUuid ?: "<empty>"}",
                        )
                    } else {
                        markDisconnected()
                        callback.onDisconnected()
                    }
                }

                override fun onError(code: Int, message: String) {
                    markConnectionFailed(callback, code, message)
                }
            })
        } else {
            Log.d(TAG, "Mock pairing with ${device.name}")
            mainHandler.postDelayed({
                val mockSocketUuid = "mock-uuid-${System.currentTimeMillis()}"
                val mockMacAddress = device.macAddress
                currentSocketUuid = mockSocketUuid
                currentMacAddress = mockMacAddress
                RokidCredentials.rememberConnection(context, mockSocketUuid, mockMacAddress)
                cancelConnectionTimeout()
                callback.onConnectionInfo(mockSocketUuid, mockMacAddress, "mock_account", 1)
            }, 1500)
        }
    }

    override fun connect(
        context: Context,
        socketUuid: String,
        macAddress: String,
        callback: RokidConnectionCallback,
        snEncrypt: ByteArray?,
        clientSecret: String?,
    ) {
        connectionCallback = callback
        currentConnectionState = RokidConstants.ConnectionState.CONNECTING
        _connectionStateFlow.value = currentConnectionState
        currentSocketUuid = socketUuid
        currentMacAddress = macAddress
        RokidCredentials.rememberConnection(context, socketUuid, macAddress)
        callback.onConnecting()

        if (!hasBluetoothPermissions(context)) {
            markConnectionFailed(
                callback,
                RokidConstants.ScanError.NO_PERMISSION,
                "连接失败：缺少蓝牙运行时权限 BLUETOOTH_SCAN/BLUETOOTH_CONNECT",
            )
            return
        }

        if (socketUuid.isBlank() || macAddress.isBlank()) {
            markConnectionFailed(
                callback,
                1,
                "连接参数非法：socketUuid=${socketUuid.ifBlank { "<empty>" }}, mac=${macAddress.ifBlank { "<empty>" }}",
            )
            return
        }

        scheduleConnectionTimeout(
            delayMs = RokidConstants.CONNECT_TIMEOUT_MS,
            callback = callback,
            message = "连接超时：SDK 没有返回成功或失败",
        )

        val managedCallback = object : RokidConnectionCallback {
            override fun onConnectionInfo(
                socketUuid: String?,
                macAddress: String?,
                rokidAccount: String?,
                glassesType: Int,
            ) {
                currentSocketUuid = socketUuid ?: currentSocketUuid
                currentMacAddress = macAddress ?: currentMacAddress
                RokidCredentials.rememberConnection(context, currentSocketUuid, currentMacAddress)
                callback.onConnectionInfo(socketUuid, macAddress, rokidAccount, glassesType)
            }

            override fun onConnected() {
                markConnected(macAddress)
                callback.onConnected()
            }

            override fun onConnecting() {
                currentConnectionState = RokidConstants.ConnectionState.CONNECTING
                _connectionStateFlow.value = currentConnectionState
                callback.onConnecting()
            }

            override fun onDisconnected() {
                if (isConnectionPending()) {
                    markConnectionFailed(
                        callback,
                        RokidConstants.ScanError.CONNECTION_INTERRUPTED,
                        "连接阶段 SDK 返回 onDisconnected: socket=${currentSocketUuid ?: "<empty>"}, mac=${currentMacAddress ?: "<empty>"}, auth=${snEncrypt?.isNotEmpty() == true && !clientSecret.isNullOrBlank()}",
                    )
                } else {
                    markDisconnected()
                    callback.onDisconnected()
                }
            }

            override fun onError(code: Int, message: String) {
                markConnectionFailed(callback, code, message)
            }
        }

        val useAuth = snEncrypt?.isNotEmpty() == true && !clientSecret.isNullOrBlank()
        Log.d(
            TAG,
            "connect requested socket=$socketUuid mac=$macAddress auth=$useAuth snBytes=${snEncrypt?.size ?: 0} secretSet=${!clientSecret.isNullOrBlank()}",
        )

        if (useAuth && bluetoothAdapter?.isEnabled == true) {
            CxrSdkProxy.connectBluetooth(context, socketUuid, macAddress, managedCallback, snEncrypt, clientSecret)
        } else if (bluetoothAdapter?.isEnabled == true) {
            CxrSdkProxy.connectBluetooth(context, socketUuid, macAddress, managedCallback)
        } else {
            mainHandler.postDelayed({
                markConnected(macAddress)
                callback.onConnected()
            }, 2000)
        }
    }

    override fun disconnect() {
        if (currentConnectionState == RokidConstants.ConnectionState.DISCONNECTED) return
        currentConnectionState = RokidConstants.ConnectionState.DISCONNECTING
        _connectionStateFlow.value = currentConnectionState
        cancelConnectionTimeout()
        mainHandler.removeCallbacksAndMessages(null)

        try {
            CxrSdkProxy.deinitBluetooth()
        } catch (e: Exception) {
            Log.w(TAG, "Error during deinitBluetooth", e)
        }

        isBluetoothConnected = false
        connectedDeviceInfo = null
        currentConnectionState = RokidConstants.ConnectionState.DISCONNECTED
        _connectionStateFlow.value = currentConnectionState
        _runtimeStatusFlow.value = RokidRuntimeStatus()
        _sceneStatusFlow.value = RokidSceneStatus()
        connectionCallback?.onDisconnected()
        Log.d(TAG, "Disconnected from Rokid device")
    }

    override fun getGlassInfo(): RokidDeviceInfo? {
        return if (isBluetoothConnected) {
            val info = CxrSdkProxy.getGlassInfo()
            if (info != null) {
                connectedDeviceInfo = info
            }
            info ?: connectedDeviceInfo
        } else {
            connectedDeviceInfo
        }
    }

    override fun sendData(topic: String, data: ByteArray): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "Sending data to topic: $topic, size: ${data.size}")
        return CxrSdkProxy.sendData(topic, data)
    }

    override fun setDataCallback(callback: RokidDataCallback?) {
        dataCallback = callback
        if (callback != null) {
            CxrSdkProxy.setDataCallback(callback)
        }
    }

    override fun setCameraCallback(callback: RokidCameraCallback?) {
        cameraCallback = callback
    }

    override fun openCamera(): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "Opening Rokid camera")
        return CxrSdkProxy.openCamera(cameraCallback)
    }

    override fun closeCamera(): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "Closing Rokid camera")
        val success = CxrSdkProxy.closeCamera()
        if (success) {
            cameraCallback?.onCameraClosed()
        }
        return success
    }

    override fun setAudioCallback(callback: RokidAudioCallback?) {
        audioCallback = callback
    }

    override fun openAudioRecord(codec: Int, mode: Int, intent: String, denoiseMode: Int): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "Opening audio record on Rokid device")
        audioCallback?.let { CxrSdkProxy.setAudioStreamListener(it) }
        return CxrSdkProxy.openAudioRecord(codec, mode, intent, denoiseMode)
    }

    override fun closeAudioRecord(): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "Closing audio record")
        return CxrSdkProxy.closeAudioRecord()
    }

    override fun startAiChat(prompt: String, callback: (String) -> Unit) {
        Log.d(TAG, "Starting AI chat with prompt: $prompt")
        mainHandler.postDelayed({
            callback("[Mock AI Response] This is a simulated response from Rokid AI assistant for: $prompt")
        }, 1000)
    }

    override fun stopAiChat() {
        Log.d(TAG, "Stopping AI chat")
    }

    // ===== CXR Scene / Content API implementations =====

    override fun pushTextToGlasses(text: String): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "pushTextToGlasses: ${text.take(50)}")
        val caps = Caps().apply {
            write("TEXT_UPDATE")
            write(text)
        }
        return CxrSdkProxy.sendCustomCmd(RokidConstants.Channel.GLASS_TEXT, caps)
    }

    override fun sendCustomCmd(channel: String, caps: Caps): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendCustomCmd channel=$channel, caps.size=${caps.size()}")
        return CxrSdkProxy.sendCustomCmd(channel, caps)
    }

    override fun sendGlobalToast(type: Int, text: String, overwrite: Boolean): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendGlobalToast type=$type text=${text.take(50)}")
        return CxrSdkProxy.sendGlobalToast(type, text, overwrite)
    }

    override fun sendGlobalMessage(type: Int, text: String, overwrite: Boolean): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendGlobalMessage type=$type text=${text.take(50)}")
        return CxrSdkProxy.sendGlobalMessage(type, text, overwrite)
    }

    override fun sendGlobalTts(text: String): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendGlobalTts: ${text.take(50)}")
        return CxrSdkProxy.sendGlobalTts(text)
    }

    override fun controlScene(sceneType: String, enable: Boolean): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "controlScene scene=$sceneType enable=$enable")
        return CxrSdkProxy.controlScene(sceneType, enable)
    }

    override fun configWordTipsText(
        fontSize: Float,
        lineSpacing: Float,
        color: String,
        bgAlpha: Int,
        textAlign: Int,
        maxLine: Int,
        speed: Int,
    ): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "configWordTipsText fontSize=$fontSize color=$color")
        return CxrSdkProxy.configWordTipsText(fontSize, lineSpacing, color, bgAlpha, textAlign, maxLine, speed)
    }

    override fun sendWordTipsContent(text: String): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendWordTipsContent: ${text.take(50)}")
        return CxrSdkProxy.sendWordTipsContent(text)
    }

    override fun setTranslationLanguage(sourceLang: String, targetLang: String, model: String): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "setTranslationLanguage $sourceLang → $targetLang model=$model")
        return CxrSdkProxy.setTranslationLanguage(sourceLang, targetLang, model)
    }

    override fun sendTranslationContent(
        srcLangIndex: Int,
        dstLangIndex: Int,
        isAsrSide: Boolean,
        isFinal: Boolean,
        text: String,
    ): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendTranslationContent: ${text.take(50)}")
        return CxrSdkProxy.sendTranslationContent(srcLangIndex, dstLangIndex, isAsrSide, isFinal, text)
    }

    override fun notifyAiStart(): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "notifyAiStart")
        return CxrSdkProxy.notifyAiStart()
    }

    override fun sendAsrContent(text: String): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendAsrContent: ${text.take(50)}")
        return CxrSdkProxy.sendAsrContent(text)
    }

    override fun notifyAsrEnd(): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "notifyAsrEnd")
        return CxrSdkProxy.notifyAsrEnd()
    }

    override fun sendTtsContent(text: String): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "sendTtsContent: ${text.take(50)}")
        return CxrSdkProxy.sendTtsContent(text)
    }

    override fun notifyTtsAudioFinished(success: Boolean): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "notifyTtsAudioFinished success=$success")
        return CxrSdkProxy.notifyTtsAudioFinished(success)
    }

    override fun takeGlassPhoto(
        width: Int,
        height: Int,
        quality: Int,
        callback: RokidPhotoResultCallback?,
    ): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "takeGlassPhoto ${width}x${height} quality=$quality")
        return CxrSdkProxy.takeGlassPhoto(width, height, quality, callback)
    }

    override fun setCustomCmdListener(listener: RokidCustomCmdListener?) {
        customCmdListener = listener
        if (isBluetoothConnected) {
            CxrSdkProxy.setCustomCmdListener(listener)
        }
        Log.d(TAG, "setCustomCmdListener: ${if (listener != null) "registered" else "cleared"}")
    }

    // ===== Additional methods used by UI screens (not on RokidApi interface) =====

    fun setAiEventListener(listener: RokidAiEventListener?) {
        aiEventListener = listener
        if (isBluetoothConnected) {
            CxrSdkProxy.setAiEventListener(listener)
        }
        Log.d(TAG, "setAiEventListener: ${if (listener != null) "registered" else "cleared"}")
    }

    fun setGlassesEventListener(listener: RokidGlassesEventListener?) {
        glassesEventListener = listener
        if (isBluetoothConnected) {
            CxrSdkProxy.setGlassesEventListener(listener)
        }
        Log.d(TAG, "setGlassesEventListener: ${if (listener != null) "registered" else "cleared"}")
    }

    fun setGlassBrightness(value: Int): Boolean {
        if (!isBluetoothConnected) return false
        val safeValue = value.coerceIn(0, 100)
        Log.d(TAG, "setGlassBrightness: $safeValue")
        val success = CxrSdkProxy.setGlassBrightness(safeValue)
        if (success) {
            _runtimeStatusFlow.value = _runtimeStatusFlow.value.copy(brightness = safeValue)
        }
        return success
    }

    fun setGlassVolume(value: Int): Boolean {
        if (!isBluetoothConnected) return false
        val safeValue = value.coerceIn(0, 100)
        Log.d(TAG, "setGlassVolume: $safeValue")
        val success = CxrSdkProxy.setGlassVolume(safeValue)
        if (success) {
            _runtimeStatusFlow.value = _runtimeStatusFlow.value.copy(volume = safeValue)
        }
        return success
    }

    fun syncGlassTime(): Boolean {
        if (!isBluetoothConnected) return false
        Log.d(TAG, "syncGlassTime")
        return CxrSdkProxy.syncGlassTime()
    }

    /** Convenience overload without callback. */
    fun takeGlassPhoto(width: Int = 1920, height: Int = 1080, quality: Int = 80): Boolean {
        return takeGlassPhoto(width, height, quality, null)
    }

    fun openGlassCamera(): Boolean = openCamera()

    fun closeGlassCamera(): Boolean = closeCamera()

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun getEnableBluetoothIntent(): Intent? {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    fun openSystemBluetoothSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open system Bluetooth settings", e)
            false
        }
    }

    fun requestSystemBluetoothPairing(context: Context, device: RokidDevice): Boolean {
        if (!hasBluetoothPermissions(context)) {
            Log.w(TAG, "System pairing skipped: missing Bluetooth permissions")
            return false
        }

        val adapter = bluetoothAdapter
        if (adapter?.isEnabled != true) {
            return openSystemBluetoothSettings(context)
        }

        val bluetoothDevice = findBluetoothDevice(device.macAddress) ?: return openSystemBluetoothSettings(context)
        return try {
            when {
                bluetoothDevice.isConnectedCompat() -> true
                bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED -> {
                    val requested = bluetoothDevice.createBond()
                    if (!requested) openSystemBluetoothSettings(context)
                    requested
                }
                else -> openSystemBluetoothSettings(context)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "System pairing failed: missing BLUETOOTH_CONNECT permission", e)
            false
        }
    }

    fun isSystemBluetoothConnected(macAddress: String? = null): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (adapter.isEnabled != true) return false
        return try {
            adapter.bondedDevices?.any { device ->
                val macMatches = macAddress.isNullOrBlank() ||
                    device.address.equals(macAddress, ignoreCase = true)
                macMatches && device.isConnectedCompat()
            } == true
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read system Bluetooth connection state", e)
            false
        }
    }

    // ===== Private helpers =====

    private fun hasBluetoothPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val connectGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return scanGranted && connectGranted
        }
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted
    }

    private fun findBluetoothDevice(macAddress: String): BluetoothDevice? {
        return try {
            bluetoothAdapter?.getRemoteDevice(macAddress)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun BluetoothDevice.isConnectedCompat(): Boolean {
        return runCatching {
            javaClass.getMethod("isConnected").invoke(this) == true
        }.getOrDefault(false)
    }

    private fun markConnected(macAddress: String?) {
        cancelConnectionTimeout()
        isBluetoothConnected = true
        currentConnectionState = RokidConstants.ConnectionState.CONNECTED
        _connectionStateFlow.value = currentConnectionState
        connectedDeviceInfo = connectedDeviceInfo ?: RokidDeviceInfo(
            glassesType = RokidConstants.GlassType.ROKID_GLASSES,
            hardwareVersion = "unknown",
            softwareVersion = "unknown",
            serialNumber = macAddress?.takeLast(6)?.let { "RK$it" } ?: "unknown",
            batteryLevel = _runtimeStatusFlow.value.batteryLevel.takeIf { it >= 0 } ?: 85,
            isCharging = false,
        )
        _runtimeStatusFlow.value = _runtimeStatusFlow.value.copy(
            batteryLevel = connectedDeviceInfo?.batteryLevel ?: 85,
            isCharging = connectedDeviceInfo?.isCharging == true,
            volume = _runtimeStatusFlow.value.volume.takeIf { it >= 0 } ?: 50,
            brightness = _runtimeStatusFlow.value.brightness.takeIf { it >= 0 } ?: 60,
            screenOn = true,
            wearingStatus = "on",
        )
        customCmdListener?.let { CxrSdkProxy.setCustomCmdListener(it) }
        aiEventListener?.let { CxrSdkProxy.setAiEventListener(it) }
        glassesEventListener?.let { CxrSdkProxy.setGlassesEventListener(it) }
    }

    private fun markDisconnected() {
        cancelConnectionTimeout()
        isBluetoothConnected = false
        connectedDeviceInfo = null
        currentConnectionState = RokidConstants.ConnectionState.DISCONNECTED
        _connectionStateFlow.value = currentConnectionState
        _runtimeStatusFlow.value = RokidRuntimeStatus()
        _sceneStatusFlow.value = RokidSceneStatus()
    }

    private fun markConnectionFailed(callback: RokidConnectionCallback?, code: Int, message: String) {
        cancelConnectionTimeout()
        Log.e(TAG, "Connection failed code=$code message=$message")
        currentConnectionState = RokidConstants.ConnectionState.FAILED
        _connectionStateFlow.value = currentConnectionState
        callback?.onError(code, message)
    }

    private fun isConnectionPending(): Boolean =
        currentConnectionState == RokidConstants.ConnectionState.PAIRING ||
            currentConnectionState == RokidConstants.ConnectionState.CONNECTING

    private fun scheduleConnectionTimeout(
        delayMs: Long,
        callback: RokidConnectionCallback,
        message: String,
    ) {
        cancelConnectionTimeout()
        val attemptId = ++connectionAttemptId
        val runnable = Runnable {
            val pending = currentConnectionState == RokidConstants.ConnectionState.PAIRING ||
                currentConnectionState == RokidConstants.ConnectionState.CONNECTING
            if (connectionAttemptId == attemptId && pending) {
                Log.w(TAG, message)
                markConnectionFailed(
                    callback,
                    RokidConstants.ScanError.CONNECTION_TIMEOUT,
                    message,
                )
            }
        }
        connectionTimeoutRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelConnectionTimeout() {
        connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        connectionTimeoutRunnable = null
        connectionAttemptId++
    }

    fun getCurrentState(): String = currentConnectionState

    fun getDiscoveredDevices(): List<RokidDevice> = discoveredDevices.toList()

    fun getSavedConnectionInfo(): Pair<String?, String?> {
        return currentSocketUuid to currentMacAddress
    }
}
