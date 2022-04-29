package com.hms.quickline.presentation.call.webrtc

import android.util.Log
import com.hms.quickline.core.util.Constants
import com.hms.quickline.data.model.CallsCandidates
import com.hms.quickline.data.model.CallsSdp
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import io.ktor.util.*
import kotlinx.coroutines.*
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class SignalingClient(
    private val meetingID : String,
    private val listener: SignalingClientListener
) : CoroutineScope {

    companion object {
        private const val TAG = "SignallingClient"
    }

    private val job = Job()

    var SDPtype : String? = null
    override val coroutineContext = Dispatchers.IO + job

    private var cloudDB: AGConnectCloudDB? = CloudDbWrapper.cloudDB
    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone
    private var mRegisterSdp: ListenerHandler? = null
    private var mRegisterCandidate: ListenerHandler? = null

    init {
        connect()
    }

    private fun connect() = launch {
        cloudDB?.enableNetwork(Constants.CloudDbZoneName).also {
            listener.onConnectionEstablished()
        }

        try {
            addCallsSdpSubscription()
            addCallsCandidatesSubscription()
        } catch (exception: Exception) {
            Log.e(TAG, "connectException: $exception")
        }
    }

    fun sendIceCandidate(iceCandidateList: ArrayList<IceCandidate?>, isJoin : Boolean) = runBlocking {
        val type = when {
            isJoin -> "answerCandidate"
            else -> "offerCandidate"
        }

        for (candidate in iceCandidateList) {
            coroutineScope {
                launch {
                    delay(200)
                    val query = CloudDBZoneQuery.where(CallsCandidates::class.java)
                    val maxQueryTask = cloudDBZone?.executeMaximumQuery(query, "id",
                        CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY)
                    maxQueryTask?.addOnSuccessListener { number ->

                        val lastId = number?.toInt() ?: 0

                        val callsCandidates = CallsCandidates()
                        callsCandidates.id = lastId + 1
                        callsCandidates.meetingID = meetingID
                        callsCandidates.serverUrl = candidate?.serverUrl
                        callsCandidates.sdpMid = candidate?.sdpMid
                        callsCandidates.sdpMLineIndex = candidate?.sdpMLineIndex
                        callsCandidates.sdpCandidate = candidate?.sdp
                        callsCandidates.callType = type

                        val upsertTask = cloudDBZone?.executeUpsert(callsCandidates)

                        upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                            Log.i("JanerSuccess", System.currentTimeMillis().toString())
                            Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
                        }?.addOnFailureListener {
                            Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
                        }
                    }?.addOnFailureListener {
                        Log.w(TAG, "Maximum query is failed: " + Log.getStackTraceString(it))
                    }
                }
            }
        }
    }

    private val mCallsSdpSnaphostListener = OnSnapshotListener<CallsSdp> { cloudDBZoneSnapshot, e ->

        e?.let {
            Log.w(TAG, "onSnapshot: " + e.message)
            return@OnSnapshotListener
        }

        val snapshot = cloudDBZoneSnapshot.snapshotObjects
        var callSdpTemp = CallsSdp()
        try {
            while (snapshot.hasNext()) {
                callSdpTemp = snapshot.next()
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "Snapshot Error: " + e.message)
        } finally {

            if (callSdpTemp.callType.toString() == "OFFER") {
                listener.onOfferReceived(SessionDescription(SessionDescription.Type.OFFER, callSdpTemp.sdp.toString()))
                SDPtype = "Offer"
            } else if (callSdpTemp.callType.toString() == "ANSWER") {
                listener.onAnswerReceived(SessionDescription(
                    SessionDescription.Type.ANSWER,callSdpTemp.sdp.toString()))
                SDPtype = "Answer"
            } else if (!Constants.isIntiatedNow && callSdpTemp.callType.toString() == "END_CALL") {
                listener.onCallEnded()
                SDPtype = "End Call"
            }
            Log.d(TAG, "Current data: $callSdpTemp")
        }
    }

    private val mCallsCandidatesSnaphostListener = OnSnapshotListener<CallsCandidates> { cloudDBZoneSnapshot, e ->

        e?.let {
            Log.w(TAG, "onSnapshot: " + e.message)
            return@OnSnapshotListener
        }

        val snapshot = cloudDBZoneSnapshot.snapshotObjects
        val callsCandidatesTemp = arrayListOf<CallsCandidates>()
        try {
            while (snapshot.hasNext()) {
                callsCandidatesTemp.add(snapshot.next())
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "Snapshot Error: " + e.message)
        } finally {

            for (data in callsCandidatesTemp) {

                if (SDPtype == "Offer" && data.callType == "offerCandidate") {
                    listener.onIceCandidateReceived(
                        IceCandidate(data.sdpMid.toString(),
                            Math.toIntExact(data.sdpMLineIndex.toLong()),
                            data.sdpCandidate.toString()))
                } else if (SDPtype == "Answer" && data.callType == "answerCandidate") {
                    listener.onIceCandidateReceived(
                        IceCandidate(data.sdpMid.toString(),
                            Math.toIntExact(data.sdpMLineIndex.toLong()),
                            data.sdpCandidate.toString()))
                }
                Log.e(TAG, "candidateQuery: $data" )
            }
        }
    }

    private fun addCallsSdpSubscription() {

        try {
            val snapshotQuery = CloudDBZoneQuery.where(CallsSdp::class.java).equalTo("shadow_flag", true)
            mRegisterSdp = cloudDBZone?.subscribeSnapshot(snapshotQuery,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mCallsSdpSnaphostListener)
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "subscribeSnapshot: " + e.message)
        }
    }

    private fun addCallsCandidatesSubscription() {

        try {
            val snapshotQuery = CloudDBZoneQuery.where(CallsCandidates::class.java).equalTo("shadow_flag", true)
            mRegisterCandidate = cloudDBZone?.subscribeSnapshot(snapshotQuery,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mCallsCandidatesSnaphostListener)
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "subscribeSnapshot: " + e.message)
        }
    }

    fun destroy() {
//        client.close()
        job.complete()
    }
}
