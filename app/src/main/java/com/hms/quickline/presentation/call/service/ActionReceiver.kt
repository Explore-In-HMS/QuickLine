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
import com.hms.quickline.core.util.Constants.ACTION_ANSWER
import com.hms.quickline.core.util.Constants.ACTION_DECLINE
import com.hms.quickline.core.util.Constants.ANSWER
import com.hms.quickline.core.util.Constants.DECLINE
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.IS_JOIN
import com.hms.quickline.core.util.Constants.UID
import com.hms.quickline.presentation.call.newwebrtc.CallActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationUtils = NotificationUtils(context)

        val answer = intent.getStringExtra(ACTION_ANSWER)
        val decline = intent.getStringExtra(ACTION_DECLINE)
        val uid = intent.getStringExtra(UID)

        if (answer == ACTION_ANSWER){
            val i = Intent(context, CallActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.putExtra(MEETING_ID, uid)
            i.putExtra(IS_JOIN, false)
            context.startActivity(i)
        }
        else if (decline == DECLINE){
            notificationUtils.getManager().cancel(150)
        }

    }
}