package com.hms.quickline.data.model

import com.huawei.agconnect.auth.AGConnectUser

sealed class HuaweiAuthResult {
    data class UserSuccessful(val user: AGConnectUser) : HuaweiAuthResult()
    data class UserFailure(val errorMessage: String?) : HuaweiAuthResult()
}