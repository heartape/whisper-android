package com.heartape.whisper.presentation.screens

import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.heartape.whisper.data.remote.WsStatus
import com.heartape.whisper.presentation.viewmodel.ProfileViewModel
import com.heartape.whisper.presentation.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*


sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Message : BottomNavItem("tab_message", "消息", Icons.AutoMirrored.Filled.Chat)
    object Profile : BottomNavItem("tab_profile", "我的", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onNavigateAddFriend: () -> Unit,
    onNavigateCreateGroup: () -> Unit,
    onNavigateJoinGroup: () -> Unit,
    onNavigateChat: (Long, String, String) -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigatePendingApplies: () -> Unit
) {
    // 为主页底部导航创建专属的（嵌套的）NavController
    val bottomNavController = rememberNavController()

    // 获取当前的路由状态，用于决定哪个 Tab 应该高亮
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(BottomNavItem.Message, BottomNavItem.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            // ★ 3. 官方推荐的底部导航标准跳转范式
                            bottomNavController.navigate(item.route) {
                                // 弹出到导航图的起始目的地，防止在切换 Tab 时后台栈无限堆积
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true // 保存之前 Tab 的状态（如滚动位置）
                                }
                                // 避免点击同一个 Tab 时创建多个实例
                                launchSingleTop = true
                                // 恢复之前保存的状态，实现切回 Tab 时界面的无缝衔接
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        // ★ 4. 使用嵌套的 NavHost 替代原本的 if-else 判定
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Message.route,
            modifier = Modifier.padding(padding).fillMaxSize(),
            // 为 Tab 切换添加非常柔和的淡入淡出特效，消除生硬感
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(BottomNavItem.Message.route) {
                // 依然保持解耦，不传递 NavController 给子组件
                SessionListScreen(
                    onNavigateChat = onNavigateChat,
                    onNavigateAddFriend = onNavigateAddFriend,
                    onNavigateCreateGroup = onNavigateCreateGroup,
                    onNavigateJoinGroup = onNavigateJoinGroup,
                    onNavigatePendingApplies = onNavigatePendingApplies
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateSettings = onNavigateSettings
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onNavigateChat: (Long, String, String) -> Unit,
    onNavigateAddFriend: () -> Unit,
    onNavigateCreateGroup: () -> Unit,
    onNavigateJoinGroup: () -> Unit,
    onNavigatePendingApplies: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    Log.d("SessionListScreen:sessions", sessions.toString())
    val wsStatus by viewModel.wsStatus.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("消息", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigatePendingApplies) {
                    Icon(Icons.Default.Notifications, contentDescription = "通知")
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                    // 更新为三个选项
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("添加朋友") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                            onClick = { showMenu = false; onNavigateAddFriend() }
                        )
                        DropdownMenuItem(
                            text = { Text("创建群聊") },
                            leadingIcon = { Icon(Icons.Default.GroupAdd, null) },
                            onClick = { showMenu = false; onNavigateCreateGroup() }
                        )
                        DropdownMenuItem(
                            text = { Text("加入群聊") },
                            leadingIcon = { Icon(Icons.Default.AddComment, null) },
                            onClick = { showMenu = false; onNavigateJoinGroup() }
                        )
                    }
                }
            }
        )

        if (wsStatus != WsStatus.CONNECTED) {
            Box(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val text = if (wsStatus == WsStatus.CONNECTING) "收取中..." else "网络连接断开"
                Text(text, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sessions, key = { it.id }) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateChat(session.id, session.type, session.name) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = session.icon,
                        contentDescription = "头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = session.name, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                        Spacer(Modifier.height(4.dp))
                        Text(text = session.lastMessage, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(text = if (session.lastTime > 0) sdf.format(Date(session.lastTime)) else "", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // 收集全局用户状态
    val user by viewModel.currentUser.collectAsState()

    Column(Modifier.fillMaxSize()) {
        // 保证标题绝对居中
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "我的",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onNavigateSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // 个人资料卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.extraLarge // 更现代的大圆角卡片
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ★ 3. 真实头像渲染
                AsyncImage(
                    model = user.avatar,
                    contentDescription = "My Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // ★ 4. 真实昵称渲染（优先展示 nickname，降级展示 username）
                    val displayName = user.username
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // ★ 5. 真实 Whisper ID
                    Text(
                        text = "ID: ${user.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(8.dp))

                    // ★ 6. 真实签名渲染
                    val bioText = user.bio.ifBlank { "暂无签名" }
                    Text(
                        text = "签名：$bioText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}