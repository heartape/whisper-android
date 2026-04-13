package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.ApplyDto
import com.heartape.whisper.data.model.UserDto
import com.heartape.whisper.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 包装 UI 状态，结合了申请信息与申请人的用户信息
data class ApplyItemUiState(
    val apply: ApplyDto,
    val applicantUser: UserDto?
)

@HiltViewModel
class PendingAppliesViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    private val _pendingList = MutableStateFlow<List<ApplyItemUiState>>(emptyList())
    val pendingList: StateFlow<List<ApplyItemUiState>> = _pendingList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadPendingApplies()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadPendingApplies() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.getPendingApplies()
            when(result) {
                is AppResult.Error -> _errorMessage.value = result.message
                is AppResult.Success<List<ApplyDto>> -> {
                    result.data.map { apply ->
                        async {
                            val result = repo.getUserInfo(apply.applicantId)

                            when(result) {
                                is AppResult.Error -> _errorMessage.value = result.message
                                is AppResult.Success<UserDto> -> ApplyItemUiState(apply, result.data)
                            }
                        }
                    }.awaitAll()
                }
            }
            _isLoading.value = false
        }
    }

    fun reviewApply(applyId: Long, approved: Boolean, reviewNote: String, aliasName: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.reviewApply(applyId, approved, reviewNote, aliasName?.ifBlank { null })
                _pendingList.value = _pendingList.value.map { uiState ->
                    if (uiState.apply.id == applyId) {
                        // 使用 copy 更新数据类中的 status 字段 ('APPROVED' 或 'REJECTED')
                        val newStatus = if (approved) "APPROVED" else "REJECTED"
                        uiState.copy(apply = uiState.apply.copy(status = newStatus))
                    } else {
                        uiState
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "处理失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
}