package com.hms.quickline.presentation.call.newwebrtc

import android.content.Context
import android.util.Log
import com.hms.quickline.core.util.Constants
import com.hms.quickline.data.model.ObjectTypeInfoHelper
import com.hms.quickline.data.model.Users
import com.huawei.agconnect.AGCRoutePolicy
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException

class CloudDbWrapper {

    companion object {

        private const val TAG = "CloudDbWrapper"

        var cloudDB: AGConnectCloudDB? = null
        private var config: CloudDBZoneConfig? = null
        var cloudDBZone: CloudDBZone? = null
        var instance: AGConnectInstance? = null

        fun initialize(
            context: Context,
            cloudDbInitializeResponse: (Boolean) -> Unit
        ) {

            if (cloudDBZone != null) {
                cloudDbInitializeResponse(true)
                return
            }

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
        }

        fun getUserById(uid: String,callback: ICloudDbWrapper){
            val queryUser = CloudDBZoneQuery.where(Users::class.java).contains("uid", uid)
            val queryTask = cloudDBZone?.executeQuery(
                queryUser,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
            )
            queryTask?.addOnSuccessListener { snapshot ->
                val usersTemp: MutableList<Users> = mutableListOf()

                try {
                    while (snapshot.snapshotObjects.hasNext()) {
                        val jobModel = snapshot.snapshotObjects.next()
                        usersTemp.add(jobModel)
                    }
                } catch (e: AGConnectCloudDBException) {
                    Log.w(TAG, "processQueryResult: " + e.message)
                } finally {
                    callback.onUserObtained(usersTemp[0])
                    snapshot.release()
                }
            }?.addOnFailureListener {
                Log.w(TAG, "processQueryResult: " + it.message)
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

    interface ICloudDbWrapper{
        fun onUserObtained(users: Users)
    }
}