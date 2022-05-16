package com.hms.quickline.presentation

import android.content.Intent
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseActivity
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.setupWithNavController
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
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {
        val navGraphIds =
            listOf(
                R.navigation.main_nav_graph,
                R.navigation.home,
                R.navigation.contacts,
                R.navigation.recentcalls
            )

        val controller = binding.bottomNav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        currentNavController = controller

        checkPermissions(this)
    }

    private fun checkPermissions(activity: Activity) {

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("checkPermissions", "No Permissions")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                100
            )
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