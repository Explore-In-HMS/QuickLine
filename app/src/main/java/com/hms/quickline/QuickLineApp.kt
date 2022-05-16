package com.hms.quickline

import android.app.Application
import android.util.Log
import com.hms.quickline.core.util.showToastLong
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuickLineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudDbWrapper.initialize(this) {
            Log.i("Application", it.toString())
        }
    }
}