package com.nanackations2.papaya.rokid

/**
 * 所有与 Rokid CXR-M SDK 相关的常量。
 *
 * 参考：
 * - CXR-M SDK 1.0.8: com.rokid.cxr.client.extend.CxrApi
 * - ValueUtil.CxrSceneType / CxrStatus / CxrStreamType / CxrBluetoothErrorCode
 */
object RokidConstants {

    // ===== SDK 元信息 =====
    const val ROKID_SDK_VERSION = "1.0.8"

    /**
     * Rokid 眼镜 BLE 广播中常见的 128-bit 服务 UUID。
     * 注：不同型号/固件可能会变，首次扫描推荐置空或仅按名称前缀过滤。
     */
    const val ROKID_SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
    const val DEVICE_NAME_PREFIX = "Rokid"

    // ===== 扫描/重连 =====
    const val SCAN_TIMEOUT_MS = 15_000L
    const val PAIR_TIMEOUT_MS = 15_000L
    const val CONNECT_TIMEOUT_MS = 45_000L
    const val RECONNECT_INTERVAL_MS = 3_000L

    // ===== 连接状态机 =====
    object ConnectionState {
        const val DISCONNECTED = "disconnected"
        const val SCANNING = "scanning"
        const val PAIRING = "pairing"
        const val CONNECTING = "connecting"
        const val CONNECTED = "connected"
        const val DISCONNECTING = "disconnecting"
        const val FAILED = "failed"
    }

    // ===== 眼镜型号 =====
    object GlassType {
        const val UNKNOWN = 0
        const val ROKID_GLASSES = 1
        const val ROKID_AR_PRO = 2
        const val ROKID_MAX = 3
        const val ROKID_AR_LITE = 4
        const val ROKID_AR_STUDIO = 5
    }

    /**
     * 与 ValueUtil.CxrSceneType 中的 sceneId 对齐。
     * 注意：这里的字符串会在 [CxrSdkProxy.controlScene] 内部映射到 SDK 枚举。
     */
    object SceneType {
        const val AI_CHAT = "ai_chat"
        const val TRANSLATION = "translation"
        const val AUDIO_RECORD = "audio_record"
        const val VIDEO_RECORD = "video_record"
        const val TELEPROMPTER = "teleprompter"  // => CxrSceneType.WORD_TIPS
        const val NAVIGATION = "navigation"
    }

    /**
     * CXR 自定义通道（仅用于应用内业务约定，SDK 会原样下发给眼镜端的 App）。
     * 真实产品里通常由客户端与眼镜端 App 各自约定。
     */
    object Channel {
        const val GLASS_TEXT = "glass_text"
        const val SCENE_CTRL = "scene_ctrl"
        const val CAPTURE_CTRL = "capture_ctrl"
        const val AI_RESULT = "ai_result"
        const val GLASS_EVENT = "glass_event"
        const val CUSTOM_DATA = "custom_data"
        const val AUDIO_STREAM = "audio_stream"
        const val NOTIFICATION = "notification"
        const val TELEPROMPTER = "teleprompter"

        // ===== Papaya 双向通信协议通道 =====
        /** 手机→眼镜：心跳/Ping */
        const val PAPAYA_PING = "papaya_ping"
        /** 眼镜→手机：心跳/Pong */
        const val PAPAYA_PONG = "papaya_pong"
        /** 双向：状态同步请求/响应 */
        const val PAPAYA_STATUS_SYNC = "papaya_status_sync"
        /** 手机→眼镜：控制命令 */
        const val PAPAYA_CMD = "papaya_cmd"
        /** 眼镜→手机：事件上报 */
        const val PAPAYA_EVENT = "papaya_event"
        /** 双向：聊天消息 */
        const val PAPAYA_CHAT = "papaya_chat"
        /** 手机→眼镜：导航指令 */
        const val PAPAYA_NAV = "papaya_nav"
        /** 手机→眼镜：通知推送 */
        const val PAPAYA_NOTIFY = "papaya_notify"
    }

