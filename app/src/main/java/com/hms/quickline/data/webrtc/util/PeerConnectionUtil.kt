package com.hms.quickline.data.webrtc.util

import android.app.Application
import org.webrtc.*

class PeerConnectionUtil(context: Application, eglBaseContext: EglBase.Context) {

    init {
        PeerConnectionFactory.InitializationOptions
            .builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials(fieldTrials)
            .createInitializationOptions().also { initializationOptions ->
                PeerConnectionFactory.initialize(initializationOptions)
            }
    }

    private val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)

    private val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

    val peerConnectionFactory: PeerConnectionFactory = PeerConnectionFactory
        .builder()
        .setVideoDecoderFactory(defaultVideoDecoderFactory)
        .setVideoEncoderFactory(defaultVideoEncoderFactory)
        .setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = true
        })
        .createPeerConnectionFactory()

    val iceServer = listOf(
        PeerConnection.IceServer.builder(serverUri).createIceServer(),
        PeerConnection.IceServer.builder(serverUri2).createIceServer()
    )

    companion object {
        private const val fieldTrials = "WebRTC-H264HighProfile/Enabled/"
        private const val serverUri = "stun:stun1.l.google.com:19302"
        private const val serverUri2 = "stun:stun2.l.google.com:19302"
    }

}