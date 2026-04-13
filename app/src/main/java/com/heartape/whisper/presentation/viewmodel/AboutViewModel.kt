package com.heartape.whisper.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.VersionDto
import com.heartape.whisper.di.AppConfig
import com.heartape.whisper.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class NoUpdate(val message: String) : UpdateState()
    data class UpdateAvailable(val info: VersionDto) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val repo: SystemRepository,
    val appConfig: AppConfig
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    fun resetState() { _updateState.value = UpdateState.Idle }

    fun checkUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            when (val result1 = repo.versionCode()) {
                is AppResult.Error -> _updateState.value = UpdateState.Error(result1.message)
                is AppResult.Success<Int> -> {
                    val currentVersionCode = result1.data

                    when (val result2 = repo.checkUpdate()) {
                        is AppResult.Error -> _updateState.value = UpdateState.Error(result2.message)
                        is AppResult.Success<VersionDto> -> {
                            val latestVersion = result2.data
                            if (latestVersion.versionCode > currentVersionCode) {
                                _updateState.value = UpdateState.UpdateAvailable(latestVersion)
                            } else {
                                _updateState.value = UpdateState.NoUpdate("当前已是最新版本 (${latestVersion.versionName})")
                            }
                        }
                    }
                }
            }
        }
    }
}