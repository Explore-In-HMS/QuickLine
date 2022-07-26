package com.hms.quickline.presentation.call.service

/**
 * 功能描述
 *
 * @author b00557735
 * @since 2022-05-12
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hms.quickline.core.util.Constants.ANSWER
import com.hms.quickline.core.util.Constants.CALLER_NAME
import com.hms.quickline.core.util.Constants.DECLINE
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.IS_JOIN
import com.hms.quickline.core.util.Constants.UID
import com.hms.quickline.data.model.Users
import com.hms.quickline.presentation.call.VideoCallActivity
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.cloud.database.CloudDBZone
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ActionReceiver : BroadcastReceiver() {
    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone
    private val TAG = "ActionReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        val callerName = intent.getStringExtra(CALLER_NAME)
        val notificationUtils = callerName?.let { NotificationUtils(context, callerName = it) }
        val uid = intent.getStringExtra(UID)
        when(intent.action){
            ANSWER -> {
                val i = Intent(context, VideoCallActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                Log.i(TAG,uid.toString())
                i.putExtra(MEETING_ID,uid)
                i.putExtra(IS_JOIN, true)
                context.startActivity(i)
                notificationUtils?.getManager()?.cancel(150)
            }
            DECLINE -> {
                notificationUtils?.getManager()?.cancel(150)


                uid?.let {
                    CloudDbWrapper.getUserById(it,object: CloudDbWrapper.ICloudDbWrapper{
                        override fun onUserObtained(users: Users) {
                            users.isCalling = false

                            val upsertTask = users.let { cloudDBZone?.executeUpsert(it) }
                            upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                                Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
                            }?.addOnFailureListener {
                                Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
                            }
                        }
                    })
                }

            }
        }

    }
}