package com.heartape.whisper.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.component.WhisperBaseDialog
import com.heartape.whisper.component.WhisperDialogTextField
import com.heartape.whisper.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 控制弹窗状态
    var showNameDialog by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 注册相册选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateAvatar(uri) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ListItem(
                    headlineContent = { Text("ID ") },
                    trailingContent = { Text(user.id.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
                HorizontalDivider()

                // 头像项
                ListItem(
                    headlineContent = { Text("头像") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                // 如果正在上传，在头像上覆盖一层遮罩转圈
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    },
                    modifier = Modifier.clickable {
                        if (!isLoading) imagePickerLauncher.launch("image/*")
                    }
                )
                HorizontalDivider()

                // 名字项
                ListItem(
                    headlineContent = { Text("名字") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    },
                    modifier = Modifier.clickable { showNameDialog = true }
                )
                HorizontalDivider()

                // 个性签名项
                ListItem(
                    headlineContent = { Text("个性签名") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.bio.ifBlank { "未设置" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    },
                    modifier = Modifier.clickable { showBioDialog = true }
                )
            }
        }
        // ====== 弹窗组件 ======
        if (showNameDialog) {
            EditDialog(
                title = "修改名字",
                initialValue = user.username,
                onDismiss = { showNameDialog = false },
                onConfirm = {
                    viewModel.updateUsername(it)
                    showNameDialog = false
                }
            )
        }
        if (showBioDialog) {
            EditDialog(
                title = "修改个性签名",
                initialValue = user.bio,
                onDismiss = { showBioDialog = false },
                onConfirm = {
                    viewModel.updateBio(it)
                    showBioDialog = false
                }
            )
        }
    }
}

// 通用的文本编辑弹窗
@Composable
fun EditDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    WhisperBaseDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(text) },
        confirmEnabled = text.isNotBlank()
    ) {
        WhisperDialogTextField(
            value = text,
            onValueChange = { text = it },
            label = "请输入内容"
        )
    }
}