package com.nanackations2.papaya.rokid

/**
 * 扫描阶段发现的 Rokid 设备基本信息。
 */
data class RokidDevice(
    val name: String,
    val macAddress: String,
    val rssi: Int = 0,
    val isPaired: Boolean = false,
)

/**
 * 连接后的眼镜详细信息（对应 CxrApi.getGlassInfo 的 GlassInfo）。
 */
data class RokidDeviceInfo(
    val glassesType: Int = RokidConstants.GlassType.UNKNOWN,
    val deviceName: String = "",
    val devicePanel: String = "",
    val hardwareVersion: String = "",
    val softwareVersion: String = "",
    val serialNumber: String = "",
    val deviceId: String = "",
    val deviceTypeId: String = "",
    val assistVersionName: String = "",
    val assistVersionCode: Long = 0L,
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val brightness: Int = -1,
    val volume: Int = -1,
    val wearingStatus: String = "",
)

/**
 * 场景状态（AI 聊天 / 翻译 / 音频录制 等）的快照。对应 CxrApi 的 SceneStatusInfo。
 */
data class RokidSceneStatus(
    val aiAssistRunning: Boolean = false,
    val aiChatRunning: Boolean = false,
    val translateRunning: Boolean = false,
    val wordTipsRunning: Boolean = false,
    val audioRecordRunning: Boolean = false,
    val videoRecordRunning: Boolean = false,
    val navigationRunning: Boolean = false,
    val phoneCallRunning: Boolean = false,
    val paymentRunning: Boolean = false,
    val otaRunning: Boolean = false,
    val notesRunning: Boolean = false,
    val brightnessRunning: Boolean = false,
    val customViewRunning: Boolean = false,
    val mixRecordRunning: Boolean = false,
    val hasDisplay: Boolean = false,
)

/**
 * 屏幕 / 电池 / 音量 / 亮度 / 穿戴状态的实时聚合。
 * 通过 CxrApi 的各个 *UpdateListener 聚合得到。
 */
data class RokidRuntimeStatus(
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val volume: Int = -1,
    val brightness: Int = -1,
    val screenOn: Boolean = true,
    val wearingStatus: String = "",
    val globalTtsStatus: String = "",
)
