package com.hms.quickline.presentation.call.webrtc

import android.app.Application
import android.content.Context
import android.util.Log
import com.hms.quickline.data.model.CallsCandidates
import com.hms.quickline.data.model.CallsSdp
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import org.webrtc.*

class RTCClient(
        context: Application,
        observer: PeerConnection.Observer
) {

    companion object {
        private const val LOCAL_TRACK_ID = "local_track"
        private const val LOCAL_STREAM_ID = "local_track"
        private const val TAG = "RTCClient"
    }

    private val rootEglBase: EglBase = EglBase.create()

    private var localAudioTrack : AudioTrack? = null
    private var localVideoTrack : VideoTrack? = null

    var remoteSessionDescription : SessionDescription? = null

    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone

    init {
        initPeerConnectionFactory(context)
    }

    private val iceServer = listOf(
            PeerConnection.IceServer.builder(listOf("stun:stun1.l.google.com:19302","stun:stun2.l.google.com:19302"))
                    .createIceServer()
    )

    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapturer by lazy { getVideoCapturer(context) }

    private val audioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val peerConnection by lazy { buildPeerConnection(observer) }

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory
                .builder()
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
                .setOptions(PeerConnectionFactory.Options().apply {
                    disableEncryption = true
                    disableNetworkMonitor = true
                })
                .createPeerConnectionFactory()
    }

    private fun buildPeerConnection(observer: PeerConnection.Observer) = peerConnectionFactory.createPeerConnection(
            iceServer,
            observer
    )

    private fun getVideoCapturer(context: Context) =
            Camera2Enumerator(context).run {
                deviceNames.find {
                    isFrontFacing(it)
                }?.let {
                    createCapturer(it, null)
                } ?: throw IllegalStateException()
            }

    fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext, null)
    }

    fun startLocalVideoCapture(localVideoOutput: SurfaceViewRenderer) {
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, localVideoOutput.context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(320, 240, 60)
        localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_TRACK_ID + "_audio", audioSource)
        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    private fun PeerConnection.call(sdpObserver: SdpObserver, meetingID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.e(TAG, "onSetFailure: $p0")
                    }

                    override fun onSetSuccess() {

                        val offerSdp = CallsSdp()
                        offerSdp.meetingID = meetingID
                        offerSdp.sdp = Text(desc?.description)
                        offerSdp.callType = desc?.type.toString()

                        val upsertTask = cloudDBZone?.executeUpsert(offerSdp)
                        upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                            Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
                        }?.addOnFailureListener {
                            Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
                        }
                        Log.e(TAG, "onSetSuccess")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.e(TAG, "onCreateSuccess: Description $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.e(TAG, "onCreateFailure: $p0")
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }

            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "onSetFailure: $p0")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailure: $p0")
            }
        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver, meetingID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                val answerSdp = CallsSdp()
                answerSdp.meetingID = meetingID
                answerSdp.sdp = Text(desc?.description)
                answerSdp.callType = desc?.type.toString()

                val upsertTask = cloudDBZone?.executeUpsert(answerSdp)
                upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
                }
                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.e(TAG, "onSetFailure: $p0")
                    }

                    override fun onSetSuccess() {
                        Log.e(TAG, "onSetSuccess")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.e(TAG, "onCreateSuccess: Description $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.e(TAG, "onCreateFailureLocal: $p0")
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailureRemote: $p0")
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver, meetingID: String) = peerConnection?.call(sdpObserver, meetingID)

    fun answer(sdpObserver: SdpObserver, meetingID: String) = peerConnection?.answer(sdpObserver, meetingID)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        remoteSessionDescription = sessionDescription
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "onSetFailure: $p0")
            }

            override fun onSetSuccess() {
                Log.e(TAG, "onSetSuccessRemoteSession")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.e(TAG, "onCreateSuccessRemoteSession: Description $p0")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailure")
            }
        }, sessionDescription)

    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun endCall(meetingID: String) {

        val queryTask = cloudDBZone?.executeQuery(
            CloudDBZoneQuery.where(CallsCandidates::class.java),
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )
        queryTask?.addOnSuccessListener { snapshot ->
            val exampleListTemp = arrayListOf<CallsCandidates>()
            try {
                while (snapshot.snapshotObjects.hasNext()) {
                    exampleListTemp.add(snapshot.snapshotObjects.next())
                }
            } catch (e: AGConnectCloudDBException) {
                Log.w(TAG, "Snapshot Error: " + e.message)
            } finally {
                val iceCandidateArray: MutableList<IceCandidate> = mutableListOf()
                for (data in exampleListTemp) {
                    if (data.callType != null && data.callType == "offerCandidate") {
                        iceCandidateArray.add(IceCandidate(
                            data.sdpMid,
                            data.sdpMLineIndex,
                            data.sdpCandidate
                        ))
                    }   else if (data.callType != null && data.callType == "answerCandidate") {
                        iceCandidateArray.add(IceCandidate(
                            data.sdpMid,
                            data.sdpMLineIndex,
                            data.sdpCandidate
                        ))
                    }
                }
                peerConnection?.removeIceCandidates(iceCandidateArray.toTypedArray())
                snapshot.release()
            }

            val callsSdp = CallsSdp()
            callsSdp.meetingID = meetingID
            callsSdp.callType = "END_CALL"
            val upsertTask = cloudDBZone?.executeUpsert(callsSdp)

            upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
            }?.addOnFailureListener {
                Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
            }
        }?.addOnFailureListener {
            Log.w(TAG, "QueryTask Failure: " + it.message)
        }

        peerConnection?.close()
    }

    fun enableVideo(videoEnabled: Boolean) {
        if (localVideoTrack !=null)
            localVideoTrack?.setEnabled(videoEnabled)
    }

    fun enableAudio(audioEnabled: Boolean) {
        if (localAudioTrack != null)
            localAudioTrack?.setEnabled(audioEnabled)
    }
    fun switchCamera() {
        videoCapturer.switchCamera(null)
    }
}