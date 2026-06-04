package com.nanackations2.papaya.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "CameraManager"
        private const val CAPTURE_WIDTH = 1280
        private const val CAPTURE_HEIGHT = 720
        private const val CAPTURE_FPS = 30
    }

    enum class CameraFacing { FRONT, BACK }

    var localVideoTrack: VideoTrack? = null
        private set

    var localAudioTrack: AudioTrack? = null
        private set

    var onVideoTrackReady: ((VideoTrack) -> Unit)? = null

    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var currentFacing = CameraFacing.FRONT
    private var isCapturing = false

    fun startCapture(
        peerConnectionFactory: PeerConnectionFactory,
        eglContext: EglBase.Context,
        facing: CameraFacing = CameraFacing.FRONT,
        enableAudio: Boolean = true
    ): Boolean {
        if (isCapturing) return true
        currentFacing = facing

        return try {
            videoCapturer = createVideoCapturer(facing)
            if (videoCapturer == null) {
                Log.e(TAG, "Failed to create video capturer")
                return false
            }

            videoSource = peerConnectionFactory.createVideoSource(false)
            surfaceTextureHelper = SurfaceTextureHelper.create(
                "CameraCaptureThread", eglContext
            )

            videoCapturer!!.initialize(
                surfaceTextureHelper!!, context, videoSource!!.capturerObserver
            )
            videoCapturer!!.startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS)

            localVideoTrack = peerConnectionFactory.createVideoTrack(
                "local_video_${System.currentTimeMillis()}", videoSource
            )
            localVideoTrack?.setEnabled(true)
            onVideoTrackReady?.invoke(localVideoTrack!!)

            if (enableAudio) {
                val audioConstraints = MediaConstraints()
                audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
                localAudioTrack = peerConnectionFactory.createAudioTrack(
                    "local_audio_${System.currentTimeMillis()}", audioSource
                )
                localAudioTrack?.setEnabled(true)
            }

            isCapturing = true
            Log.d(TAG, "Camera capture started: facing=$facing, audio=$enableAudio")
            true
        } catch (e: Exception) {
            Log.e(TAG, "startCapture failed", e)
            stopCapture()
            false
        }
    }

    fun stopCapture() {
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping capturer", e)
        }
        try {
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing capturer", e)
        }

        surfaceTextureHelper?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()

        videoCapturer = null
        videoSource = null
        audioSource = null
        surfaceTextureHelper = null
        localVideoTrack = null
        localAudioTrack = null
        isCapturing = false

        Log.d(TAG, "Camera capture stopped")
    }

    fun switchCamera(
        peerConnectionFactory: PeerConnectionFactory,
        eglContext: EglBase.Context,
        enableAudio: Boolean = true
    ): Boolean {
        val newFacing = when (currentFacing) {
            CameraFacing.FRONT -> CameraFacing.BACK
            CameraFacing.BACK -> CameraFacing.FRONT
        }
        stopCapture()
        return startCapture(peerConnectionFactory, eglContext, newFacing, enableAudio)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun isFrontFacing(): Boolean = currentFacing == CameraFacing.FRONT

    private fun createVideoCapturer(facing: CameraFacing): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(context)) {
            createCamera2Capturer(facing)
        } else {
            createCamera1Capturer(facing)
        }
    }

    private fun createCamera2Capturer(facing: CameraFacing): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        return try {
            val deviceNames = enumerator.deviceNames
            for (name in deviceNames) {
                val isFront = enumerator.isFrontFacing(name)
                if ((facing == CameraFacing.FRONT && isFront) ||
                    (facing == CameraFacing.BACK && !isFront)
                ) {
                    return enumerator.createCapturer(name, null)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Camera2 enumerator failed", e)
            null
        }
    }

    private fun createCamera1Capturer(facing: CameraFacing): VideoCapturer? {
        val enumerator = Camera1Enumerator(true)
        return try {
            val deviceNames = enumerator.deviceNames
            for (name in deviceNames) {
                val isFront = enumerator.isFrontFacing(name)
                if ((facing == CameraFacing.FRONT && isFront) ||
                    (facing == CameraFacing.BACK && !isFront)
                ) {
                    return enumerator.createCapturer(name, null)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Camera1 enumerator failed", e)
            null
        }
    }
}
