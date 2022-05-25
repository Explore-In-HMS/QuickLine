package com.hms.quickline.presentation.contacts

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.hms.quickline.R
import com.hms.quickline.core.base.BaseFragment
import com.hms.quickline.core.common.viewBinding
import com.hms.quickline.data.model.Users
import com.hms.quickline.databinding.FragmentContactsBinding
import com.hms.quickline.presentation.call.VideoCallActivity
import com.hms.quickline.presentation.call.VoiceCallActivity
import com.hms.quickline.presentation.call.newwebrtc.CloudDbWrapper
import com.huawei.agconnect.cloud.database.CloudDBZone
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException

class ContactsFragment : BaseFragment(R.layout.fragment_contacts),
    ContactsAdapter.ICallDialogAdapter {

    private val binding by viewBinding(FragmentContactsBinding::bind)

    private lateinit var adapter: ContactsAdapter

    private var cloudDBZone: CloudDBZone? = null

    private val TAG = "ContactsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cloudDBZone = CloudDbWrapper.cloudDBZone

        queryUsers()

        binding.swipeRefreshLayout.setOnRefreshListener {
            queryUsers()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun queryUsers() {

        val queryUsers = CloudDBZoneQuery.where(Users::class.java)

        val queryTask = cloudDBZone?.executeQuery(
            queryUsers,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )
        queryTask?.addOnSuccessListener { snapshot ->
            val usersList = arrayListOf<Users>()
            try {
                while (snapshot.snapshotObjects.hasNext()) {
                    val user = snapshot.snapshotObjects.next()
                    usersList.add(user)
                }
            } catch (e: AGConnectCloudDBException) {
                Log.w("TAG", "processQueryResult: " + e.message)
            } finally {
                binding.rvMeetingIdList.layoutManager = LinearLayoutManager(requireContext())
                adapter = ContactsAdapter(usersList, this)
                binding.rvMeetingIdList.adapter = adapter
                snapshot.release()
            }
        }?.addOnFailureListener {
            Log.e("TAG", "Fail processQueryResult: " + it.message)
        }
    }

    override fun onItemSelected(isVoiceCall: Boolean, user: Users) {
        val intent = if (isVoiceCall)
            Intent(requireActivity(), VoiceCallActivity::class.java)
        else
            Intent(requireActivity(), VideoCallActivity::class.java)
        intent.putExtra("isMeetingContact", true)
        intent.putExtra("meetingID", user.uid)
        intent.putExtra("name", user.name)
        intent.putExtra("isJoin", false)
        startActivity(intent)

        user.isCalling = true

        val upsertTask = cloudDBZone?.executeUpsert(user)
        upsertTask?.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Calls Sdp Upsert success: $cloudDBZoneResult")
        }?.addOnFailureListener {
            Log.i(TAG, "Calls Sdp Upsert failed: ${it.message}")
        }

    }
}