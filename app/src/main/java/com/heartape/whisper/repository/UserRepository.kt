package com.heartape.whisper.repository

import android.content.Context
import com.heartape.whisper.data.model.UserDto
import com.heartape.whisper.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton
import com.heartape.whisper.data.local.DatabaseManager
import com.heartape.whisper.data.local.PrefsManager
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.UpdateAvatarReq
import com.heartape.whisper.data.model.UpdateBioReq
import com.heartape.whisper.data.model.UpdatePasswordReq
import com.heartape.whisper.data.model.UpdatePhoneReq
import com.heartape.whisper.data.model.UpdateUsernameReq
import com.heartape.whisper.data.model.unwrap
import com.heartape.whisper.utils.ErrorUtils.runSafe
import com.heartape.whisper.utils.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Singleton
class UserRepository @Inject constructor(
    private val api: ApiService,
    private val prefsManager: PrefsManager,
    @ApplicationContext private val context: Context
) {
    private val _currentUserFlow = MutableStateFlow<UserDto>(prefsManager.currentUserProfile)
    val currentUserFlow: StateFlow<UserDto> = _currentUserFlow.asStateFlow()

    private fun updateLocalUser(updater: (UserDto) -> UserDto) {
        _currentUserFlow.value.let { current ->
            val updated = updater(current)
            prefsManager.currentUserProfile = updated
            _currentUserFlow.value = updated
        }
    }

    suspend fun updateUsername(username: String): AppResult<Unit> = runSafe {
        api.updateUsername(UpdateUsernameReq(username)).unwrap()
        updateLocalUser { it.copy(username = username) }
    }

    suspend fun updateAvatar(file: File): AppResult<Unit> = runSafe {
        val compressedFile = FileUtils.compressImage(context, file, 100)
        val requestFile = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)
        val uploadDto = api.uploadAvatar(body).unwrap()
        api.updateAvatar(UpdateAvatarReq(uploadDto.path)).unwrap()
        updateLocalUser { it.copy(avatar = uploadDto.url) }
    }

    suspend fun updateBio(bio: String): AppResult<Unit> = runSafe {
        api.updateBio(UpdateBioReq(bio)).unwrap()
    }

    suspend fun updatePhone(phone: String): AppResult<Unit> = runSafe {
        api.updatePhone(UpdatePhoneReq(phone)).unwrap()
        prefsManager.currentUserProfile = prefsManager.currentUserProfile.copy(phone = phone)
    }

    // 从服务器拉取并更新缓存
    suspend fun fetchAndCacheCurrentUser(): AppResult<UserDto> = runSafe {
        val userDto = api.getCurrentUser().unwrap()
        prefsManager.currentUserProfile = userDto
        _currentUserFlow.value = userDto
        return@runSafe userDto
    }

    // 清除本地缓存和 Token（退出登录用）
    fun clearAuth() {
        prefsManager.clearAuthData()
    }

    suspend fun updatePassword(password: String): AppResult<Unit> = runSafe {
        api.updatePassword(UpdatePasswordReq(password)).unwrap()
    }
}