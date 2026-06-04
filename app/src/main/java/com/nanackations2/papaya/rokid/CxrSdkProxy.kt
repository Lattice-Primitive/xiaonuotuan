package com.nanackations2.papaya.rokid

import android.content.Context
import android.util.Log
import com.rokid.cxr.Caps
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.BluetoothStatusCallback
import com.rokid.cxr.client.extend.callbacks.GlassInfoResultCallback
import com.rokid.cxr.client.extend.callbacks.PhotoResultCallback
import com.rokid.cxr.client.extend.listeners.AudioStreamListener
import com.rokid.cxr.client.extend.listeners.AiEventListener as CxrAiEventListener
import com.rokid.cxr.client.extend.listeners.CustomCmdListener
import com.rokid.cxr.client.extend.listeners.GlassStatusUpdateListener
import com.rokid.cxr.client.extend.listeners.MediaStreamListener
import com.rokid.cxr.client.extend.listeners.ScreenStatusUpdateListener
import com.rokid.cxr.client.utils.ValueUtil.CxrBluetoothErrorCode
import com.rokid.cxr.client.utils.ValueUtil.CxrSceneType
import com.rokid.cxr.client.utils.ValueUtil.CxrStatus

/**
 * Proxy to the actual CXR-M SDK.
 *
 * This class wraps the real Rokid CXR-M SDK classes so that the rest of
 * the codebase does not directly depend on the proprietary SDK.
 *
 * When the real CXR-M SDK is available via the Rokid Maven repository,
 * uncomment the implementation blocks below to use real SDK calls.
 *
 * Rokid Maven Repo: https://maven.rokid.com/repository/maven-public/
 * SDK Artifact: com.rokid.cxr:client-m:1.0.8
 *
 * Required credentials (add to local.properties, NOT committed to git):
 *   rokid.clientId=your_client_id
 *   rokid.clientSecret=your_client_secret
 *   rokid.accessKey=your_access_key
 */
object CxrSdkProxy {
    private const val TAG = "CxrSdkProxy"

    private var cxrApi: CxrApi? = null

    private fun api(): CxrApi =
        cxrApi ?: CxrApi.getInstance().also { cxrApi = it }

    private fun isSuccessful(status: CxrStatus?): Boolean =
        status == CxrStatus.REQUEST_SUCCEED ||
            status == CxrStatus.REQUEST_WAITING ||
            status == CxrStatus.RESPONSE_SUCCEED ||
            status == CxrStatus.BLUETOOTH_AVAILABLE ||
            status == CxrStatus.BLUETOOTH_INIT

    private fun bridgeBluetoothCallback(
        callback: RokidConnectionCallback,
        operation: String,
        socketUuid: String? = null,
        macAddress: String? = null,
        authUsed: Boolean = false,
    ): BluetoothStatusCallback {
        return object : BluetoothStatusCallback {
            override fun onConnectionInfo(
                socketUuid: String?,
                macAddress: String?,
                rokidAccount: String?,
                glassesType: Int,
            ) {
                Log.d(
                    TAG,
                    "$operation onConnectionInfo socket=${socketUuid.orEmpty()} mac=${macAddress.maskMac()} type=$glassesType account=${rokidAccount.maskId()}",
                )
                callback.onConnectionInfo(socketUuid, macAddress, rokidAccount, glassesType)
            }

            override fun onConnected() {
                Log.d(TAG, "$operation onConnected socket=${socketUuid.orEmpty()} mac=${macAddress.maskMac()} auth=$authUsed")
                callback.onConnected()
            }

            override fun onInActiveConnected(socketUuid: String?, macAddress: String?) {
                Log.d(TAG, "$operation onInActiveConnected socket=${socketUuid.orEmpty()} mac=${macAddress.maskMac()} auth=$authUsed")
                callback.onConnectionInfo(socketUuid, macAddress, null, RokidConstants.GlassType.UNKNOWN)
                callback.onConnected()
            }

            override fun onDisconnected() {
                Log.w(TAG, "$operation onDisconnected socket=${socketUuid.orEmpty()} mac=${macAddress.maskMac()} auth=$authUsed")
                callback.onDisconnected()
            }

            override fun onFailed(error: CxrBluetoothErrorCode?) {
                val code = error?.errorCode ?: RokidConstants.ScanError.ADAPTER_UNAVAILABLE
                val sdkName = error?.name ?: "UNKNOWN"
                val detail = buildString {
                    append("SDK=$sdkName")
                    append(", op=$operation")
                    append(", socket=")
                    append(socketUuid?.takeIf { it.isNotBlank() } ?: "<empty>")
                    append(", mac=")
                    append(macAddress?.takeIf { it.isNotBlank() } ?: "<empty>")
                    append(", auth=")
                    append(authUsed)
                }
                Log.e(TAG, "$operation onFailed code=$code $detail")
                callback.onError(code, detail)
            }
        }
    }

