package com.hms.quickline.ui.login

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hms.quickline.core.base.BaseViewModel
import com.hms.quickline.data.model.HuaweiAuthResult
import com.hms.quickline.data.Resource
import com.hms.quickline.data.model.Users
import com.hms.quickline.domain.repository.CloudDbWrapper
import com.hms.quickline.domain.usecase.LoginUseCase
import com.huawei.agconnect.auth.AGConnectUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginUseCase: LoginUseCase) : BaseViewModel() {

    private val TAG = "LoginViewModel"

    private val signInHuaweiIdLiveData: MutableLiveData<Resource<AGConnectUser?>> =
        MutableLiveData()

    fun getSignInHuaweiIdLiveData(): LiveData<Resource<AGConnectUser?>> = signInHuaweiIdLiveData

    private val checkUserLiveData: MutableLiveData<Boolean> = MutableLiveData()
    fun getCheckUserLiveData(): LiveData<Boolean> = checkUserLiveData


    fun signInWithHuaweiId(requestCode: Int, data: Intent?) {
        viewModelScope.launch {
            signInHuaweiIdLiveData.value = Resource.loading()

            when (val result = loginUseCase.signInWithHuaweiId(requestCode, data)) {
                is HuaweiAuthResult.UserSuccessful -> {
                    signInHuaweiIdLiveData.value = Resource.success(result.user)
                }
                is HuaweiAuthResult.UserFailure -> {
                    signInHuaweiIdLiveData.value = Resource.Failed(result.errorMessage!!)
                }
            }
        }
    }

    /**
     * Check user exists in CloudDatabase
     */
    fun checkUserLogin(userId: String) {
        CloudDbWrapper.checkUserById(userId, object : CloudDbWrapper.ResultListener {

            override fun onSuccess(result: Any?) {
                val resultList: ArrayList<Users>? = result as? ArrayList<Users>

                resultList?.forEach {
                    checkUserLiveData.value = it.uid == userId
                }
            }

            override fun onFailure(e: Exception) {
                e.localizedMessage?.let {
                    Log.e(TAG, it)
                }
            }
        })
    }
}