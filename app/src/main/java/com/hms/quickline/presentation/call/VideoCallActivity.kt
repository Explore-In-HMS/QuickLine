package com.hms.quickline.presentation.call

import android.content.ComponentName
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.hms.quickline.R
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.*
import com.hms.quickline.core.util.Constants.IS_MEETING_CONTACT
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.NAME
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
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.CloudDBZone
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.common.ApiException
import com.huawei.hms.wireless.IQoeCallBack
import com.huawei.hms.wireless.IQoeService
import com.huawei.hms.wireless.WirelessClient
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.EglBase
import org.webrtc.VideoTrack
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

    private var millisecondTime = 0L
    private var startTime = 0L

    private var seconds = 0
    private var minutes = 0

    private var handler: Handler? = null
    private var isToastShown = false

    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone

    private val netQoeLevel = IntArray(4)
    private var qoeService: IQoeService? = null
    private val srcConn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            qoeService = IQoeService.Stub.asInterface(service)
            var ret = 0
            if (qoeService != null) {
                try {
                    ret = qoeService!!.registerNetQoeCallBack(
                        application.packageName,
                        wirelessListener
                    )
                    Log.i(TAG, ret.toString())
                } catch (ex: RemoteException) {
                    Log.e(TAG, "no registerNetQoeCallback api")
                }
            }
            Log.i(TAG, "Connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            qoeService = null
            Log.i(TAG, "onServiceDisConnected.")
            Log.i(TAG, "Disconnected")
        }
    }

    private val wirelessListener: IQoeCallBack = object : IQoeCallBack.Stub() {
        @Throws(RemoteException::class)
        override fun callBack(type: Int, qoeInfo: Bundle) {
            if (type != NETWORK_QOE_INFO_TYPE) {
                Log.e(TAG, "callback failed.type:$type")
                return
            }
            var channelNum = 0
            if (qoeInfo.containsKey("channelNum")) {
                channelNum = qoeInfo.getInt("channelNum")
            }
            for (i in 0 until channelNum) {
                netQoeLevel[i] = qoeInfo.getInt("netQoeLevel$i")
                Log.i(TAG, netQoeLevel[0].toString())

                if (netQoeLevel[0] == 5) {
                    runOnUiThread {
                        binding.ivWifi.setImageResource(R.drawable.ic_wifi_weak)
                        if (!isToastShown){
                            isToastShown = true
                            showToastLongCenter(this@VideoCallActivity,
                                getString(R.string.network_warn)
                                ,Toast.LENGTH_LONG)
                        }
                    }
                }else {
                    binding.ivWifi.setImageResource(R.drawable.ic_wifi_healty)
                }
            }
        }
    }


    private fun bindQoeService() {
        val networkQoeClient = WirelessClient.getNetworkQoeClient(this)
        networkQoeClient?.networkQoeServiceIntent?.addOnSuccessListener(OnSuccessListener { wirelessResult ->
            val intent = wirelessResult.intent
            if (intent == null) {
                Log.i(TAG, "intent is null.")
                return@OnSuccessListener
            }
            this.bindService(intent, srcConn, BIND_AUTO_CREATE)
        })?.addOnFailureListener { exception ->
            if (exception is ApiException) {
                val errCode = exception.statusCode
                Log.e(
                    TAG,
                    "Get intent failed:$errCode"
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        bindQoeService()

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

        setUserAvailability(false)
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
        signalingClient =
            SignalingClient(meetingID = meetingID, signalingListener = SignalingListenerObserver(
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
                    webRtcClient.answer()
                },

                onAnswerReceivedCallback = {
                    Log.d(
                        SIGNALING_LISTENER_TAG,
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
                    Log.i(TAG, "User Calling Info Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.e(TAG, "User Calling Info Upsert failed: ${it.message}")
                }
            }
        })
    }

    private fun setUserAvailability(isAvailable : Boolean) {
        lateinit var userId : String
        AGConnectAuth.getInstance().currentUser?.let {
            userId = it.uid
        }

        CloudDbWrapper.getUserById(userId, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {
                users.isAvailable = isAvailable

                cloudDBZone?.executeUpsert(users)?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i(TAG, "User Calling Info Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.e(TAG, "User Calling Info Upsert failed: ${it.message}")
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
        this.unbindService(srcConn)
        super.onDestroy()
        handleRing(false)
        setFalseUserCalling()
        setUserAvailability(true)
    }

    companion object {
        private const val TAG = "ui_CallFragment"
        private const val WEB_RTC_DATA_CHANNEL_TAG = "ui_WebRtcDataChannel"
        private const val SIGNALING_LISTENER_TAG = "signalingListener"
        private const val NETWORK_QOE_INFO_TYPE = 0
    }
}