package com.hms.quickline.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hms.quickline.core.base.BaseViewModel
import com.hms.quickline.data.model.CallsSdp
import com.hms.quickline.data.model.Users
import com.hms.quickline.domain.repository.CloudDbWrapper
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {
    private val TAG = "HomeViewModel"

    private val availableLiveData: MutableLiveData<Boolean> = MutableLiveData()
    fun getAvailableLiveData(): LiveData<Boolean> = availableLiveData

    /**
     * Check room exists in CloudDatabase
     */
    fun checkMeetingId(meetingId: String, hasMeetingId: (Boolean) -> Unit) {

        CloudDbWrapper.checkMeetingId(meetingId, object : CloudDbWrapper.ResultListener {
            override fun onSuccess(result: Any?) {
                val resultList: ArrayList<CallsSdp>? = result as? ArrayList<CallsSdp>

                resultList?.forEach {
                    if (it.meetingID == meetingId) hasMeetingId(true) else hasMeetingId(false)
                }
            }

            override fun onFailure(e: Exception) {
                e.localizedMessage?.let {
                    if (it == "noElements")
                        hasMeetingId(false)
                    else
                        Log.e(TAG,"Error MeetingIdCheck")
                }
            }
        })
    }

    fun checkAvailable(id : String) {
        CloudDbWrapper.getUserById(id, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {
                availableLiveData.value = users.isAvailable
            }
        })
    }

    fun updateAvailable(id : String, isAvailable: Boolean,cloudDBZone : CloudDBZone) {
        CloudDbWrapper.getUserById(id, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {

                users.isAvailable = isAvailable

                cloudDBZone.executeUpsert(users)?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i("HomeFragmentBusy", "Available data success: $cloudDBZoneResult")
                }?.addOnFailureListener {
                    Log.e("HomeFragmentBusy", "Available data failed: ${it.message}")
                }
            }
        })
    }
}