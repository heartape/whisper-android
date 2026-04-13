package com.heartape.whisper.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.UploadDto
import com.heartape.whisper.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _avatarPath = MutableStateFlow<String?>(null)

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    // 处理图片上传
    fun uploadIcon(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = repo.uploadIcon(uri)
            when (result) {
                is AppResult.Error -> _errorMessage.value = result.message
                is AppResult.Success<UploadDto> -> {
                    val uploadDto = result.data
                    Log.d("uploadAvatar:url", uploadDto.url)
                    Log.d("uploadAvatar:path", uploadDto.path)
                    _avatarUrl.value = uploadDto.url
                    _avatarPath.value = uploadDto.path
                }
            }
            _isUploading.value = false
        }
    }

    // 提交创建群聊请求
    fun createGroup(name: String, onSuccess: () -> Unit) {
        val currentAvatar = _avatarPath.value
        if (currentAvatar == null) {
            _errorMessage.value = "请先上传群头像"
            return
        }
        if (name.isBlank()) {
            _errorMessage.value = "群名称不能为空"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result1 = repo.createGroup(name, currentAvatar)
            when (result1) {
                is AppResult.Error -> _errorMessage.value = result1.message
                is AppResult.Success<Long> -> {
                    val result2 = repo.syncSession(result1.data)

                    when (result2) {
                        is AppResult.Error -> _errorMessage.value = result2.message
                        is AppResult.Success<*> -> onSuccess()
                    }
                }
            }
            _isLoading.value = false
        }
    }
}