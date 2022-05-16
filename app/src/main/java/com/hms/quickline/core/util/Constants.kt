package com.hms.quickline.core.util

object Constants {

    const val CloudDbZoneName = "COMDBZone"

    const val VIDEO_WIDTH = 280
    const val VIDEO_HEIGHT = 200
    const val VIDEO_FPS = 60

    const val LOCAL_TRACK_ID = "local_track"
    const val LOCAL_STREAM_ID = "stream_track"

    const val DATA_CHANNEL_NAME = "sendDataChannel"

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

