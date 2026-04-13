package com.heartape.whisper.repository

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.VersionDto
import com.heartape.whisper.data.model.unwrap
import com.heartape.whisper.data.remote.ApiService
import com.heartape.whisper.utils.ErrorUtils.runSafe
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun versionCode(): AppResult<Int> = runSafe {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return@runSafe packageInfo.versionCode
    }
    // 检查更新
    suspend fun checkUpdate(): AppResult<VersionDto> = runSafe {
        return@runSafe api.checkUpdate().unwrap()
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearAppCache(): AppResult<Unit> = runSafe {
        deleteDir(context.cacheDir)
        context.externalCacheDir?.let { deleteDir(it) }
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }

    private fun deleteDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child -> deleteDir(child) }
        }
        dir.delete()
    }
}