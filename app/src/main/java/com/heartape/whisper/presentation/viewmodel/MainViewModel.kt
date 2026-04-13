package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.VersionDto
import com.heartape.whisper.di.AppConfig
import com.heartape.whisper.utils.GlobalEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(val appConfig: AppConfig) : ViewModel() {

    private val _forceUpdateInfo = MutableStateFlow<VersionDto?>(null)
    val forceUpdateInfo: StateFlow<VersionDto?> = _forceUpdateInfo

    init {
        // 全局监听强制更新事件 (被 426 状态码拦截器触发)
        viewModelScope.launch {
            GlobalEventBus.forceUpdateEvent.collect { versionInfo ->
                _forceUpdateInfo.value = versionInfo
            }
        }
    }
}