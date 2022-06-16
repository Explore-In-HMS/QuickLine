package com.hms.quickline.presentation.call

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.hms.quickline.R
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.Constants
import com.hms.quickline.core.util.Constants.IS_MEETING_CONTACT
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.NAME
import com.hms.quickline.core.util.gone
import com.hms.quickline.core.util.visible
import com.hms.quickline.data.model.Users
import com.hms.quickline.databinding.ActivityVideoCallBinding
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.hms.quickline.presentation.call.newwebrtc.RTCAudioManager
import com.hms.quickline.presentation.call.newwebrtc.SignalingClient
import com.hms.quickline.presentation.call.newwebrtc.WebRtcClient
import com.hms.quickline.presentation.call.newwebrtc.listener.SignalingListenerObserver
import com.hms.quickline.presentation.call.newwebrtc.observer.DataChannelObserver
import com.hms.quickline.presentation.call.newwebrtc.observer.PeerConnectionObserver
import com.hms.quickline.presentation.call.newwebrtc.util.PeerConnectionUtil
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.*
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class VideoCallActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityVideoCallBinding::inflate)

    private lateinit var meetingID: String
    private var name: String? = null
    private var isMeetingContact by Delegates.notNull<Boolean>()
    private var isJoin by Delegates.notNull<Boolean>()

    private lateinit var webRtcClient: WebRtcClient
    private lateinit var peerConnectionUtil: PeerConnectionUtil

    private lateinit var signalingClient: SignalingClient

    private lateinit var mediaPlayer: MediaPlayer

    @Inject
    lateinit var eglBase: EglBase

    private var isMute = false
    private var isVideoPaused = false

    private val audioManager by lazy { RTCAudioManager.create(this) }

    var millisecondTime = 0L
    var startTime = 0L

    var seconds = 0
    var minutes = 0

    var handler: Handler? = null

    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())
        mediaPlayer = MediaPlayer.create(this, R.raw.quickline_call_ring)

        receivingPreviousActivityData()
        initializingClasses()

        with(binding) {

            if (isMeetingContact) {
                clVideoLoading.visible()
                handleRing(clVideoLoading.isVisible)
            }

            name?.let {
                //  tvCallingUser.text = name
                tvCallingUserLoading.text = name
            }

            micBtn.setOnClickListener {
                isMute = !isMute
                webRtcClient.enableAudio(!isMute)
                if (isMute) micBtn.setImageResource(R.drawable.ic_mic_off)
                else micBtn.setImageResource(R.drawable.ic_mic)
            }

            videoBtn.setOnClickListener {
                isVideoPaused = !isVideoPaused
                webRtcClient.enableVideo(!isVideoPaused)
                if (isVideoPaused) videoBtn.setImageResource(R.drawable.ic_videocam_off)
                else videoBtn.setImageResource(R.drawable.ic_videocam)
            }

            switchCameraBtn.setOnClickListener {
                webRtcClient.switchCamera()
                webRtcClient.startLocalVideoCapture(binding.localView)
            }

            audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

            endCallBtn.setOnClickListener {
                webRtcClient.endCall()
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

        isMeetingContact = intent.getBooleanExtra(IS_MEETING_CONTACT, false)

        intent.getStringExtra(MEETING_ID)?.let {
            meetingID = it
        }

        intent.getStringExtra(NAME)?.let {
            name = it
        }

        isJoin = intent.getBooleanExtra(Constants.IS_JOIN, false)

        Log.d(TAG, "receivingPreviousFragmentData: roomName = $meetingID & isJoin = $isJoin")
    }

    private fun initializingClasses() {
        peerConnectionUtil = PeerConnectionUtil(application, eglBase.eglBaseContext)

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
                    runOnUiThread {
                        val videoTrack = it.receiver.track() as VideoTrack
                        videoTrack.addSink(binding.remoteView)
                    }
                },
                onAddStreamCallback = {
                    runOnUiThread {
                        Log.d(TAG, "onAddStreamCallback: ${it.videoTracks.first()}")
                        Log.d(TAG, "onAddStreamCallback: ${it.videoTracks}")
                        Log.d(TAG, "onAddStreamCallback: $it")
                        it.videoTracks.first().addSink(binding.remoteView)
                    }
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
            ))

        webRtcClient.createLocalDataChannel()
        gettingCameraPictureToShowInLocalView()
    }

    private fun gettingCameraPictureToShowInLocalView() {
        runOnUiThread {
            webRtcClient.initSurfaceView(binding.remoteView)
            webRtcClient.initSurfaceView(binding.localView)
            webRtcClient.startLocalVideoCapture(binding.localView)
        }

        handlingSignalingClient()
    }

    private fun handlingSignalingClient() {
        signalingClient = SignalingClient(meetingID = meetingID, signalingListener = SignalingListenerObserver(
                onConnectionEstablishedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onConnectionEstablishedCallback called"
                    )
                    binding.endCallBtn.isClickable = true
                },

                onOfferReceivedCallback = {
                    Log.d(SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onOfferReceivedCallback called"
                    )
                    webRtcClient.setRemoteDescription(it)
                    webRtcClient.answer()
                },

                onAnswerReceivedCallback = {
                    Log.d(SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onAnswerReceivedCallback called"
                    )
                    webRtcClient.setRemoteDescription(it)
                    runOnUiThread {
                        binding.clVideoLoading.gone()
                        handleRing(binding.clVideoLoading.isVisible)
                        handler?.postDelayed(runnable, 0)
                        startTime = SystemClock.uptimeMillis()
                    }
                },

                onIceCandidateReceivedCallback = {
                    Log.d(SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onIceCandidateReceivedCallback called"
                    )
                    webRtcClient.addIceCandidate(it)
                },

                onCallEndedCallback = {
                    Log.d(SIGNALING_LISTENER_TAG,
                        "handlingSignalingClient: onCallEndedCallback called"
                    )

                    setFalseUserCalling()
                    finish()
                }
            )
        )

        if (!isJoin)
            webRtcClient.call()
    }

    private fun handleRing(loadingVisibility: Boolean) {
        when (loadingVisibility) {
            true -> {
                mediaPlayer.start()
                mediaPlayer.isLooping = true
            }
            false -> {
                mediaPlayer.pause()
                mediaPlayer.seekTo(0)
            }
        }
    }

    private fun setFalseUserCalling() {
        CloudDbWrapper.getUserById(meetingID, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {
                users.isCalling = false

                cloudDBZone?.executeUpsert(users)?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.e(TAG, "Calls Sdp Upsert failed: ${it.message}")
                }
            }
        })
    }

    private fun closeConnection() {
        webRtcClient.clearCandidates()
        webRtcClient.closePeerConnection()
        webRtcClient.clearSdp()
        signalingClient.removeEventsListener()
        signalingClient.destroy()
    }

    override fun onPause() {
        super.onPause()
        closeConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        handleRing(false)
        setFalseUserCalling()
    }

    companion object {
        private const val TAG = "ui_CallFragment"
        private const val WEB_RTC_DATA_CHANNEL_TAG = "ui_WebRtcDataChannel"
        private const val SIGNALING_LISTENER_TAG = "signalingListener"
    }
}