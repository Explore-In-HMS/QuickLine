package com.hms.quickline.presentation.call.service

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hms.quickline.R
import com.hms.quickline.core.util.Constants.ANSWER
import com.hms.quickline.core.util.Constants.DECLINE
import com.hms.quickline.core.util.Constants.UID
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class NotificationUtils(context: Context,private val uid: String? = null) : ContextWrapper(context) {

    private var manager: NotificationManager? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val channel = NotificationChannel(MY_CHANNEL_ID, MY_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        channel.enableVibration(true)
        getManager().createNotificationChannel(channel)
    }

    fun getManager() : NotificationManager {
        if (manager == null) manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager as NotificationManager
    }

    fun getNotificationBuilder(): NotificationCompat.Builder {
        val intentDecline = Intent(applicationContext, ActionReceiver::class.java)
        intentDecline.putExtra(UID, uid)
        intentDecline.action = DECLINE

        val intentAnswer = Intent(applicationContext, ActionReceiver::class.java)
        intentAnswer.action = ANSWER
        intentAnswer.putExtra(UID, uid)


        val pendingIntentAnswer = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intentAnswer,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pendingIntentDecline = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intentDecline,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        val actionDecline = NotificationCompat.Action.Builder(
            0,
            getString(R.string.decline), pendingIntentDecline
        ).build()

        val actionAnswer = NotificationCompat.Action.Builder(
            0,
            getString(R.string.answer), pendingIntentAnswer
        )
            .build()

        return NotificationCompat.Builder(applicationContext, MY_CHANNEL_ID)
            .setContentTitle("Quick Line")
            .setContentText("Incoming Call!")
            .setSmallIcon(R.drawable.hwid_auth_button_normal)
            .setColor(Color.YELLOW)
            .addAction(actionAnswer)
            .addAction(actionDecline)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
    }

    companion object {
        private const val MY_CHANNEL_ID = "App Alert Notification ID"
        private const val MY_CHANNEL_NAME = "App Alert Notification"
    }
}
