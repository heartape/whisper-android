package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.heartape.whisper.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateSecurity: () -> Unit,
    onNavigateGeneral: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateTerms: () -> Unit,   // ★ 新增：跳转服务协议
    onNavigatePrivacy: () -> Unit, // ★ 新增：跳转隐私政策
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            Spacer(modifier = Modifier.height(8.dp))

            SettingsListItem(title = "个人资料设置", onClick = onNavigateProfile)
            SettingsListItem(title = "账号与安全", onClick = onNavigateSecurity)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            SettingsListItem(title = "通用设置", onClick = onNavigateGeneral)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            SettingsListItem(title = "关于耳语", onClick = onNavigateAbout)
            SettingsListItem(title = "用户服务协议", onClick = onNavigateTerms)
            SettingsListItem(title = "隐私政策", onClick = onNavigatePrivacy)

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.logout(onSuccess = onLogout) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
            ) {
                Text("退出登录", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsListItem(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "进入") },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}