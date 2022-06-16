package com.hms.quickline.presentation.home

import android.util.Log
import com.hms.quickline.core.base.BaseViewModel
import com.hms.quickline.data.model.CallsSdp
import com.hms.quickline.domain.usecase.HomeUseCase
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val homeUseCase: HomeUseCase) : BaseViewModel() {
    private val TAG = "HomeViewModel"

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
}