package com.heartape.whisper.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.UserDto
import com.heartape.whisper.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: UserRepository,
) : ViewModel() {

    val currentUser: StateFlow<UserDto> = repo.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, UserDto.EMPTY)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() { _errorMessage.value = null }

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repo.fetchAndCacheCurrentUser()) {
                is AppResult.Error -> _errorMessage.value = result.message
                is AppResult.Success<*> -> {}
            }
            _isLoading.value = false
        }
    }

    fun updateUsername(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repo.updateUsername(newName)) {
                is AppResult.Success -> { }
                is AppResult.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun updateBio(newBio: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repo.updateBio(newBio)) {
                is AppResult.Success -> { }
                is AppResult.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun updatePhone(newPhone: String) {
        if (newPhone.length != 11) {
            _errorMessage.value = "手机号格式不正确"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repo.updatePhone(newPhone)) {
                is AppResult.Success -> { }
                is AppResult.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.updateAvatar(uri)
            if (result is AppResult.Error) {
                _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun logout(onSuccess: () -> Unit) {
        repo.clearAuth()
        onSuccess()
    }

    fun updatePassword(password: String) {
        if (password.length < 6) {
            _errorMessage.value = "密码长度不能小于 6 位"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.updatePassword(password)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "修改密码失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
}