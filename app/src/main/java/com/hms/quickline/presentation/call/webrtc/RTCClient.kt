package com.hms.quickline.presentation.call.webrtc

import android.app.Application
import android.content.Context
import android.util.Log
import com.hms.quickline.data.model.CallsCandidates
import com.hms.quickline.data.model.CallsSdp
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import org.webrtc.*
import org.webrtc.PeerConnection.RTCConfiguration
import java.util.*
import kotlin.collections.ArrayList

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

    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private var remoteSessionDescription: SessionDescription? = null

    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone

    init {
        initPeerConnectionFactory(context)
    }

    private val iceServer = listOf(
        PeerConnection.IceServer.builder(
            listOf(
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302"
            )
        )
            .createIceServer()
    )

    var STUNList: List<String> = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302",
        "stun:stun3.l.google.com:19302",
        "stun:stun4.l.google.com:19302",
        "stun:stun.vodafone.ro:3478",
        "stun:stun.samsungsmartcam.com:3478",
        "stun:stun.services.mozilla.com:3478"
    )


    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapturer by lazy { getVideoCapturer(context) }

    private val audioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
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
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    rootEglBase.eglBaseContext,
                    true,
                    true
                )
            )
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private fun buildPeerConnection(observer: PeerConnection.Observer) :PeerConnection  {

        val iceServers: ArrayList<PeerConnection.IceServer> = ArrayList()

        val iceServerBuilder: PeerConnection.IceServer.Builder = PeerConnection.IceServer.builder(STUNList)
        iceServerBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)

        val iceServer = iceServerBuilder.createIceServer()
        iceServers.add(iceServer)

        val rtcConfig = RTCConfiguration(iceServers)

        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED

        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT

        return peerConnectionFactory.createPeerConnection(rtcConfig, observer)!!
    }


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
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(
            surfaceTextureHelper,
            localVideoOutput.context,
            localVideoSource.capturerObserver
        )
        videoCapturer.startCapture(320, 240, 60)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(LOCAL_TRACK_ID + "_audio", audioSource)
        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection.addStream(localStream)
    }

    private fun PeerConnection.call(sdpObserver: SdpObserver, meetingID: String, callSdpUUID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        }

        createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.e(TAG, "onSetFailure: $p0")
                    }

                    override fun onSetSuccess() {

                        val offerSdp = CallsSdp()
                        offerSdp.uuid = callSdpUUID
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

    private fun PeerConnection.answer(sdpObserver: SdpObserver, meetingID: String, callSdpUUID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        }
        createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {

                val answerSdp = CallsSdp()
                answerSdp.uuid = callSdpUUID
                answerSdp.meetingID = meetingID
                answerSdp.sdp = Text(desc?.description)
                answerSdp.callType = desc?.type.toString()

                val upsertTask = cloudDBZone?.executeUpsert(answerSdp)
                upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
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
                    Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
                }
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailureRemote: $p0")
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver, meetingID: String, callSdpUUID: String) =
        peerConnection.call(sdpObserver, meetingID, callSdpUUID)

    fun answer(sdpObserver: SdpObserver, meetingID: String, callSdpUUID: String) =
        peerConnection.answer(sdpObserver, meetingID, callSdpUUID)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        remoteSessionDescription = sessionDescription
        peerConnection.setRemoteDescription(object : SdpObserver {
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
        peerConnection.addIceCandidate(iceCandidate)
    }

    fun endCall(meetingID: String, callSdpUUID: String) {

        val queryMeetingID = CloudDBZoneQuery.where(CallsCandidates::class.java).equalTo("meetingID", meetingID)
        val queryTask = cloudDBZone?.executeQuery(
            queryMeetingID,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )
        queryTask?.addOnSuccessListener { snapshot ->
            val exampleListTemp = mutableListOf<CallsCandidates>()
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
                        iceCandidateArray.add(
                            IceCandidate(
                                data.sdpMid,
                                data.sdpMLineIndex,
                                data.sdpCandidate
                            )
                        )
                    } else if (data.callType != null && data.callType == "answerCandidate") {
                        iceCandidateArray.add(
                            IceCandidate(
                                data.sdpMid,
                                data.sdpMLineIndex,
                                data.sdpCandidate
                            )
                        )
                    }
                }
                peerConnection.removeIceCandidates(iceCandidateArray.toTypedArray())

                val deleteTask = cloudDBZone?.executeDelete(exampleListTemp)
                deleteTask?.addOnSuccessListener {
                    Log.i(TAG, "Candidates Delete success: $it")
                }?.addOnFailureListener {
                    Log.i(TAG, "Candidates Delete failed: $it")
                }
                snapshot.release()
            }

            val callsSdp = CallsSdp()
            callsSdp.uuid = callSdpUUID
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

        peerConnection.close()
    }

    fun enableVideo(videoEnabled: Boolean) {
        if (localVideoTrack != null)
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