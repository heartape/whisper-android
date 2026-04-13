package com.heartape.whisper.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.repository.SettingsRepository
import com.heartape.whisper.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val systemRepository: SystemRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isDarkMode = settingsRepo.isDarkMode
    val isNotificationsEnabled = settingsRepo.isNotificationsEnabled

    // 缓存大小字符串状态
    private val _cacheSizeStr = MutableStateFlow("计算中...")
    val cacheSizeStr: StateFlow<String> = _cacheSizeStr.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    init {
        calculateCacheSize()
    }

    fun toggleDarkMode(enabled: Boolean) = settingsRepo.setDarkMode(enabled)
    fun toggleNotifications(enabled: Boolean) = settingsRepo.setNotificationsEnabled(enabled)

    // 计算真实缓存大小
    private fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = getDirSize(context.cacheDir) + getDirSize(context.externalCacheDir)
            _cacheSizeStr.value = formatSize(size)
        }
    }

    // 清理缓存
    @OptIn(ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _isClearing.value = true
            if (systemRepository.clearAppCache() is AppResult.Error) {

            }
            calculateCacheSize()
            _isClearing.value = false
        }
    }

    // --- 内部辅助方法 ---
    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size: Long = 0
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                size += if (child.isDirectory) getDirSize(child) else child.length()
            }
        } else {
            size = dir.length()
        }
        return size
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return false
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child -> deleteDir(child) }
        }
        return dir.delete()
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.##").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}