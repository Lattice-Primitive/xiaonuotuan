package com.nanackations2.papaya.webrtc

import android.util.Log
import okhttp3.*
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SignalingClient(
    serverUrl: String = "wss://signal.papaya.example.com"
) : SignalingAdapter {

    private val serverUrl: String = serverUrl.ifBlank { "wss://signal.papaya.example.com" }

    companion object {
        private const val TAG = "SignalingClient"
        private const val NORMAL_CLOSURE = 1000
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var roomId: String = ""
    private var remoteSdpHandler: ((SessionDescription) -> Unit)? = null
    private var remoteIceCandidateHandler: ((IceCandidate) -> Unit)? = null
    private var shouldReconnect = false

    private val wsListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected to $serverUrl")
            joinRoom(roomId)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received: ${text.take(200)}")
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code $reason")
            webSocket.close(NORMAL_CLOSURE, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            if (shouldReconnect) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}", t)
            if (shouldReconnect) {
                scheduleReconnect()
            }
        }
    }

    override fun onRemoteSdp(handler: (SessionDescription) -> Unit) {
        remoteSdpHandler = handler
    }

    override fun onRemoteIceCandidate(handler: (IceCandidate) -> Unit) {
        remoteIceCandidateHandler = handler
    }

    override fun sendLocalSdp(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", sdp.type.canonicalForm())
            put("sdp", sdp.description)
            put("roomId", roomId)
        }
        send(json.toString())
    }

    override fun sendLocalIceCandidate(candidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("roomId", roomId)
        }
        send(json.toString())
    }

    override fun connect(roomId: String) {
        this.roomId = roomId
        shouldReconnect = true
        establishConnection()
    }

    override fun disconnect() {
        shouldReconnect = false
        leaveRoom(roomId)
        webSocket?.close(NORMAL_CLOSURE, "Client disconnect")
        webSocket = null
        remoteSdpHandler = null
        remoteIceCandidateHandler = null
    }

    private fun establishConnection() {
        val request = Request.Builder()
            .url(serverUrl)
            .build()
        webSocket = client.newWebSocket(request, wsListener)
    }

    private fun scheduleReconnect() {
        Log.d(TAG, "Scheduling reconnect in ${RECONNECT_DELAY_MS}ms")
        Thread {
            try {
                Thread.sleep(RECONNECT_DELAY_MS)
                if (shouldReconnect) {
                    Log.d(TAG, "Attempting reconnect")
                    establishConnection()
                }
            } catch (e: InterruptedException) {
                Log.w(TAG, "Reconnect interrupted")
            }
        }.start()
    }

    private fun joinRoom(roomId: String) {
        val json = JSONObject().apply {
            put("type", "join")
            put("roomId", roomId)
        }
        send(json.toString())
    }

    private fun leaveRoom(roomId: String) {
        if (webSocket == null) return
        val json = JSONObject().apply {
            put("type", "leave")
            put("roomId", roomId)
        }
        send(json.toString())
    }

    private fun send(message: String) {
        webSocket?.send(message) ?: Log.w(TAG, "WebSocket not connected, cannot send")
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "offer", "answer" -> {
                    val sdp = SessionDescription(
                        if (type == "offer") SessionDescription.Type.OFFER
                        else SessionDescription.Type.ANSWER,
                        json.getString("sdp")
                    )
                    remoteSdpHandler?.invoke(sdp)
                }
                "candidate" -> {
                    val candidate = IceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("candidate")
                    )
                    remoteIceCandidateHandler?.invoke(candidate)
                }
                "joined" -> {
                    Log.d(TAG, "Joined room: ${json.optString("roomId")}")
                }
                "left" -> {
                    Log.d(TAG, "Left room: ${json.optString("roomId")}")
                }
                "error" -> {
                    Log.e(TAG, "Server error: ${json.optString("message")}")
                }
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }
}
