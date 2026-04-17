package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.RegisterReq
import com.heartape.whisper.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun login(phone: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepo.login(phone, code)) {
                is AppResult.Error -> _errorMessage.value = result.message
                is AppResult.Success -> {

                    when (val result = authRepo.initProfile()) {
                        is AppResult.Error -> _errorMessage.value = result.message
                        is AppResult.Success<*> -> onSuccess()
                    }
                }
            }

            _isLoading.value = false
        }
    }

    fun register(req: RegisterReq, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepo.register(req)) {
                is AppResult.Success -> {
                    onSuccess()
                }
                is AppResult.Error -> {
                    _errorMessage.value = result.message // 这里拿到的一定是友好的中文提示
                }
            }
            _isLoading.value = false
        }
    }
}