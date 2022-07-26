package com.hms.quickline.ui.recentcalls

import android.os.Bundle
import android.view.View
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.databinding.FragmentRecentCallsBinding

class RecentCallsFragment : BaseFragment(R.layout.fragment_recent_calls) {

    private val binding by viewBinding(FragmentRecentCallsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }
}