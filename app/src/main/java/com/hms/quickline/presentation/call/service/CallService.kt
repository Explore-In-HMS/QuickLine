package com.hms.quickline.presentation.call.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.hms.quickline.core.util.Constants
import com.hms.quickline.data.model.Users

import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import kotlinx.coroutines.ExperimentalCoroutinesApi


/**
 * 功能描述
 *
 * @author b00557735
 * @since 2022-05-12
 */
@ExperimentalCoroutinesApi
class CallService: Service() {
    private var cloudDBZone: CloudDBZone? = CloudDbWrapper.cloudDBZone
    private var registerUser: ListenerHandler? = null
    private val TAG = "CallService"


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        addUserSubscription()
        return super.onStartCommand(intent, flags, startId)
    }

    private val userSnapshotListener = OnSnapshotListener<Users> { cloudDBZoneSnapshot, e ->

        e?.let {
            Log.w(TAG, "onSnapshot: " + e.message)
            return@OnSnapshotListener
        }

        val snapshot = cloudDBZoneSnapshot.snapshotObjects
        var users = Users()

        try {
            while (snapshot.hasNext()) {
                users = snapshot.next()
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "Snapshot Error: " + e.message)
        } finally {
            Log.i(TAG,"isCalling:${users.isCalling}")
            if (users.isCalling == true){
                showCallNotification(users.uid)
            }
        }
    }

    private fun showCallNotification(uid: String) {
        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).putExtra(Constants.UID,uid)

        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent)
    }

    private fun addUserSubscription() {

        try {
            val snapshotQuery =
                CloudDBZoneQuery.where(Users::class.java).equalTo(Constants.UID, Constants.UID)
            registerUser = cloudDBZone?.subscribeSnapshot(
                snapshotQuery,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY,
                userSnapshotListener
            )
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "subscribeSnapshot: " + e.message)
        }
    }
}