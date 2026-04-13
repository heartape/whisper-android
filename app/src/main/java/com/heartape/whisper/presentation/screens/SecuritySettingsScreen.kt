package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.heartape.whisper.component.WhisperBaseDialog
import com.heartape.whisper.component.WhisperDialogTextField
import com.heartape.whisper.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账号与安全") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            ListItem(
                headlineContent = { Text("绑定手机号") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentUser.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                },
                modifier = Modifier.clickable { showPhoneDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("修改密码") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable {
                    showPasswordDialog = true
                    passwordInput = ""
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("注销账号") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.clickable {
                    scope.launch {
                        snackbarHostState.showSnackbar("功能暂未开放")
                    }
                }
            )
        }

        if (showPhoneDialog) {
            WhisperBaseDialog(
                title = "绑定手机号",
                onDismiss = { showPhoneDialog = false },
                onConfirm = {
                    viewModel.updatePhone(phoneInput)
                    showPhoneDialog = false
                },
                confirmEnabled = phoneInput.isNotBlank()
            ) {
                WhisperDialogTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = "请输入内容"
                )
            }
        }

        if (showPasswordDialog) {
            WhisperBaseDialog(
                title = "修改密码",
                onDismiss = { showPasswordDialog = false },
                onConfirm = {
                    viewModel.updatePassword(passwordInput)
                    showPasswordDialog = false
                },
                confirmEnabled = passwordInput.isNotBlank()
            ) {
                WhisperDialogTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    label = "请输入内容"
                )
            }
        }
    }
}