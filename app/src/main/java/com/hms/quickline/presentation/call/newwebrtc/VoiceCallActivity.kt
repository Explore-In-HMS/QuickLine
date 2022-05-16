package com.hms.quickline.presentation.call.newwebrtc

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.hms.quickline.R
import com.hms.quickline.core.util.invisible
import com.hms.quickline.core.util.visible
import com.hms.quickline.databinding.ActivityVoiceCallBinding
import com.hms.quickline.presentation.call.newwebrtc.listener.SignalingListenerObserver
import com.hms.quickline.presentation.call.newwebrtc.observer.DataChannelObserver
import com.hms.quickline.presentation.call.newwebrtc.observer.PeerConnectionObserver
import com.hms.quickline.presentation.call.newwebrtc.util.PeerConnectionUtil
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.*
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class VoiceCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceCallBinding

    private lateinit var meetingID: String
    private var name: String? = null
    private var isJoin by Delegates.notNull<Boolean>()

    private lateinit var webRtcClient: WebRtcClient
    private lateinit var peerConnectionUtil: PeerConnectionUtil

    private lateinit var signalingClient: SignalingClient

    @Inject
    lateinit var eglBase: EglBase

    private var isMute = false
    private var inSpeakerMode = true

    private val audioManager by lazy { RTCAudioManager.create(this) }

    private lateinit var callSdpUUID: String

    var millisecondTime = 0L
    var startTime = 0L

    var seconds = 0
    var minutes = 0

    var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())

        callSdpUUID = UUID.randomUUID().toString()

        receivingPreviousActivityData()
        initializingClasses()

        with(binding) {

            name?.let {
                tvCallingUser.text = name
            }

            micBtn.setOnClickListener {
                isMute = !isMute
                webRtcClient.enableAudio(!isMute)
                if (isMute) micBtn.setImageResource(R.drawable.ic_mic_off)
                else micBtn.setImageResource(R.drawable.ic_mic)
            }

            btnAudioOutput.setOnClickListener {
                inSpeakerMode = !inSpeakerMode
                if (inSpeakerMode) {
                    btnAudioOutput.setImageResource(R.drawable.ic_hearing)
                    audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                } else {
                    btnAudioOutput.setImageResource(R.drawable.ic_speaker_up)
                    audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }
            }

            endCallBtn.setOnClickListener {
                webRtcClient.endCall(callSdpUUID)
                signalingClient.removeEventsListener()
                signalingClient.destroy()
                finish()
            }
        }
    }

    private var runnable: Runnable = object : Runnable {

        override fun run() {

            millisecondTime = SystemClock.uptimeMillis() - startTime

            seconds = (millisecondTime / 1000).toInt()

            minutes = seconds / 60

            seconds %= 60

            if (minutes.toString().length < 2) {
                "0$minutes:".also { binding.tvCallingTimeMinute.text = it }
            } else {
                binding.tvCallingTimeMinute.text = minutes.toString()
            }

            if (seconds.toString().length < 2) {
                "0$seconds".also { binding.tvCallingTimeSecond.text = it }
            } else {
                binding.tvCallingTimeSecond.text = seconds.toString()
            }

            handler?.postDelayed(this, 0)
        }
    }

    private fun receivingPreviousActivityData() {

        intent.getStringExtra("meetingID")?.let {
            meetingID = it
        }

        intent.getStringExtra("name")?.let {
            name = it
        }

        isJoin = intent.getBooleanExtra("isJoin", false)

        Log.d(TAG, "receivingPreviousFragmentData: roomName = $meetingID & isJoin = $isJoin")
    }

    private fun initializingClasses() {
        peerConnectionUtil = PeerConnectionUtil(
            application,
            eglBase.eglBaseContext
        )

        webRtcClient = WebRtcClient(
            context = application,
            eglBase = eglBase,
            meetingID = meetingID,
            dataChannelObserver = DataChannelObserver(
                onBufferedAmountChangeCallback = {
                    Log.d(WEB_RTC_DATA_CHANNEL_TAG, "onBufferedAmountChange: called")
                },
                onStateChangeCallback = {
                    Log.d(WEB_RTC_DATA_CHANNEL_TAG, "onStateChange: called")
                    webRtcClient.checkDataChannelState()
                },
                onMessageCallback = {
                    Log.d(WEB_RTC_DATA_CHANNEL_TAG, "onMessage: called")
                }
            ),
            peerConnectionObserver = PeerConnectionObserver(
                onIceCandidateCallback = {
                    signalingClient.sendIceCandidateModelToUser(it, isJoin)
                    webRtcClient.addIceCandidate(it)
                },
                onTrackCallback = {

                },
                onAddStreamCallback = {

                },
                onDataChannelCallback = { dataChannel ->
                    Log.d(
                        WEB_RTC_DATA_CHANNEL_TAG,
                        "onDataChannelCallback: state -> ${dataChannel.state()}"
                    )
                    dataChannel.registerObserver(
                        DataChannelObserver(
                            onStateChangeCallback = {
                                Log.d(
                                    WEB_RTC_DATA_CHANNEL_TAG,
                                    "onDataChannelCallback - onStateChangeCallback - remote data channel state -> ${
                                        dataChannel.state()
                                    }"
                                )
                            },
                            onMessageCallback = {
                                Log.d(
                                    WEB_RTC_DATA_CHANNEL_TAG,
                                    "onDataChannelCallback - onMessageCallback -> got Message"
                                )
                            }
                        )
                    )
                }
            )
        )
        webRtcClient.createLocalDataChannel()
        initVoice()
    }

    private fun initVoice() {
        webRtcClient.startVoice()
        handlingSignalingClient()
    }

    private fun handlingSignalingClient() {
        signalingClient = SignalingClient(
            meetingID = meetingID,
            signalingListener = SignalingListenerObserver(
                onConnectionEstablishedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onConnectionEstablishedCallback called"
                    )
                    binding.endCallBtn.isClickable = true
                },
                onOfferReceivedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onOfferReceivedCallback called"
                    )
                    webRtcClient.setRemoteDescription(it)
                    webRtcClient.answer(callSdpUUID)

                },
                onAnswerReceivedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onAnswerReceivedCallback called"
                    )
                    webRtcClient.setRemoteDescription(it)
                    runOnUiThread {
                        with(binding) {
                            imgVoiceLoading.invisible()
                            tvCallingText.invisible()
                            imgUserImage.visible()
                            tvCallingTimeMinute.visible()
                            tvCallingTimeSecond.visible()

                            Glide.with(this@VoiceCallActivity)
                                .load("https://media-exp1.licdn.com/dms/image/D4D03AQEweV5ra2apTw/profile-displayphoto-shrink_800_800/0/1630667862366?e=1658361600&v=beta&t=qlNpziZO8fxddUwj5eiVQYygZJA0tNHNdFZTkBbdg-A")
                                .circleCrop()
                                .into(imgUserImage)
                        }

                        handler?.postDelayed(runnable, 0)
                        startTime = SystemClock.uptimeMillis()
                    }
                },
                onIceCandidateReceivedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onIceCandidateReceivedCallback called"
                    )
                    webRtcClient.addIceCandidate(it)
                },
                onCallEndedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onCallEndedCallback called"
                    )
                    webRtcClient.endCall(callSdpUUID)
                    signalingClient.removeEventsListener()
                    signalingClient.destroy()
                    finish()
                }
            )
        )

        if (!isJoin)
            webRtcClient.call(callSdpUUID)
    }

    override fun onDestroy() {
        super.onDestroy()
        webRtcClient.endCall(callSdpUUID)
        signalingClient.removeEventsListener()
        signalingClient.destroy()
    }

    companion object {
        private const val TAG = "ui_CallFragment"
        private const val WEB_RTC_DATA_CHANNEL_TAG = "ui_WebRtcDataChannel"
        private const val SIGNALING_LISTENER_TAG = "signalingListener"
        private const val PERMISSION_CODE = 101
    }
}