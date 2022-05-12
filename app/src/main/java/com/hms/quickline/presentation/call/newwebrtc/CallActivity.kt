package com.hms.quickline.presentation.call.newwebrtc

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.codefunvideocallingtest.rtc.regular.WebRtcClient
import com.hms.quickline.R
import com.hms.quickline.databinding.ActivityCallBinding
import com.hms.quickline.presentation.call.newwebrtc.listener.SignalingListenerObserver
import com.hms.quickline.presentation.call.newwebrtc.observer.DataChannelObserver
import com.hms.quickline.presentation.call.newwebrtc.observer.PeerConnectionObserver
import com.hms.quickline.presentation.call.newwebrtc.util.PeerConnectionUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.webrtc.*
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

    private lateinit var meetingID: String

    private var isJoin by Delegates.notNull<Boolean>()

    private lateinit var webRtcClient: WebRtcClient
    private lateinit var peerConnectionUtil: PeerConnectionUtil

    private lateinit var signalingMedium: SignalingMedium

    @Inject
    lateinit var eglBase: EglBase

    private var isMute = false
    private var isVideoPaused = false
    private var inSpeakerMode = true

    private val audioManager by lazy { RTCAudioManager.create(this) }

    private lateinit var callSdpUUID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callSdpUUID = UUID.randomUUID().toString()

        checkPermission(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            PERMISSION_CODE
        )

        receivingPreviousActivityData()
        initializingClasses()

        with(binding) {
            micBtn.setOnClickListener {
                if (isMute) {
                    isMute = false
                    micBtn.setImageResource(R.drawable.ic_baseline_mic_off_24)
                } else {
                    isMute = true
                    micBtn.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                webRtcClient.enableAudio(isMute)
            }

            videoBtn.setOnClickListener {
                if (isVideoPaused) {
                    isVideoPaused = false
                    videoBtn.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                } else {
                    isVideoPaused = true
                    videoBtn.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                webRtcClient.enableVideo(isVideoPaused)
            }

            switchCameraBtn.setOnClickListener {
                webRtcClient.switchCamera()
            }

            audioOutputBtn.setOnClickListener {
                if (inSpeakerMode) {
                    inSpeakerMode = false
                    audioOutputBtn.setImageResource(R.drawable.ic_baseline_hearing_24)
                    audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                } else {
                    inSpeakerMode = true
                    audioOutputBtn.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }
            }

            endCallBtn.setOnClickListener {
                webRtcClient.endCall(callSdpUUID)
                signalingMedium.destroy()
            }
        }
    }

    private fun receivingPreviousActivityData() {
        if (intent.hasExtra("meetingID"))
            meetingID = intent.getStringExtra("meetingID")!!
        if (intent.hasExtra("isJoin"))
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
                    signalingMedium.sendIceCandidateModelToUser(it, isJoin)
                    webRtcClient.addIceCandidate(it)
                },
                onTrackCallback = {
                    val videoTrack = it.receiver.track() as VideoTrack
                    videoTrack.addSink(binding.remoteView)
                },
                onAddStreamCallback = {
                    Log.d(TAG, "onAddStreamCallback: ${it.videoTracks.first()}")
                    Log.d(TAG, "onAddStreamCallback: ${it.videoTracks}")
                    Log.d(TAG, "onAddStreamCallback: $it")
                    it.videoTracks.first().addSink(binding.remoteView)
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
        gettingCameraPictureToShowInLocalView()
    }

    private fun gettingCameraPictureToShowInLocalView() {
        webRtcClient.initSurfaceView(binding.remoteView)
        webRtcClient.initSurfaceView(binding.localView)
        webRtcClient.startLocalVideoCapture(binding.localView)

        handlingSignalingClient()
    }

    private fun handlingSignalingClient() {
        signalingMedium = SignalingMedium(
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
                    signalingMedium.destroy()
                }
            ), isJoin
        )

        if (!isJoin)
            webRtcClient.call(callSdpUUID)
    }

    private fun checkPermission(permission: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(this, permission, requestCode)
    }

    companion object {
        private const val TAG = "ui_CallFragment"
        private const val WEB_RTC_DATA_CHANNEL_TAG = "ui_WebRtcDataChannel"
        private const val SIGNALING_LISTENER_TAG = "signalingListener"
        private const val PERMISSION_CODE = 101
    }
}