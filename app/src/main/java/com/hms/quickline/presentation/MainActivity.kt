package com.hms.quickline.presentation

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseActivity
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.setupWithNavController
import com.hms.quickline.core.util.showToastLong
import com.hms.quickline.databinding.ActivityMainBinding
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.hms.quickline.presentation.call.service.CallService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity(), BaseFragment.FragmentNavigation {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        startService(Intent(this,CallService::class.java))

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }

        CloudDbWrapper.initialize(this) {
            showToastLong(this, "$it")
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {
        val navGraphIds =
            listOf(
                R.navigation.main_nav_graph,
                R.navigation.home
            )

        val controller = binding.bottomNav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        controller.observe(this) { navController ->
            NavigationUI.setupActionBarWithNavController(this, navController)
        }
        currentNavController = controller
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.actionbar_call, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_call -> {
                CallDialog().show(supportFragmentManager,"CallDialog")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun giveAction(action: Int) {
        currentNavController?.value?.navigate(action)
    }

    override fun navigateUP() {
        currentNavController?.value?.navigateUp()
    }

    override fun setBottomBarVisibility(isVisible: Boolean) {
        binding.bottomNav.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}