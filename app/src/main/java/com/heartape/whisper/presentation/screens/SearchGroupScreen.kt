package com.heartape.whisper.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.data.model.SessionDto
import com.heartape.whisper.presentation.viewmodel.SearchApplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchGroupScreen(
    onBack: () -> Unit,
    viewModel: SearchApplyViewModel = hiltViewModel()
) {
    val results by viewModel.groupResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var keyword by remember { mutableStateOf("") }

    var selectedGroup by remember { mutableStateOf<SessionDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索群聊") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("请输入群聊名称") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                trailingIcon = {
                    TextButton(onClick = { viewModel.searchGroup(keyword) }) { Text("搜索") }
                }
            )

            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { group ->
                    GroupResultItem(group) { selectedGroup = group }
                }
            }
        }

        // 入群申请对话框
        if (selectedGroup != null) {
            ApplyGroupDialog(
                group = selectedGroup!!,
                onDismiss = { selectedGroup = null },
                onConfirm = { info ->
                    viewModel.applyGroup(selectedGroup!!.id, info) {
                        selectedGroup = null
                        onBack()
                    }
                }
            )
        }
    }
}

@Composable
fun GroupResultItem(group: SessionDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = group.icon, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant) // 群聊头像建议用圆角矩形
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(group.name ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("ID: ${group.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ApplyGroupDialog(group: SessionDto, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var applyInfo by remember { mutableStateOf("请求加入群聊") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("申请加入群聊") },
        text = {
            Column {
                Text("申请加入：${group.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = applyInfo,
                    onValueChange = { applyInfo = it },
                    label = { Text("验证消息") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(applyInfo) }) { Text("发送") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}