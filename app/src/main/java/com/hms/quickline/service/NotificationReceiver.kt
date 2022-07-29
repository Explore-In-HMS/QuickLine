package com.hms.quickline.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hms.quickline.R
import com.hms.quickline.core.util.Constants
import com.huawei.hms.feature.dynamic.b.u
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uid = intent.getStringExtra(Constants.UID)
        val callerName = intent.getStringExtra(Constants.CALLER_NAME) ?: context.resources.getString(R.string.unknown)

        val notificationUtils = NotificationUtils(context,uid, callerName)
        val notification = notificationUtils.getNotificationBuilder().build()
        notificationUtils.getManager().notify(150, notification)
    }
}