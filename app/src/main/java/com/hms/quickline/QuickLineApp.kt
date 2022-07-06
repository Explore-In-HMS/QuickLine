package com.hms.quickline

import android.app.Application
import android.util.Log
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.crash.AGConnectCrash
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuickLineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AGConnectCrash.getInstance()

        CloudDbWrapper.initialize(this) {
            Log.i("Application", it.toString())
        }
    }
}