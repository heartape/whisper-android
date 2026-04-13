package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.data.local.MemberEntity
import com.heartape.whisper.presentation.viewmodel.SessionSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSettingsScreen(
    onBack: () -> Unit,
    onNavigateAnnouncement: () -> Unit = {},
    viewModel: SessionSettingsViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val members by viewModel.members.collectAsState()
    val exitSuccess by viewModel.exitSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exitSuccess) { if (exitSuccess) onBack() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 状态判定
    val isGroup = session?.type == "GROUP"
    val currentUserId = viewModel.currentUserId
    val myMember = members.find { it.userId == currentUserId }
    val myRole = myMember?.role ?: "MEMBER"
    val peerMember = if (!isGroup) members.find { it.userId != currentUserId } else null

    // 弹窗状态
    var showAliasDialog by remember { mutableStateOf(false) }
    var selectedMemberForManage by remember { mutableStateOf<MemberEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天信息") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ================= 成员头像网格 =================
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
            ) {
                // 私聊排查自己，群聊展示所有人
                val displayMembers = if (isGroup) members else members.filter { it.userId != currentUserId }

                items(displayMembers) { member ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            // ★ 群聊管理权限判定：自己必须是 OWNER 或 ADMIN，且不能操作自己，且不能越级操作
                            if (isGroup && member.userId != currentUserId) {
                                if (myRole == "OWNER" || (myRole == "ADMIN" && member.role == "MEMBER")) {
                                    selectedMemberForManage = member
                                }
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = member.avatar, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            // 群主或管理员角标标识
                            if (isGroup && (member.role == "OWNER" || member.role == "ADMIN")) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) {
                                    Text(if (member.role == "OWNER") "群" else "管", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.wrapContentSize())
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val name = member.aliasName?.ifBlank { member.username } ?: member.username
                        Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            HorizontalDivider()

            // ================= 动态功能列表 =================

            // 群聊名称
            if (isGroup) {
                ListItem(headlineContent = { Text("群聊名称") }, trailingContent = { Text(session?.name ?: "") })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                // 群公告
                ListItem(
                    headlineContent = { Text("群公告") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { onNavigateAnnouncement() },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }

            // 备注功能 (私聊给对方备注，群聊给自己备注)
            ListItem(
                headlineContent = { Text(if (isGroup) "我在本群的昵称" else "设置备注") },
                trailingContent = {
                    Row {
                        val currentAlias = if (isGroup) myMember?.aliasName else peerMember?.aliasName
                        Text(currentAlias ?: "未设置", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                },
                modifier = Modifier.clickable { showAliasDialog = true }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // 私聊拉黑功能
            if (!isGroup && peerMember != null) {
                ListItem(
                    headlineContent = { Text("加入黑名单") },
                    trailingContent = {
                        Switch(
                            checked = peerMember.isBlock,
                            onCheckedChange = { isBlocked -> viewModel.toggleBlockPeer(peerMember.userId, isBlocked) }
                        )
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            // 删除/退出按钮
            Button(
                onClick = { viewModel.exitSession() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (isGroup) "退出群聊" else "删除好友", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }

        // ================= 弹窗组件 =================

        // 备注弹窗
        if (showAliasDialog) {
            var aliasInput by remember { mutableStateOf(if (isGroup) (myMember?.aliasName ?: "") else (peerMember?.aliasName ?: "")) }

            AlertDialog(
                onDismissRequest = { showAliasDialog = false },
                title = { Text(if (isGroup) "设置我的群昵称" else "设置好友备注") },
                text = {
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.setAlias(aliasInput)
                        showAliasDialog = false
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showAliasDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 群管理底层弹窗 (操作其他群成员)
        if (selectedMemberForManage != null) {
            val target = selectedMemberForManage!!
            ModalBottomSheet(onDismissRequest = { selectedMemberForManage = null }) {
                Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(
                        text = "管理成员: ${target.username}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()

                    // 仅群主可以设置/取消管理员
                    if (myRole == "OWNER") {
                        if (target.role == "MEMBER") {
                            ListItem(headlineContent = { Text("设为管理员") }, modifier = Modifier.clickable {
                                viewModel.manageGroupMember(target.userId, "SET_ADMIN"); selectedMemberForManage = null
                            })
                        } else if (target.role == "ADMIN") {
                            ListItem(headlineContent = { Text("取消管理员") }, modifier = Modifier.clickable {
                                viewModel.manageGroupMember(target.userId, "REMOVE_ADMIN"); selectedMemberForManage = null
                            })
                        }
                    }

                    // 通用管理动作
                    ListItem(headlineContent = { Text("移出群聊") }, modifier = Modifier.clickable {
                        viewModel.manageGroupMember(target.userId, "KICK"); selectedMemberForManage = null
                    })
                    ListItem(headlineContent = { Text("移出群聊并拉黑", color = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable {
                        viewModel.manageGroupMember(target.userId, "BLOCK_AND_KICK"); selectedMemberForManage = null
                    })
                }
            }
        }
    }
}