    private fun String?.maskMac(): String =
        this?.takeIf { it.isNotBlank() }?.replace(Regex("^([0-9A-Fa-f]{2}:){4}"), "**:**:**:**:") ?: "<empty>"

    private fun String?.maskId(): String =
        this?.takeIf { it.isNotBlank() }?.let { value ->
            if (value.length <= 8) "***" else "${value.take(4)}...${value.takeLast(4)}"
        } ?: "<empty>"

    fun initBluetooth(
        context: Context,
        device: android.bluetooth.BluetoothDevice,
        callback: RokidConnectionCallback,
    ) {
        try {
            api().initBluetooth(context, device, bridgeBluetoothCallback(
                callback = callback,
                operation = "initBluetooth",
                macAddress = device.address,
            ))
            Log.d(TAG, "CXR initBluetooth called mac=${device.address.maskMac()}")
        } catch (e: Throwable) {
            Log.e(TAG, "CXR initBluetooth failed", e)
            callback.onError(RokidConstants.ScanError.ADAPTER_UNAVAILABLE, e.message ?: "initBluetooth failed")
        }
    }

    fun connectBluetooth(
        context: Context,
        socketUuid: String,
        macAddress: String,
        callback: RokidConnectionCallback,
    ) {
        try {
            api().connectBluetooth(
                context,
                socketUuid,
                macAddress,
                "Papaya",
                bridgeBluetoothCallback(
                    callback = callback,
                    operation = "connectBluetooth",
                    socketUuid = socketUuid,
                    macAddress = macAddress,
                    authUsed = false,
                ),
                null,
                null,
            )
            Log.d(TAG, "CXR connectBluetooth called socket=$socketUuid mac=${macAddress.maskMac()} auth=false")
        } catch (e: Throwable) {
            Log.e(TAG, "CXR connectBluetooth failed", e)
            callback.onError(RokidConstants.ScanError.ADAPTER_UNAVAILABLE, e.message ?: "connectBluetooth failed")
        }
    }

    fun connectBluetooth(
        context: Context,
        socketUuid: String,
        macAddress: String,
        callback: RokidConnectionCallback,
        snEncrypt: ByteArray,
        clientSecret: String,
    ) {
        try {
            api().connectBluetooth(
                context,
                socketUuid,
                macAddress,
                "Papaya",
                bridgeBluetoothCallback(
                    callback = callback,
                    operation = "connectBluetooth",
                    socketUuid = socketUuid,
                    macAddress = macAddress,
                    authUsed = true,
                ),
                snEncrypt,
                clientSecret,
            )
            Log.d(TAG, "CXR connectBluetooth called socket=$socketUuid mac=${macAddress.maskMac()} auth=true snBytes=${snEncrypt.size}")
        } catch (e: Throwable) {
            Log.e(TAG, "CXR connectBluetooth with auth failed", e)
            callback.onError(RokidConstants.ScanError.ADAPTER_UNAVAILABLE, e.message ?: "connectBluetooth failed")
        }
    }

    fun deinitBluetooth() {
        try {
            cxrApi?.deinitBluetooth()
            cxrApi = null
            Log.d(TAG, "CXR deinitBluetooth called")
        } catch (e: Throwable) {
            Log.w(TAG, "CXR deinitBluetooth failed", e)
        }
    }

