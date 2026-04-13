package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.heartape.whisper.component.WhisperBaseDialog
import com.heartape.whisper.presentation.viewmodel.AboutViewModel
import com.heartape.whisper.presentation.viewmodel.UpdateState
import com.heartape.whisper.utils.AppStoreNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于耳语") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("W", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            Text("耳语 (Whisper)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // 获取当前 App 版本用于展示
            val currentVersionName = remember {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
            Text("Version $currentVersionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(60.dp))

            // ★ 版本更新入口
            ListItem(
                headlineContent = { Text("版本更新") },
                trailingContent = {
                    if (updateState is UpdateState.Checking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("检查新版本", color = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable {
                    if (updateState !is UpdateState.Checking) {
                        viewModel.checkUpdate()
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            Spacer(Modifier.weight(1f))
            Text("Copyright © 2026 Whisper Inc.\nAll Rights Reserved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(bottom = 32.dp))
        }

        // ================= 处理各类更新状态的弹窗 =================

        when (val state = updateState) {
            is UpdateState.NoUpdate -> {
                WhisperBaseDialog(
                    title = "检查更新",
                    onDismiss = viewModel::resetState,
                    onConfirm = viewModel::resetState,
                    confirmText = "我知道了",
                    cancelText = "关闭"
                ) { Text(state.message) }
            }
            is UpdateState.UpdateAvailable -> {
                WhisperBaseDialog(
                    title = "发现新版本: ${state.info.versionName}",
                    onDismiss = viewModel::resetState,
                    onConfirm = { AppStoreNavigator.goToStore(context, state.info.downloadUrl, viewModel.appConfig.fallbackWebUrl) },
                    confirmText = "前往应用商店更新"
                ) {
                    Text("更新日志：\n${state.info.releaseNotes}")
                }
            }
            is UpdateState.Error -> {
                // ... Error 弹窗保持原样
            }
            else -> {}
        }
    }
}