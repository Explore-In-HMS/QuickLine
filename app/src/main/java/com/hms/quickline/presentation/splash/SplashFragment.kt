package com.hms.quickline.presentation.splash

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.navigate
import com.hms.quickline.core.util.showToastShort
import com.hms.quickline.data.model.Users
import com.hms.quickline.databinding.FragmentSplashBinding
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.cloud.database.CloudDBZone
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment(R.layout.fragment_splash) {

    private val binding by viewBinding(FragmentSplashBinding::bind)

    private var cloudDBZone: CloudDBZone? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFragmentNavigation.setBottomBarVisibility(false)

        lifecycleScope.launch {
            delay(2000)

            AGConnectAuth.getInstance().currentUser?.let {
                navigate(SplashFragmentDirections.actionSplashFragmentToHome())
            }

            cloudDBZone = CloudDbWrapper.cloudDBZone

            binding.btnAuth.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setListener(null)
            }

            binding.btnAuth.setOnClickListener {
                AGConnectAuth.getInstance()
                    .signIn(requireActivity(), AGConnectAuthCredential.HMS_Provider)
                    .addOnSuccessListener {

                        AGConnectAuth.getInstance().currentUser.apply {
                            val currentUser = Users()
                            currentUser.uid = uid
                            currentUser.name = displayName
                            currentUser.email = email
                            currentUser.photo = photoUrl
                            currentUser.phone = phone

                            val upsertTask = cloudDBZone?.executeUpsert(currentUser)
                            upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                                Log.i("UpsertUser", "User Upsert success: $cloudDBZoneResult")
                                navigate(SplashFragmentDirections.actionSplashFragmentToHome())
                            }?.addOnFailureListener {
                                Log.i("UpsertUser", "User Upsert failed: ${it.message}")
                            }
                        }
                    }
                    .addOnFailureListener {}
            }
        }
    }
}