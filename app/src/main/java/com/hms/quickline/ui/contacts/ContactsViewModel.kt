package com.hms.quickline.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hms.quickline.core.base.BaseViewModel
import com.hms.quickline.data.model.Users
import com.hms.quickline.domain.repository.CloudDbWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor () : BaseViewModel() {

    private val userLiveData: MutableLiveData<Users> = MutableLiveData()
    fun getUserLiveData(): LiveData<Users> = userLiveData

    private val userListLiveData: MutableLiveData<ArrayList<Users>> = MutableLiveData()
    fun getUserListLiveData(): LiveData<ArrayList<Users>> = userListLiveData

    fun getUser(uid: String) {

        CloudDbWrapper.getUserById(uid, object : CloudDbWrapper.ICloudDbWrapper {
            override fun onUserObtained(users: Users) {
                userLiveData.value = users
            }
        })
    }

    fun getUserList() {
        CloudDbWrapper.queryUsers {
           userListLiveData.value = it
       }
    }

}