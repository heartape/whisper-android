package com.heartape.whisper.data.local

import android.content.SharedPreferences
import com.google.gson.Gson
import com.heartape.whisper.data.model.UserDto
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import com.heartape.whisper.utils.GlobalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class PrefsManager @Inject constructor(
    private val prefs: SharedPreferences,
    private val gson: Gson
) {
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_PROFILE = "current_user_profile"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_NOTIFICATIONS = "pref_notifications"
    }

    // ================== 1. 核心状态流 (SSOT) ==================

    // 初始化时从磁盘读取一次，随后常驻内存
    private val _currentUserFlow = MutableStateFlow<UserDto?>(readUserProfileFromPrefs())

    // 供外部订阅的只读流
    val currentUserFlow: StateFlow<UserDto?> = _currentUserFlow.asStateFlow()

    private fun readUserProfileFromPrefs(): UserDto? {
        val json = prefs.getString(KEY_USER_PROFILE, null)
        return if (json != null) gson.fromJson(json, UserDto::class.java) else null
    }

    // ================== 2. 读写拦截与内存同步 ==================

    private var currentUserProfile: UserDto?
        // 直接读内存，不再频繁反序列化 JSON！
        get() = _currentUserFlow.value
        set(value) {
            // 1. 写磁盘
            val json = if (value != null) gson.toJson(value) else null
            prefs.edit { putString(KEY_USER_PROFILE, json) }
            // 2. 写内存并触发 UI 刷新
            _currentUserFlow.value = value
        }

    private var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    // 深色模式开关
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    // 通知开关
    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    /**
     * ★ 新增：提供给上层的便捷局部更新方法
     */
    fun updateCurrentUser(updater: (UserDto) -> UserDto) {
        val current = currentUserProfile ?: return
        currentUserProfile = updater(current) // 触发上面的 set() 魔法
    }

    // ================== 3. 强断言 (Fail-Fast) ==================

    fun requireUserId(): Long {
        val id = requireUserProfile().id
        if (id == 0L) {
            GlobalEventBus.tryEmitAuthError()
            throw CancellationException("用户未登录，强制中断执行流")
        }
        return id
    }

    fun requireUserProfile(): UserDto {
        return currentUserProfile ?: run {
            GlobalEventBus.tryEmitAuthError()
            throw CancellationException("用户未登录，强制中断执行流")
        }
    }

    // ================== 4. 其他操作 ==================

    fun getAccessToken(): String? = token

    fun isLoggedIn(): Boolean = token != null && currentUserProfile != null

    fun saveToken(token: String) {
        this.token = token
    }

    fun saveProfile(userProfile: UserDto) {
        currentUserProfile = userProfile
    }

    fun clearAuthData() {
        prefs.edit {
            remove(KEY_TOKEN)
                .remove(KEY_USER_PROFILE)
        }
        _currentUserFlow.value = null
    }


}