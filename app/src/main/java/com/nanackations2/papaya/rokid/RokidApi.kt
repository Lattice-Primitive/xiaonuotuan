package com.nanackations2.papaya.rokid

import android.content.Context
import com.rokid.cxr.Caps

interface RokidConnectionCallback {
    fun onConnectionInfo(socketUuid: String?, macAddress: String?, rokidAccount: String?, glassesType: Int)
    fun onConnected()
    fun onConnecting()
    fun onDisconnected()
    fun onError(code: Int, message: String)
}

interface RokidScanCallback {
    fun onDeviceFound(device: RokidDevice)
    fun onScanStarted()
    fun onScanStopped()
    fun onScanFailed(errorCode: Int)
}

interface RokidDataCallback {
    fun onMessageReceived(topic: String, data: ByteArray)
    fun onDataChannelOpened(channelId: String)
    fun onDataChannelClosed(channelId: String)
}

interface RokidCameraCallback {
    fun onCameraOpened()
    fun onCameraClosed()
    fun onCameraError()
    fun onCameraFrame(data: ByteArray, timestamp: Long)
}

interface RokidAudioCallback {
    fun onAudioStreamStarted(streamId: Int)
    fun onAudioData(streamId: Int, data: ByteArray, offset: Int, size: Int)
    fun onAudioStreamFinished(streamId: Int)
}

/** Callback for receiving photos taken by the glasses. */
interface RokidPhotoResultCallback {
    fun onPhotoResult(statusOk: Boolean, data: ByteArray)
}

/** Listener for custom commands received from the glasses. */
interface RokidCustomCmdListener {
    fun onCustomCmd(channel: String, args: Caps?)
}

/** Listener for AI-related key events from the glasses. */
interface RokidAiEventListener {
    fun onAiKeyDown() {}
    fun onAiKeyUp() {}
    fun onAiExit() {}
}

/** Listener for generic glasses events. */
interface RokidGlassesEventListener {
    fun onGlassesEvent(event: String, data: String)
}

interface RokidApi {
    val isBluetoothConnected: Boolean
    val isInitialized: Boolean
    val connectedDeviceInfo: RokidDeviceInfo?

    fun initialize(context: Context)
    fun release()

    fun startScan(callback: RokidScanCallback)
    fun stopScan()

    fun pairDevice(context: Context, device: RokidDevice, callback: RokidConnectionCallback)
    fun connect(
        context: Context,
        socketUuid: String,
        macAddress: String,
        callback: RokidConnectionCallback,
        snEncrypt: ByteArray? = null,
        clientSecret: String? = null,
    )
    fun disconnect()

    fun getGlassInfo(): RokidDeviceInfo?

    fun sendData(topic: String, data: ByteArray): Boolean
    fun setDataCallback(callback: RokidDataCallback?)

    fun setCameraCallback(callback: RokidCameraCallback?)
    fun openCamera(): Boolean
    fun closeCamera(): Boolean

    fun setAudioCallback(callback: RokidAudioCallback?)
    fun openAudioRecord(codec: Int = 0, mode: Int = 0, intent: String = "default", denoiseMode: Int = 2): Boolean
    fun closeAudioRecord(): Boolean

    fun startAiChat(prompt: String, callback: (String) -> Unit)
    fun stopAiChat()

    // ===== CXR Scene / Content APIs (used by CxrSBridge) =====

    /** Push a text string to the glasses' text display channel. */
    fun pushTextToGlasses(text: String): Boolean

    /** Send a custom command via a named channel with Caps payload. */
    fun sendCustomCmd(channel: String, caps: Caps): Boolean

    /** Show a global toast on the glasses. */
    fun sendGlobalToast(type: Int, text: String, overwrite: Boolean): Boolean

    /** Show a global message (notification-style) on the glasses. */
    fun sendGlobalMessage(type: Int, text: String, overwrite: Boolean): Boolean

    /** Speak text via glasses TTS. */
    fun sendGlobalTts(text: String): Boolean

    /** Control (start/stop) a scene on the glasses. */
    fun controlScene(sceneType: String, enable: Boolean): Boolean

    /** Configure word-tips (teleprompter) text appearance. */
    fun configWordTipsText(
        fontSize: Float = 28f,
        lineSpacing: Float = 1.4f,
        color: String = "#FFFFFF",
        bgAlpha: Int = 0,
        textAlign: Int = 0,
        maxLine: Int = 3,
        speed: Int = 50,
    ): Boolean

    /** Push content to the word-tips (teleprompter) scene. */
    fun sendWordTipsContent(text: String): Boolean

    /** Set the translation source/target language. */
    fun setTranslationLanguage(sourceLang: String, targetLang: String, model: String): Boolean

    /** Push translation content to the glasses. */
    fun sendTranslationContent(
        srcLangIndex: Int,
        dstLangIndex: Int,
        isAsrSide: Boolean,
        isFinal: Boolean,
        text: String,
    ): Boolean

    /** Notify the glasses that an AI turn is starting. */
    fun notifyAiStart(): Boolean

    /** Push ASR (user speech) content to the glasses. */
    fun sendAsrContent(text: String): Boolean

    /** Notify the glasses that ASR has ended. */
    fun notifyAsrEnd(): Boolean

    /** Push TTS (AI reply) content to the glasses. */
    fun sendTtsContent(text: String): Boolean

    /** Notify the glasses that TTS playback has finished. */
    fun notifyTtsAudioFinished(success: Boolean): Boolean

    /** Take a photo using the glasses camera. */
    fun takeGlassPhoto(
        width: Int = 1920,
        height: Int = 1080,
        quality: Int = 80,
        callback: RokidPhotoResultCallback? = null,
    ): Boolean

    /** Register a listener for custom commands from the glasses. */
    fun setCustomCmdListener(listener: RokidCustomCmdListener?)
}
