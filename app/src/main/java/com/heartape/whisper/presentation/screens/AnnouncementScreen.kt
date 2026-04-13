package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.component.WhisperBaseDialog
import com.heartape.whisper.component.WhisperDialogTextField
import com.heartape.whisper.presentation.viewmodel.AnnouncementViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementScreen(
    onBack: () -> Unit,
    viewModel: AnnouncementViewModel = hiltViewModel()
) {
    val announcements by viewModel.announcements.collectAsState()
    val membersMap by viewModel.membersMap.collectAsState()
    val canPublish by viewModel.canPublish.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showPublishDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("群公告") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    // 仅当用户是 OWNER 或 ADMIN 时，显示发布入口
                    if (canPublish) {
                        IconButton(onClick = { showPublishDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "发布公告")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && announcements.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (announcements.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("暂无群公告", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(announcements, key = { it.publishTime }) { ann ->
                        // 从成员字典中查找发布者信息
                        val publisher = membersMap[ann.userId]
                        val avatarUrl = publisher?.avatar ?: "https://picsum.photos/200"
                        val publisherName = publisher?.aliasName ?: publisher?.username ?: "管理员"

                        AnnouncementCard(
                            content = ann.content,
                            publisherName = publisherName,
                            avatarUrl = avatarUrl,
                            publishTime = ann.publishTime
                        )
                    }
                }
            }
        }

        // 发布公告的弹窗
        if (showPublishDialog) {
            PublishAnnouncementDialog(
                isLoading = isLoading,
                onDismiss = { showPublishDialog = false },
                onConfirm = { content ->
                    viewModel.publishAnnouncement(content) {
                        showPublishDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun AnnouncementCard(
    content: String,
    publisherName: String,
    avatarUrl: String,
    publishTime: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = publisherName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(publishTime))
                    Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PublishAnnouncementDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }

    WhisperBaseDialog(
        title = "发布新公告",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(content) },
        confirmText = "发布",
        isLoading = isLoading,
        confirmEnabled = content.isNotBlank()
    ) {
        WhisperDialogTextField(
            value = content,
            onValueChange = { content = it },
            label = "公告正文",
            singleLine = false,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
        )
    }
}