    /**
     * Papaya 双向通信子命令（写入 Caps 的第一个字段作为子协议标识）
     */
    object BidiCmd {
        // 状态同步
        const val REQUEST_STATUS = "REQ_STATUS"
        const val RESPONSE_STATUS = "RSP_STATUS"

        // 轻量控制命令（手机→眼镜）。物理按键、拍照、录像和唤醒词由 Rokid 系统处理。
        const val SET_BRIGHTNESS = "SET_BRIGHTNESS"
        const val SET_VOLUME = "SET_VOLUME"
        const val SHOW_TOAST = "SHOW_TOAST"
        const val SHOW_CARD = "SHOW_CARD"
        const val DISMISS_CARD = "DISMISS_CARD"

        // 状态上报（眼镜→手机）
        const val BATTERY_UPDATE = "BATTERY_UPDATE"
        const val APP_STATE_CHANGED = "APP_STATE_CHANGED"
        const val SENSOR_DATA = "SENSOR_DATA"

        // 聊天消息
        const val CHAT_TEXT = "CHAT_TEXT"
        const val CHAT_IMAGE = "CHAT_IMAGE"
        const val CHAT_VOICE = "CHAT_VOICE"
    }

    /** CxrApi.sendGlobalMsgContent / sendGlobalToastContent 的类型枚举。 */
    object GlobalMsgType {
        const val INFO = 0
        const val SUCCESS = 1
        const val WARN = 2
        const val ERROR = 3
    }

    /** CxrApi.openAudioRecord 的 codec 参数。 */
    object AudioCodec {
        const val PCM = 0
        const val OPUS = 1
        const val AAC = 2
    }

    /** CxrApi.openAudioRecord 的 denoiseMode 参数。 */
    object AudioDenoise {
        const val OFF = 0
        const val LOW = 1
        const val HIGH = 2
    }

    /** 扫描错误码（Papaya 内部自定义，方便 UI 层辨识）。 */
    object ScanError {
        const val NO_CONTEXT = -1
        const val NO_PERMISSION = -100
        const val SECURITY_EXCEPTION = -101
        const val BLUETOOTH_OFF = -102
        const val ADAPTER_UNAVAILABLE = -103
        const val CONNECTION_TIMEOUT = -104
        const val CONNECTION_INTERRUPTED = -105
    }

    /**
     * 将 CXR 蓝牙错误码转换为用户可读的中文提示。
     * 对应 ValueUtil.CxrBluetoothErrorCode.getErrorCode() 的返回值。
     */
    fun bluetoothErrorMessage(code: Int): String = when {
        code == ScanError.CONNECTION_TIMEOUT -> "连接超时，请确认眼镜保持唤醒并靠近手机"
        code == ScanError.CONNECTION_INTERRUPTED -> "连接过程中被 SDK 断开"
        kotlin.math.abs(code) == 0 -> "成功"
        kotlin.math.abs(code) == 1 -> "参数非法"
        kotlin.math.abs(code) == 2 -> "BLE 连接失败，请确认眼镜处于配对模式"
        kotlin.math.abs(code) == 3 -> "Socket 连接失败：BLE 已拿到连接信息，但手机和眼镜的 socket 通道没有建起来"
        kotlin.math.abs(code) == 4 -> "SN 校验失败，可能是配对参数不匹配"
        else -> "未知错误 ($code)"
    }

    fun glassTypeName(type: Int): String = when (type) {
        GlassType.ROKID_GLASSES -> "Rokid Glasses"
        GlassType.ROKID_AR_PRO -> "Rokid AR Pro"
        GlassType.ROKID_MAX -> "Rokid Max"
        GlassType.ROKID_AR_LITE -> "Rokid AR Lite"
        GlassType.ROKID_AR_STUDIO -> "Rokid AR Studio"
        else -> "Unknown"
    }
}
