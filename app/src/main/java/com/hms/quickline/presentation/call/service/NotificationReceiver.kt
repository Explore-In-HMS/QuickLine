package com.hms.quickline.presentation.call.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hms.quickline.core.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uid = intent.getStringExtra(Constants.UID)!!

        val notificationUtils = NotificationUtils(context,uid)
        val notification = notificationUtils.getNotificationBuilder().build()
        notificationUtils.getManager().notify(150, notification)
    }
}