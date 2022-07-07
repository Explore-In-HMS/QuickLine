package com.hms.quickline

import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class QuickLineMessagingService : HmsMessageService() {

    companion object {
        private const val TAG = "QuickLineMessagingService"
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        Log.i(TAG, "onMessageReceived is called")

        // Check whether the message is empty.
        if (message == null)
        {
            Log.e(TAG, "Received message entity is null!")
            return
        }

        // Obtain the message content.
        Log.i(TAG, """getData: ${message.data}        
        getFrom: ${message.from}        
        getTo: ${message.to}        
        getMessageId: ${message.messageId}
        getSentTime: ${message.sentTime}           
        getDataMap: ${message.dataOfMap}
        getMessageType: ${message.messageType}   
        getTtl: ${message.ttl}        
        getToken: ${message.token}""".trimIndent())

        val judgeWhetherIn10s = false
        // If the message is not processed within 10 seconds, create a job to process it.
        if (judgeWhetherIn10s) {
            startWorkManagerJob()
        } else {
            // Process the message within 10 seconds.
            processWithin10s()
        }
    }
    private fun startWorkManagerJob() {
        Log.d(TAG, "Start new Job processing.")
    }
    private fun processWithin10s() {
        Log.d(TAG, "Processing now.")
    }

}