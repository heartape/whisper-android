package com.heartape.whisper.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.repository.ChatRepository
import com.heartape.whisper.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionSettingsViewModel @Inject constructor(
    private val repo: ChatRepository,
    userRepo: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLong() ?: 0L
    val currentUserId: Long = userRepo.currentUserIdFlow.value
    val session = repo.getSession(sessionId).stateIn(viewModelScope, SharingStarted.Lazily, null)
    val members = repo.getSessionMembers(sessionId).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _exitSuccess = MutableStateFlow(false)
    val exitSuccess: StateFlow<Boolean> = _exitSuccess

    fun clearError() { _errorMessage.value = null }

    // ================= 成员管理核心逻辑 =================

    // 1. 设置备注 (私聊设对方，群聊设自己)
    fun setAlias(aliasName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.updateAlias(sessionId, aliasName)
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("setAlias", _errorMessage.value, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 2. 拉黑/取消拉黑 (私聊)
    fun toggleBlockPeer(targetUserId: Long, isBlock: Boolean) {
        val action = if (isBlock) "BLOCK" else "UNBLOCK"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.managePeerMember(sessionId, targetUserId, action)
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("toggleBlockPeer", _errorMessage.value, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 3. 复杂的群成员管理 (踢人、设管理、禁言等)
    fun manageGroupMember(targetUserId: Long, action: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.manageGroupMember(sessionId, targetUserId, action)
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("manageGroupMember", _errorMessage.value, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 退出或解散会话
    fun exitSession() {
//        val isGroup = session.value?.type == "GROUP"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.exit(sessionId)
                repo.deleteLocalSessionData(sessionId)
                _exitSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("exitSession", _errorMessage.value, e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}