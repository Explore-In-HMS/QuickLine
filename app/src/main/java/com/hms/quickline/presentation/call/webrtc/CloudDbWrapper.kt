package com.hms.quickline.presentation.call.webrtc

import android.content.Context
import android.util.Log
import com.hms.quickline.core.util.Constants
import com.hms.quickline.data.model.ObjectTypeInfoHelper
import com.huawei.agconnect.AGCRoutePolicy
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.*

class CloudDbWrapper {

    companion object {

        private const val TAG = "CloudDbWrapper"

        var cloudDB: AGConnectCloudDB? = null
        var config: CloudDBZoneConfig? = null
        var cloudDBZone: CloudDBZone? = null
        var instance: AGConnectInstance? = null

        fun initialize(
            context: Context,
            cloudDbInitializeResponse: (Boolean) -> Unit
        ) {
            if (cloudDBZone == null) {

                AGConnectCloudDB.initialize(context)

                instance = AGConnectInstance.buildInstance(
                    AGConnectOptionsBuilder().setRoutePolicy(AGCRoutePolicy.GERMANY).build(context)
                )

                cloudDB = AGConnectCloudDB.getInstance(
                    AGConnectInstance.getInstance(),
                    AGConnectAuth.getInstance()
                )
                cloudDB?.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo())

                config = CloudDBZoneConfig(
                    Constants.CloudDbZoneName,
                    CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                    CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC
                )

                config!!.persistenceEnabled = true
                val task = cloudDB?.openCloudDBZone2(config!!, true)
                task?.addOnSuccessListener {
                    Log.i(TAG, "Open cloudDBZone success")
                    cloudDBZone = it
                    cloudDbInitializeResponse(true)
                }?.addOnFailureListener {
                    Log.w(TAG, "Open cloudDBZone failed for " + it.message)
                    cloudDbInitializeResponse(false)
                }
            } else {
                cloudDbInitializeResponse(true)
            }
        }

        fun closeCloudDBZone() {
            try {
                cloudDB?.closeCloudDBZone(cloudDBZone)
                Log.w("CloudDB zone close", "Cloud was closed")
            } catch (e: Exception) {
                Log.w("CloudDBZone", e)
            }
        }
    }
}