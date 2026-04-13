package com.heartape.whisper.utils

import com.heartape.whisper.data.model.VersionDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局事件总线：用于处理超越单个 ViewModel 生命周期的系统级事件
 */
object GlobalEventBus {
    // 强制更新事件流
    private val _forceUpdateEvent = MutableSharedFlow<VersionDto>(replay = 1)
    val forceUpdateEvent = _forceUpdateEvent.asSharedFlow()

    // 触发强制更新
    suspend fun emitForceUpdate(versionInfo: VersionDto) {
        _forceUpdateEvent.emit(versionInfo)
    }
}