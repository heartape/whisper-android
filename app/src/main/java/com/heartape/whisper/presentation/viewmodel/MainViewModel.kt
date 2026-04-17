package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.VersionDto
import com.heartape.whisper.di.AppConfig
import com.heartape.whisper.repository.UserRepository
import com.heartape.whisper.utils.GlobalEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val appConfig: AppConfig,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _forceUpdateInfo = MutableStateFlow<VersionDto?>(null)
    val forceUpdateInfo: StateFlow<VersionDto?> = _forceUpdateInfo

    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin = _navigateToLogin.asSharedFlow()

    init {
        // 全局监听强制更新事件 (被 426 状态码拦截器触发)
        viewModelScope.launch {
            GlobalEventBus.forceUpdateEvent.collect { versionInfo ->
                _forceUpdateInfo.value = versionInfo
            }
        }

        viewModelScope.launch {
            GlobalEventBus.authErrorEvent.collect {
                // 1. 瞬间清空所有本地数据、断开当前数据库连接
                userRepo.clearAuth()
                // 2. 发送信号让 MainActivity 执行跳转
                _navigateToLogin.emit(Unit)
            }
        }
    }
}