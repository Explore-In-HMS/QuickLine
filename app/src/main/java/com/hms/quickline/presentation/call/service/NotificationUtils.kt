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
import com.hms.quickline.core.util.Constants.ACTION_ANSWER
import com.hms.quickline.core.util.Constants.ACTION_DECLINE
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
        val answerIntent = Intent(this, ActionReceiver::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val declineIntent = Intent(this, ActionReceiver::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        answerIntent.putExtra(ACTION_ANSWER, ANSWER)
        answerIntent.putExtra(UID, uid)
        declineIntent.putExtra(ACTION_DECLINE, DECLINE)

        val pendingAnswerIntent = PendingIntent.getBroadcast(this, 0, answerIntent, 0)
        val pendingIntent = PendingIntent.getActivity(this, 0, answerIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingDeclineIntent = PendingIntent.getBroadcast(this, 0, declineIntent, 0)

        return NotificationCompat.Builder(applicationContext, MY_CHANNEL_ID)
            .setContentTitle("Alarm!")
            .setContentText("Your AlarmManager is working.")
            .setSmallIcon(R.drawable.hwid_auth_button_normal)
            .setColor(Color.YELLOW)
           // .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_baseline_call_end_24,getString(R.string.answer),pendingAnswerIntent)
            .addAction(R.drawable.upsdk_cancel_normal,getString(R.string.decline),pendingDeclineIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
    }

    companion object {
        private const val MY_CHANNEL_ID = "App Alert Notification ID"
        private const val MY_CHANNEL_NAME = "App Alert Notification"
    }
}
