package com.hms.quickline.domain.usecase

import android.content.Context
import android.content.Intent
import com.hms.quickline.R
import com.hms.quickline.core.util.Constants
import com.hms.quickline.data.model.HuaweiAuthResult
import com.hms.quickline.domain.repository.LoginRepository
import com.huawei.agconnect.auth.HwIdAuthProvider
import com.huawei.hms.support.account.AccountAuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoginUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loginRepository: LoginRepository) {

    suspend fun signInWithHuaweiId(requestCode: Int, data: Intent?): HuaweiAuthResult =
        suspendCoroutine { continuation ->
            if (requestCode == Constants.HUAWEI_ID_SIGN_IN) {
                val authAccountTask = AccountAuthManager.parseAuthResultFromIntent(data)

                if (authAccountTask.isSuccessful) {
                    val authAccount = authAccountTask.result
                    val credential = HwIdAuthProvider.credentialWithToken(authAccount.accessToken)

                    loginRepository.signInWithHuaweiId(credential)
                        .addOnSuccessListener { signInResult ->
                            val user = signInResult.user
                            user?.let {
                                continuation.resume(HuaweiAuthResult.UserSuccessful(it))
                            }
                        }.addOnFailureListener {
                            it?.let {
                                continuation.resume(HuaweiAuthResult.UserFailure(it.message))
                            }
                        }
                } else {
                    continuation.resume(HuaweiAuthResult.UserFailure(authAccountTask?.exception?.message))
                }
            } else {
                continuation.resume(HuaweiAuthResult.UserFailure(context.getString(R.string.request_code_invalid)))
            }
        }
}