package com.nanackations2.papaya.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

/**
 * WebRTC 生命周期管理。
 *
 * 注意事项：
 * - [PeerConnectionFactory.initialize] 是全局静态方法，只能在进程内调用一次，
 *   因此用类级别的 [globalInitialized] 做幂等保护。
 * - [EglBase] / [PeerConnectionFactory] 是重资源，应与 Activity/组合生命周期一致，
 *   只在整个监控功能退出时 [dispose] 一次；切换房间/模式只需 [disconnect]。
 */
class WebRTCManager(context: Context) {

    companion object {
        private const val TAG = "WebRTCManager"

        @Volatile
        private var globalInitialized = false

        private val ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        )
    }

    enum class State {
        IDLE, CONNECTING, CONNECTED, DISCONNECTED, FAILED
    }

    enum class Mode {
        VIEWER,
        PUBLISHER,
        BIDIRECTIONAL
    }

    private val appContext = context.applicationContext
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var signaling: SignalingAdapter? = null
    private var currentMode: Mode = Mode.VIEWER
    private var initialized = false

    var remoteVideoTrack: VideoTrack? = null
        private set

    var state: State = State.IDLE
        private set(value) {
            if (field != value) {
                field = value
                onStateChange?.invoke(value)
            }
        }

    var onRemoteVideoTrack: ((VideoTrack?) -> Unit)? = null
    var onStateChange: ((State) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val pendingCandidates = java.util.Collections.synchronizedList(mutableListOf<IceCandidate>())
    private var remoteDescriptionSet = false

    val eglContext: EglBase.Context?
        get() = eglBase?.eglBaseContext

    val factory: PeerConnectionFactory?
        get() = peerConnectionFactory

    /**
     * 初始化 EGL + PeerConnectionFactory。多次调用安全，只会真正执行一次工厂创建。
     */
    fun initialize(): Boolean {
        if (initialized && peerConnectionFactory != null) return true
        return try {
            if (!globalInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setFieldTrials("")
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                globalInitialized = true
            }

            if (eglBase == null) {
                eglBase = EglBase.create()
            }
            val ctx = eglBase!!.eglBaseContext

            if (peerConnectionFactory == null) {
                peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(PeerConnectionFactory.Options())
                    .setVideoEncoderFactory(DefaultVideoEncoderFactory(ctx, true, true))
                    .setVideoDecoderFactory(DefaultVideoDecoderFactory(ctx))
                    .createPeerConnectionFactory()
            }

            initialized = true
            Log.d(TAG, "WebRTC initialized")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "WebRTC init failed", e)
            onError?.invoke("WebRTC 初始化失败: ${e.message}")
            false
        }
    }

    /**
     * 连接到房间。会清理上一次的 PeerConnection/信令，然后建立一条新的。
     */
    fun connect(adapter: SignalingAdapter, roomId: String, mode: Mode = Mode.VIEWER) {
        // 优先清理上一次残留
        teardownPeerConnection()

        signaling = adapter
        currentMode = mode
        state = State.CONNECTING

        val pc = createPeerConnection()
        if (pc == null) {
            state = State.FAILED
            onError?.invoke("创建 PeerConnection 失败")
            return
        }

        adapter.onRemoteSdp { sdp -> setRemoteDescription(pc, sdp) }
        adapter.onRemoteIceCandidate { candidate -> addRemoteIceCandidate(pc, candidate) }

        // WHEP(纯观看) 由信令端主动 createOffer；WS 信令则等对端发 offer
        if (adapter is WhepSignalingClient) {
            addRecvTransceivers(pc)
            createOffer(pc)
        }

        adapter.connect(roomId)
    }

    /**
     * 断开当前连接，但保留 factory / egl（可以立即再 connect）。
     */
    fun disconnect() {
        try {
            signaling?.disconnect()
        } catch (e: Throwable) {
            Log.w(TAG, "signaling disconnect failed", e)
        }
        signaling = null
        teardownPeerConnection()
        state = State.IDLE
    }

    private fun teardownPeerConnection() {
        pendingCandidates.clear()
        remoteDescriptionSet = false

        peerConnection?.let {
            try {
                it.close()
                it.dispose()
            } catch (e: Throwable) {
                Log.w(TAG, "pc dispose failed", e)
            }
        }
        peerConnection = null

        if (remoteVideoTrack != null) {
            remoteVideoTrack = null
            onRemoteVideoTrack?.invoke(null)
        }
        currentMode = Mode.VIEWER
    }

    /**
     * 完全释放。调用后必须再次 [initialize] 才能使用。
     */
    fun dispose() {
        disconnect()
        try { peerConnectionFactory?.dispose() } catch (_: Throwable) {}
        peerConnectionFactory = null
        try { eglBase?.release() } catch (_: Throwable) {}
        eglBase = null
        initialized = false
        onRemoteVideoTrack = null
        onStateChange = null
        onError = null
        Log.d(TAG, "WebRTC disposed")
    }

    fun publish(videoTrack: VideoTrack, audioTrack: AudioTrack? = null) {
        val pc = peerConnection ?: run {
            Log.e(TAG, "PeerConnection is null, cannot publish")
            return
        }
        // 防止同一条 track 被重复 addTrack（会抛异常）
        val existingTrackIds = pc.senders.mapNotNull { it.track()?.id() }.toSet()
        if (videoTrack.id() !in existingTrackIds) {
            pc.addTrack(videoTrack, listOf("papaya_stream"))
        }
        if (audioTrack != null && audioTrack.id() !in existingTrackIds) {
            pc.addTrack(audioTrack, listOf("papaya_stream"))
        }
        Log.d(TAG, "Local tracks added, creating offer")
        createOffer(pc)
    }

    fun unpublish() {
        val pc = peerConnection ?: return
        pc.senders.forEach { sender ->
            sender.track()?.let { track ->
                try {
                    pc.removeTrack(sender)
                    Log.d(TAG, "Removed track: ${track.id()}")
                } catch (e: Throwable) {
                    Log.w(TAG, "removeTrack failed", e)
                }
            }
        }
    }

    private fun addRecvTransceivers(pc: PeerConnection) {
        val init = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, init)
        pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, init)
    }

    private fun createPeerConnection(): PeerConnection? {
        val factory = peerConnectionFactory ?: return null

        val config = PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        return factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { signaling?.sendLocalIceCandidate(it) }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track() ?: return
                if (track.kind() == "video") {
                    remoteVideoTrack = track as VideoTrack
                    Log.d(TAG, "Remote video track: ${track.id()}")
                    onRemoteVideoTrack?.invoke(remoteVideoTrack)
                    state = State.CONNECTED
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track() ?: return
                if (track.kind() == "video") {
                    remoteVideoTrack = track as VideoTrack
                    Log.d(TAG, "Remote video via transceiver: ${track.id()}")
                    onRemoteVideoTrack?.invoke(remoteVideoTrack)
                    state = State.CONNECTED
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "Connection state: $newState")
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> state = State.CONNECTED
                    PeerConnection.PeerConnectionState.DISCONNECTED -> state = State.DISCONNECTED
                    PeerConnection.PeerConnectionState.FAILED -> {
                        state = State.FAILED
                        onError?.invoke("P2P 连接失败")
                    }
                    PeerConnection.PeerConnectionState.CLOSED -> state = State.IDLE
                    else -> {}
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onStandardizedIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        })?.also { peerConnection = it }
    }

    private fun setRemoteDescription(pc: PeerConnection, sdp: SessionDescription) {
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set, type=${sdp.type}")
                remoteDescriptionSet = true
                pendingCandidates.forEach { pc.addIceCandidate(it) }
                pendingCandidates.clear()

                when {
                    sdp.type == SessionDescription.Type.OFFER && currentMode != Mode.PUBLISHER -> {
                        createAnswer(pc)
                    }
                    sdp.type == SessionDescription.Type.ANSWER && currentMode == Mode.PUBLISHER -> {
                        Log.d(TAG, "Publisher received answer")
                        state = State.CONNECTED
                    }
                    sdp.type == SessionDescription.Type.ANSWER && currentMode == Mode.BIDIRECTIONAL -> {
                        Log.d(TAG, "Bidirectional received answer")
                        state = State.CONNECTED
                    }
                }
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "setRemoteDescription failed: $error")
                onError?.invoke("setRemoteDescription 失败: $error")
            }
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sdp)
    }

    private fun createOffer(pc: PeerConnection) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                Log.d(TAG, "Offer created")
                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        signaling?.sendLocalSdp(sdp)
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "setLocalDescription(offer) failed: $error")
                        onError?.invoke("setLocalDescription(offer) 失败: $error")
                    }
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createOffer failed: $error")
                onError?.invoke("createOffer 失败: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun createAnswer(pc: PeerConnection) {
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                Log.d(TAG, "Answer created")
                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { signaling?.sendLocalSdp(sdp) }
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "setLocalDescription(answer) failed: $error")
                    }
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createAnswer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    private fun addRemoteIceCandidate(pc: PeerConnection, candidate: IceCandidate) {
        if (remoteDescriptionSet) {
            pc.addIceCandidate(candidate)
        } else {
            pendingCandidates.add(candidate)
        }
    }
}
