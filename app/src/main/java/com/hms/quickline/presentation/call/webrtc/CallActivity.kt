package com.hms.quickline.presentation.call.webrtc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.hms.quickline.R
import com.hms.quickline.core.util.Constants
import com.hms.quickline.databinding.ActivityCallBinding
import io.ktor.util.*
import kotlinx.coroutines.*
import org.webrtc.*

@ExperimentalCoroutinesApi
class CallActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_AUDIO_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    }

    private lateinit var rtcClient: RTCClient
    @OptIn(KtorExperimentalAPI::class)
    private lateinit var signallingClient: SignalingClient

    private val audioManager by lazy { RTCAudioManager.create(this) }

    private val TAG = "CallActivity"

    private var meetingID : String = "test-call"

    private var isJoin = false

    private var isMute = false

    private var isVideoPaused = false

    private var inSpeakerMode = true

    private var iceCandidateList = arrayListOf<IceCandidate?>()

    private lateinit var binding: ActivityCallBinding

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
//            signallingClient.send(p0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)

        if (intent.hasExtra("meetingID"))
            meetingID = intent.getStringExtra("meetingID")!!
        if (intent.hasExtra("isJoin"))
            isJoin = intent.getBooleanExtra("isJoin",false)

        checkCameraAndAudioPermission()
        audioManager.selectAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        binding.ivSwitchCamera.setOnClickListener {
            rtcClient.switchCamera()
        }

        binding.ivAudioOutput.setOnClickListener {
            if (inSpeakerMode) {
                inSpeakerMode = false
                binding.ivAudioOutput.setImageResource(R.drawable.ic_baseline_hearing_24)
                audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
            } else {
                inSpeakerMode = true
                binding.ivAudioOutput.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
            }
        }
        binding.btnVideo.setOnClickListener {
            if (isVideoPaused) {
                isVideoPaused = false
                binding.btnVideo.setImageResource(R.drawable.ic_baseline_videocam_off_24)
            } else {
                isVideoPaused = true
                binding.btnVideo.setImageResource(R.drawable.ic_baseline_videocam_24)
            }
            rtcClient.enableVideo(isVideoPaused)
        }
        binding.btnMic.setOnClickListener {
            if (isMute) {
                isMute = false
                binding.btnMic.setImageResource(R.drawable.ic_baseline_mic_off_24)
            } else {
                isMute = true
                binding.btnMic.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            rtcClient.enableAudio(isMute)
        }
        binding.btnEndCall.setOnClickListener {
            rtcClient.endCall(meetingID)
            binding.vRemote.isGone = false
            Constants.isCallEnded = true
            finish()
        }
    }

    private fun checkCameraAndAudioPermission() {
        if ((ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(this, AUDIO_PERMISSION)
                    != PackageManager.PERMISSION_GRANTED)) {
            requestCameraAndAudioPermission()
        } else {
            onCameraAndAudioPermissionGranted()
        }
    }

    @OptIn(KtorExperimentalAPI::class, DelicateCoroutinesApi::class)
    private fun onCameraAndAudioPermissionGranted() {
        rtcClient = RTCClient(
            application,
            object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    iceCandidateList.add(p0)
                    rtcClient.addIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    Log.e(TAG, "onAddStream: $p0")
                    p0?.videoTracks?.get(0)?.addSink(binding.vRemote)
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    Log.e(TAG, "onIceConnectionChange: $p0")
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                    Log.e(TAG, "onIceConnectionReceivingChange: $p0")
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    Log.e(TAG, "onConnectionChange: $newState")
                }

                override fun onDataChannel(p0: DataChannel?) {
                    Log.e(TAG, "onDataChannel: $p0")
                }

                override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    Log.e(TAG, "onStandardizedIceConnectionChange: $newState")
                }

                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                    Log.e(TAG, "onAddTrack: $p0 \n $p1")
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    Log.e(TAG, "onTrack: $transceiver" )
                }
            }
        )

        rtcClient.initSurfaceView(binding.vRemote)
        rtcClient.initSurfaceView(binding.vLocal)
        rtcClient.startLocalVideoCapture(binding.vLocal)
        signallingClient = SignalingClient(meetingID, createSignallingClientListener())
        if (!isJoin)
            rtcClient.call(sdpObserver,meetingID)

        GlobalScope.launch {
            delay(2000)
            signallingClient.sendIceCandidate(iceCandidateList, isJoin)
        }
    }

    @OptIn(DelicateCoroutinesApi::class, KtorExperimentalAPI::class)
    private fun createSignallingClientListener() = object : SignalingClientListener {
        override fun onConnectionEstablished() {
            runOnUiThread {
                binding.btnEndCall.isClickable = true
            }
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            Constants.isIntiatedNow = false
            rtcClient.answer(sdpObserver, meetingID)
            runOnUiThread {
                binding.pvProgress.isGone = true
            }
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            Constants.isIntiatedNow = false
            runOnUiThread {
                binding.vRemote.isGone = true
            }
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            rtcClient.addIceCandidate(iceCandidate)
        }

        override fun onCallEnded() {
            if (!Constants.isCallEnded) {
                Constants.isCallEnded = true
                rtcClient.endCall(meetingID)
                finish()
            }
        }
    }

    private fun requestCameraAndAudioPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) &&
            ActivityCompat.shouldShowRequestPermissionRationale(this, AUDIO_PERMISSION) &&
            !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION, AUDIO_PERMISSION), CAMERA_AUDIO_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera And Audio Permission Required")
            .setMessage("This app need the camera and audio to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraAndAudioPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_AUDIO_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onCameraAndAudioPermissionGranted()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera and Audio Permission Denied", Toast.LENGTH_LONG).show()
    }

    @OptIn(KtorExperimentalAPI::class)
    override fun onDestroy() {
        signallingClient.destroy()
        super.onDestroy()
    }
}