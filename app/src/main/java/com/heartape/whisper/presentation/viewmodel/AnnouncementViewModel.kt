package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AnnouncementDto
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.repository.ChatRepository
import com.heartape.whisper.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val repo: ChatRepository,
    userRepo: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLong() ?: 0L
    private val currentUserId: Long = userRepo.currentUserIdFlow.value

    // 成员字典，用于快速查找发布者的头像和昵称
    val membersMap = repo.getSessionMembers(sessionId)
        .map { members -> members.associateBy { it.userId } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // 计算当前用户的权限 (只有 OWNER 和 ADMIN 可以发布)
    val canPublish = membersMap.map { map ->
        val myRole = map[currentUserId]?.role
        myRole == "OWNER" || myRole == "ADMIN"
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _announcements = MutableStateFlow<List<AnnouncementDto>>(emptyList())
    val announcements: StateFlow<List<AnnouncementDto>> = _announcements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadAnnouncements()
    }

    fun clearError() { _errorMessage.value = null }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.getAnnouncements(sessionId)
            when (result) {
                is AppResult.Error -> _errorMessage.value = result.message
                is AppResult.Success<List<AnnouncementDto>> -> _announcements.value = result.data
            }
            _isLoading.value = false
        }
    }

    fun publishAnnouncement(content: String, onSuccess: () -> Unit) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.publishAnnouncement(sessionId, content)
            when(result) {
                is AppResult.Error -> _errorMessage.value = result.message
                is AppResult.Success<*> -> {

                    val result2 = repo.getAnnouncements(sessionId)
                    when(result2) {
                        is AppResult.Error -> _errorMessage.value = result2.message
                        is AppResult.Success<*> -> onSuccess()
                    }
                }
            }
            _isLoading.value = false
        }
    }
}