package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.heartape.whisper.component.WhisperConfirmDialog
import com.heartape.whisper.presentation.viewmodel.GeneralSettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    // 监听真实数据流
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val cacheSizeStr by viewModel.cacheSizeStr.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()

    var showClearCacheDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通用设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            HorizontalDivider()
            // 深色模式切换
            ListItem(
                headlineContent = { Text("深色模式") },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            )
            HorizontalDivider()

            // 消息通知开关
            ListItem(
                headlineContent = { Text("消息通知") },
                supportingContent = { Text("开启后，在后台也能接收到新消息提醒") },
                trailingContent = {
                    Switch(
                        checked = isNotificationsEnabled,
//                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        onCheckedChange = null,
                        enabled = false
                    )
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        snackbarHostState.showSnackbar("功能暂未开放")
                    }
                }
            )
            HorizontalDivider()

            // 清理缓存按钮
            ListItem(
                headlineContent = { Text("清理缓存") },
                supportingContent = { Text("清理本地图片、语音及临时文件，不影响聊天记录") },
                trailingContent = {
                    if (isClearing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(cacheSizeStr, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                modifier = Modifier.clickable {
                    if (!isClearing && cacheSizeStr != "0 B") showClearCacheDialog = true
                }
            )
            HorizontalDivider()
        }

        // 清理缓存的确认弹窗
        if (showClearCacheDialog) {
            WhisperConfirmDialog(
                title = "清理缓存",
                message = "确定要清理应用缓存吗？这会释放设备存储空间。",
                onDismiss = { showClearCacheDialog = false },
                onConfirm = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }
            )
        }
    }
}