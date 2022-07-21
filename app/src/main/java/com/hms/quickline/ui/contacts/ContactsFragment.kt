package com.hms.quickline.ui.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.core.util.Constants.IS_JOIN
import com.hms.quickline.core.util.Constants.IS_MEETING_CONTACT
import com.hms.quickline.core.util.Constants.MEETING_ID
import com.hms.quickline.core.util.Constants.NAME
import com.hms.quickline.core.util.gone
import com.hms.quickline.core.util.showToastShort
import com.hms.quickline.core.util.visible
import com.hms.quickline.data.model.Users
import com.hms.quickline.databinding.FragmentContactsBinding
import com.hms.quickline.ui.call.VideoCallActivity
import com.hms.quickline.ui.call.VoiceCallActivity
import com.hms.quickline.domain.repository.CloudDbWrapper
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.CloudDBZone
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCapture
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCaptureConfig
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCaptureConfig.DETECT_MASK
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCaptureResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ContactsFragment : BaseFragment(R.layout.fragment_contacts),
    ContactsAdapter.ICallDialogAdapter {

    companion object  {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    private val binding by viewBinding(FragmentContactsBinding::bind)
    private val viewModel: ContactsViewModel by viewModels()


    private lateinit var adapter: ContactsAdapter

    private var cloudDBZone: CloudDBZone? = null
    private lateinit var user: Users

    @Inject
    lateinit var agConnectAuth: AGConnectAuth

    private val TAG = "ContactsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cloudDBZone = CloudDbWrapper.cloudDBZone

        viewModel.getUserList()
        observeData()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.getUserList()
            binding.swipeRefreshLayout.isRefreshing = false
        }
        binding.btnVerify.setOnClickListener { detect() }

        agConnectAuth.currentUser?.let {
            viewModel.getUser(it.uid)
        }
    }

    private fun observeData() {
        viewModel.getUserLiveData().observe(viewLifecycleOwner, {
            if (!it.isVerified)
                binding.btnVerify.visible()

            user = it
        })

        viewModel.getUserListLiveData().observe(viewLifecycleOwner, {

            binding.rvMeetingIdList.layoutManager = LinearLayoutManager(requireContext())
            adapter = ContactsAdapter(it, this)
            binding.rvMeetingIdList.adapter = adapter
        })
    }

    private fun detect(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            livenessDetection()
            return
        }

        ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, 0)
    }

    private fun livenessDetection() {
        //Obtain liveness detection config and set detect mask and sunglasses
        val captureConfig: MLLivenessCaptureConfig = MLLivenessCaptureConfig.Builder().setOptions(DETECT_MASK).build()

        // Obtains the liveness detection plug-in instance.
        val capture: MLLivenessCapture = MLLivenessCapture.getInstance()
        capture.setConfig(captureConfig)
        capture.startDetect(requireActivity(), this.callback)
    }

    override fun onItemSelected(isVoiceCall: Boolean, user: Users) {
        val intent = if (isVoiceCall)
            Intent(requireActivity(), VoiceCallActivity::class.java)
        else
            Intent(requireActivity(), VideoCallActivity::class.java)
        intent.apply {
            putExtra(IS_MEETING_CONTACT, true)
            putExtra(MEETING_ID, user.uid)
            putExtra(NAME, user.name)
            putExtra(IS_JOIN, false)
        }

        startActivity(intent)

        user.isCalling = true

        val upsertTask = cloudDBZone?.executeUpsert(user)
        upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
        }?.addOnFailureListener {
            Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
        }

    }

    //Callback for receiving the liveness detection result.
    private val callback: MLLivenessCapture.Callback = object : MLLivenessCapture.Callback {
        /**
         * Liveness detection success callback.
         * @param result result
         */
        override fun onSuccess(result: MLLivenessCaptureResult) {
            Log.i(TAG, "success")
            if (result.isLive){
                user.isVerified = true

                val upsertTask = cloudDBZone?.executeUpsert(user)
                upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
                    Log.i("UpsertUser", "User Upsert success: $cloudDBZoneResult")
                    showToastShort(requireContext(),resources.getText(R.string.user_verified_message).toString())

                    viewModel.getUserList()
                    binding.btnVerify.gone()
                }?.addOnFailureListener {
                    Log.i("UpsertUser", "User Upsert failed: ${it.message}")

                }
            }else{
                showToastShort(requireContext(),resources.getText(R.string.user_verified_error_message).toString())
            }
        }

        override fun onFailure(errorCode: Int) {
            Log.i(TAG, "error")
        }
    }

    // Permission application callback.
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult ")

        livenessDetection()

    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.i(
            TAG,
            "onActivityResult requestCode $requestCode, resultCode $resultCode"
        )
    }
}