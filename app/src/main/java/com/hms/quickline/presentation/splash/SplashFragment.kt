package com.hms.quickline.presentation.splash

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.navigate
import com.hms.quickline.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment(R.layout.fragment_splash) {

    private val binding by viewBinding(FragmentSplashBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFragmentNavigation.setBottomBarVisibility(false)

        lifecycleScope.launch {
            delay(2000)
            navigate(SplashFragmentDirections.actionSplashFragmentToHome())
        }
    }
}