package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.ApplyGroupReq
import com.heartape.whisper.data.model.ApplyPeerReq
import com.heartape.whisper.data.model.SessionDto
import com.heartape.whisper.data.model.UserSearchDto
import com.heartape.whisper.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchApplyViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    private val _userResults = MutableStateFlow<List<UserSearchDto>>(emptyList())
    val userResults: StateFlow<List<UserSearchDto>> = _userResults

    private val _groupResults = MutableStateFlow<List<SessionDto>>(emptyList())
    val groupResults: StateFlow<List<SessionDto>> = _groupResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun searchUser(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.searchUsers(keyword)
            when(result) {
                is AppResult.Error -> {}
                is AppResult.Success<List<UserSearchDto>> -> _userResults.value = result.data
            }
            _isLoading.value = false
        }
    }

    fun searchGroup(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _groupResults.value = emptyList()
            _isLoading.value = true
            val result = repo.searchGroups(keyword)
            when(result) {
                is AppResult.Error -> {}
                is AppResult.Success<List<SessionDto>> -> _groupResults.value = result.data
            }
            _isLoading.value = false
        }
    }

    fun applyFriend(targetId: Long, applyInfo: String, aliasName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repo.applyFriend(ApplyPeerReq(targetId, applyInfo, aliasName.ifBlank { null }))
            when(result) {
                is AppResult.Error -> {}
                is AppResult.Success<*> -> onSuccess()
            }
        }
    }

    fun applyGroup(sessionId: Long, applyInfo: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repo.applyGroup(ApplyGroupReq(sessionId, applyInfo))
            when(result) {
                is AppResult.Error -> {}
                is AppResult.Success<*> -> onSuccess()
            }
        }
    }
}