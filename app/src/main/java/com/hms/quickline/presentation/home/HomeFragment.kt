package com.hms.quickline.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.Constants
import com.hms.quickline.databinding.FragmentHomeBinding
import com.hms.quickline.presentation.call.VideoCallActivity
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)

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

            btnJoin.setOnClickListener {
                val intent = Intent(requireActivity(), VideoCallActivity::class.java)
                intent.putExtra("isMeetingContact", false)
                intent.putExtra("meetingID", etMeetingId.text.toString())
                intent.putExtra("name", name)
                intent.putExtra(Constants.IS_JOIN, true)
                startActivity(intent)
            }

            btnCreate.setOnClickListener {
                val intent = Intent(requireActivity(), VideoCallActivity::class.java)
                intent.putExtra("meetingID", etMeetingId.text.toString())
                intent.putExtra(Constants.IS_JOIN, false)
                startActivity(intent)
            }
        }
    }
}