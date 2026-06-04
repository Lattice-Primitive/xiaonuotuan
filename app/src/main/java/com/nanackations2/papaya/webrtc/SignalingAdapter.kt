package com.nanackations2.papaya.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * 信令适配层：把 WebRTC 协议无关的"发送本地/接收远端 SDP·ICE"抽象出来。
 * 不同协议（WebSocket 自研 / WHIP / WHEP / SRS HTTP 等）各实现一份。
 */
interface SignalingAdapter {
    fun onRemoteSdp(handler: (SessionDescription) -> Unit)
    fun onRemoteIceCandidate(handler: (IceCandidate) -> Unit)
    fun sendLocalSdp(sdp: SessionDescription)
    fun sendLocalIceCandidate(candidate: IceCandidate)
    fun connect(roomId: String)
    fun disconnect()
}

/**
 * 支持的推流/拉流协议。
 *
 * - [WEBSOCKET]：自研 WebSocket 信令 + P2P（默认，支持 VIEW/PUSH/BIDI）
 * - [WHIP]：WebRTC-HTTP Ingest Protocol（推流到 SRS / MediaMTX / OvenMediaEngine / Janus）
 * - [WHEP]：WebRTC-HTTP Egress Protocol（从 SRS / MediaMTX 拉流观看）
 */
enum class StreamProtocol(val displayName: String) {
    WEBSOCKET("WebSocket"),
    WHIP("WHIP 推流"),
    WHEP("WHEP 观看"),
}
