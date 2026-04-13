package com.heartape.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.heartape.whisper.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {

    // 直接代理 UserRepository 的全局状态流
    // 由于是 StateFlow，当用户在设置里修改了头像或昵称时，这里的值会自动更新，从而驱动 ProfileScreen 重新组合
    val currentUser = userRepository.currentUserFlow

}