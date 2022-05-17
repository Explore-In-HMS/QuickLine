/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2020. All rights reserved.
 * Generated by the CloudDB ObjectType compiler.  DO NOT EDIT!
 */
package com.hms.quickline.data.model;

import com.huawei.agconnect.cloud.database.CloudDBZoneObject;
import com.huawei.agconnect.cloud.database.Text;
import com.huawei.agconnect.cloud.database.annotations.DefaultValue;
import com.huawei.agconnect.cloud.database.annotations.PrimaryKeys;

/**
 * Definition of ObjectType CallsSdp.
 *
 * @since 2022-05-12
 */
@PrimaryKeys({"uuid"})
public final class CallsSdp extends CloudDBZoneObject {
    private String uuid;

    private String meetingID;

    private Text sdp;

    private String callType;

    @DefaultValue(booleanValue = true)
    private Boolean shadow_flag;

    public CallsSdp() {
        super(CallsSdp.class);
        this.shadow_flag = true;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setMeetingID(String meetingID) {
        this.meetingID = meetingID;
    }

    public String getMeetingID() {
        return meetingID;
    }

    public void setSdp(Text sdp) {
        this.sdp = sdp;
    }

    public Text getSdp() {
        return sdp;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getCallType() {
        return callType;
    }

    public void setShadow_flag(Boolean shadow_flag) {
        this.shadow_flag = shadow_flag;
    }

    public Boolean getShadow_flag() {
        return shadow_flag;
    }

}