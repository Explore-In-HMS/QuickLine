package com.hms.quickline.core.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment {
    lateinit var mFragmentNavigation: FragmentNavigation
    var baseContext: Context? = null

    constructor() : super()
    constructor(contentLayoutId: Int) : super(contentLayoutId)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseContext = context
        mFragmentNavigation = context as FragmentNavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.v("LifeCycle", "${name()} onViewCreated")
    }

    private fun name(): String {
        return this.javaClass.simpleName
    }

    interface FragmentNavigation {
        fun setBottomBarVisibility(isVisible: Boolean = true)
        fun giveAction(action: Int)
        fun navigateUP()
        fun navigateTop()
    }
}