package com.hms.quickline.ui.home

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
import com.hms.quickline.core.util.showToastShort
import com.hms.quickline.databinding.FragmentHomeBinding
import com.hms.quickline.ui.call.VideoCallActivity
import com.huawei.agconnect.auth.AGConnectAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var agConnectAuth: AGConnectAuth

    private var name = ""
    private var userId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFragmentNavigation.setBottomBarVisibility(true)

        agConnectAuth.currentUser?.let {
            name = it.displayName
            userId = it.uid
        }

        initClickListeners()
        observeData()
        viewModel.getPushToken(requireContext())
    }

    private fun observeData() {
        viewModel.getUserPushTokenLiveData().observe(viewLifecycleOwner) {
            Log.i("PushNotificationTAG", "get token:$it")
        }
    }

    private fun initClickListeners() {
        with(binding) {

            btnJoin.setOnClickListener {
                val selectedMeetingId = etMeetingId.text.toString()

                viewModel.checkMeetingId(selectedMeetingId) { hasMeetingId ->

                    if (hasMeetingId && selectedMeetingId.isNotEmpty()) {
                        val intent = Intent(requireActivity(), VideoCallActivity::class.java)

                        intent.apply {
                            putExtra(IS_MEETING_CONTACT, false)
                            putExtra(MEETING_ID, selectedMeetingId)
                            putExtra(NAME, name)
                            putExtra(Constants.IS_JOIN, true)
                        }

                        startActivity(intent)

                    } else {
                        showToastLong(binding.root.context, getString(R.string.no_room_message))
                    }
                }
            }

            btnCreate.setOnClickListener {
                val selectedMeetingId = etMeetingId.text.toString()

                if (selectedMeetingId.isEmpty()) {
                    showToastShort(
                        requireContext(),
                        resources.getString(R.string.empty_meetingid_error_message)
                    )
                    return@setOnClickListener
                }

                val intent = Intent(requireActivity(), VideoCallActivity::class.java)
                intent.apply {
                    putExtra(MEETING_ID, selectedMeetingId)
                    putExtra(Constants.IS_JOIN, false)
                }

                startActivity(intent)
                //navigate(HomeFragmentDirections.actionHomeFragmentToSplash())
            }
        }
    }
}
