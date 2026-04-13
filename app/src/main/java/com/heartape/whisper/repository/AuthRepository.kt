package com.heartape.whisper.repository

import com.heartape.whisper.data.model.LoginReq
import com.heartape.whisper.data.model.RegisterReq
import com.heartape.whisper.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton
import com.heartape.whisper.data.local.PrefsManager
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.unwrap
import com.heartape.whisper.utils.ErrorUtils.runSafe

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val prefsManager: PrefsManager
) {
    suspend fun login(phone: String, code: String): AppResult<Unit> = runSafe {
        val loginData = api.login(LoginReq(phone, code)).unwrap()
        prefsManager.token = loginData.Authorization

        val userProfile = api.getCurrentUser().unwrap()
        prefsManager.currentUserProfile = userProfile
    }

    suspend fun register(req: RegisterReq): AppResult<Unit> = runSafe {
        api.register(req).unwrap()
    }
}