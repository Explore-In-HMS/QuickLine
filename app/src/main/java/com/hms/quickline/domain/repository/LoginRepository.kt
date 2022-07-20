package com.hms.quickline.domain.repository

import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.auth.SignInResult
import com.huawei.hmf.tasks.Task
import javax.inject.Inject

class LoginRepository @Inject constructor(private val agConnectAuth: AGConnectAuth) {

    fun signInWithHuaweiId(credential: AGConnectAuthCredential): Task<SignInResult> =
        agConnectAuth.signIn(credential)
}