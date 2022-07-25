package com.hms.quickline.data.webrtc.observer

import org.webrtc.DataChannel

open class DataChannelObserver(
    private val onBufferedAmountChangeCallback: (Long) -> Unit = {},
    private val onStateChangeCallback: () -> Unit = {},
    private val onMessageCallback: (DataChannel.Buffer) -> Unit = {},
) : DataChannel.Observer {
    override fun onBufferedAmountChange(p0: Long) {
        onBufferedAmountChangeCallback(p0)
    }

    override fun onStateChange() {
        onStateChangeCallback()
    }

    override fun onMessage(p0: DataChannel.Buffer?) {
        p0?.let {
            onMessageCallback(it)
        }
    }
}