package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.data.model.UserSearchDto
import com.heartape.whisper.presentation.viewmodel.SearchApplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onBack: () -> Unit,
    viewModel: SearchApplyViewModel = hiltViewModel()
) {
    val results by viewModel.userResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var keyword by remember { mutableStateOf("") }

    // 申请弹窗状态
    var selectedUser by remember { mutableStateOf<UserSearchDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加朋友") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // 搜索框
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("请输入用户名进行搜索") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                trailingIcon = {
                    TextButton(onClick = { viewModel.searchUser(keyword) }) { Text("搜索") }
                }
            )

            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { user ->
                    UserResultItem(user) { selectedUser = user }
                }
            }
        }

        // 申请理由与备注对话框
        if (selectedUser != null) {
            ApplyFriendDialog(
                user = selectedUser!!,
                onDismiss = { selectedUser = null },
                onConfirm = { info, alias ->
                    viewModel.applyFriend(selectedUser!!.id, info, alias) {
                        selectedUser = null
                        onBack() // 发送申请后返回上页或提示
                    }
                }
            )
        }
    }
}

@Composable
fun UserResultItem(user: UserSearchDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatar, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("ID: ${user.id} · ${user.bio}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ApplyFriendDialog(user: UserSearchDto, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var applyInfo by remember { mutableStateOf("你好，我是...") }
    var aliasName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("申请添加朋友", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("向 ${user.username} 发送申请", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = applyInfo, onValueChange = { applyInfo = it },
                    label = { Text("申请消息") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = aliasName, onValueChange = { aliasName = it },
                    label = { Text("备注名 (选填)") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(applyInfo, aliasName) }) { Text("发送") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}