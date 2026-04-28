package me.wjz.nekocrypt.util

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ResultRelay {
    // replay=1：确保 handler 被临时反激活后重新激活时，仍能收到最近一次发送的 URI
    private val _flow = MutableSharedFlow<Uri>(replay = 1)
    val flow = _flow.asSharedFlow()

    suspend fun send(uri: Uri) {
        _flow.emit(uri)
    }

    /**
     * 在 handler 消费完 URI 后调用，防止 replay 导致重复处理
     */
    fun consumeLast() {
        _flow.resetReplayCache()
    }
}