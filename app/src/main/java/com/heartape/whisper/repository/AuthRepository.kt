package com.heartape.whisper.repository

import com.heartape.whisper.data.local.DatabaseManager
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
    private val prefsManager: PrefsManager,
    private val dbManager: DatabaseManager
) {
    suspend fun login(phone: String, code: String): AppResult<Unit> = runSafe {
        val loginData = api.login(LoginReq(phone, code)).unwrap()
        prefsManager.saveToken(loginData.Authorization)
    }

    suspend fun initProfile(): AppResult<Unit> = runSafe {
        val userProfile = api.getCurrentUser().unwrap()
        prefsManager.saveProfile(userProfile)
        dbManager.openDatabase(userProfile.id)
    }

    suspend fun register(req: RegisterReq): AppResult<Unit> = runSafe {
        api.register(req).unwrap()
    }
}