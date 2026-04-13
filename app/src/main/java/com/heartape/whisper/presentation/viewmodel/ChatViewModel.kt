package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.repository.ChatRepository
import com.heartape.whisper.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    userRepo: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLong() ?: 0L
    val sessionName: String = savedStateHandle.get<String>("sessionName") ?: "聊天"
    val currentUser = userRepo.currentUserFlow

    private val _messageLimit = MutableStateFlow(50)
    val currentLimit: Int get() = _messageLimit.value

    // 流式读取内存安全的活跃消息列表
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _messageLimit.flatMapLatest { limit ->
        repo.getActiveMessages(sessionId, limit)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val membersMap = repo.getSessionMembers(sessionId)
        .map { members -> members.associateBy { it.userId } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun loadMoreMessages() {
        _messageLimit.value += 50
    }

    fun sendMessage(type: String, content: String) {
        repo.sendWsMessage(sessionId, type, content)
    }
}