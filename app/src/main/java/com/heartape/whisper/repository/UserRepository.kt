package com.heartape.whisper.repository

import android.content.Context
import android.net.Uri
import com.heartape.whisper.data.local.DatabaseManager
import com.heartape.whisper.data.model.UserDto
import com.heartape.whisper.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton
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
import com.heartape.whisper.utils.FileUtils.detectMime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@Singleton
class UserRepository @Inject constructor(
    private val api: ApiService,
    private val prefsManager: PrefsManager,
    private val dbManager: DatabaseManager,
    @ApplicationContext private val context: Context
) {
    val currentUserFlow: Flow<UserDto> = prefsManager.currentUserFlow.filterNotNull()

    fun requireUserId(): Long = prefsManager.requireUserId()

    private val _currentUserIdFlow = MutableStateFlow<Long>(0)
    val currentUserIdFlow: StateFlow<Long> = _currentUserIdFlow.asStateFlow()

    suspend fun updateUsername(username: String): AppResult<Unit> = runSafe {
        api.updateUsername(UpdateUsernameReq(username)).unwrap()
        prefsManager.updateCurrentUser { it.copy(username = username) }
    }

    suspend fun updateAvatar(uri: Uri): AppResult<Unit> = runSafe {
        val rawFile = FileUtils.getFileFromUri(context, uri)
        val compressedFile = FileUtils.compressImage(context, rawFile, 100)
        val mimeType = detectMime(rawFile)
        val requestFile = compressedFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)
        val uploadDto = api.uploadAvatar(body).unwrap()
        api.updateAvatar(UpdateAvatarReq(uploadDto.path)).unwrap()
        prefsManager.updateCurrentUser { it.copy(avatar = uploadDto.url) }
    }

    suspend fun updateBio(bio: String): AppResult<Unit> = runSafe {
        api.updateBio(UpdateBioReq(bio)).unwrap()
        prefsManager.updateCurrentUser { it.copy(bio = bio) }
    }

    suspend fun updatePhone(phone: String): AppResult<Unit> = runSafe {
        api.updatePhone(UpdatePhoneReq(phone)).unwrap()
        prefsManager.updateCurrentUser { it.copy(phone = phone) }
    }

    // 从服务器拉取并更新缓存
    suspend fun fetchAndCacheCurrentUser(): AppResult<UserDto> = runSafe {
        val userDto = api.getCurrentUser().unwrap()
        prefsManager.saveProfile(userDto)
        return@runSafe userDto
    }

    // 清除本地缓存和 Token（退出登录用）
    fun clearAuth() {
        dbManager.closeDatabase()
        prefsManager.clearAuthData()
    }

    suspend fun updatePassword(password: String): AppResult<Unit> = runSafe {
        api.updatePassword(UpdatePasswordReq(password)).unwrap()
    }
}