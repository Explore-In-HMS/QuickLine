package com.hms.quickline.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.showToastShort
import com.hms.quickline.databinding.FragmentHomeBinding
import com.hms.quickline.presentation.call.webrtc.CallActivity
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)

    private val viewModel: HomeViewModel by viewModels()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFragmentNavigation.setBottomBarVisibility(true)

        binding.btnAuth.setOnClickListener {
            AGConnectAuth.getInstance()
                .signIn(requireActivity(), AGConnectAuthCredential.HMS_Provider)
                .addOnSuccessListener {
                    showToastShort(requireContext(), "giriş yapıldı")
                }
                .addOnFailureListener {  }
        }

        binding.btnJoin.setOnClickListener {
            val intent = Intent(requireActivity(), CallActivity::class.java)
            intent.putExtra("meetingID","Meeting3")
            intent.putExtra("isJoin",true)
            startActivity(intent)
        }
    }
}