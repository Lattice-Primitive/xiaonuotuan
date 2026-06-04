package com.nanackations2.papaya.glass.protocol

/**
 * Papaya 眼镜↔手机 双向通信协议文档。
 *
 * 通信基于 Rokid CXR SDK 的自定义通道（CustomCmd），消息使用 [com.rokid.cxr.Caps] 序列化。
 *
 * ## Caps 消息格式
 * 每条消息的 Caps 至少包含 2 个字段：
 * - caps[0]: String — 子命令标识（SubCmd）
 * - caps[1]: String — 负载数据（Payload），通常为 JSON 或纯文本
 *
 * ## 通道定义
 *
 * | 通道名                  | 方向        | 用途                           |
 * |------------------------|------------|-------------------------------|
 * | papaya_ping            | 手机→眼镜   | 心跳 Ping / 状态请求            |
 * | papaya_pong            | 眼镜→手机   | 心跳 Pong / 状态响应            |
 * | papaya_status_sync     | 双向        | 完整状态同步                    |
 * | papaya_cmd             | 手机→眼镜   | 轻量控制命令（亮度/音量等）        |
 * | papaya_event           | 眼镜→手机   | 状态上报（电量等）                |
 * | papaya_chat            | 双向        | 聊天消息                        |
 * | papaya_nav             | 手机→眼镜   | 导航指令                        |
 * | papaya_notify          | 手机→眼镜   | 通知推送                        |
 *
 * ## 子命令列表
 *
 * ### 状态同步
 * - REQ_STATUS: 请求对方返回当前状态
 * - RSP_STATUS: 返回 JSON 格式的设备状态
 *
 * ### 控制命令（手机→眼镜）
 * - SET_BRIGHTNESS: payload = "0-100"
 * - SET_VOLUME: payload = "0-100"
 * - SHOW_TOAST: payload = "要显示的文本"
 * - SHOW_CARD: payload = JSON 卡片数据
 * - DISMISS_CARD: payload = ""
 *
 * ### 事件上报（眼镜→手机）
 * - BATTERY_UPDATE: payload = "电量百分比"
 * - APP_STATE_CHANGED: payload = JSON
 * - SENSOR_DATA: payload = JSON (加速度/陀螺仪)
 *
 * 眼镜系统保留的触控板、返回、拍照、录像和唤醒词交互由 YodaOS-Sprite 处理，
 * glass-app 不定义这些物理交互的替代语义。
 *
 * ### 聊天消息（双向）
 * - CHAT_TEXT: payload = 文本内容
 * - CHAT_IMAGE: payload = Base64 图片
 * - CHAT_VOICE: payload = Base64 音频
 *
 * ## RSP_STATUS JSON 格式
 * ```json
 * {
 *   "battery": 85,
 *   "charging": false,
 *   "brightness": 60,
 *   "volume": 40,
 *   "timestamp": 1714639200000,
 *   "app_version": "0.1.0",
 *   "device": "Rokid Max"
 * }
 * ```
 */
object GlassProtocol {
    const val PROTOCOL_VERSION = "1.0.0"
    const val MIN_CAPS_FIELDS = 2 // 每条消息至少 subCmd + payload
}
