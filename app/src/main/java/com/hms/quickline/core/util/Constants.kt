package com.hms.quickline.core.util

object Constants {

    const val CloudDbZoneName = "COMDBZone"

    const val VIDEO_WIDTH = 1280
    const val VIDEO_HEIGHT = 720
    const val VIDEO_FPS = 30

    const val REQUEST_CODE = 100

    const val HUAWEI_ID_SIGN_IN = 8888

    const val LOCAL_TRACK_ID = "local_track"
    const val LOCAL_STREAM_ID = "stream_track"
    const val AUDIO ="_audio"


    const val DATA_CHANNEL_NAME = "sendDataChannel"

    const val MEETING_ID = "meetingID"
    const val IS_JOIN = "isJoin"
    const val ANSWER = "answer"
    const val UID = "uid"
    const val USER = "user"
    const val CALLER_NAME = "callerName"
    const val DECLINE = "decline"
    const val IS_MEETING_CONTACT = "isMeetingContact"
    const val NAME = "name"


    enum class USERTYPE {
        OFFER_USER,
        ANSWER_USER
    }

    enum class TYPE {
        OFFER,
        ANSWER,
        END
    }
}

