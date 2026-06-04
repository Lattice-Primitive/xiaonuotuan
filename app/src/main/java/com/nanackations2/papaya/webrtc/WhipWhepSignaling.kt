package com.nanackations2.papaya.webrtc

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * WHIP (RFC 9725) 推流信令。
 *
 * 典型端点：
 *   - SRS: http://<host>:1985/rtc/v1/whip/?app=live&stream=livestream
 *   - MediaMTX: http://<host>:8889/<path>/whip
 *   - OvenMediaEngine: http://<host>:3333/<app>/<stream>?direction=whip
 *
 * 流程：
 *   1. Android 本地创建 Offer（带 sendonly video/audio）→ setLocalDescription
 *   2. POST SDP 到 endpoint，Content-Type: application/sdp
 *   3. 服务端返回 201，Body 是 Answer SDP，Location 头是 resource URL
 *   4. 收到后调 [onRemoteSdp] 给 PeerConnection setRemoteDescription
 *   5. 断开时 DELETE resource URL
 *
 * Trickle ICE（可选）通过 PATCH resource + application/trickle-ice-sdpfrag。
 * 为兼容更多实现，这里默认把 candidate 等 gather 完再一次性发，也实现了 PATCH 作为尽力而为。
 */
class WhipSignalingClient(
    endpoint: String,
    private val bearerToken: String? = null,
) : SignalingAdapter {

    companion object {
        private const val TAG = "WhipSignaling"
    }

    private val endpoint: String = endpoint.trim()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val resourceUrl = AtomicReference<String?>(null)

    private var remoteSdpHandler: ((SessionDescription) -> Unit)? = null
    private var remoteIceHandler: ((IceCandidate) -> Unit)? = null

    override fun onRemoteSdp(handler: (SessionDescription) -> Unit) { remoteSdpHandler = handler }
    override fun onRemoteIceCandidate(handler: (IceCandidate) -> Unit) { remoteIceHandler = handler }

    override fun sendLocalSdp(sdp: SessionDescription) {
        // WHIP 只需要上传 Offer 一次
        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(sdp.description.toRequestBody("application/sdp".toMediaType()))
            .addHeader("Accept", "application/sdp")
        bearerToken?.takeIf { it.isNotBlank() }?.let {
            reqBuilder.addHeader("Authorization", "Bearer $it")
        }

        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "WHIP POST failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "WHIP POST code=${it.code}")
                        return
                    }
                    resourceUrl.set(it.header("Location")?.let { loc ->
                        if (loc.startsWith("http")) loc else resolveRelative(endpoint, loc)
                    })
                    val answer = it.body?.string().orEmpty()
                    if (answer.isBlank()) {
                        Log.e(TAG, "WHIP empty answer")
                        return
                    }
                    remoteSdpHandler?.invoke(SessionDescription(SessionDescription.Type.ANSWER, answer))
                }
            }
        })
    }

    override fun sendLocalIceCandidate(candidate: IceCandidate) {
        // Trickle ICE：尽力而为；服务端不支持时忽略错误即可。
        val url = resourceUrl.get() ?: return
        val frag = buildIceFragment(candidate)
        val req = Request.Builder()
            .url(url)
            .patch(frag.toRequestBody("application/trickle-ice-sdpfrag".toMediaType()))
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Trickle ICE PATCH failed (ignored): ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    override fun connect(roomId: String) {
        // WHIP 没有"房间"概念，roomId 可由 endpoint query 表达；这里 no-op
        Log.d(TAG, "WHIP connect target=$endpoint room=$roomId")
    }

    override fun disconnect() {
        val url = resourceUrl.getAndSet(null) ?: return
        val req = Request.Builder().url(url).delete().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "WHIP DELETE failed", e)
            }
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    private fun buildIceFragment(c: IceCandidate): String {
        return buildString {
            append("a=candidate:${c.sdp}\r\n")
        }
    }

    private fun resolveRelative(base: String, path: String): String {
        return try {
            val baseUrl = java.net.URI(base)
            baseUrl.resolve(path).toString()
        } catch (e: Throwable) {
            path
        }
    }
}

/**
 * WHEP (WebRTC-HTTP Egress Protocol) 观看端信令。
 *
 * 典型端点：
 *   - SRS: http://<host>:1985/rtc/v1/whep/?app=live&stream=livestream
 *   - MediaMTX: http://<host>:8889/<path>/whep
 */
class WhepSignalingClient(
    endpoint: String,
    private val bearerToken: String? = null,
) : SignalingAdapter {

    companion object {
        private const val TAG = "WhepSignaling"
    }

    private val endpoint: String = endpoint.trim()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val resourceUrl = AtomicReference<String?>(null)

    private var remoteSdpHandler: ((SessionDescription) -> Unit)? = null
    private var remoteIceHandler: ((IceCandidate) -> Unit)? = null

    override fun onRemoteSdp(handler: (SessionDescription) -> Unit) { remoteSdpHandler = handler }
    override fun onRemoteIceCandidate(handler: (IceCandidate) -> Unit) { remoteIceHandler = handler }

    override fun sendLocalSdp(sdp: SessionDescription) {
        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(sdp.description.toRequestBody("application/sdp".toMediaType()))
            .addHeader("Accept", "application/sdp")
        bearerToken?.takeIf { it.isNotBlank() }?.let {
            reqBuilder.addHeader("Authorization", "Bearer $it")
        }
        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "WHEP POST failed", e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "WHEP POST code=${it.code}")
                        return
                    }
                    resourceUrl.set(it.header("Location")?.let { loc ->
                        if (loc.startsWith("http")) loc else {
                            try { java.net.URI(endpoint).resolve(loc).toString() } catch (e: Throwable) { loc }
                        }
                    })
                    val answer = it.body?.string().orEmpty()
                    if (answer.isBlank()) return
                    remoteSdpHandler?.invoke(SessionDescription(SessionDescription.Type.ANSWER, answer))
                }
            }
        })
    }

    override fun sendLocalIceCandidate(candidate: IceCandidate) {
        // 与 WHIP 相同，PATCH 可选
        val url = resourceUrl.get() ?: return
        val body = "a=candidate:${candidate.sdp}\r\n"
        val req = Request.Builder()
            .url(url)
            .patch(body.toRequestBody("application/trickle-ice-sdpfrag".toMediaType()))
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Trickle ICE PATCH failed (ignored): ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    override fun connect(roomId: String) {
        Log.d(TAG, "WHEP connect target=$endpoint room=$roomId")
    }

    override fun disconnect() {
        val url = resourceUrl.getAndSet(null) ?: return
        val req = Request.Builder().url(url).delete().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { Log.w(TAG, "WHEP DELETE failed", e) }
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }
}
