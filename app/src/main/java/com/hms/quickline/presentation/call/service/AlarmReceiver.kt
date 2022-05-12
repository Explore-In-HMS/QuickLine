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
import com.hms.quickline.presentation.call.newwebrtc.CallActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationUtils = NotificationUtils(context)
        val notification = notificationUtils.getNotificationBuilder().build()
        notificationUtils.getManager().notify(150, notification)

        val answer = intent.getStringExtra("actionAnswer")
        if (answer == "answer"){
            val intent = Intent(context,CallActivity::class.java)
            intent.putExtra("meetingID", intent.getStringExtra("uid"))
            intent.putExtra("isJoin", true)
            context.startActivity(intent)
        }else if (intent.action == "decline"){
            notificationUtils.getManager().cancel(150)
        }
    }
}