package com.heartape.whisper.repository

import com.heartape.whisper.data.local.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val prefsManager: PrefsManager
) {
    // 监听深色模式（默认跟随系统或设为 false）
    private val _isDarkMode = MutableStateFlow(prefsManager.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // 监听消息通知开关（默认开启）
    private val _isNotificationsEnabled = MutableStateFlow(prefsManager.isNotificationsEnabled)
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefsManager.isDarkMode = enabled
        _isDarkMode.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefsManager.isNotificationsEnabled = enabled
        _isNotificationsEnabled.value = enabled
    }
}