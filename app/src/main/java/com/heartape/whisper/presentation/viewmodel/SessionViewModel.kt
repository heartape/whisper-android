package com.heartape.whisper.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.local.PrefsManager
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.remote.StompManager
import com.heartape.whisper.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repo: ChatRepository,
    stomp: StompManager,
    prefsManager: PrefsManager
) : ViewModel() {

    val wsStatus = repo.wsStatus
    val sessions = repo.getActiveSessions().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isSyncing = MutableStateFlow(false)

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    init {
        val token = prefsManager.getAccessToken()
        stomp.connect(token!!)
        refreshData()
    }

    fun clearError() { _syncError.value = null }

    fun refreshData() {
        if (_isSyncing.value) return // 防连点抖动

        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = repo.syncAllData()) {
                is AppResult.Success -> {
                    Log.d("ChatRepository", "数据同步成功")
                }
                is AppResult.Error -> {
                    _syncError.value = result.message
                }
            }
            _isSyncing.value = false
        }
    }
}