package com.heartape.whisper.data.local

import android.content.SharedPreferences
import com.google.gson.Gson
import com.heartape.whisper.data.model.UserDto
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PrefsManager @Inject constructor(
    private val prefs: SharedPreferences,
    private val gson: Gson
) {
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_USER_PROFILE = "current_user_profile"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_NOTIFICATIONS = "pref_notifications"
    }

    // 鉴权 Token
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    // 当前用户完整档案 (自动处理 Gson 序列化与反序列化)
    var currentUserProfile: UserDto
        get() {
            val json = prefs.getString(KEY_USER_PROFILE, null)
            if (json == null) {
                throw Exception("未登录")
            }
            return gson.fromJson(json, UserDto::class.java)
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit { putString(KEY_USER_PROFILE, json) }
        }

    // 深色模式开关
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    // 通知开关
    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    // 退出登录时统一清理账号数据 (保留通用设置)
    fun clearAuthData() {
        prefs.edit {
            remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_PROFILE)
        }
    }
}