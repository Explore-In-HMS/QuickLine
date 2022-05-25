package com.hms.quickline.presentation.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.Constants
import com.hms.quickline.core.util.Constants.IS_MEETING_CONTACT
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.NAME
import com.hms.quickline.core.util.showToastLong
import com.hms.quickline.data.model.CallsSdp
import com.hms.quickline.data.model.Users
import com.hms.quickline.databinding.FragmentHomeBinding
import com.hms.quickline.presentation.call.VideoCallActivity
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val TAG = "HomeFragmentTag"


    private val viewModel: HomeViewModel by viewModels()

    private var cloudDBZone: CloudDBZone? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFragmentNavigation.setBottomBarVisibility(true)

        cloudDBZone = CloudDbWrapper.cloudDBZone

        var name = ""

        AGConnectAuth.getInstance().currentUser?.let {
            name = it.displayName
        }

        with(binding) {
            val selectedMeetingId = etMeetingId.text.toString()

            btnJoin.setOnClickListener {
                checkMeetingId(selectedMeetingId) { hasMeetingId ->
                    if (hasMeetingId) {
                        val intent = Intent(requireActivity(), VideoCallActivity::class.java)
                        intent.putExtra(IS_MEETING_CONTACT, false)
                        intent.putExtra(MEETING_ID, selectedMeetingId)
                        intent.putExtra(NAME, name)
                        intent.putExtra(Constants.IS_JOIN, true)
                        startActivity(intent)
                    } else {
                        showToastLong(binding.root.context, getString(R.string.no_room_message))
                    }
                }

            }

            btnCreate.setOnClickListener {
                val intent = Intent(requireActivity(), VideoCallActivity::class.java)
                intent.putExtra(MEETING_ID, selectedMeetingId)
                intent.putExtra(Constants.IS_JOIN, false)
                startActivity(intent)
            }
        }
    }

    /**
     * Check room exists in CloudDatabase
     */
    private fun checkMeetingId(meetingId: String, hasMeetingId: (Boolean) -> Unit) {

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