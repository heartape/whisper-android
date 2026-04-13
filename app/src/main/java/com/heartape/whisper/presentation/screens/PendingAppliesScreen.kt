package com.heartape.whisper.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.presentation.viewmodel.ApplyItemUiState
import com.heartape.whisper.presentation.viewmodel.PendingAppliesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingAppliesScreen(
    onBack: () -> Unit,
    viewModel: PendingAppliesViewModel = hiltViewModel()
) {
    val applies by viewModel.pendingList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 弹窗状态管理
    var selectedApplyForApproval by remember { mutableStateOf<ApplyItemUiState?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && applies.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (applies.isEmpty()) {
                // 空状态占位图
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "No applies",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无新的申请", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(applies, key = { it.apply.id }) { item ->
                        ApplyItemCard(
                            item = item,
                            onApprove = { selectedApplyForApproval = item },
                            onReject = {
                                viewModel.reviewApply(item.apply.id, approved = false, reviewNote = "拒绝", aliasName = null)
                            }
                        )
                    }
                }
            }

            // 处理同意动作时的遮罩层（如果有网络请求在进行中）
            AnimatedVisibility(visible = isLoading && applies.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // 审批同意的弹窗（好友申请需要填备注，群申请直接确认）
        selectedApplyForApproval?.let { item ->
            ApproveApplyDialog(
                item = item,
                onDismiss = { selectedApplyForApproval = null },
                onConfirm = { note, alias ->
                    viewModel.reviewApply(item.apply.id, approved = true, reviewNote = note, aliasName = alias)
                    selectedApplyForApproval = null
                }
            )
        }
    }
}

@Composable
fun ApplyItemCard(
    item: ApplyItemUiState,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isGroup = item.apply.type == "GROUP"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 申请人头像
            AsyncImage(
                model = item.applicantUser?.avatar ?: "https://picsum.photos/200",
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 申请类型与名称
                val title = if (isGroup) {
                    "${item.applicantUser?.username ?: "用户"} 申请加入群聊"
                } else {
                    "${item.applicantUser?.username ?: "用户"} 申请添加好友"
                }

                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))

                // 附言
                Text(
                    text = "附言：${item.apply.applyInfo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ★ 核心修改：动态渲染操作区域 (按钮 or 状态文字)
            Box(
                modifier = Modifier.padding(start = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                when (item.apply.status) {
                    "PENDING" -> {
                        // 待处理状态，展示双按钮
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = onApprove,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("同意", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onReject,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("拒绝", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    "APPROVED" -> {
                        // 已同意状态
                        Text(
                            text = "已同意",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    "REJECTED" -> {
                        // 已拒绝状态
                        Text(
                            text = "已拒绝",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    else -> {
                        // 兼容未知状态兜底
                        Text(
                            text = item.apply.status,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApproveApplyDialog(
    item: ApplyItemUiState,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var reviewNote by remember { mutableStateOf("欢迎！") }
    var aliasName by remember { mutableStateOf("") }

    val isFriend = item.apply.type == "FRIEND"

    AlertDialog(
        onDismissRequest = onDismiss,
        // 显式绑定 M3 核心调色板，剥离多余的 Tonal Elevation 颜色混合
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("同意申请") },
        text = {
            Column {
                OutlinedTextField(
                    value = reviewNote,
                    onValueChange = { reviewNote = it },
                    label = { Text("回复消息") },
                    modifier = Modifier.fillMaxWidth(),
                    // 统一输入框背景色，防止输入框泛白
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                // 仅添加好友时，可以选择设置备注
                if (isFriend) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aliasName,
                        onValueChange = { aliasName = it },
                        label = { Text("设置备注名 (选填)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reviewNote, if (isFriend) aliasName else null) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}