    fun getGlassInfo(): RokidDeviceInfo? {
        return try {
            var result: RokidDeviceInfo? = null
            api().getGlassInfo(object : GlassInfoResultCallback {
                override fun onGlassInfoResult(status: CxrStatus?, info: com.rokid.cxr.client.extend.infos.GlassInfo?) {
                    Log.d(TAG, "CXR getGlassInfo status=$status info=$info")
                    if (info != null) {
                        result = RokidDeviceInfo(
                            glassesType = RokidConstants.GlassType.UNKNOWN,
                            hardwareVersion = info.devicePanel.orEmpty(),
                            softwareVersion = info.systemVersion.orEmpty(),
                            serialNumber = info.deviceId.orEmpty(),
                            batteryLevel = info.batteryLevel,
                            isCharging = info.isCharging,
                        )
                    }
                }
            })
            result
        } catch (e: Throwable) {
            Log.w(TAG, "CXR getGlassInfo failed", e)
            null
        }
    }

    fun sendData(topic: String, data: ByteArray): Boolean {
        val caps = Caps().apply {
            write("BINARY_DATA")
            writeBinary(data)
        }
        return sendCustomCmd(topic, caps)
    }

    fun setDataCallback(callback: RokidDataCallback) {
        Log.d(TAG, "setDataCallback registered in app layer: $callback")
    }

