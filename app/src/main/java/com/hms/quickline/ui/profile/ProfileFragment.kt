package com.hms.quickline.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.databinding.FragmentProfileBinding
import com.hms.quickline.domain.repository.CloudDbWrapper
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment(R.layout.fragment_profile) {

    private val binding by viewBinding(FragmentProfileBinding::bind)
    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var agConnectAuth: AGConnectAuth

    private var cloudDBZone: CloudDBZone? = null

    private var name = ""
    private var userId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        agConnectAuth.currentUser?.let {
            name = it.displayName
            userId = it.uid
        }

        cloudDBZone = CloudDbWrapper.cloudDBZone

        CloudDbWrapper.updateLastSeen(userId, Date())

        initAvailable()
        observeData()
        viewModel.checkAvailable(userId)
    }

    private fun initAvailable() {
        binding.btnBusy.setOnCheckedChangeListener { _, isChecked ->
            cloudDBZone?.let {
                viewModel.updateAvailable(userId, !isChecked, it)
            }
        }
    }

    private fun observeData() {
        viewModel.getAvailableLiveData().observe(viewLifecycleOwner, {
            binding.btnBusy.isChecked = !it
        })

        viewModel.getUserPushTokenLiveData().observe(viewLifecycleOwner, {
            Log.i("PushNotificationTAG", "get token:$it")
        })
    }

}
