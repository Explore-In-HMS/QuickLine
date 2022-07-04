package com.hms.quickline.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.UserDictionary.Words.APP_ID
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.api.entity.safetydetect.SysIntegrityRequest
import com.huawei.hms.support.api.safetydetect.SafetyDetect
import com.huawei.hms.support.api.safetydetect.SafetyDetectStatusCodes
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

@AndroidEntryPoint
class MainActivity : BaseActivity(), BaseFragment.FragmentNavigation {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        invokeSysIntegrity()

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

    override fun onDestroy() {
        CloudDbWrapper.closeCloudDBZone()
        super.onDestroy()
    }

    private fun invokeSysIntegrity() {
// TODO (developer): Change the nonce generation to include your own value.
        val nonce = ByteArray(24)
        try {
            val random: SecureRandom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SecureRandom.getInstanceStrong()
            } else {
                SecureRandom.getInstance("SHA1PRNG")
            }
            random.nextBytes(nonce)
        } catch (e: NoSuchAlgorithmException) {
            Log.e("NoSuchAlgorithmException", e.message!!)
        }
// TODO (developer): Change your app ID. You can obtain your app ID in AppGallery Connect.
        val sysIntegrityRequest = SysIntegrityRequest()
        sysIntegrityRequest.appId = "105993909"
        sysIntegrityRequest.nonce = nonce
        sysIntegrityRequest.alg = "RS256"

        SafetyDetect.getClient(this)
            .sysIntegrity(sysIntegrityRequest)
            .addOnSuccessListener { response -> // Indicates communication with the service was successful.
                // Use response.getResult() to obtain the result data.
                val jwsStr = response.result

// Process the result data here.
                val jwsSplit = jwsStr.split(".").toTypedArray()
                val jwsPayloadStr = jwsSplit[1]
                val payloadDetail = String(
                    Base64.decode(
                        jwsPayloadStr.toByteArray(StandardCharsets.UTF_8),
                        Base64.URL_SAFE
                    ), StandardCharsets.UTF_8
                )
                try {
                    val jsonObject = JSONObject(payloadDetail)
                    val basicIntegrity = jsonObject.getBoolean("basicIntegrity")
                    //fg_button_sys_integrity_go.setBackgroundResource(if (basicIntegrity) R.drawable.btn_round_green else R.drawable.btn_round_red)
                    //fg_button_sys_integrity_go.setText(R.string.rerun)
                    val isBasicIntegrity = basicIntegrity.toString()
                    val basicIntegrityResult = "Basic Integrity: $isBasicIntegrity"
                   // fg_payloadBasicIntegrity.text = basicIntegrityResult
                    if (!basicIntegrity) {
                        val advice = "Advice: " + jsonObject.getString("advice")
                      //  fg_payloadAdvice.text = advice
                    }
                } catch (e: JSONException) {
                    val errorMsg = e.message
                    Log.e("JsonException", errorMsg ?: "unknown error")
                }
            }
            .addOnFailureListener { e -> // There was an error communicating with the service.
                val errorMsg: String?
                errorMsg = if (e is ApiException) {
// An error with the HMS API contains some additional details.
                    val apiException = e as ApiException
                    SafetyDetectStatusCodes.getStatusCodeString(apiException.statusCode) +
                            ": " + apiException.message
// You can use the apiException.getStatusCode() method to obtain the status code.
                } else {
// An unknown type of error has occurred.
                    e.message
                }
                Log.e("aaa", errorMsg!!)
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
               // fg_button_sys_integrity_go.setBackgroundResource(R.drawable.btn_round_yellow)
                //fg_button_sys_integrity_go.setText(R.string.rerun)
            }
    }
}