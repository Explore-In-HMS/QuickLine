package com.hms.quickline.service

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
import com.hms.quickline.core.util.Constants.DECLINE
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.IS_JOIN
import com.hms.quickline.core.util.Constants.UID
import com.hms.quickline.data.model.Users
import com.hms.quickline.ui.call.VideoCallActivity
import com.hms.quickline.domain.repository.CloudDbWrapper
import com.huawei.agconnect.cloud.database.CloudDBZone
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ActionReceiver : BroadcastReceiver() {

    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone
    private val TAG = "ActionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val notificationUtils = NotificationUtils(context)
        val uid = intent.getStringExtra(UID)

        when(intent.action){
            ANSWER -> {
                val videoIntent = Intent(context, VideoCallActivity::class.java)
                videoIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.i(TAG,uid.toString())
                    putExtra(MEETING_ID,uid)
                    putExtra(IS_JOIN, true)
                }

                context.startActivity(videoIntent)
                notificationUtils.getManager().cancel(150)
            }
            DECLINE -> {
                notificationUtils.getManager().cancel(150)

                uid?.let { id ->
                    CloudDbWrapper.getUserById(id,object: CloudDbWrapper.ICloudDbWrapper{
                        override fun onUserObtained(users: Users) {
                            users.isCalling = false

                            val upsertTask = users.let { cloudDBZone?.executeUpsert(it) }
                            upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                                Log.i(TAG, "Calls UserCalling Upsert success: $cloudDBZoneResult")
                            }?.addOnFailureListener { exp->
                                Log.i(TAG, "Calls UserCalling Upsert failed: ${exp.message}")
                            }
                        }
                    })
                }

            }
        }

    }
}