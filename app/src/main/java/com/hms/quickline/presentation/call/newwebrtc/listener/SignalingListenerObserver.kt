package com.hms.quickline.presentation.call.newwebrtc.listener

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SignalingListenerObserver(
    private val onConnectionEstablishedCallback: () -> Unit = {},
    private val onOfferReceivedCallback: (SessionDescription) -> Unit = {},
    private val onAnswerReceivedCallback: (SessionDescription) -> Unit = {},
    private val onIceCandidateReceivedCallback: (IceCandidate) -> Unit = {},
    private val onCallEndedCallback: () -> Unit = {}
) : SignalingListener {
    override fun onConnectionEstablished() {
        onConnectionEstablishedCallback()
    }

    override fun onOfferReceived(description: SessionDescription) {
        onOfferReceivedCallback(description)
    }

    override fun onAnswerReceived(description: SessionDescription) {
        onAnswerReceivedCallback(description)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        onIceCandidateReceivedCallback(iceCandidate)
    }

    override fun onCallEnded() {
        onCallEndedCallback()
    }
}