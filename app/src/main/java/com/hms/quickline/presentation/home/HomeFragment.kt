package com.hms.quickline.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.Constants
import com.hms.quickline.core.util.Constants.IS_MEETING_CONTACT
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.NAME
import com.hms.quickline.core.util.navigate
import com.hms.quickline.core.util.showToastLong
import com.hms.quickline.databinding.FragmentHomeBinding
import com.hms.quickline.presentation.call.VideoCallActivity
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.crash.AGConnectCrash
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFragmentNavigation.setBottomBarVisibility(true)

        var name = ""
        AGConnectAuth.getInstance().currentUser?.let {
            name = it.displayName
        }

        with(binding) {
            btnJoin.setOnClickListener {
                val selectedMeetingId = etMeetingId.text.toString()
                viewModel.checkMeetingId(selectedMeetingId) { hasMeetingId ->
                    if (hasMeetingId && selectedMeetingId.isNotEmpty()) {
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
                val selectedMeetingId = etMeetingId.text.toString()
                if (selectedMeetingId.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        resources.getString(R.string.empty_meetingid_error_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    AGConnectCrash.getInstance()
                    return@setOnClickListener
                }

                val intent = Intent(requireActivity(), VideoCallActivity::class.java)
                intent.putExtra(MEETING_ID, selectedMeetingId)
                intent.putExtra(Constants.IS_JOIN, false)
                startActivity(intent)
            }

            ivLogout.setOnClickListener {
                AGConnectAuth.getInstance().signOut()
                navigate(HomeFragmentDirections.actionHomeFragmentToSplash())
            }
        }
    }
}