    fun openCamera(callback: RokidCameraCallback?): Boolean {
        return try {
            callback?.let {
                api().setMediaStreamListener(object : MediaStreamListener {
                    override fun onCameraOpened() {
                        it.onCameraOpened()
                    }

                    override fun onCameraClosed() {
                        it.onCameraClosed()
                    }

                    override fun onCameraError() {
                        it.onCameraError()
                    }

                    override fun onCameraFrame(data: ByteArray?, timestamp: Long) {
                        if (data != null) it.onCameraFrame(data, timestamp)
                    }
                })
            }
            val status = api().openGlassCamera(1280, 720, 75)
            Log.d(TAG, "CXR openGlassCamera status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.e(TAG, "CXR openCamera failed", e)
            callback?.onCameraError()
            false
        }
    }

    fun closeCamera(): Boolean {
        return try {
            val status = api().closeCameraVideo()
            Log.d(TAG, "CXR closeCameraVideo status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR closeCamera failed", e)
            false
        }
    }

    fun setAudioStreamListener(callback: RokidAudioCallback) {
        try {
            api().setAudioStreamListener(object : AudioStreamListener {
                override fun onStartAudioStream(streamId: Int, codec: Int, mode: Int, intent: String?) {
                    callback.onAudioStreamStarted(streamId)
                }

                override fun onAudioStream(streamId: Int, data: ByteArray?, offset: Int, size: Int) {
                    if (data != null) callback.onAudioData(streamId, data, offset, size)
                }

                override fun onAudioStreamFinish(streamId: Int) {
                    callback.onAudioStreamFinished(streamId)
                }
            })
            Log.d(TAG, "CXR setAudioStreamListener called")
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setAudioStreamListener failed", e)
        }
    }

    fun openAudioRecord(codec: Int, mode: Int, intent: String, denoiseMode: Int): Boolean {
        return try {
            val status = api().openAudioRecord(codec, mode, intent, denoiseMode)
            Log.d(TAG, "CXR openAudioRecord status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR openAudioRecord failed", e)
            false
        }
    }

    fun closeAudioRecord(): Boolean {
        return try {
            val status = api().closeAudioRecord("default")
            Log.d(TAG, "CXR closeAudioRecord status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR closeAudioRecord failed", e)
            false
        }
    }

    fun sendCustomCmd(channel: String, caps: Caps): Boolean {
        return try {
            val status = api().sendCustomCmd(channel, caps)
            Log.d(TAG, "CXR sendCustomCmd channel=$channel status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.e(TAG, "CXR sendCustomCmd failed channel=$channel", e)
            false
        }
    }

    fun setCustomCmdListener(listener: RokidCustomCmdListener?) {
        try {
            val sdkListener = listener?.let {
                CustomCmdListener { channel, caps ->
                    it.onCustomCmd(channel, caps)
                }
            }
            api().setCustomCmdListener(sdkListener)
            Log.d(TAG, "CXR setCustomCmdListener registered=${listener != null}")
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setCustomCmdListener failed", e)
        }
    }

    fun setAiEventListener(listener: RokidAiEventListener?) {
        try {
            val sdkListener = listener?.let {
                object : CxrAiEventListener {
                    override fun onAiKeyDown() {
                        it.onAiKeyDown()
                    }

                    override fun onAiKeyUp() {
                        it.onAiKeyUp()
                    }

                    override fun onAiExit() {
                        it.onAiExit()
                    }
                }
            }
            api().setAiEventListener(sdkListener)
            Log.d(TAG, "CXR setAiEventListener registered=${listener != null}")
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setAiEventListener failed", e)
        }
    }

    fun setGlassesEventListener(listener: RokidGlassesEventListener?) {
        try {
            api().setGlassStatusUpdateListener(listener?.let {
                object : GlassStatusUpdateListener {
                    override fun onWearingStatusUpdated(status: String?) {
                        it.onGlassesEvent("wearing_status", status.orEmpty())
                    }

                    override fun onGlassTempleStatusUpdated(status: String?) {
                        it.onGlassesEvent("temple_status", status.orEmpty())
                    }

                    override fun onGlassGlobalTtsStatusUpdated(status: String?) {
                        it.onGlassesEvent("global_tts_status", status.orEmpty())
                    }
                }
            })
            api().setScreenStatusUpdateListener(listener?.let {
                ScreenStatusUpdateListener { screenOn ->
                    it.onGlassesEvent("screen_status", screenOn.toString())
                }
            })
            Log.d(TAG, "CXR setGlassesEventListener registered=${listener != null}")
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setGlassesEventListener failed", e)
        }
    }

    fun setGlassBrightness(value: Int): Boolean {
        return try {
            val status = api().setGlassBrightness(value.coerceIn(0, 100))
            Log.d(TAG, "CXR setGlassBrightness status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setGlassBrightness failed", e)
            false
        }
    }

    fun setGlassVolume(value: Int): Boolean {
        return try {
            val status = api().setGlassVolume(value.coerceIn(0, 100))
            Log.d(TAG, "CXR setGlassVolume status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setGlassVolume failed", e)
            false
        }
    }

    fun syncGlassTime(): Boolean {
        return try {
            val status = api().setGlassTime()
            Log.d(TAG, "CXR setGlassTime status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setGlassTime failed", e)
            false
        }
    }

    fun sendGlobalToast(type: Int, text: String, overwrite: Boolean): Boolean {
        return try {
            val status = api().sendGlobalToastContent(type, text, overwrite)
            Log.d(TAG, "CXR sendGlobalToast status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendGlobalToast failed", e)
            false
        }
    }

    fun sendGlobalMessage(type: Int, text: String, overwrite: Boolean): Boolean {
        return try {
            val status = api().sendGlobalMsgContent(type, text, overwrite)
            Log.d(TAG, "CXR sendGlobalMessage status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendGlobalMessage failed", e)
            false
        }
    }

    fun sendGlobalTts(text: String): Boolean {
        return try {
            val status = api().sendGlobalTtsContent(text)
            Log.d(TAG, "CXR sendGlobalTts status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendGlobalTts failed", e)
            false
        }
    }

    fun controlScene(sceneType: String, enable: Boolean): Boolean {
        val cxrSceneType = when (sceneType) {
            RokidConstants.SceneType.AI_CHAT -> CxrSceneType.AI_CHAT
            RokidConstants.SceneType.TRANSLATION -> CxrSceneType.TRANSLATE
            RokidConstants.SceneType.AUDIO_RECORD -> CxrSceneType.AUDIO_RECORD
            RokidConstants.SceneType.VIDEO_RECORD -> CxrSceneType.VIDEO_RECORD
            RokidConstants.SceneType.TELEPROMPTER -> CxrSceneType.WORD_TIPS
            RokidConstants.SceneType.NAVIGATION -> CxrSceneType.NAVIGATION
            else -> return false
        }
        return try {
            val status = api().controlScene(cxrSceneType, enable, "")
            Log.d(TAG, "CXR controlScene scene=$sceneType enable=$enable status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR controlScene failed scene=$sceneType", e)
            false
        }
    }

    fun configWordTipsText(
        fontSize: Float,
        lineSpacing: Float,
        color: String,
        bgAlpha: Int,
        textAlign: Int,
        maxLine: Int,
        speed: Int,
    ): Boolean {
        return try {
            val status = api().configWordTipsText(fontSize, lineSpacing, color, bgAlpha, textAlign, maxLine, speed)
            Log.d(TAG, "CXR configWordTipsText status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR configWordTipsText failed", e)
            false
        }
    }

    fun sendWordTipsContent(text: String): Boolean {
        return try {
            val status = api().sendWordTipsAsrContent(text)
            Log.d(TAG, "CXR sendWordTipsContent status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendWordTipsContent failed", e)
            false
        }
    }

    fun takeGlassPhoto(
        width: Int,
        height: Int,
        quality: Int,
        callback: RokidPhotoResultCallback?,
    ): Boolean {
        return try {
            val status = api().takeGlassPhoto(width, height, quality, object : PhotoResultCallback {
                override fun onPhotoResult(status: CxrStatus?, data: ByteArray?) {
                    val ok = isSuccessful(status) && data != null && data.isNotEmpty()
                    callback?.onPhotoResult(ok, data ?: ByteArray(0))
                }
            })
            Log.d(TAG, "CXR takeGlassPhoto status=$status")
            isSuccessful(status)
        } catch (e: Throwable) {
            Log.e(TAG, "CXR takeGlassPhoto failed", e)
            callback?.onPhotoResult(false, ByteArray(0))
            false
        }
    }

    fun sendAsrContent(text: String): Boolean {
        return try {
            isSuccessful(api().sendAsrContent(text))
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendAsrContent failed", e)
            false
        }
    }

    fun notifyAiStart(): Boolean {
        return try {
            isSuccessful(api().notifyAiStart())
        } catch (e: Throwable) {
            Log.w(TAG, "CXR notifyAiStart failed", e)
            false
        }
    }

    fun notifyAsrEnd(): Boolean {
        return try {
            isSuccessful(api().notifyAsrEnd())
        } catch (e: Throwable) {
            Log.w(TAG, "CXR notifyAsrEnd failed", e)
            false
        }
    }

    fun sendTtsContent(text: String): Boolean {
        return try {
            isSuccessful(api().sendTtsContent(text))
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendTtsContent failed", e)
            false
        }
    }

    fun notifyTtsAudioFinished(success: Boolean): Boolean {
        return try {
            isSuccessful(api().notifyTtsAudioFinished(success))
        } catch (e: Throwable) {
            Log.w(TAG, "CXR notifyTtsAudioFinished failed", e)
            false
        }
    }

    fun setTranslationLanguage(sourceLang: String, targetLang: String, model: String): Boolean {
        return try {
            isSuccessful(api().setTranslationLanguage(sourceLang, targetLang, model))
        } catch (e: Throwable) {
            Log.w(TAG, "CXR setTranslationLanguage failed", e)
            false
        }
    }

    fun sendTranslationContent(
        srcLangIndex: Int,
        dstLangIndex: Int,
        isAsrSide: Boolean,
        isFinal: Boolean,
        text: String,
    ): Boolean {
        return try {
            isSuccessful(api().sendTranslationContent(srcLangIndex, dstLangIndex, isAsrSide, isFinal, text))
        } catch (e: Throwable) {
            Log.w(TAG, "CXR sendTranslationContent failed", e)
            false
        }
    }
}
