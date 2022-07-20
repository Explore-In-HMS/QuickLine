package com.hms.quickline.ui.call

import android.util.Log
import com.hms.quickline.core.base.BaseViewModel
import com.hms.quickline.data.model.Users
import com.hms.quickline.domain.repository.CloudDbWrapper
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VideoCallViewModel @Inject constructor() : BaseViewModel() {

    private val TAG = "VideoCallViewModel"

    fun getUserCalling(meetingID: String, cloudDBZone: CloudDBZone) {

        CloudDbWrapper.getUserById(meetingID, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {
                users.isCalling = false

                cloudDBZone.executeUpsert(users)?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i(TAG, "User Calling Info Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.e(TAG, "User Calling Info Upsert failed: ${it.message}")
                }
            }
        })
    }

    fun getUserAvailable(userId: String, isAvailable: Boolean, cloudDBZone: CloudDBZone) {

        CloudDbWrapper.getUserById(userId, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {
                users.isAvailable = isAvailable

                cloudDBZone.executeUpsert(users)?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i(TAG, "User Calling Info Upsert success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.e(TAG, "User Calling Info Upsert failed: ${it.message}")
                }
            }
        })
    }

}